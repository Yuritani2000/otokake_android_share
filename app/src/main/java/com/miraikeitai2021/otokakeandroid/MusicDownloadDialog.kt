package com.miraikeitai2021.otokakeandroid

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import org.w3c.dom.Text
import java.lang.IllegalStateException

class MusicDownloadDialog(private val onClickCancelButton: () -> Unit): DialogFragment() {
    private lateinit var downloadMusicProgressBar: ProgressBar
    private lateinit var downloadMusicProgressTextView: TextView
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let{
            val builder = AlertDialog.Builder(it)
            // ダイアログに埋め込むレイアウトを読み込むインフレーターを呼び出す．
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.dialog_download_music, null)

            builder.setView(view)
                .setTitle(R.string.downloading_music)
                .setNegativeButton(R.string.cancel,
                    DialogInterface.OnClickListener{ dialog, id ->
                        onClickCancelButton()
                    })

            downloadMusicProgressBar = view.findViewById<ProgressBar>(R.id.download_music_progress_bar)
            downloadMusicProgressTextView = view.findViewById<TextView>(R.id.download_music_progress_text_view)
            downloadMusicProgressBar.max = 100
            downloadMusicProgressBar.progress = 0


            builder.create()
        }?: throw IllegalStateException("Activity cannot be null")
    }

    fun onProgressUpdated(progressPercentage: Int){
        downloadMusicProgressBar.progress = progressPercentage
        downloadMusicProgressTextView.text = progressPercentage.toString()
        if(progressPercentage == 100){
            this.dismiss()
        }
    }
}