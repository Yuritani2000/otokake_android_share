package com.miraikeitai2021.otokakeandroid

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PlaylistActivity : AppCompatActivity(),AddPlaylistDialogFragment.DialogListener{

    val playlists: MutableList<MutableMap<String, Any>> = mutableListOf()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist)

//        val actionbar = supportActionBar
//        if(actionbar != null){
//            actionbar.hide()
//        }

        supportActionBar?.hide()

        val db1 = PlaylistDatabase.getInstance(this)    //PlayListのDB作成
        val db1Dao = db1.PlaylistDao()  //Daoと接続
        val db3 = MiddleListDatabase.getInstance(this)
        val db3Dao = db3.MiddleListDao()
        val adapter = RecyclerListAdapter(playlists,db1Dao,db3Dao)

        getPlaylist(adapter, db1Dao)

        //RecyclerViewを取得
        val recyclerview = findViewById<RecyclerView>(R.id.playlist_recycler_view)
        //LinearLayoutManagerオブジェクトを生成
        val layout = LinearLayoutManager(this@PlaylistActivity)
        //RecyclerViewにレイアウトマネージャーとしてLinearLayoutを設定
        recyclerview.layoutManager = layout
        //アダプタオブジェクトを生成
        //RecyclerViewにアダプタオブジェクトを設定
        recyclerview.adapter = adapter
        //区切り専用オブジェクトを生成
        val decorator = DividerItemDecoration(this@PlaylistActivity, layout.orientation)
        //RecyclerViewに区切り線オブジェクトを設定
        recyclerview.addItemDecoration(decorator)

        val returnButton = findViewById<ImageButton>(R.id.return_button)
        returnButton.setOnClickListener{
            finish()
        }

        val addButton = findViewById<ImageButton>(R.id.add_button)
        addButton.setOnClickListener{
            val dialogFragment = AddPlaylistDialogFragment()
            dialogFragment.show(supportFragmentManager, "AddPlaylistDialogFragment")
        }
    }

    private fun getPlaylist(adapter: RecyclerListAdapter, db1Dao: PlaylistDao){
        if(db1Dao.getAll().isEmpty()){
            createPlaylist(db1Dao)
        }
        else{
            loadPlaylist(adapter, db1Dao)
        }
    }

    private fun createPlaylist(db1Dao: PlaylistDao) {
        val listName = "お気に入り"
        val firstList = Playlist(0,listName)
        db1Dao.deleteAll()
        db1Dao.insert(firstList)
        val list = db1Dao.getId(listName)
        val id = list.playlist_id
        val playList = mutableMapOf<String, Any>("listName" to listName, "id" to id)
        playlists.add(playList)
    }

    //プレイリストをロードする関数
    @SuppressLint("NotifyDataSetChanged")
    private fun loadPlaylist(adapter: RecyclerListAdapter, db1Dao: PlaylistDao){
        val lists = db1Dao.getAll()

        lists.forEach{
            val listName = it.name
            val id = it.playlist_id
            val playlist = mutableMapOf<String, Any>("listName" to listName, "id" to id)
            playlists.add(playlist)
        }

        adapter.notifyDataSetChanged()

    }

    private inner class RecyclerListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

        var listNameRow: TextView = itemView.findViewById(R.id.list_title)
        var deleteButtonRow: ImageButton = itemView.findViewById(R.id.delete_list_button)

    }

    private inner class RecyclerListAdapter(private val _listData: MutableList<MutableMap<String, Any>>, private val db1Dao: PlaylistDao, private val db3Dao: MiddleListDao):
        RecyclerView.Adapter<RecyclerListViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerListViewHolder {
            //レイアウトインフレータを取得
            val inflater = LayoutInflater.from(this@PlaylistActivity)
            //row.xmlをインフレ―トし、1行分の画面部品とする
            val view = inflater.inflate(R.layout.playlist_row, parent, false)
            //生成したビューホルダをリターン
            return RecyclerListViewHolder(view)
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onBindViewHolder(holder: RecyclerListViewHolder, position: Int) {
            //リストデータから該当1行分のデータを取得
            val item = _listData[position]
            //メニュー名文字列を取得
            val listTitle = item["listName"] as String

            //ビューホルダ中のTextViewに設定
            holder.listNameRow.text = listTitle

            //ここのカスタムフォントはアプリ全体に適応されるため将来削除
            val customFont = Typeface.createFromAsset(getAssets(), "Kaisotai-Next-UP-B.ttf")
            holder.listNameRow.setTypeface(customFont)

            //削除ボタンクリック時
            holder.deleteButtonRow.setOnClickListener {
                val listMap = playlists[position]
                val id = listMap["id"] as Int
                val listName = listMap["listName"] as String
                val deleteList = Playlist(id ,listName)
                db1Dao.delete(deleteList)
                db3Dao.deletePlaylist(id)
                _listData.removeAt(position)
                this.notifyDataSetChanged()
            }

            //再生リストクリック時
            holder.listNameRow.setOnClickListener {
                val listMap = playlists[position]
                val listId = listMap["id"] as Int

                val intent = Intent(this@PlaylistActivity, PlaylistPlayActivity::class.java)
                intent.putExtra("playlist_id",listId)
                startActivity(intent)
            }

        }

        override fun getItemCount(): Int {
            //リストデータ中の件数をリターン
            return _listData.size
        }

    }

    private fun checkDuplicateName(text: String) : Boolean{
        var result = true

        playlists.forEach{
            val name = it["listName"]
            if(name == text){
                result = false
                return result
            }
        }

        return result

    }

    override fun onDialogTextReceive(dialog: DialogFragment, text: String, db1Dao: PlaylistDao) {
        //値を受け取る

        val checkResult = checkDuplicateName(text)

        if(checkResult) {
            val newList = Playlist(0, text)
            db1Dao.insert(newList)
            val list = db1Dao.getId(text)
            val id = list.playlist_id
            val playlist = mutableMapOf<String, Any>("listName" to text, "id" to id)
            playlists.add(playlist)
        }
        else{
            val msg = getString(R.string.duplicateName_ng)
            Toast.makeText(this@PlaylistActivity, msg, Toast.LENGTH_LONG).show()
        }


    }
    override fun onDialogPositive(dialog: DialogFragment) {
        //実装なし
    }
    override fun onDialogNegative(dialog: DialogFragment) {
        //キャンセル時
        val msg = getString(R.string.make_playlist_cancel)
        Toast.makeText(this@PlaylistActivity, msg, Toast.LENGTH_LONG).show()
    }
}