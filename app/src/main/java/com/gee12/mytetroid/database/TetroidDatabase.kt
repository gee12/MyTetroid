package com.gee12.mytetroid.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gee12.mytetroid.database.dao.StoragesDao
import com.gee12.mytetroid.database.entity.StorageEntity

@Database(entities = [StorageEntity::class], version = 1)
abstract class TetroidDatabase : RoomDatabase() {
    abstract val storagesDao: StoragesDao

    companion object {
        private const val DATABASE_NAME = "mytetroid.db"

        fun create(applicationContext: Context): TetroidDatabase =
            Room.databaseBuilder(
                applicationContext,
                TetroidDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
    }
}
