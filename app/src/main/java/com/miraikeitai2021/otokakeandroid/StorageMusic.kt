package com.miraikeitai2021.otokakeandroid

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class StorageMusic {
    /**
     * ストレージに曲データを保存するとき，androidのAPIによって処理を分ける関数
     * まずここが呼ばれる
     * context: 呼び出し元ActivityのContext
     * inputStream: 保存したい音楽データのInputStream
     * backendId: 保存したい音楽ファイルの一意なバックエンドId
     * musicFileName: 保存したい音楽ファイルの名前
     */
    fun storageInMusic(context: Context, inputStream: InputStream, backendId:Int){

        //APIレベル28以前の機種の場合の処理
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS")
        val date = Date(System.currentTimeMillis())
        val timeStamp = dateFormat.format(date)
        val musicFileName = "otokake_" + backendId.toString() + "_" + timeStamp + ".mp3"

        //保存用
        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P){
            //APIレベル28以前の機種の場合の処理
            storageInMusicLessAPI28(context, inputStream, musicFileName)
        }else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            //APIレベル29以降の機種の場合の処理
            storageInMusicMoreAPI29(context, inputStream, musicFileName)
        }
    }

    /**
     * ストレージに曲データを保存する関数(端末のAPIが29以上の時)
     * context: 呼び出し元ActivityのContext
     * inputStream: 保存したい音楽データのinputStream
     * musicFileName: 保存したい音楽ファイルの名前
     */
    @RequiresApi(Build.VERSION_CODES.Q)

    private fun storageInMusicMoreAPI29(context: Context, inputStream: InputStream, musicFileName:String){

        val values= ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, musicFileName) //保存する曲ファイルの名前を入力
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        //「content://media/external/audio/media/」までを作製
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        //「content://media/external/audio/media/ストレージId」を作製
        val destination = context.contentResolver.insert(collection, values)!!

        var outputStream: OutputStream? = null

        try {
            //コピー先であるoutputStreamにMediaStoreの情報を格納
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
     * musicFileName: 保存したい音楽ファイルの名前
     */
    private fun storageInMusicLessAPI28(context: Context, inputStream: InputStream, musicFileName: String){
        //文字列パスを作製
        val path = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC).toString()
        val file = "$path/$musicFileName"

        //外部ストレージに直接inputStreamの音楽データをコピー
        val state = Environment.getExternalStorageState()
        if(Environment.MEDIA_MOUNTED == state) {
            val fileOutputStream = FileOutputStream(file, true)

            var musicByteArray = inputStream.readBytes()
            fileOutputStream.write(musicByteArray)
            inputStream.close()
            fileOutputStream.close()
        }

        val values = ContentValues().apply{
            put(MediaStore.Audio.Media.DISPLAY_NAME, musicFileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp3")
            put(MediaStore.Audio.Media.DATA,file)
        }

        //外部ストレージにある音楽ファイルの直接的なUriをMediaStore用のUriに変換
        context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
    }

    fun checkStorageId(context: Context): Long{
        //↓ID検索用

        //projection: 欲しい情報を定義
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
        )

        //上のprojectionとselectionを利用した問い合わせ変数を作製
        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, //外部ストレージ
            projection,
            null,
            null, // selectionArgs,
            null
        )

        var id: Long = -1

        cursor?.use{
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)

            while (cursor.moveToNext()) {
                id = cursor.getLong(idColumn)
            }
        }
        return id
    }

    /**
     * 音楽ファイルのメタデータからジャケット画像を取得するメソッド
     * storageId: 取得先の音楽ファイルのストレージId
     * context: 呼び出し元ActivityのContext
     */
    fun getImage(storageId: Long, context: Context): Bitmap? {

        val mmr = MediaMetadataRetriever()
        val checkMusicUri = CheckMusicUri()

        //ストレージId指定の音楽のメタデータを取得
        mmr.setDataSource(context, checkMusicUri.checkUri(storageId.toInt(), context.contentResolver))

        //ジャケット画像をByte型で取得
        val byte: ByteArray? = mmr.embeddedPicture

        var albumArt: Bitmap? = null

        //Byte型をBitmap型に変換
        if(byte != null){
            albumArt = BitmapFactory.decodeByteArray(byte, 0, byte.size)
        }

        return albumArt
    }
}