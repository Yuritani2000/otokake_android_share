package com.miraikeitai2021.otokakeandroid


import android.os.*
import android.util.Log
import com.github.kittinunf.fuel.core.requests.CancellableRequest
import com.github.kittinunf.fuel.httpDownload
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream

// HTTP通信を行うスレッドとUIスレッドとの間で値の受け渡しを行うHandlerで使用する識別コード．
const val HANDLE_MUSIC_DOWNLOAD_PROGRESS_UPDATED = 200
const val HANDLE_GET_MUSIC_LIST_FAILED = 201
const val HANDLE_GET_MUSIC_LIST_SUCCESS = 202
const val HANDLE_MUSIC_DOWNLOAD_FAILED = 203
const val HANDLE_MUSIC_DOWNLOAD_SUCCESS = 204

/**
 * 受け取ったJSON文字列をオブジェクトに起こし，プログラム内で扱う際の型定義．
 * JSONの内容はnullになり得るため，プロパティはすべてnullableである．
 * この中の値を使用する際はnullチェックを行うこと．
 */
@Serializable
@SerialName("MusicInfo")
data class MusicInfo(
    @SerialName("musicID")val musicID: Int?,
    @SerialName("musicName")val musicName: String?,
    @SerialName("musicArtist")val musicArtist: String?,
    @SerialName("musicURL")val musicURL: String?
)

/**
 * 曲一覧の取得，曲のダウンロードに関するHTTPリクエストをまとめたもの．
 */
class MusicHttpRequests(){
    /**
     *  曲一覧の取得先となるURL.サーバが本番のものになり次第変更する必要がある．
     */
    private val url = "http://172.20.10.3:8080/getMusicInfoAll"

    /**曲がダウンロードされている時は，この中にCancellableRequestのオブジェクトが入る．*
      */
    private var musicDownloadingRequest: CancellableRequest? = null

    /**
     * 曲一覧の取得を行うメソッド．handlerのMessage経由で曲一覧を取得する．
     */
    fun requestGetMusicList(handler: GetMusicListHandler){
        val httpAsync = url.httpGet().responseString { _, _, result ->
            when (result) {
                // HTTPリクエストが失敗した場合
                is Result.Failure -> {
                    val ex = result.getException()
                    Log.e("debug", ex.toString())
                    // このスレッドはHTTP通信を行うスレッドなので，UI(メイン)スレッドに値を渡す．
                    // 以下の同じメソッドでも同じことをしている．
                    handler.obtainMessage(HANDLE_GET_MUSIC_LIST_FAILED, ex)
                        .sendToTarget()
                }
                // HTTPリクエストが成功した場合
                is Result.Success -> {
                    // HTTPリクエストの結果からJSONの文字列を取り出す．
                    val resultStr = result.value
                    Log.d("debug", "resultStr: $resultStr")
                    try {
                        // JSON文字列からMusicInfoオブジェクトのListに変換
                        val musicList = Json.decodeFromString<List<MusicInfo>>(resultStr)
                        Log.d("debug", "result converted to object: $musicList")
                        handler.obtainMessage(HANDLE_GET_MUSIC_LIST_SUCCESS, musicList)
                            .sendToTarget()
                    }catch(ex: Exception){
                        Log.e("debug" , ex.toString())
                        handler.obtainMessage(HANDLE_GET_MUSIC_LIST_FAILED, ex)
                            .sendToTarget()
                    }
                }
            }
        }
        httpAsync.join()
    }

    /**
     * 与えられたURLから曲のダウンロードを行うメソッド．
     * HandlerのMessage経由で，ダウンロードした曲のInputStreamを取得する．
     */
    fun requestDownloadMusic(
        url: String,
        handler: MusicDownloadHandler)
    {
        musicDownloadingRequest = url.httpDownload().fileDestination { _, _ ->
            File.createTempFile("temp", ".tmp")
        }.progress{ readBytes, totalBytes ->
            // ダウンロードするデータの総量と，ダウンロードしたデータ量の割合を求める．
            val progress = readBytes.toFloat() / totalBytes.toFloat()
            // 割合を百分率にしてInt型に変換したものをUIスレッドに送る．
            handler.obtainMessage(HANDLE_MUSIC_DOWNLOAD_PROGRESS_UPDATED, (progress * 100).toInt(), -1)
                .sendToTarget()
        }.response{ _, _, result ->
            when(result){
                // HTTPリクエストが失敗した場合
                is Result.Failure -> {
                    val ex = result.getException()
                    Log.e("debug", ex.toString())
                    handler.obtainMessage(HANDLE_MUSIC_DOWNLOAD_FAILED, ex)
                        .sendToTarget()
                }
                // HTTPリクエストが成功した場合
                is Result.Success -> {
                    // HTTPリクエストの結果に入っているバイト配列をInputStreamに変換．これが曲データとなる．
                    val inputStream = result.value.inputStream()
                    Log.d("debug", "downloaded data is: $inputStream")
                    handler.obtainMessage(HANDLE_MUSIC_DOWNLOAD_SUCCESS, inputStream)
                        .sendToTarget()
                }
            }
        }
    }

