package pl.brightinventions.slf4android

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import java.io.File


fun showLogSharingPrompt(
    activityContext: Context,
    message: String?,
    emailAddresses: List<String?>?,
    emailSubject: String?,
    emailBody: String?,
    attachmentTasks: Iterable<AsyncTask<Context, Void, File>>
) {
    val emailErrorReport = EmailErrorReport(
        message, emailAddresses, emailSubject, emailBody
    )
    for (attachment in attachmentTasks) {
        attachment.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, activityContext)
        emailErrorReport.addFileAttachmentFrom(attachment)
    }

    sendEmailWithError(
        activityContext,
        emailErrorReport
    )
}


private fun sendEmailWithError(
    activityContext: Context,
    emailErrorReport: EmailErrorReport
) {
    val sendEmail = Intent(Intent.ACTION_SEND_MULTIPLE)
    sendEmail.type = "message/rfc822"
    emailErrorReport.configureRecipients(sendEmail)
    emailErrorReport.configureSubject(sendEmail)
    emailErrorReport.configureMessage(sendEmail)
    emailErrorReport.configureAttachments(sendEmail, activityContext)
    activityContext.startActivity(Intent.createChooser(sendEmail, "Send logs..."))
}
