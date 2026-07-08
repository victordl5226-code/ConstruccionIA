package com.construccionia.app

import android.app.Application
import android.os.Environment
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Application class principal.
 * Anotada con @HiltAndroidApp para habilitar la inyección de dependencias.
 * Implementa Configuration.Provider para integrar WorkManager con Hilt.
 */
@HiltAndroidApp
class ConstruccionIAApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // ── Inicializar Timber ──
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // En producción podrías plantar un árbol personalizado
            // que reporte a Crashlytics
        }

        // Inicializar Crashlytics
        try {
            FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(true)
                log("App iniciada — ConstruccionIA v1.0.0")
            }
        } catch (e: Exception) {
            Timber.w("Firebase no configurado: ${e.localizedMessage}")
        }
    }

    /** Directorio compartido para infografías descargadas. */
    val imagesDir: File by lazy {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            DOWNLOADS_SUBDIR
        )
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    companion object {
        const val DOWNLOADS_SUBDIR = "ConstruccionIA"
    }
}
