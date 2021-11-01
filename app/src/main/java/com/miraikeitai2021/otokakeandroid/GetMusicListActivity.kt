package com.miraikeitai2021.otokakeandroid


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

/**
 * サーバから，曲一覧を取得するAPIにアクセスして，登録されている曲の情報を排列で取得するActivity.
 * 今は仮版としてActivityとして実装しているが，いずれは長谷川君のPlaylisActivityに合わせることになる．
 * */
class GetMusicListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_music_list)

        // 曲の一覧を取得するHTTPリクエスト
        val musicListRepository = MusicListRepository()
        val musicListViewModel = MusicListViewModel(musicListRepository)
        musicListViewModel.getMusicList()
    }

    private inner class MusicRecyclerViewAdapter(private val dataList: Array<MusicInfo>)
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


class MusicListViewModel(private val musicListRepository: MusicListRepository): ViewModel(){
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
}

sealed class HttpResult<out R>{
    data class Success<out T>(val data: T) : HttpResult<T>()
    data class Error(val exception: Exception) : HttpResult<Nothing>()
}

class MusicListRepository(){
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
}