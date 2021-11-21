package com.miraikeitai2021.otokakeandroid

import android.content.Context

class PlayMusicContinue() {
    private val checkMusicUri = CheckMusicUri()

    companion object{
        private var listSize = 0
        private var order = 0
        private var myStorageIdList: Array<Long> = arrayOf()
    }

    /**
     * 再生リストが押されたら先ずここが呼ばれる．
     * 1曲目のみmyStorageIdListやorderの設定を行い
     * 2曲目以降はstartMusic関数からcallBackPlayMusicのみ呼ばれるため
     * 設定処理はスキップされる
     */
    fun orderMusic(storageIdList: Array<Long>, context: Context, playMusic: PlayMusic){
        listSize = storageIdList.size

        myStorageIdList = storageIdList
        resetOrder()

        playMusic.startMusic(checkMusicUri.checkUri(myStorageIdList[order].toInt(),context.contentResolver))
    }

    /**
     * order変数の値を0にリセットする関数．
     * 異なる再生リストがよばれて，異なる配列が送られて来た時に動作する．
     */
    private fun resetOrder(){
        order = 0
    }

    /**
     * 曲の再生が終了した時に呼ばれるコールバック変数．
     * ここで再度startMusicを呼ぶことでループ処理をしている．
     * 配列の最後まで進んだ時はstopMusicのみを呼んでループを終了する．
     */
    fun callBackPlayMusic(context: Context, playMusic: PlayMusic){
        order += 1
        if(order >= listSize){
            if(playMusic.getMediaPlayer() != null){
                playMusic.stopMusic()
            }
        }else{
            if(playMusic.getMediaPlayer() != null){
                playMusic.stopMusic()
            }
            if(playMusic.getMediaPlayer() == null){
                playMusic.startMusic(checkMusicUri.checkUri(myStorageIdList[order].toInt(), context.contentResolver))
            }
        }
    }

    /**
     * ストレージ配列からストレージIDを返す．
     * orderがストレージ配列の長さを以上の値になってしまっている場合は
     * ストレージ配列の最後の値を指定する．
     */
    fun getStorageId(): Long{
        if(order >= listSize){
            order = listSize - 1
        }
        return myStorageIdList[order]
    }
}