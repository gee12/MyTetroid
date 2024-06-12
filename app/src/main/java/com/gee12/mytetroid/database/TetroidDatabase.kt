package com.gee12.mytetroid.database

import android.content.Context
import androidx.room.*
import com.gee12.mytetroid.database.dao.*
import com.gee12.mytetroid.database.entity.*

@Database(
    version = 23,
    entities = [
        StorageDbEntity::class,
        FavoriteDbEntity::class,
        ScriptDbEntity::class,
        ScriptToObjectDbEntity::class,
        HistoryDbEntity::class,
    ],
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 21, to = 22), // scripts
        AutoMigration(from = 22, to = 23), // history
    ],
)
@TypeConverters(DataConverter::class)
abstract class TetroidDatabase : RoomDatabase() {

    abstract val storagesDao: StoragesDao

    abstract val favoritesDao: FavoritesDao

    abstract val scriptsDao: ScriptsDao

    abstract val scriptsToObjectsDao: ScriptsToObjectsDao

    abstract val historyDao: HistoryDao

    companion object {
        private const val DATABASE_NAME = "mytetroid.db"

        fun create(applicationContext: Context): TetroidDatabase {
            return Room.databaseBuilder(
                context = applicationContext,
                klass = TetroidDatabase::class.java,
                name = DATABASE_NAME,
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
