package com.gee12.mytetroid.database

import android.content.Context
import androidx.room.*
import com.gee12.mytetroid.database.dao.FavoritesDao
import com.gee12.mytetroid.database.dao.ScriptsDao
import com.gee12.mytetroid.database.dao.ScriptsToObjectsDao
import com.gee12.mytetroid.database.dao.StoragesDao
import com.gee12.mytetroid.database.entity.FavoriteDbEntity
import com.gee12.mytetroid.database.entity.ScriptDbEntity
import com.gee12.mytetroid.database.entity.ScriptToObjectDbEntity
import com.gee12.mytetroid.database.entity.StorageDbEntity

@Database(
    version = 22,
    entities = [
        StorageDbEntity::class,
        FavoriteDbEntity::class,
        ScriptDbEntity::class,
        ScriptToObjectDbEntity::class,
    ],
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 21, to = 22)
    ],
)
@TypeConverters(DataConverter::class)
abstract class TetroidDatabase : RoomDatabase() {

    abstract val storagesDao: StoragesDao

    abstract val favoritesDao: FavoritesDao

    abstract val scriptsDao: ScriptsDao

    abstract val scriptsToObjectsDao: ScriptsToObjectsDao

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
