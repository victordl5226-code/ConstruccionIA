package com.construccionia.app.auth

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Worker que ejecuta la sincronización en segundo plano.
 * Usa Hilt para inyección de dependencias (requiere HiltWorker).
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncService: SyncService,
    private val authRepository: AuthRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("SyncWorker: Iniciando sincronización en background")

        // Verificar si el usuario está autenticado
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            Timber.w("SyncWorker: Usuario no autenticado, saltando sync")
            return Result.success()
        }

        return try {
            syncService.syncAll()
            Timber.d("SyncWorker: Sincronización completada exitosamente")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker: Error en sincronización")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "periodic_sync"
        private const val INTERVAL_HOURS = 6L

        /**
         * Programa la sincronización periódica.
         * @param context Contexto de la aplicación
         * @param replace Si true, reemplaza el trabajo existente
         */
        fun schedule(context: Context, replace: Boolean = false) {
            val workRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                INTERVAL_HOURS, TimeUnit.HOURS
            )
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()
                )
                .build()

            val policy = if (replace) {
                ExistingPeriodicWorkPolicy.UPDATE
            } else {
                ExistingPeriodicWorkPolicy.KEEP
            }

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, policy, workRequest)

            Timber.d("SyncWorker: Sincronización programada cada $INTERVAL_HOURS horas")
        }

        /**
         * Cancela la sincronización periódica.
         */
        fun cancel(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(WORK_NAME)
            Timber.d("SyncWorker: Sincronización cancelada")
        }
    }
}
