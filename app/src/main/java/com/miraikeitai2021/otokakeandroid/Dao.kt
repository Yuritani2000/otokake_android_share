package com.miraikeitai2021.otokakeandroid

import androidx.room.*

//PlaylistのDao
@Dao
interface PlaylistDao {
    //全要素取得
    @Query("SELECT * FROM Playlist")
    fun getAll(): List<Playlist>

    //全要素削除
    @Query("DELETE FROM Playlist")
    fun deleteAll()

    //PlayListのid取得
    @Query("SELECT * FROM Playlist WHERE name = :title")
    fun getId(title: String): Playlist

    //追加
    @Insert
    fun insert(user : Playlist)

    //更新
    @Update
    fun update(user : Playlist)

    //削除
    @Delete
    fun delete(user : Playlist)

    //playlist名の取得
    @Query("SELECT name FROM Playlist WHERE playlist_id = :playlist")
    fun getTitle(playlist: Int): String


}

//MusicのDao
@Dao
interface MusicDao{
    //全要素取得
    @Query("SELECT * FROM Music")
    fun getAll(): List<Music>

    //追加
    @Insert
    fun insert(user : Music)

    //更新
    @Update
    fun update(user : Music)

    //削除
    @Delete
    fun delete(user : Music)

    //曲の登録
    @Query("INSERT INTO Music (storage_id,title,artist) VALUES (:a,:b,:c)")
    fun insertMusic(a: Long,b: String,c: String)

    //タイトルからストレージIDを取得
    @Query("SELECT storage_id FROM Music WHERE backend_id = :backend")
    fun getId(backend: Int): Long

    //再生リストに登録された曲の取得
    @Query("SELECT * FROM Music WHERE backend_id = :backend")
    fun getMusic(backend: Int): Music

    //バックエンドIDからストレージIDを出力する
    @Query("SELECT storage_id FROM music WHERE backend_id = :backend")
    fun getStorageId(backend: Int): Long

    //バックエンドID一覧の取得
    @Query("SELECT backend_id From Music")
    fun getBackendId(): Array<Int>

    //HTTP通信で受け取った曲の登録
    @Query("INSERT INTO Music (backend_id,title,artist,url) VALUES (:backend, :title, :artist, :url)")
    fun insertHTTPMusic(backend: Int,title: String,artist: String?,url: String?)

    //ダウンロードした曲のストレージIDを登録する
    @Query("UPDATE Music SET storage_id = :storageId WHERE backend_id = :backendId")
    fun updateStorageId(backendId: Int,storageId: Long)

    //タップした曲のストレージIDを出力
    @Query("SELECT storage_id FROM Music WHERE backend_id = :backendId")
    fun tap(backendId: Int): Long?
}


//中間テーブルのDao
@Dao
interface MiddleListDao{
    //全要素取得
    @Query("SELECT * FROM MiddleList")
    fun getAll(): List<MiddleList>

    //追加
    @Insert
    fun insert(user : MiddleList)

    //更新
    @Update
    fun update(user : MiddleList)

    //getPlaylist(1) でプレイリスト1の曲をリストで返す
    @Query("SELECT middle_backend_id FROM MiddleList WHERE middle_playlist_id = :playlist")
    fun getPlaylist(playlist: Int): List<Int>

    //deletePlaylist(1) でプレイリスト1の情報を全て削除
    @Query("DELETE FROM MiddleList WHERE middle_playlist_id = :playlist")
    fun deletePlaylist(playlist: Int)

    //再生リストの1曲登録
    @Query("INSERT INTO MiddleList (middle_playlist_id,middle_backend_id) VALUES (:playlist,:backend)")
    fun insertMusic(playlist: Int,backend: Int)

    //再生リストの1曲削除
    @Query("DELETE FROM MiddleList WHERE middle_playlist_id = :playlist AND middle_backend_id = :backend")
    fun deleteMusic(playlist: Int,backend: Int)

    //該当の再生リストのデータを出力
    @Query("SELECT * FROM MiddleList WHERE middle_playlist_id = :playlist ORDER BY middle_backend_id ASC")
    fun getRegisteredMusic(playlist: Int): List<MiddleList>

    //再生リストの登録件数の取得
    @Query("SELECT COUNT (middle_playlist_id = :playlist) FROM MiddleList")
    fun count(playlist: Int): Int

    //タップした曲以降のバックエンドIDの配列を出力
    @Query("SELECT middle_backend_id FROM MiddleList WHERE middle_playlist_id = :playlist AND middle_backend_id >= :backend ORDER BY middle_backend_id ASC")
    fun tap(playlist: Int,backend: Int): Array<Int>
}