package com.iptvwala.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.iptvwala.domain.repository.SourceRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class SourceRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val sourceRepository: SourceRepository
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            val sourceId = inputData.getLong(KEY_SOURCE_ID, -1L)
            
            if (sourceId > 0) {
                sourceRepository.refreshSource(sourceId)
            } else {
                val sources = sourceRepository.getAllSources().first()
                sources.forEach { source ->
                    sourceRepository.refreshSource(source.id)
                }
            }
            
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    companion object {
        const val KEY_SOURCE_ID = "source_id"
        
        fun buildOneTimeRequest(sourceId: Long? = null): OneTimeWorkRequest {
            val data = Data.Builder().apply {
                sourceId?.let { putLong(KEY_SOURCE_ID, it) }
            }.build()
            
            return OneTimeWorkRequestBuilder<SourceRefreshWorker>()
                .setInputData(data)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
        }
        
        fun buildPeriodicRequest(intervalHours: Long): PeriodicWorkRequest {
            return PeriodicWorkRequestBuilder<SourceRefreshWorker>(
                intervalHours, TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
        }
    }
}

@HiltWorker
class EpgRefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val sourceRepository: SourceRepository,
    private val epgRepository: com.iptvwala.domain.repository.EpgRepository
) : CoroutineWorker(context, workerParams) {
    
    override suspend fun doWork(): Result {
        return try {
            epgRepository.refreshEpg()
            epgRepository.clearOldPrograms()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
    
    companion object {
        fun buildOneTimeRequest(): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<EpgRefreshWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
        }
        
        fun buildPeriodicRequest(): PeriodicWorkRequest {
            return PeriodicWorkRequestBuilder<EpgRefreshWorker>(
                12, TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
        }
    }
}
