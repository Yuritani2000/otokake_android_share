package com.miraikeitai2021.otokakeandroid

import android.app.Dialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
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

        val customFont: Typeface = Typeface.createFromAsset(activity?.assets,"Kaisotai-Next-UP-B.otf")

        musicTitle?.let {musicTitle ->
            dialog.findViewById<TextView>(R.id.music_title_fragment).text = musicTitle
            dialog.findViewById<TextView>(R.id.music_title_fragment).typeface = customFont
        }
        musicArtist?.let {musicArtist ->
            dialog.findViewById<TextView>(R.id.music_artist_fragment).text = musicArtist
            dialog.findViewById<TextView>(R.id.music_artist_fragment).typeface = customFont
        }
        storageIdList?.let{storageIdList ->
            activity?.let {activity ->
                dialog.findViewById<ImageView>(R.id.logo_image).setImageBitmap(StorageMusic().getImage(storageIdList[0],activity))

            }
        }


        //バツボタンタップ時
        dialog.findViewById<ImageButton>(R.id.cross_button).setOnClickListener {
            dialog.dismiss()
        }

        //プレイ開始ボタンタップ時
        dialog.findViewById<ImageButton>(R.id.start_button_2).setOnClickListener {

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