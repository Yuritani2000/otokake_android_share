package com.miraikeitai2021.otokakeandroid

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.media.PlaybackParams
import android.net.Uri
import android.util.Log
import java.io.IOException

class PlayMusic(context: Context) {
    private val myContext = context
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var playBackParams: PlaybackParams
    private val checkRunBpm: CheckRunBpm = CheckRunBpm()
    private var changedMusicSpeed = 0f
    private val playMusicContinue = PlayMusicContinue()

    /**
     * 音楽を再生するメソッド
     * musicUri：ストレージから再生する音楽を指定するURI
     * musicSpeed：加工前の曲のbpm
     */
    fun startMusic(musicUri: Uri){ //fun startMusic(musicUri: Uri, musicSpeed: Float)
        if(mediaPlayer == null){

            //曲の再生開始
            try {
                mediaPlayer = MediaPlayer()
                mediaPlayer!!.setDataSource(myContext, musicUri)
                mediaPlayer!!.prepare()
                mediaPlayer!!.start()
                mediaPlayer!!.setOnCompletionListener{ playMusicContinue.collBackPlayMusic(myContext, this) }
            } catch (e: IllegalArgumentException) {
                //Toast.makeText(myContext, "Exception($e)", Toast.LENGTH_LONG).show()
            } catch (e: IllegalStateException) {
                //Toast.makeText(myContext, "Exception($e)", Toast.LENGTH_LONG).show()
            } catch (e: IOException) {
                //Toast.makeText(myContext, "Exception($e)", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 音楽を停止するメソッド
     */
    fun stopMusic(){
        //歩調のbpm情報をリセット
        if(mediaPlayer != null) {
            checkRunBpm.resetRunBpm()
            //Toast.makeText(myContext, "${checkRunBpm.getRunBpm()}", Toast.LENGTH_LONG).show()

            mediaPlayer!!.stop()
            mediaPlayer!!.reset()
            mediaPlayer!!.release()
            mediaPlayer = null
        }
    }

    /**
     * 歩調のbpmにより，音楽の再生速度を変えるメソッド
     * runSpeed：歩調のbpm
     */
    fun changeSpeedMusic(runBpm: Float, orgMusicBpm: Float){

        if(mediaPlayer != null) {
            playBackParams = PlaybackParams()

            if(orgMusicBpm > 0.0f){
                changedMusicSpeed = runBpm / orgMusicBpm
            }else{
                changedMusicSpeed = 0.001f
            }
            if(changedMusicSpeed < 0.1f || changedMusicSpeed > 6.0f){
                return
            }
            Log.d("debug", "PlaybackParams instance: $playBackParams")
            Log.d("debug", "changedMusicSpeed: $changedMusicSpeed")
            val speed = playBackParams.setSpeed(changedMusicSpeed)
            Log.d("debug", "playbackParams instance: $speed")
            mediaPlayer!!.setPlaybackParams(speed)
            /*
            Toast.makeText(
                myContext,
                "runBpm: $runSpeed, MusicSpeed: $chengedMusicSpeed",
                Toast.LENGTH_SHORT
            ).show()

             */

        }


    }

    /**
     * テスト用
     * mediaPlayer変数の初期化
     */
    fun setMediaPlayer(){
        mediaPlayer = MediaPlayer()
    }

    /**
     * テスト用
     * mediaPlayer変数の値を得る
     */
    fun getMediaPlayer(): MediaPlayer?{
        return mediaPlayer
    }

    /**
     * テスト用
     * chengedMusicSpeedの値取得
     */
    fun getChangedMusicSpeed(): Float{
        return changedMusicSpeed
    }

    /**
     * 曲が再生用かどうかの状態を取得する
     */
    fun isPlaying(): Boolean{
        val isPlaying =  mediaPlayer?.isPlaying
        isPlaying?.let {
            return it
        }
        return false
    }
}