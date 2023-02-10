package com.gknot

import java.util.ArrayList
import java.util.HashMap

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Handler
import android.os.Message
import android.util.Log

/**
 * Helper to register for and send broadcasts of Intents to local objects
 * within your process.  This is has a number of advantages over sending
 * global broadcasts with [android.content.Context.sendBroadcast]:
 *
 *  *  You know that the data you are broadcasting won't leave your app, so
 * don't need to worry about leaking private data.
 *  *  It is not possible for other applications to send these broadcasts to
 * your app, so you don't need to worry about having security holes they can
 * exploit.
 *  *  It is more efficient than sending a global broadcast through the
 * system.
 *
 */
class LocalBroadcastManager private constructor(private val mAppContext: Context) {

    private val mReceivers = HashMap<BroadcastReceiver, ArrayList<IntentFilter>>()
    private val mActions = HashMap<String, ArrayList<ReceiverRecord>>()

    private val mPendingBroadcasts = ArrayList<BroadcastRecord>()

    private val mHandler: Handler

    private class ReceiverRecord internal constructor(internal val filter: IntentFilter, internal val receiver: BroadcastReceiver) {
        internal var broadcasting: Boolean = false

        override fun toString(): String {
            val builder = StringBuilder(128)
            builder.append("Receiver{")
            builder.append(receiver)
            builder.append(" filter=")
            builder.append(filter)
            builder.append("}")
            return builder.toString()
        }
    }

    private class BroadcastRecord internal constructor(internal val intent: Intent, internal val receivers: ArrayList<ReceiverRecord>)

