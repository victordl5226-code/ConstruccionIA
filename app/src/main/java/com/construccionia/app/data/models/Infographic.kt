package com.construccionia.app.data.models

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Representa una infografía (imagen) gestionada por la aplicación.
 * Entidad de Room para persistencia local.
 */
@Entity(tableName = "infographics")
data class Infographic(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val uriString: String,
    val filePath: String,
    @ColumnInfo(name = "is_demo")
    val isDemo: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    val version: Int = 1
) {
    /** URI conveniente para acceso desde el código. Se reconstruye desde [uriString]. */
    val uri: Uri get() = try { Uri.parse(uriString) } catch (e: Exception) { Uri.EMPTY }
}
