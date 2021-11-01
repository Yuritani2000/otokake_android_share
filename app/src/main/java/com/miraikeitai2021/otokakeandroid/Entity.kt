package com.miraikeitai2021.otokakeandroid

import androidx.room.*

/*
    参考リンク:https://developer.android.com/training/data-storage/room/relationships?hl=ja
*/

//プレイリストテーブルのEntity
@Entity
data class Playlist(
    @PrimaryKey(autoGenerate = true) val playlist_id: Int,   //主キー:プレイリストID(自動生成ON)
    val name: String    //プレイリスト名
)

//曲目テーブルのEntity
@Entity
data class Music(
    @PrimaryKey(autoGenerate = true) val backend_id: Int=0, //主キー:バックエンドID(とりあえず自動生成ON)
    val storage_id: Long?,  //ストレージID(NULL許容)
    val title: String,     //曲名
    val artist: String?,    //アーティスト名(NULL許容)
    val url: String?         //URL(NULL許容)
)

//中間テーブルのEntity
@Entity
data class MiddleList(
    @PrimaryKey(autoGenerate = true) val id: Int=0,   //主キーに重複しない番号が必要なため，ほんとにただのid
    val middle_playlist_id: Int,    //プレイリスト番号
    val middle_backend_id: Int      //そのプレイリストに含まれてる曲のbackend_id
)
