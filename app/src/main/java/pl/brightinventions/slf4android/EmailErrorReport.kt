package pl.brightinventions.slf4android

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import java.io.File

/**
 * Minimal in-tree stub replacing slf4android's EmailErrorReport, used by [showLogSharingPrompt].
 * It composes the "Send logs" email intent (recipients/subject/body). Attaching the zipped log
 * files is a no-op: slf4android shipped its own android.support FileProvider to expose the files,
 * and that provider went away with the dependency. The attachment tasks are still executed (so the
 * zip is still produced), but they are not attached to the email until a FileProvider is reinstated.
 */
class EmailErrorReport(
    private val message: String?,
    private val emailAddresses: List<String?>?,
    private val emailSubject: String?,
    private val emailBody: String?
) {

    private val attachmentTasks = mutableListOf<AsyncTask<Context, Void, File>>()

    fun addFileAttachmentFrom(task: AsyncTask<Context, Void, File>) {
        attachmentTasks.add(task)
    }

    fun configureRecipients(intent: Intent) {
        emailAddresses?.filterNotNull()?.toTypedArray()?.let {
            if (it.isNotEmpty()) intent.putExtra(Intent.EXTRA_EMAIL, it)
        }
    }

    fun configureSubject(intent: Intent) {
        emailSubject?.let { intent.putExtra(Intent.EXTRA_SUBJECT, it) }
    }

    fun configureMessage(intent: Intent) {
        val body = listOfNotNull(message, emailBody).filter { it.isNotEmpty() }.joinToString("\n\n")
        if (body.isNotEmpty()) intent.putExtra(Intent.EXTRA_TEXT, body)
    }

    fun configureAttachments(intent: Intent, context: Context) {
        // Drain the attachment tasks so they still run (the log zip is produced as before),
        // but do not attach: there is no FileProvider to expose the files cross-process.
        for (task in attachmentTasks) {
            try {
                task.get()
            } catch (ignored: Exception) {
            }
        }
    }
}
