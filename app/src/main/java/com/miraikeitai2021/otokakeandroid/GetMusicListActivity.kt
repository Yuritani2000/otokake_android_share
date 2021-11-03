package com.miraikeitai2021.otokakeandroid


import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.httpDownload
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTransformingSerializer
import java.io.File
import java.io.InputStream

/**
 * サーバから，曲一覧を取得するAPIにアクセスして，登録されている曲の情報を排列で取得するActivity.
 * 今は仮版としてActivityとして実装しているが，いずれは長谷川君のPlaylisActivityに合わせることになる．
 * */
class GetMusicListActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_music_list)

        // 曲の一覧を取得するHTTPリクエスト
        val musicRepository = MusicRepository()
        val musicViewModel = MusicViewModel(musicRepository)
        musicViewModel.getMusicList()

        findViewById<Button>(R.id.download_music_button).setOnClickListener {
            musicViewModel.downloadMusic()
        }
    }
}

@Serializable
data class MusicList(
    @Serializable(with = MusicListSerializer::class)
    val musicList: List<MusicInfo>
)

@Serializable
@SerialName("MusicInfo")
data class MusicInfo(
    @SerialName("musicID")val musicID: Int?,
    @SerialName("musicName")val musicName: String?,
    @SerialName("musicArtist")val musicArtist: String?,
    @SerialName("musicURL")val musicURL: String?
)

object MusicListSerializer : JsonTransformingSerializer<List<MusicInfo>>(ListSerializer(MusicInfo.serializer())){
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element is JsonArray) JsonArray(listOf(element)) else element
}


class MusicViewModel(private val musicListRepository: MusicRepository): ViewModel(){
    fun getMusicList() {
        var musicListResponse: List<MusicInfo>? = null
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val httpResult = musicListRepository.requestGetMusicList()
                when(httpResult){
                    is HttpResult.Success<List<MusicInfo>> -> {
                        musicListResponse = httpResult.data
                        musicListResponse?.get(0)?.let{
                            Log.d("debug", "musicID: ${it.musicID}");
                            Log.d("debug", "musicID: ${it.musicName}");
                            Log.d("debug", "musicID: ${it.musicArtist}");
                            Log.d("debug", "musicID: ${it.musicURL}");

                        }
                    }
                }
            }catch(e: Exception){
                Log.e("debug", e.toString())
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun downloadMusic() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val httpResult = musicListRepository.requestDownloadMusic()
                when (httpResult){
                    is HttpResult.Success<InputStream> -> {
                    }
                }
            }catch(e: Exception){
                Log.e("debug", e.toString())
            }
        }
    }
}

sealed class HttpResult<out R>{
    data class Success<out T>(val data: T) : HttpResult<T>()
    data class Error(val exception: Exception) : HttpResult<Nothing>()
}

class MusicRepository(){
    private val url = "http://192.168.2.107:8080/getMusicInfoAll"

    suspend fun requestGetMusicList(): HttpResult<List<MusicInfo>> {
        var resultStr = "no data"
        var httpResult: HttpResult<List<MusicInfo>> = HttpResult.Error(Exception("No http request was executed."))

        withContext(Dispatchers.IO){
            val (_, _, result) = url.httpGet()
                .responseString()

            when(result) {
                is Result.Failure -> {
                    val ex = result.getException()
                    Log.e("debug", ex.toString())
                    httpResult = HttpResult.Error(Exception(ex.toString()))
                }
                is Result.Success -> {
                    resultStr = result.value
                    Log.d("debug", "resultStr: $resultStr")
                    try {
                        val musicList = Json.decodeFromString<List<MusicInfo>>(resultStr)
                        Log.d("debug", "result converted to object: ${musicList}")
                        httpResult = HttpResult.Success(musicList)
                    }catch(e: Exception){
                        Log.e("debug" , e.toString())
                        httpResult = HttpResult.Error(e)
                    }
                }
            }
        }
        return httpResult
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun requestDownloadMusic(): HttpResult<InputStream> {
        val url = "https://testotokake.s3.ap-northeast-1.amazonaws.com/music/maoudamashii-halzion.mp3"
        var httpResult: HttpResult<InputStream> = HttpResult.Error(Exception("No http request was executed."))
        withContext(Dispatchers.IO){
            url.httpDownload().fileDestination { response, url ->
                File.createTempFile("temp", ".tmp")
            }.progress{ readBytes, totalBytes ->
                val progress = readBytes.toFloat() / totalBytes.toFloat()
                Log.d("debug", "current progress: $progress");
            }.response{ res, req, result ->
                when(result){
                    is Result.Failure -> {
                        val ex = result.getException()
                        Log.e("debug", ex.toString())
                        httpResult = HttpResult.Error(Exception(ex.toString()))
                    }
                    is Result.Success -> {
                        val inputStream = result.value.inputStream()
                        Log.d("debug", "downloaded data is: $inputStream");
                        httpResult = HttpResult.Success<InputStream>(inputStream)
                    }
                }
            }
        }
        return httpResult
    }
}