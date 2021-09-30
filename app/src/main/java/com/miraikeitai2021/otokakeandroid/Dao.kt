package com.miraikeitai2021.otokakeandroid

import androidx.room.*

//PlaylistのDao
@Dao
interface PlaylistDao {
    //全要素取得
    @Query("SELECT * FROM Playlist")
    fun getAll(): List<Playlist>

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
}

/*
//中間テーブルのDao
@Dao
interface MiddleListDao{
    @Transaction
    @Query(
        """
            SELECT * FROM Music INNER JOIN middlelist
            ON Music.backend_id=middleList.middle_backend_id
            WHERE middleList.middle_backend_id=:playlistnum
        """
    )
    suspend fun getMusic(vararg playlistnum: Int): List<Music>

    // SQL文について
    // 全てのMusicテーブルから，middleListテーブルも参考にしながら(1行目)，
    // middleListに登録されていて(2行目)，
    // そのmiddleListのmiddle_backend_idが，引数で渡されたplaylistnumの番号と同じMusicをとってきて(3行目)
    // となるらしい　参考:https://qiita.com/FeliTech/items/1a979654c547f3ca6c9a

    //返り値がMusicの配列になるので，これをList表示すればよさそう？


    //InsertやDeleteは未実装
}

 */