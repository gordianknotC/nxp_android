package com.gknot.utils

import android.app.Activity
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NdefRecord.TNF_MIME_MEDIA
import com.gknot.MainActivity
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*


class NdefUtil{
   companion object {
      private val default_uri:String =
              "http://www.nxp.com/products/identification_and_security/" +
              "smart_label_and_tag_ics/ntag/series/NT3H1101_NT3H1201.html"

      @JvmStatic fun creatNdefDefaultMessage(packageName: String): NdefMessage {
         val uri_record = NdefRecord.createUri(default_uri)
         val text = "NTAG I2C Demoboard LPC812"
         val lang = "en"
         val textBytes = text.toByteArray()
         val langBytes = lang.toByteArray(StandardCharsets.US_ASCII)
         val langLength = langBytes.size
         val textLength = textBytes.size
         val payload = ByteArray(1 + langLength + textLength)
         payload[0] = langLength.toByte()
         System.arraycopy(langBytes, 0, payload, 1, langLength)
         System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength)
         val textRecord = NdefRecord(
                 NdefRecord.TNF_WELL_KNOWN,
                 NdefRecord.RTD_TEXT, ByteArray(0), payload)
         val spRecords = arrayOf(uri_record, textRecord)
         val spMessage = NdefMessage(spRecords)
         val sp_record = NdefRecord(
                 NdefRecord.TNF_WELL_KNOWN,
                 NdefRecord.RTD_SMART_POSTER, ByteArray(0),
                 spMessage.toByteArray())
         val aap_record = NdefRecord.createApplicationRecord(packageName)
         val records = arrayOf(sp_record, aap_record)
         return NdefMessage(records)
      }

      @JvmStatic fun creatNdefDefaultMessage(main: Activity): NdefMessage {
         return creatNdefDefaultMessage(main.packageName)
      }

      @JvmStatic fun createTextRecord(lanCode:String?, text:String?): NdefRecord {
         if (text == null) throw NullPointerException("text is null")
         val textBytes = text.toByteArray(StandardCharsets.UTF_8)
         var langCodeByte: ByteArray? = null
         if (lanCode != null && !lanCode.isEmpty()) {
            langCodeByte = lanCode.toByteArray(StandardCharsets.US_ASCII)
         } else {
            // Locale.TAIWAN.language.toString()
            langCodeByte = Locale.getDefault().language.toByteArray(StandardCharsets.US_ASCII)
         }
         // We only have 6 bits to indicate ISO/IANA language code.
         if (langCodeByte.size >= 64) {
            throw IllegalArgumentException("language code is too long, must be <64 bytes.")
         }
         val buffer = ByteBuffer.allocate(1 + langCodeByte.size + textBytes.size)

         val status = (langCodeByte.size and 0xFF).toByte()
         buffer.put(status)
         buffer.put(langCodeByte)
         buffer.put(textBytes)

         return NdefRecord(
                 NdefRecord.TNF_WELL_KNOWN,
                 NdefRecord.RTD_TEXT,
                 null, buffer.array())
      }

      @JvmStatic fun createMime(mimeType: String?, mimeData: ByteArray): NdefRecord {
         if (mimeType == null) throw NullPointerException("mimeType is null")
         var mime = mimeType;
         // We only do basic MIME type validation: trying to follow the
         // RFCs strictly only ends in tears, since there are lots of MIME
         // types in common use that are not strictly valid as per RFC rules
         mime = Intent.normalizeMimeType(mime)
         if (mime.length == 0) throw IllegalArgumentException("mimeType is empty")
         val slashIndex = mime.indexOf('/')
         if (slashIndex == 0) throw IllegalArgumentException("mimeType must have major type")
         if (slashIndex == mime.length - 1) {
            throw IllegalArgumentException("mimeType must have minor type")
         }
         // missing '/' is allowed
         // MIME RFCs suggest ASCII encoding for content-type
         val typeBytes = mime.toByteArray(StandardCharsets.US_ASCII)
         return NdefRecord(TNF_MIME_MEDIA, typeBytes, null, mimeData)
      }

      @JvmStatic @Throws(UnsupportedEncodingException::class)
      fun createNdefTextMessage(langCode: String?, text: String): NdefMessage? {
         if (text.length == 0) {
            return null
         }
         val records = arrayOf(createTextRecord(langCode, text))
         return NdefMessage(records)
      }

      @JvmStatic @Throws(UnsupportedEncodingException::class)
      fun createNdefMessage(text: String, AAR: Boolean = false): NdefMessage {
         val lang = "en"
         val textBytes = text.toByteArray()
         val langBytes = lang.toByteArray(StandardCharsets.US_ASCII)
         val langLength = langBytes.size
         val textLength = textBytes.size
         val payload = ByteArray(1 + langLength + textLength)
         payload[0] = langLength.toByte()
         System.arraycopy(langBytes, 0, payload, 1, langLength)
         System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength)

         val record = NdefRecord(
                 NdefRecord.TNF_WELL_KNOWN,
                 NdefRecord.RTD_TEXT, ByteArray(0), payload)

         if (AAR){
            val aar = NdefRecord.createApplicationRecord(MainActivity.instance?.packageName);
            return NdefMessage(arrayOf(record, aar))
         }else {
            val records = arrayOf(record)
            return NdefMessage(records)
         }
      }
   }
}