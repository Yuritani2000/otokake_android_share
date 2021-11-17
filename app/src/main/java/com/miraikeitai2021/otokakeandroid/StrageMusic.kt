package com.miraikeitai2021.otokakeandroid

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.FileOutputStream

class StrageMusic {
    /**
     * ストレージに曲データを保存する関数
     * context: MainActivity（呼び出し元Activity)のContext
     */

    @RequiresApi(Build.VERSION_CODES.Q)

    fun StrageInMusic(context: Context){

        val values= ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "natutanken.mp3") //保存する曲ファイルの名前を入力
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val item = context.contentResolver.insert(collection, values)!!

        context.contentResolver.openFileDescriptor(item, "w", null).use{
            FileOutputStream(it!!.fileDescriptor).use{ outputStream ->
                val audioInputStream = context.resources.openRawResource(R.raw.natsuyasuminotanken) //保存する曲ファイルをローカルフォルダから指定
                while(true){
                    val data = audioInputStream.read()
                    if(data == -1){
                        break
                    }
                    outputStream.write(data)
                }
                audioInputStream.close()
                outputStream.close()
            }
        }

        values.clear()
        values.put(MediaStore.Audio.Media.IS_PENDING, 0)
        context.contentResolver.update(item, values, null, null)
    }



}