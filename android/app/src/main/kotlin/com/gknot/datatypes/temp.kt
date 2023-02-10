package com.gknot.datatypes

import android.os.Parcel
import android.os.Parcelable
import com.gknot.pseudo.ParcelableMap


class Album :  ParcelableMap {
    var userId: Int = 0
        private set
    var id: Int = 0
        private set
    var title: String = ""
        private set

    constructor(userId: Int, id: Int, title: String) {
        this.userId = userId
        this.id = id
        this.title = title
    }
    override fun toMap(): Map<String, Any>{
        return mapOf(
                "userId" to(userId),
                "id" to(id),
                "title" to(title)
        )
    }
    /* 以上都跟原來一樣 */

    /* 以下為新增的Parcelable部分 */
    // 讀取參數，參數順序要和建構子一樣
    protected constructor(`in`: Parcel) {
        userId = `in`.readInt()
        id = `in`.readInt()
        title = `in`.readString() as String
    }

    override fun describeContents(): Int {
        return 0
    }

    // 寫入參數，參數順序要和建構子一樣
    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(userId)
        parcel.writeInt(id)
        parcel.writeString(title)
    }

    companion object {
        @JvmField val CREATOR: Parcelable.Creator<Album> = object : Parcelable.Creator<Album> {
            override fun createFromParcel(`in`: Parcel): Album {
                return Album(`in`)
            }

            override fun newArray(size: Int): Array<Album?> {
                return arrayOfNulls(size)
            }
        }
    }
}


/*
🎵 Musical Note
🗒️ Spiral Notepad
🎶 Musical Notes
💷 Pound Banknote
📝 Memo
💴 Yen Banknote
📓 Notebook
💵 Dollar Banknote
💌 Love Letter
💶 Euro Banknote
📌 Pushpin
👨‍🎤 Man Singer
👩‍🎤 Woman Singer
👛 Purse
💼 Briefcase
🎤 Microphone
🎅 Santa Claus
😗 Kissing Face

📘 Blue Book
📙 Orange Book
📕 Closed Book
📖 Open Book

🖊️ Pen
✒️ Black Nib
✍️ Writing Hand
🏴󠁴󠁷󠁰󠁥󠁮󠁿 Flag for Penghu (TW-PEN)

⛳ Flag in Hole
🏁 Chequered Flag
🏳️ White Flag
🏴 Black Flag
*/