package com.v2ray.ang.ui.main

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.res.stringResource
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.DeleteConfirmDialog

data class ServerDeleteTarget(
    val guid: String,
    val profileName: String,
) {
    companion object {
        val Saver = listSaver<ServerDeleteTarget?, String>(
            save = { target -> target?.let { listOf(it.guid, it.profileName) }.orEmpty() },
            restore = { ServerDeleteTarget(it[0], it[1]) },
        )
    }
}

@Composable
fun MainDialogs(
    showDelAllConfirm: Boolean,
    onDismissDelAll: () -> Unit,
    onConfirmDelAll: () -> Unit,
    showDelDuplicateConfirm: Boolean,
    onDismissDelDuplicate: () -> Unit,
    onConfirmDelDuplicate: () -> Unit,
    showDelInvalidConfirm: Boolean,
    onDismissDelInvalid: () -> Unit,
    onConfirmDelInvalid: () -> Unit,
    showRemoveConfirm: ServerDeleteTarget?,
    onDismissRemove: () -> Unit,
    onConfirmRemove: (String) -> Unit,
) {
    if (showDelAllConfirm) {
        DeleteConfirmDialog(
            message = stringResource(R.string.confirm_delete_visible_profiles),
            onConfirm = onConfirmDelAll,
            onDismiss = onDismissDelAll
        )
    }
    if (showDelDuplicateConfirm) {
        DeleteConfirmDialog(
            message = stringResource(R.string.confirm_delete_duplicate_profiles),
            onConfirm = onConfirmDelDuplicate,
            onDismiss = onDismissDelDuplicate
        )
    }
    if (showDelInvalidConfirm) {
        DeleteConfirmDialog(
            message = stringResource(R.string.confirm_delete_invalid_profiles),
            onConfirm = onConfirmDelInvalid,
            onDismiss = onDismissDelInvalid
        )
    }
    showRemoveConfirm?.let { target ->
        DeleteConfirmDialog(
            message = stringResource(R.string.confirm_delete_profile),
            itemName = target.profileName,
            onConfirm = { onConfirmRemove(target.guid) },
            onDismiss = onDismissRemove
        )
    }
}

/** Matixray: asks where configs the user just added go: a new group (named here) or a group they already have. */
@Composable
fun ImportNameDialog(
    groups: List<com.v2ray.ang.dto.GroupMapItem>,
    onConfirm: (name: String, targetGroupId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf<String?>(null) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { androidx.compose.material3.Text(stringResource(R.string.mx_import_name_title)) },
        text = {
            ImportTargetPicker(
                groups = groups,
                targetId = target,
                onTargetChange = { target = it },
                name = name,
                onNameChange = { name = it },
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = { onConfirm(name, target) }) {
                androidx.compose.material3.Text(stringResource(R.string.mx_import_name_ok))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text(stringResource(android.R.string.cancel))
            }
        },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    )
}

/**
 * Matixray: the update dialog. "Update now" downloads the new APK inside the app, shows the progress and then opens the
 * system installer; a link that is not a direct APK link (a release page) is opened in the browser instead.
 */
@Composable
fun InAppUpdateDialog(version: String, url: String?, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state by com.v2ray.ang.handler.ApkUpdater.state.collectAsStateWithLifecycle()
    val downloading = state is com.v2ray.ang.handler.ApkUpdater.State.Downloading
    androidx.compose.runtime.LaunchedEffect(state) {
        val ready = state as? com.v2ray.ang.handler.ApkUpdater.State.Ready ?: return@LaunchedEffect
        com.v2ray.ang.handler.ApkUpdater.install(context, ready.file)
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        title = { androidx.compose.material3.Text(stringResource(R.string.mx_update_notice_title)) },
        text = {
            androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                androidx.compose.material3.Text(stringResource(R.string.mx_update_notice_body, version))
                when (val s = state) {
                    is com.v2ray.ang.handler.ApkUpdater.State.Downloading -> {
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { s.percent / 100f },
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                        )
                        androidx.compose.material3.Text(stringResource(R.string.mx_update_downloading, s.percent))
                    }
                    is com.v2ray.ang.handler.ApkUpdater.State.Ready ->
                        androidx.compose.material3.Text(stringResource(R.string.mx_update_ready))
                    com.v2ray.ang.handler.ApkUpdater.State.Failed ->
                        androidx.compose.material3.Text(stringResource(R.string.mx_update_failed))
                    com.v2ray.ang.handler.ApkUpdater.State.Idle -> {}
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                enabled = !downloading,
                onClick = {
                    val ready = state as? com.v2ray.ang.handler.ApkUpdater.State.Ready
                    when {
                        ready != null -> com.v2ray.ang.handler.ApkUpdater.install(context, ready.file)
                        com.v2ray.ang.handler.ApkUpdater.isApkUrl(url) -> com.v2ray.ang.handler.ApkUpdater.start(context, url!!)
                        else -> {
                            url?.let { com.v2ray.ang.util.Utils.openUri(context, it) }
                            onDismiss()
                        }
                    }
                },
            ) {
                androidx.compose.material3.Text(
                    stringResource(if (state is com.v2ray.ang.handler.ApkUpdater.State.Ready) R.string.mx_update_install else R.string.update_now)
                )
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = {
                com.v2ray.ang.handler.ApkUpdater.reset()
                onDismiss()
            }) { androidx.compose.material3.Text(stringResource(R.string.mx_update_later)) }
        },
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    )
}
