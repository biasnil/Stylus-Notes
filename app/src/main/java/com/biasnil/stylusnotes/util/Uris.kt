package com.biasnil.stylusnotes.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

/** The file name the user sees for a picked document, if the provider tells us. */
fun displayName(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    }
}.getOrNull()
