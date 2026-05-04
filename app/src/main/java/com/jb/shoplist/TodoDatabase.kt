package com.jb.shoplist

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TacheEntity::class, CategorieEntity::class, ParametreEntity::class],
    version = 3,
    exportSchema = false
)
abstract class TodoDatabase : RoomDatabase() {

    abstract fun tacheDao(): TacheDao
    abstract fun categorieDao(): CategorieDao
    abstract fun parametreDao(): ParametreDao

    companion object {
        @Volatile
        private var INSTANCE: TodoDatabase? = null

        fun obtenir(context: Context): TodoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TodoDatabase::class.java,
                    "todo_database"
                )
                .fallbackToDestructiveMigration()
                .build().also { INSTANCE = it }
            }
        }
    }
}