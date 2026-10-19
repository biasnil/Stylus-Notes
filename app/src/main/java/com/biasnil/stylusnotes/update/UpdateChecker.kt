package com.biasnil.stylusnotes.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/** Where releases and problem reports go. */
object ProjectLinks {
    const val OWNER = "biasnil"
    const val REPO = "Stylus-Notes"
    const val GITHUB = "https://github.com/$OWNER/$REPO"
    const val RELEASES = "$GITHUB/releases"
    const val LATEST_API = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"
    const val REPORT_EMAIL = "workacckwanrong@gmail.com"
}

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tag: String,
    val name: String? = null,
    @SerialName("html_url") val url: String = ProjectLinks.RELEASES,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
)

sealed interface UpdateResult {
    data class Available(val release: GithubRelease) : UpdateResult
    data object UpToDate : UpdateResult
    /** The repository has no published release yet. */
    data object NoReleases : UpdateResult
    data class Failed(val reason: String) : UpdateResult
}

/**
 * Asks GitHub for the latest published release (GitHub's "latest" skips drafts and pre-releases)
 * and compares its tag with the installed version name.
 */
object UpdateChecker {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun check(currentVersion: String): UpdateResult = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            conn = (URL(ProjectLinks.LATEST_API).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "StylusNotes/$currentVersion")
            }
            when (val code = conn.responseCode) {
                200 -> {
                    val text = conn.inputStream.bufferedReader().use { it.readText() }
                    val release = json.decodeFromString(GithubRelease.serializer(), text)
                    if (Versions.isNewer(release.tag, currentVersion)) UpdateResult.Available(release)
                    else UpdateResult.UpToDate
                }
                404 -> UpdateResult.NoReleases
                else -> UpdateResult.Failed("HTTP $code")
            }
        } catch (e: Exception) {
            UpdateResult.Failed(e.message ?: e.javaClass.simpleName)
        } finally {
            conn?.disconnect()
        }
    }
}
