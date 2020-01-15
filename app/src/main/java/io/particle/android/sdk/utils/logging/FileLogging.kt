package io.particle.android.sdk.utils.logging

import android.app.Application
import io.particle.mesh.setup.flow.Scopes
import kotlinx.coroutines.*
import mu.KotlinLogging
import pl.brightinventions.slf4android.LoggerConfiguration
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


private const val NUM_LOG_FILES_TO_KEEP_BEFORE_ROLLOVER = 10
// the name of the subdir under the app's main private data dir where logs are kept
private const val LOGS_DATA_DIR_NAME = "logs"
// the SimpleDateFormat format string for dates used as the filename for logs
private const val LOG_FILENAME_DATE_FORMAT = "yyyy-MM-dd_HH-mm"


class FileLogging(private val app: Application) {

    private val log = KotlinLogging.logger {}

    private val logsDir by lazy { File(app.applicationInfo.dataDir, LOGS_DATA_DIR_NAME) }
    private val zipFile by lazy { File(app.externalCacheDir, "particle_android_app_logs.zip") }
    private var isInitialized = false

    fun initLogging(scopes: Scopes) {
        if (isInitialized) {
            return
        }
        isInitialized = true

        // ensure logs dir exists
        logsDir.mkdirs()
        app.cacheDir.mkdirs()

        val fileHandler = LoggerConfiguration.fileLogHandler(app)
        val dateStr = SimpleDateFormat(LOG_FILENAME_DATE_FORMAT, Locale.getDefault()).format(Date())
        val pathPattern = File(logsDir, "${dateStr}_%g.log").absolutePath
        fileHandler.setFullFilePathPattern(pathPattern)
        LoggerConfiguration.configuration().addHandlerToRootLogger(fileHandler)

        scopes.onWorker {
            delay(2000)
            // give the app a moment to launch before doing disk I/O
            withContext(Dispatchers.IO) {
                deleteOldLogs()
                deleteOldZip()
            }
        }
    }

    fun createZipOfLogFiles(): File {
        deleteOldZip()
        val logFiles = getAllLogFiles()
        zip(logFiles, zipFile)
        log.info { "Created zip file: $zipFile" }
        return zipFile
    }

    private fun deleteOldLogs() {
        for (oldLog in getOldLogFiles()) {
            try {
                log.info { "Deleting old log file: $oldLog" }
                oldLog.delete()
            } catch (ex: Exception) {
                log.error(ex) { "Error trying to delete old log file: $oldLog" }
            }
        }
    }

    private fun getAllLogFiles(): List<File> {
        val logFiles = logsDir.listFiles().filter { it.extension == "log" }
        log.info { "Found ${logFiles.size} files ending in '.log'" }
        return logFiles
    }

    private fun getOldLogFiles(): List<File> {
        val oldLogFiles = getAllLogFiles()
            .sortedDescending()                           // sort them from newest to oldest
            .drop(NUM_LOG_FILES_TO_KEEP_BEFORE_ROLLOVER)  // ignore the first <# logs to keep>
        // what we're left with now is just the log files to be deleted (normally this should
        // only ever be 0 or 1 file.)

        // also delete the stale lock files that the logger creates, too
        val oldLockFiles = oldLogFiles.map { File(it.parent, it.name + ".lck") }

        val allOldFiles = (oldLogFiles + oldLockFiles).sortedDescending()

        for (oldFile in allOldFiles) {
            log.debug { "Found log file to roll over: $oldFile" }
        }

        return allOldFiles
    }

    private fun deleteOldZip() {
        if (!zipFile.exists()) {
            log.info { "No old zip file to delete, bailing out. Zip file path: $zipFile" }
            return
        }

        try {
            log.info { "Deleting previous zip file: $zipFile" }
            zipFile.delete()
        } catch (ex: Exception) {
            if (zipFile.exists()) {
                log.error(ex) { "Error trying to delete old ZIP file: $zipFile" }
            }
        }
    }

}


@Throws(IOException::class)
private fun zip(files: List<File>, zipFile: File?) {
    val BUFFER_SIZE = 2048
    val zipOutputStream = ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile)))
    zipOutputStream.use { out ->
        var origin: BufferedInputStream
        val data = ByteArray(BUFFER_SIZE)
        for (file in files) {
            val fileInputStream = FileInputStream(file)
            origin = BufferedInputStream(fileInputStream, BUFFER_SIZE)
            val filePath = file.absolutePath
            try {
                val entry = ZipEntry(filePath.substring(filePath.lastIndexOf("/") + 1))
                out.putNextEntry(entry)
                var count: Int
                while (origin.read(data, 0, BUFFER_SIZE).also { count = it } != -1) {
                    out.write(data, 0, count)
                }
            } finally {
                origin.close()
            }
        }
    }
}