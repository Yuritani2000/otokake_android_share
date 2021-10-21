package com.miraikeitai2021.otokakeandroid

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast

class CheckMusicUri {

    /**
     * ストレージにある曲のUriを取得するメソッド
     * context: 呼び出し元ActivityのContext
     * id: ストレージにある曲のid
     */
    fun checkUri(context: Context, id: Int, contentResolver: ContentResolver): Uri {

        //projection: 欲しい情報を定義
        val projection = arrayOf(
            MediaStore.Audio.Media._ID
        )

        //selection: filterで指定したIDの音楽ファイルのみURIを得るように定義
        val selection = MediaStore.Audio.Media._ID + "=" + id


        //上のprojectionとselectionを利用した問い合わせ変数を作製
        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, //外部ストレージ
            projection,
            selection,  // selection,
            null, // selectionArgs,
            null
        )

        var contentUri: Uri = Uri.parse("null")

        cursor?.use{
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)

            while (cursor.moveToNext()) {
                val sid = cursor.getLong(idColumn)

                contentUri = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    sid.toString()
                )
            }
        }

        return contentUri
    }
}