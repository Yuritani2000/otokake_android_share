package com.miraikeitai2021.otokakeandroid

import android.content.Context
import android.media.MediaMetadataRetriever
import kotlin.properties.Delegates

class CheckMusicBpm {
    private var musicBpm by Delegates.notNull<Float>()
    private var musicId = -1000
    private val checkMusicUri = CheckMusicUri()

    /**
     * 曲のBpmを得るメソッド
     * context: 呼び出し元Activityのcontext
     * id: ストレージの曲Id
     * musicUri: ストレージの曲のUri
     */
    fun checkMusicBpm(context: Context, id: Int): Float{
        //曲のBpmを取得する処理を書く
        /**
         * シャイニングスター:157.75
         * テックポーター:131.02
         *　saintofsilence: 64.0
         *  natuyasuminotanken: 120.03
         */
        var bpm = "100.0"
        if(id != musicId){
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(context, checkMusicUri.checkUri(id, context.contentResolver))

            if(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) != null) {
                bpm = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)!!.toString()
            }

            musicBpm = bpm.toFloat()
            musicId = id
        }

        //歩調のBpmを返す
        return musicBpm
    }

    fun getMusicBpms(): Float{
        return musicBpm
    }
}