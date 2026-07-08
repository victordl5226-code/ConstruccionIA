package com.construccionia.app.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.construccionia.app.data.models.Infographic
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones CRUD sobre infografías.
 */
@Dao
interface InfographicDao {

    /** Obtener todas las infografías ordenadas por fecha descendente. */
    @Query("SELECT * FROM infographics ORDER BY created_at DESC")
    fun getAllInfographics(): Flow<List<Infographic>>

    /** Obtener una infografía por ID. */
    @Query("SELECT * FROM infographics WHERE id = :id")
    suspend fun getById(id: Long): Infographic?

    /** Obtener una infografía por nombre de archivo. */
    @Query("SELECT * FROM infographics WHERE filePath = :filePath")
    suspend fun getByFileName(filePath: String): Infographic?

    /** Buscar infografías por nombre. */
    @Query("SELECT * FROM infographics WHERE name LIKE '%' || :query || '%' ORDER BY created_at DESC")
    fun searchByName(query: String): Flow<List<Infographic>>

    /** Insertar una infografía. Retorna el ID generado. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(infographic: Infographic): Long

    /** Insertar múltiples infografías. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(infographics: List<Infographic>)

    /** Actualizar una infografía existente. */
    @Update
    suspend fun update(infographic: Infographic)

    /** Eliminar una infografía por objeto. */
    @Delete
    suspend fun delete(infographic: Infographic)

    /** Eliminar una infografía por ID. */
    @Query("DELETE FROM infographics WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Eliminar una infografía por ruta de archivo. */
    @Query("DELETE FROM infographics WHERE filePath = :filePath")
    suspend fun deleteByPath(filePath: String)

    /** Eliminar todas las infografías. */
    @Query("DELETE FROM infographics")
    suspend fun deleteAll()

    /** Contar el total de infografías. */
    @Query("SELECT COUNT(*) FROM infographics")
    fun count(): Flow<Int>
}