    init {
        mHandler = object : Handler(mAppContext.mainLooper) {

            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    MSG_EXEC_PENDING_BROADCASTS -> executePendingBroadcasts()
                    else -> super.handleMessage(msg)
                }
            }
        }
    }

    /**
     * Register a receive for any local broadcasts that match the given IntentFilter.
     *
     * @param receiver The BroadcastReceiver to handle the broadcast.
     * @param filter Selects the Intent broadcasts to be received.
     *
     * @see .unregisterReceiver
     */
    fun registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter) {
        synchronized(mReceivers) {
            val entry = ReceiverRecord(filter, receiver)
            var filters = mReceivers[receiver]
            if (filters == null) {
                filters = ArrayList(1)
                mReceivers[receiver] = filters
            }
            filters.add(filter)
            for (i in 0 until filter.countActions()) {
                val action = filter.getAction(i)
                var entries = mActions[action]
                if (entries == null) {
                    entries = ArrayList(1)
                    mActions[action] = entries
                }
                entries.add(entry)
            }
        }
    }

    /**
     * Unregister a previously registered BroadcastReceiver.  *All*
     * filters that have been registered for this BroadcastReceiver will be
     * removed.
     *
     * @param receiver The BroadcastReceiver to unregister.
     *
     * @see .registerReceiver
     */
    fun unregisterReceiver(receiver: BroadcastReceiver) {
        synchronized(mReceivers) {
            val filters = mReceivers.remove(receiver) ?: return
            for (i in filters.indices) {
                val filter = filters[i]
                for (j in 0 until filter.countActions()) {
                    val action = filter.getAction(j)
                    val receivers = mActions[action]
                    if (receivers != null) {
                        var k = 0
                        while (k < receivers.size) {
                            if (receivers[k].receiver === receiver) {
                                receivers.removeAt(k)
                                k--
                            }
                            k++
                        }
                        if (receivers.size <= 0) {
                            mActions.remove(action)
                        }
                    }
                }
            }
        }
    }

    /**
     * Broadcast the given intent to all interested BroadcastReceivers.  This
     * call is asynchronous; it returns immediately, and you will continue
     * executing while the receivers are run.
     *
     * @param intent The Intent to broadcast; all receivers matching this
     * Intent will receive the broadcast.
     *
     * @see .registerReceiver
     */
    fun sendBroadcast(intent: Intent): Boolean {
        synchronized(mReceivers) {

            val type = intent.resolveTypeIfNeeded(
                    mAppContext.contentResolver)
            val data = intent.data
            val scheme = intent.scheme
            val categories = intent.categories

            val debug = DEBUG || intent.flags and Intent.FLAG_DEBUG_LOG_RESOLUTION != 0
            if (debug)
                Log.v(
                        TAG, "Resolving type " + type + " scheme " + scheme
                        + " of intent " + intent)
            val action = intent.action
            val entries =   if (action == null) null else mActions[action]
            if (entries != null) {
                if (debug) Log.v(TAG, "Action list: $entries")

                var receivers: ArrayList<ReceiverRecord>? = null
                for (i in entries.indices) {
                    val receiver = entries[i]
                    if (debug) Log.v(TAG, "Matching against filter " + receiver.filter)

                    if (receiver.broadcasting) {
                        if (debug) {
                            Log.v(TAG, "  Filter's target already added")
                        }
                        continue
                    }

                    val match = receiver.filter.match(action, type, scheme, data,
                            categories, "LocalBroadcastManager")
                    if (match >= 0) {
                        if (debug)
                            Log.v(TAG, "  Filter matched!  match=0x" + Integer.toHexString(match))
                        if (receivers == null) {
                            receivers = ArrayList()
                        }
                        receivers.add(receiver)
                        receiver.broadcasting = true
                    } else {
                        if (debug) {
                            val reason: String
                            when (match) {
                                IntentFilter.NO_MATCH_ACTION -> reason = "action"
                                IntentFilter.NO_MATCH_CATEGORY -> reason = "category"
                                IntentFilter.NO_MATCH_DATA -> reason = "data"
                                IntentFilter.NO_MATCH_TYPE -> reason = "type"
                                else -> reason = "unknown reason"
                            }
                            Log.v(TAG, "  Filter did not match: $reason")
                        }
                    }
                }

                if (receivers != null) {
                    for (i in receivers.indices) {
                        receivers[i].broadcasting = false
                    }
                    mPendingBroadcasts.add(BroadcastRecord(intent, receivers))
                    if (!mHandler.hasMessages(MSG_EXEC_PENDING_BROADCASTS)) {
                        mHandler.sendEmptyMessage(MSG_EXEC_PENDING_BROADCASTS)
                    }
                    return true
                }
            }
        }
        return false
    }

    /**
     * Like [.sendBroadcast], but if there are any receivers for
     * the Intent this function will block and immediately dispatch them before
     * returning.
     */
    fun sendBroadcastSync(intent: Intent) {
        if (sendBroadcast(intent)) {
            executePendingBroadcasts()
        }
    }

    private fun executePendingBroadcasts() {
        while (true) {
            var brs: Array<BroadcastRecord?>? = null
            synchronized(mReceivers) {
                val N:Int = mPendingBroadcasts.size
                if (N <= 0) {
                    return
                }

                brs = arrayOfNulls(N)
                mPendingBroadcasts.toTypedArray()
                mPendingBroadcasts.clear()
            }
            for (i in brs!!.indices) {
                val br = brs!![i]
                for (j in br!!.receivers.indices) {
                    br.receivers[j].receiver.onReceive(mAppContext, br.intent)
                }
            }
        }
    }

    companion object {

        private val TAG = "LocalBroadcastManager"
        private val DEBUG = false

        internal val MSG_EXEC_PENDING_BROADCASTS = 1

        private val mLock = Any()
        private var mInstance: LocalBroadcastManager? = null

        fun getInstance(context: Context): LocalBroadcastManager {
            synchronized(mLock) {
                val _mInstance: LocalBroadcastManager =
                    if (mInstance == null)
                        LocalBroadcastManager(context.applicationContext)
                    else mInstance as LocalBroadcastManager
                return _mInstance;
            }
        }
    }
}