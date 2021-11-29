package com.miraikeitai2021.otokakeandroid

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import java.lang.IllegalStateException

class AddPlaylistDialogFragment : DialogFragment(){

    interface DialogListener {
        fun onDialogPositive(dialog: DialogFragment)
        fun onDialogNegative(dialog: DialogFragment)
        fun onDialogTextReceive(dialog: DialogFragment, text: String, db1Dao: PlaylistDao)//Activity側へStringを渡します。
    }

    private var listener:DialogListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val db1 = PlaylistDatabase.getInstance(requireContext())  //PlayListのDB作成
        val db1Dao = db1.PlaylistDao()  //Daoと接続
        val dialog = activity?.let{
            val builder = AlertDialog.Builder(it)
            val inflater = activity?.layoutInflater
            val dialogView = inflater?.inflate(R.layout.addplaylistdialog_layout,null)//作ったダイアログ用のレイアウトファイルを設定します。
            builder.setView(dialogView)
            builder.setTitle(R.string.dialog_title)
            builder.setMessage(R.string.dialog_msg)

            builder.setPositiveButton(R.string.dialog_btn_ok){ _, _ ->
                val text = dialogView?.findViewById<EditText>(R.id.dialog_text)?.text//EditTextのテキストを取得

                if (!text.isNullOrEmpty()){//textが空でなければ
                    listener?.onDialogTextReceive(this,text.toString(),db1Dao)
                }

            }
            builder.setNegativeButton(R.string.dialog_btn_ng){ _, _ ->
                listener?.onDialogNegative(this)
            }


            builder.create()
        }

        return dialog?: throw IllegalStateException("アクティビティがnullです")
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            listener = context as DialogListener
        }catch (e: Exception){
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}