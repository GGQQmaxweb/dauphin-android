package app.dauphin.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.*
import java.util.*
import java.util.concurrent.TimeUnit

class WidgetUpdateWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        CourseCardWidget().updateAll(context)
        scheduleNextUpdate(context)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "WidgetUpdateWorker"

        fun scheduleNextUpdate(context: Context) {
            val delay = calculateDelayUntilNextUpdate()
            val workRequest = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        private fun calculateDelayUntilNextUpdate(): Long {
            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis

            // Target is XX:40
            calendar.set(Calendar.MINUTE, 40)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            if (calendar.timeInMillis <= now) {
                // If it's already past XX:40, schedule for next hour
                calendar.add(Calendar.HOUR_OF_DAY, 1)
            }

            return calendar.timeInMillis - now
        }
    }
}
