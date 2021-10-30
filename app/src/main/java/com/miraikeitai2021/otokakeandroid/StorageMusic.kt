package com.miraikeitai2021.otokakeandroid

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.*

class StorageMusic {
    /**
     * ストレージに曲データを保存するとき，androidのAPIによって処理を分ける関数
     * まずここが呼ばれる
     * context: 呼び出し元ActivityのContext
     * inputStream: 保存したい音楽データのInputStream
     * backendId: 保存したい音楽ファイルの一意なバックエンドId
     */
    fun storageInMusic(context: Context, inputStream: InputStream, backendId:Int){
        //保存用
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P){
            //APIレベル28以前の機種の場合の処理
            storageInMusicLessAPI28(context, inputStream, backendId)
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            //APIレベル29以降の機種の場合の処理
            storageInMusicMoreAPI29(context, inputStream)
        }
    }

    /**
     * ストレージに曲データを保存する関数(端末のAPIが29以上の時)
     * context: 呼び出し元ActivityのContext
     * inputStream: 保存したい音楽データのinputStream
     */
    @RequiresApi(Build.VERSION_CODES.Q)

    private fun storageInMusicMoreAPI29(context: Context, inputStream: InputStream){

        val values= ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "natutanken.mp3") //保存する曲ファイルの名前を入力
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        //「content://media/external/audio/media/」までを作製
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        //「content://media/external/audio/media/ストレージId」を作製
        val destination = context.contentResolver.insert(collection, values)!!

        var outputStream: OutputStream? = null

        try {
            outputStream = destination.let { context.contentResolver.openOutputStream(it) } ?: error("保存メディアファイルを開けない")
            //inputStreamの音楽データをoutputStream(MediaStore)にコピー
            inputStream.copyTo(outputStream)
        } catch (e: FileNotFoundException) {
            throw IllegalStateException(e)
        } catch (e: IOException) {
            throw e
        } finally {
            inputStream.close()
            outputStream?.close()
        }
        destination.let {
            context.contentResolver.update(it, ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, false)
            }, null, null)
        }

    }

    /**
     * ストレージに曲データを保存する関数(端末のAPIが28以下の時)
     * context: 呼び出し元ActivityのContext
     * inputStream: 保存したい音楽データのinputStream
     * backendId: 保存したい音楽ファイルの一意なバックエンドId
     */
    private fun storageInMusicLessAPI28(context: Context, inputStream: InputStream, backendId: Int){
        //文字列パスを作製
        val fileName = "music" + backendId.toString() + ".mp3"
        val path = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC).toString()
        val file = path + "/" + fileName

        //外部ストレージに直接inputStreamの音楽データをコピー
        val state = Environment.getExternalStorageState()
        if(Environment.MEDIA_MOUNTED == state) {
            val fileOutputStream = FileOutputStream(file, true)

            while (true) {
                val data = inputStream.read()
                if (data == -1) {
                    break
                }
                fileOutputStream.write(data)
            }
            inputStream.close()
            fileOutputStream.close()
        }

        val values = ContentValues().apply{
            put(MediaStore.Audio.Media.DISPLAY_NAME, "tecpoter")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp3")
            put(MediaStore.Audio.Media.DATA,file)
        }

        //外部ストレージにある音楽ファイルの直接的なUriをMediaStore用のUriに変換
        context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
    }
}