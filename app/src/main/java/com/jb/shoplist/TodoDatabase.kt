package com.jb.shoplist

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE taches ADD COLUMN quantite TEXT NOT NULL DEFAULT ''")
    }
}

@Database(
    entities = [TacheEntity::class, CategorieEntity::class, ParametreEntity::class],
    version = 4,
    exportSchema = true
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
                .addMigrations(MIGRATION_3_4)
                .build().also { INSTANCE = it }
            }
        }
    }
}