    /**
     * 曲のダウンロードをキャンセルするためのメソッド．
     * 曲のダウンロードが行なわれていない時は何もしない．
     */
    fun cancelDownloadingMusic(){
        Log.d("debug", "cancel musicDownloadingRequest: $musicDownloadingRequest")
        musicDownloadingRequest?.cancel()
    }
}

/**
 * 各HTTPメソッドは別スレッドで呼ばれる．
 * 曲をダウンロードした後の処理によって，ダイアログの表示などを行いたいが，
 * 別スレッド上でUIを更新することは許されていないため，
 * 各メソッドが呼ばれたスレッドからUI更新可能なメインスレッドに，取得した情報を渡してやる必要がある．
 * その処理を担うのがこのHandler.
 * このHandlerは，曲のダウンロードに関する値を受け取った時の処理を書く．
 */
open class MusicDownloadHandler(
    private val handleMusicDownloadProgressUpdated: (progressPercentage: Int) -> Unit,
    private val handleMusicDownloadFailed: (exception: Exception) -> Unit,
    private val handleMusicDownloadSuccess: (inputStream: InputStream) -> Unit,
): Handler(Looper.getMainLooper()){
    override fun handleMessage(msg: Message){
        when(msg.what){
            // 曲のダウンロードの進捗が更新された場合
            HANDLE_MUSIC_DOWNLOAD_PROGRESS_UPDATED -> {
                handleMusicDownloadProgressUpdated(msg.arg1)
            }
            // 曲のダウンロードに失敗した場合
            HANDLE_MUSIC_DOWNLOAD_FAILED -> {
                val exception = msg.obj as? Exception
                exception?.let{
                    handleMusicDownloadFailed(it)
                }
            }
            // 曲のダウンロードに成功した場合
            HANDLE_MUSIC_DOWNLOAD_SUCCESS -> {
                val inputStream = msg.obj as? InputStream
                inputStream?.let{
                    handleMusicDownloadSuccess(it)
                }
            }
        }
    }
}

/**
 * 各HTTPメソッドは別スレッドで呼ばれる．
 * 曲一覧を取得した後の処理によって，ダイアログの表示などを行いたいが，
 * 別スレッド上でUIを更新することは許されていないため，
 * 各メソッドが呼ばれたスレッドからUI更新可能なメインスレッドに，取得した情報を渡してやる必要がある．
 * その処理を担うのがこのHandler.
 * 上のMusicDownloadHandlerと同じく，こちらは曲一覧の取得に関する値を受け取った時の処理を書く．
 */
class GetMusicListHandler(
    private val handleGetMusicListFailed: (exception: Exception) -> Unit,
    private val handleGetMusicListSuccess: (musicList: List<MusicInfo>) -> Unit
): Handler(Looper.getMainLooper()){
    override fun handleMessage(msg: Message){
        when(msg.what){
            // 曲一覧の取得に失敗した場合
            HANDLE_GET_MUSIC_LIST_FAILED -> {
                val exception = msg.obj as? Exception
                exception?.let{
                    handleGetMusicListFailed(it)
                }
            }
            // 曲一覧の取得に成功した場合
            HANDLE_GET_MUSIC_LIST_SUCCESS -> {
                // msg.objはAny型なので，きちんとList<MusicInfo>であることを担保する．
                val musicList = (msg.obj as? List<*>)?.filterIsInstance<MusicInfo>()
                Log.d("debug", "handler called: $musicList")
                if(musicList?.size == 0){
                    Log.e("debug", "gotten music list size is zero.")
                }
                musicList?.let{
                    handleGetMusicListSuccess(it)
                }
            }
        }
    }
}