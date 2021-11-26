package com.miraikeitai2021.otokakeandroid

import android.content.Context
import android.net.Uri

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
        // orderの値を100ms起きに参照して曲情報を持ってくることにした．
        // つまり，orderの値が配列の範囲外を指すと例外が起こるので，
        // orderの値はゼロから配列長-1の範囲外にならないようにする．
        if(order + 1>= listSize){
            if(playMusic.getMediaPlayer() != null){
                // スキップボタンから呼び出された場合(曲がまだ終わっていない場合)，動作を無効にする．
                if(playMusic.getProgress() >= playMusic.getDuration()){
                    // 最後の曲の再生が終わった後に好きな位置に戻れるように，stopではなくpauseにしておく．
                    playMusic.pauseMusic()
//                    playMusic.stopMusic()
                }
            }
        }else{
            // orderのインクリメント位置をずらす．最後以外の曲の再生が終わった時だけ，インクリメントすることで，
            // orderが範囲外にならないようにする．
            order++
            if(playMusic.getMediaPlayer() != null){
                playMusic.stopMusic()
            }
            if(playMusic.getMediaPlayer() == null){
                playMusic.startMusic(checkMusicUri.checkUri(myStorageIdList[order].toInt(), context.contentResolver))
            }
        }
    }

    /**
     * 1曲前の曲を再生する．
     */
    fun playPreviousTrack(context: Context, playMusic: PlayMusic){
        // orderの値を100ms起きに参照して曲情報を持ってくることにした．
        // つまり，orderの値が配列の範囲外を指すと例外が起こるので，
        // orderの値はゼロから配列長-1の範囲外にならないようにする．
        if(order - 1 < 0){// 再生リスト配列の最初であった場合は，位置0に再生位置を合わせるのみにする．
            playMusic.seekTo(0)
            return
        }else{
            // orderのデクリメントの位置をずらす．最初以外の曲で巻き戻しボタンを押したときだけ，デクリメントすることで，
            // orderが範囲外にならないようにする．
            order--
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

    /**
     * 現在再生中の曲の順番を返す．
     */
    fun getOrder(): Int{
        return order
    }
}