package pl.brightinventions.slf4android

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import io.particle.android.sdk.tinker.TinkerApplication
import mu.KotlinLogging
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException


class LogTask : AsyncTask<Context, Void, File>() {

    private val log = KotlinLogging.logger {}

    override fun doInBackground(vararg params: Context?): File? {
        val paramZero = if (params.isNullOrEmpty()) null else params[0]
        if (paramZero == null) {
            log.warn("Wrong arguments passed to assemble logs")
            return null
        }

        val ctx: Context = paramZero

        return try {
            val logging = (ctx.applicationContext as TinkerApplication).fileLogging
            log.info("Creating zip of log files!")
            val zip = logging.createZipOfLogFiles()
            log.info("Created zip file! path=${zip.absolutePath}")
            zip

        } catch (e: IOException) {
            log.warn("Error creating temp file, did you enable write permissions?", e)
            null
        }
    }
}