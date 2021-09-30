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
    @PrimaryKey(autoGenerate = true) val backend_id: Int, //主キー:バックエンドID(とりあえず自動生成ON)
    val storage_id: Long?,  //ストレージID(NULL許容)
    val title: String,     //曲名
    val artist: String?,    //アーティスト名(NULL許容)
    val url: String?         //URL(NULL許容)
)

/*
//中間テーブルのEntity
@Entity(
    primaryKeys = ["middle_playlist_id","middle_backend_id"], //主キーはプレイリストIDとバックエンドID
    foreignKeys = [ //外部キーの関係性を記述
        ForeignKey(
            entity = Playlist::class,               //Playlistデータベースの
            parentColumns = ["playlist_id"],        //playlist_idを
            childColumns = ["middle_playlist_id"]   //middle_playlist_idとして主キーに使用
        ),
        ForeignKey(
            entity = Music::class,                  //Musicデータベースも同様に
            parentColumns = ["backend_id"],
            childColumns = ["middle_backend_id"]
        )
    ]
)
data class MiddleList(
    @ColumnInfo(name="middle_playlist_id")
    val middle_playlist_id: Int,
    @ColumnInfo(name="middle_backend_id")
    val middle_backend_id: Int
)

 */
