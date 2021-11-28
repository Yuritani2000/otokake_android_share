package com.miraikeitai2021.otokakeandroid

import android.app.Dialog
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.DialogFragment
import com.miraikeitai2021.otokakeandroid.databinding.PlaylistPlayFragmentBinding

class PlaylistPlayFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog{
        val dialog = Dialog(requireContext())
        val binding = PlaylistPlayFragmentBinding.inflate(requireActivity().layoutInflater)
        dialog.setContentView(binding.root)

        //ボタンタップ時
        dialog.findViewById<ImageButton>(R.id.crossButton).setOnClickListener {
            dialog.dismiss()
        }

        //プレイ開始ボタンタップ時
        dialog.findViewById<ImageButton>(R.id.startButton).setOnClickListener {

        }

        return dialog
    }

}