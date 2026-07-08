package com.construccionia.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.construccionia.app.data.models.Infographic
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Base de datos local de Room para ConstruccionIA.
 * Almacena metadatos de infografías para acceso rápido offline.
 *
 * Historial de versiones:
 *   - Versión 1: Esquema inicial con id, name, uriString, filePath, is_demo, created_at.
 *   - Versión 2: Se agrega columna 'version' a la tabla infographics.
 */
@Database(
    entities = [Infographic::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun infographicDao(): InfographicDao

    companion object {
        private const val DB_NAME = "construccionia.db"

        /**
         * Migración de versión 1 a 2: Agregar columna 'version' a la tabla infographics.
         *
         * Los dispositivos que ya tienen la BD versión 1 conservarán sus datos
         * y obtendrán la nueva columna con valor por defecto 1.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE infographics ADD COLUMN version INTEGER NOT NULL DEFAULT 1")
                Timber.d("Room migration 1->2: added version column to infographics")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Obtiene la instancia singleton de la base de datos.
         * Usa patrón double-check para seguridad en hilos.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: create(context).also { INSTANCE = it }
            }
        }

        /**
         * Crea (o reusa) la base de datos con las migraciones registradas.
         * Este método es usado tanto por [getInstance] como por Hilt.
         */
        fun create(context: Context): AppDatabase {
            return buildDatabase(context)
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}

/**
 * Factory injectable por Hilt para obtener la base de datos.
 * Útil cuando se necesita inyección manual fuera del módulo principal.
 */
@Singleton
class AppDatabaseFactory @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun create(): AppDatabase = AppDatabase.create(context)
}
