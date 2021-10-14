package com.miraikeitai2021.otokakeandroid

import android.content.Context

class CheckRunBpm {
    private var runBpm = 120.03f //一般的な人の走る速さのテンポを140とする
    private var startTime: Long? = null //計測初め時間
    private var endTime: Long? = null //計測終わりの時間
    private val checkMusicBpm = CheckMusicBpm()

    /**
     * 歩調のBpmを得るメソッド
     * 今はランダムで取得
     */
    fun checkRunBpm(context: Context, id: Int): Float{
        //歩調のBpmを取得する処理を書く
        val timeOneStep = timeOneStep()
        if(timeOneStep <= 0.0f){ //結果が0ms以下の時は加工前の曲のbpmを返す．
            runBpm = checkMusicBpm.checkMusicBpm(context, id)
        }else{
            runBpm = 60.0f * (1000.0f / timeOneStep)
        }

        /*
        var randomInteger = (0..22).shuffled().first()
        randomInteger -= 10
        randomInteger.toFloat()
        runBpm += randomInteger

         */

        //歩調のBpmを返す
        return runBpm
    }

    fun resetRunBpm(){
        runBpm = 120.03f
    }

    fun getRunBpm():Float{
        return runBpm
    }

    /**
     * 2つの踏まれたという信号から，その間にかかった時間をミリ単位で
     * 算出する．
     * 初回の算出時はstartTimeとendTimeの値が同値となる
     */
    private val stepTimeHistory = arrayOf(0L, 0L, 0L, 0L, 0L, 0L)

    fun timeOneStep(): Float {
        val time = System.currentTimeMillis()
        if (endTime == null) {
            startTime = time
            endTime = time
        } else {
            startTime = endTime
            endTime = time
        }

        val timeOneStep = endTime!! - startTime!!
//        if(stepTimeHistory.sum() == 0L){
//            stepTimeHistory.forEachIndexed { index, _ ->
//                stepTimeHistory[index] = timeOneStep
//            }
//        }
//        stepTimeHistory.forEachIndexed { index, _ ->
//            if(index == stepTimeHistory.lastIndex){
//                stepTimeHistory[index] = timeOneStep
//            }else{
//                stepTimeHistory[index] = stepTimeHistory[index + 1]
//            }
//        }
        val reTime = timeOneStep.toFloat()
//        val reTime = stepTimeHistory.average().toFloat()

        return reTime
    }
}