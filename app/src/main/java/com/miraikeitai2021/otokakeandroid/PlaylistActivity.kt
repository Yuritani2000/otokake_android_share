package com.miraikeitai2021.otokakeandroid

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
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

        val db1 = PlaylistDatabase.getInstance(this)    //PlayListのDB作成
        val db1Dao = db1.PlaylistDao()  //Daoと接続
        val db3 = MiddlelistDatabase.getInstance(this)
        val db3Dao = db3.MiddlelistDao()
        val adapter = RecyclerListAdapter(playlists,db1Dao,db3Dao)

        //db1Dao.deleteAll()

        getPlaylist(adapter, db1Dao)

        //RecyclerViewを取得
        val recyclerview = findViewById<RecyclerView>(R.id.playlist)
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

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun getPlaylist(adapter: RecyclerListAdapter, db1Dao: PlaylistDao){
        if(db1Dao.getAll().size == 0){
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
        Log.v("TAG","test insert ${db1Dao.getAll().toString()}")    //ログに要素を全て出力
        var playList = mutableMapOf<String, Any>("listName" to listName, "id" to id)
        playlists.add(playList)
    }

    //プレイリストをロードする関数
    private fun loadPlaylist(adapter: RecyclerListAdapter, db1Dao: PlaylistDao){
        val lists = db1Dao.getAll()

        lists.forEach{
            var listName = it.name
            var id = it.playlist_id
            var playlist = mutableMapOf<String, Any>("listName" to listName, "id" to id)
            playlists.add(playlist)
        }
        Log.v("TAG","test insert ${db1Dao.getAll().toString()}")    //ログに要素を全て出力

        adapter.notifyDataSetChanged()

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_options_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var returnVal = true

        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            R.id.addPlaylist -> {
                val dialogFragment = AddPlaylistDialogFragment()
                dialogFragment.show(supportFragmentManager, "AddPlaylistDialogFragment")
            }
            else -> {
                returnVal = super.onOptionsItemSelected(item)
            }
        }

        return returnVal
    }

    private inner class RecyclerListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

        var _listNameRow: TextView

        var _deleteButtonRow: ImageButton

        init {
            //引数で渡されたリスト1行分の画面部品から表示に使われるTextViewを取得
            _listNameRow = itemView.findViewById(R.id.listTitle)
            _deleteButtonRow = itemView.findViewById(R.id.deleteListButton)
        }
    }

    private inner class RecyclerListAdapter(private val _listData: MutableList<MutableMap<String, Any>>, private val db1Dao: PlaylistDao, private val db3Dao: MiddlelistDao):
        RecyclerView.Adapter<RecyclerListViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerListViewHolder {
            //レイアウトインフレータを取得
            val inflater = LayoutInflater.from(this@PlaylistActivity)
            //row.xmlをインフレ―トし、1行分の画面部品とする
            val view = inflater.inflate(R.layout.playlist_row, parent, false)
            //ビューホルダオブジェクトを生成
            val holder = RecyclerListViewHolder(view)
            //生成したビューホルダをリターン
            return holder
        }

        override fun onBindViewHolder(holder: RecyclerListViewHolder, position: Int) {
            //リストデータから該当1行分のデータを取得
            val item = _listData[position]
            //メニュー名文字列を取得
            val listTitle = item["listName"] as String

            //ビューホルダ中のTextViewに設定
            holder._listNameRow.text = listTitle

            //削除ボタンクリック時
            holder._deleteButtonRow.setOnClickListener {
                val listMap = playlists[position]
                val id = listMap["id"] as Int
                val listName = listMap["listName"] as String
                val deleteList = Playlist(id ,listName)
                db1Dao.delete(deleteList)
                db3Dao.deletePlaylist(id)
                Log.v("TAG","test delete ${db1Dao.getAll().toString()}")    //ログに要素を全て出力
                _listData.removeAt(position)
                this.notifyDataSetChanged()
            }

            //再生リストクリック時
            holder._listNameRow.setOnClickListener {
                val listMap = playlists[position]
                val listId = listMap["id"] as Int
                val id = listId.toString()
                Toast.makeText(this@PlaylistActivity, id, Toast.LENGTH_SHORT).show()

                val intent2MenuThanks = Intent(this@PlaylistActivity, PlaylistPlayActivity::class.java)
                intent2MenuThanks.putExtra("playlist_id",id)
                startActivity(intent2MenuThanks)
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
            var name = it["listName"]
            if(name == text){
                result = false
                return result
            }
        }

        return result

    }

    override fun onDialogTextRecieve(dialog: DialogFragment, text: String, db1Dao: PlaylistDao) {
        //値を受け取る
        Log.v("dialog",text)

        val checkResult = checkDuplicateName(text)

        if(checkResult) {
            val listName = text
            val newList = Playlist(0, listName)
            db1Dao.insert(newList)
            val list = db1Dao.getId(listName)
            val id = list.playlist_id
            var playlist = mutableMapOf<String, Any>("listName" to listName, "id" to id)
            playlists.add(playlist)
            //adapter.notifyDataSetChanged()
            Log.v("TAG", "test add ${db1Dao.getAll().toString()}")    //ログに要素を全て出力
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