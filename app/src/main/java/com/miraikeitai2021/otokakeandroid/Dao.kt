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

    @Query("SELECT title FROM Music")
    fun getMusic(): List<String>

    @Query("SELECT storage_id FROM Music WHERE title = :Title")
    fun getId(Title: String): Long
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

    //削除
    @Delete
    fun delete(user : Middlelist)

    //getPlaylist(1) でプレイリスト1の曲をリストで返す
    @Query("SELECT * FROM Middlelist WHERE middle_playlist_id = :playlistnum")
    fun getPlaylist(playlistnum: Int): List<Middlelist>

    //deletePlaylist(1) でプレイリスト1の情報を全て削除
    @Query("DELETE FROM Middlelist WHERE middle_playlist_id = :playlistnum")
    fun deletePlaylist(playlistnum: Int)


}
