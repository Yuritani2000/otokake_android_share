package com.miraikeitai2021.otokakeandroid

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast

class CheckMusicUri {

    /**
     * ストレージからUriを得るメソッド
     * context：Activityのcontext
     */
    fun checkUri(context: Context, musicId: Int): Uri {

        //projection: 欲しい情報を定義
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME
        )

        //selection: filterでAudioFileのみのIDを得るように定義
        val selection = MediaStore.Audio.Media._ID + "=" + musicId.toString()

        //上のprojectionとselectionを利用した問い合わせ変数を作製
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, //外部ストレージ
            projection,
            selection,  // selection,
            null, // selectionArgs,
            null
        )

        lateinit var contentUri: Uri

        cursor?.use{
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(displayNameColumn)
                contentUri = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                Toast.makeText(context,
                    "id: $id, displayName: $displayName, contentUir: $contentUri", Toast.LENGTH_LONG).show()
            }
        }
        return contentUri
    }
}