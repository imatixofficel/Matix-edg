package com.v2ray.ang.handler

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.v2ray.ang.AppConfig
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

/**
 * Matixray: updates the app from inside the app. The APK of the new release is downloaded into the cache (straight, and
 * through the app's own local proxy when the straight way fails) and handed to the system installer.
 */
object ApkUpdater {
    sealed interface State {
        data object Idle : State
        data class Downloading(val percent: Int) : State
        data class Ready(val file: File) : State
        data object Failed : State
    }

    private const val MIN_APK_BYTES = 1_000_000L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()
    private var job: Job? = null

    /** True when [url] is a direct link to an APK file (a release page link is opened in the browser instead). */
    fun isApkUrl(url: String?): Boolean = url != null && url.substringBefore('?').endsWith(".apk", ignoreCase = true)

    fun reset() {
        job?.cancel()
        job = null
        _state.value = State.Idle
    }

    fun start(context: Context, url: String) {
        if (job?.isActive == true) return
        val target = File(File(context.cacheDir, "updates").apply { mkdirs() }, "matixray-update.apk")
        _state.value = State.Downloading(0)
        job = scope.launch {
            val ok = download(url, target, null) || download(url, target, Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", SettingsManager.getHttpPort())))
            _state.value = if (ok) State.Ready(target) else State.Failed
        }
    }

    private fun download(url: String, target: File, proxy: Proxy?): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            val link = URL(url)
            connection = (if (proxy != null) link.openConnection(proxy) else link.openConnection()) as HttpURLConnection
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.instanceFollowRedirects = true
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return false
            val total = connection.contentLengthLong
            var done = 0L
            var lastPercent = -1
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) {
                            val percent = (done * 100 / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                _state.value = State.Downloading(percent)
                            }
                        }
                    }
                }
            }
            // A real APK is a zip file ("PK") and far bigger than an error page.
            val isZip = target.inputStream().use { it.read() == 'P'.code && it.read() == 'K'.code }
            target.length() >= MIN_APK_BYTES && isZip
        } catch (e: Exception) {
            LogUtil.e(AppConfig.TAG, "Update download failed (proxy=${proxy != null})", e)
            false
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Opens the system installer for the downloaded [file]. Returns false when the user first has to allow installs from
     * this app; the settings page for that is opened and the install is asked for again afterwards.
     */
    fun install(context: Context, file: File): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.cache", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }
}
