package com.biasnil.stylusnotes.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.biasnil.stylusnotes.R
import com.biasnil.stylusnotes.StylusNotesApp
import com.biasnil.stylusnotes.update.GithubRelease
import com.biasnil.stylusnotes.update.ProjectLinks

fun openLink(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        (context.applicationContext as StylusNotesApp).message(R.string.no_browser)
    }
}

/** Opens the email app with a problem report addressed to the developer. */
fun sendReport(context: Context, version: String) {
    val subject = context.getString(R.string.report_subject, version)
    val body = context.getString(
        R.string.report_body,
        version,
        "${Build.MANUFACTURER} ${Build.MODEL}",
        "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
    )
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(ProjectLinks.REPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        (context.applicationContext as StylusNotesApp).message(R.string.no_email_app, ProjectLinks.REPORT_EMAIL)
    }
}

@Composable
fun UpdateDialog(release: GithubRelease, current: String, onLater: () -> Unit, onSkip: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.update_available_title, release.tag)) },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.update_available_detail, current, release.tag))
                val notes = release.body?.trim().orEmpty()
                if (notes.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.update_whats_new), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(notes, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                openLink(context, release.url)
                onLater()
            }) { Text(stringResource(R.string.update_download)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onLater) { Text(stringResource(R.string.update_later)) }
                TextButton(onClick = onSkip) { Text(stringResource(R.string.update_skip)) }
            }
        },
    )
}
