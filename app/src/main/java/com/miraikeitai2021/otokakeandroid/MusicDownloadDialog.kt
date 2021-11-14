package com.miraikeitai2021.otokakeandroid

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import java.lang.IllegalStateException

class MusicDownloadDialog(private val onClickCancelButton: () -> Unit): DialogFragment() {
    private lateinit var downloadMusicProgressBar: ProgressBar
    private lateinit var downloadMusicProgressTextView: TextView
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // ダウンロード中にダイアログを消してActivityから抜けられるとバグの発生につながるため，領域外タップを無効にする
        this.isCancelable = false

        return activity?.let{
            val builder = AlertDialog.Builder(it)
            // ダイアログに埋め込むレイアウトを読み込むインフレーターを呼び出す．
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.dialog_download_music, null)

            // ダイアログに表示するタイトルと，キャンセルボタンをセットする
            builder.setView(view)
                .setTitle(R.string.downloading_music)
                .setNegativeButton(R.string.cancel
                ) { _, _ ->
                    onClickCancelButton()
                }

            // ダウンロードの進捗を示すProgressBarの初期値をセットする
            downloadMusicProgressBar = view.findViewById<ProgressBar>(R.id.download_music_progress_bar)
            downloadMusicProgressTextView = view.findViewById<TextView>(R.id.download_music_progress_text_view)
            downloadMusicProgressBar.max = 100
            downloadMusicProgressBar.progress = 0


            builder.create()
        }?: throw IllegalStateException("Activity cannot be null")
    }

    /**
     * 曲のダウンロード状況が更新されるたびにダイアログの表示内容を更新するメソッド
     */
    fun onProgressUpdated(progressPercentage: Int){
        // 進捗を示すProgressBarとTextViewの内容を更新する．
        downloadMusicProgressBar.progress = progressPercentage
        downloadMusicProgressTextView.text = progressPercentage.toString()
        // ダウンロードが終了すると，ダイアログを閉じる．
        if(progressPercentage == 100){
            this.dismiss()
        }
    }
}