package com.miraikeitai2021.otokakeandroid

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

//参考URL:https://hirauchi-genta.com/kotlin-room/

//Playlistデータベースの実装
@Database(entities = [Playlist::class],version = 1,exportSchema = false)
abstract class PlaylistDatabase : RoomDatabase(){
    abstract fun PlaylistDao(): PlaylistDao

    companion object {  //ここではすでにインスタンスがあればそのインスタンスを返し，なければ新たにインスタンスを作成してるみたい.ぶっちゃけわからん！(参考URL参照)

        private var INSTANCE: PlaylistDatabase? = null

        private val lock = Any()

        fun getInstance(context: Context): PlaylistDatabase {   //db名.getInstance(Context)でDB作成できる
            synchronized(lock) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                        PlaylistDatabase::class.java, "PlaylistDatabase")   //たぶんストレージに保存されるDB名Pla
                        .allowMainThreadQueries()
                        .build()
                }
                return INSTANCE!!
            }
        }
    }
}

//Musicデータベースの実装
@Database(entities = [Music::class],version = 1,exportSchema = false)
abstract class MusicDatabase : RoomDatabase(){
    abstract fun MusicDao(): MusicDao

    companion object {

        private var INSTANCE: MusicDatabase? = null

        private val lock = Any()

        fun getInstance(context: Context): MusicDatabase {
            synchronized(lock) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                        MusicDatabase::class.java, "MusicDatabase")
                        .allowMainThreadQueries()
                        .build()
                }
                return INSTANCE!!
            }
        }
    }
}

//MiddleListデータベースの実装
@Database(entities = [MiddleList::class],version = 1,exportSchema = false)
abstract class MiddleListDatabase : RoomDatabase(){
    abstract fun MiddleListDao(): MiddleListDao

    companion object {

        private var INSTANCE: MiddleListDatabase? = null

        private val lock = Any()

        fun getInstance(context: Context): MiddleListDatabase {
            synchronized(lock) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.applicationContext,
                        MiddleListDatabase::class.java, "MiddleListDatabase")
                        .allowMainThreadQueries()
                        .build()
                }
                return INSTANCE!!
            }
        }
    }
}