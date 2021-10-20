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

    @Query("INSERT INTO Music (storage_id,title,artist) VALUES (:a,:b,:c)")
    fun insertMusic(a: Long,b: String,c: String)

    //タイトルからストレージIDを取得
    @Query("SELECT storage_id FROM Music WHERE backend_id = :backendnum")
    fun getId(backendnum: Int?): Long

    //再生リストに登録された曲の取得
    @Query("SELECT * FROM Music WHERE backend_id = :backendnum")
    fun getMusic(backendnum: Int): Music
}


//中間テーブルのDao
@Dao
interface MiddlelistDao{
    //全要素取得
    @Query("SELECT * FROM Middlelist")
    fun getAll(): List<Middlelist>

    //追加
    @Insert
    fun insert(user : Middlelist)

    //更新
    @Update
    fun update(user : Middlelist)

    //getPlaylist(1) でプレイリスト1の曲をリストで返す
    @Query("SELECT middle_backend_id FROM Middlelist WHERE middle_playlist_id = :playlistnum")
    fun getPlaylist(playlistnum: Int?): List<Int>

    //deletePlaylist(1) でプレイリスト1の情報を全て削除
    @Query("DELETE FROM Middlelist WHERE middle_playlist_id = :playlistnum")
    fun deletePlaylist(playlistnum: Int)

    //再生リストの1曲登録
    @Query("INSERT INTO Middlelist (middle_playlist_id,middle_backend_id) VALUES (:playlistnum,:backendnum)")
    fun insertMusic(playlistnum: Int?,backendnum: Int)

    //再生リストの1曲削除
    @Query("DELETE FROM Middlelist WHERE middle_playlist_id = :playlistnum AND middle_backend_id = :backendnum")
    fun deleteMusic(playlistnum: Int?,backendnum: Int)

    //該当の再生リストのデータを出力
    @Query("SELECT * FROM Middlelist WHERE middle_playlist_id = :playlistnum ORDER BY middle_backend_id ASC")
    fun getResisteredMusic(playlistnum: Int?): List<Middlelist>

    //再生リストの登録件数の取得
    @Query("SELECT COUNT (middle_playlist_id = :playlistnum) FROM Middlelist")
    fun count(playlistnum: Int?): Int

    //タップした曲以降のデータを出力
    @Query("SELECT middle_backend_id FROM Middlelist WHERE middle_backend_id >= :backendnum ORDER BY middle_backend_id ASC")
    fun tap(backendnum: Int): Array<Int>
}
