package com.miraikeitai2021.otokakeandroid

import android.app.Dialog
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.miraikeitai2021.otokakeandroid.databinding.PlaylistPlayFragmentBinding

class PlaylistPlayFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog{
        val dialog = Dialog(requireContext())
        val binding = PlaylistPlayFragmentBinding.inflate(requireActivity().layoutInflater)
        dialog.setContentView(binding.root)


        val musicTitle: String? = arguments?.getString("musicTitle")
        val musicArtist: String? = arguments?.getString("musicArtist")
        val storageIdList: Array<Long>? = arguments?.getSerializable("storageIdList") as? Array<Long>

        storageIdList?.forEach {
            Log.d("debug","${it}")
        }


        musicTitle?.let {musicTitle ->
            dialog.findViewById<TextView>(R.id.musicTitleFragment).text = musicTitle
        }
        musicArtist?.let {musicArtist ->
            dialog.findViewById<TextView>(R.id.musicArtistFragment).text = musicArtist
        }

        //バツボタンタップ時
        dialog.findViewById<ImageButton>(R.id.crossButton).setOnClickListener {
            dialog.dismiss()
        }

        //プレイ開始ボタンタップ時
        dialog.findViewById<ImageButton>(R.id.startButton).setOnClickListener {

            storageIdList?.let{
                //インテント処理
                val intent = Intent(activity, PlayMusicActivity::class.java)
                intent.putExtra("storageIdList",it)
                startActivity(intent)
            }

            dialog.dismiss()
        }

        return dialog
    }

}