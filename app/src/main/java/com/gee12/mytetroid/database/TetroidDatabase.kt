package com.gee12.mytetroid.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.gee12.mytetroid.database.dao.StoragesDao
import com.gee12.mytetroid.database.entity.StorageEntity

@Database(entities = [StorageEntity::class], version = 16)
@TypeConverters(DataConverter::class)
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
