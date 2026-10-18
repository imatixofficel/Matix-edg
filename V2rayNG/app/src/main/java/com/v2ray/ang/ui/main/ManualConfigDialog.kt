package com.v2ray.ang.ui.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.MatixButtonBrush
import com.v2ray.ang.ui.compose.MatixCutShape
import com.v2ray.ang.ui.compose.onMatixButton

/** A link of a config or a subscription: a scheme, "://" and the rest up to the next space or line. */
private val configLink = Regex("""[A-Za-z][A-Za-z0-9+.\-]{1,15}://\S+""")

/**
 * Matixray: the box that opens when the user adds configs by hand. It takes any number of links at once, one per line
 * (vmess, vless, trojan, ss, hysteria2, a subscription link and so on), counts them while the user types or pastes,
 * and hands the text to [onSubmit]; [onSubmit] answers through its callback with the number it really added, and the
 * box closes on a success or says so when nothing in the text was a valid config.
 */
@Composable
fun ManualConfigDialog(
    onDismiss: () -> Unit,
    groups: List<com.v2ray.ang.dto.GroupMapItem>,
    onSubmit: (text: String, groupName: String, targetGroupId: String?, done: (added: Int) -> Unit) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var text by rememberSaveable { mutableStateOf("") }
    var groupName by rememberSaveable { mutableStateOf("") }
    var targetId by rememberSaveable { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    val found = remember(text) { configLink.findAll(text).count() }
    val panel = RoundedCornerShape(20.dp)
    val fieldShape = RoundedCornerShape(14.dp)

    Dialog(
        onDismissRequest = { if (!working) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(panel)
                .background(scheme.surface)
                .border(1.dp, scheme.outlineVariant, panel)
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(scheme.onSurface.copy(alpha = 0.07f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mx_edit_24dp),
                        contentDescription = null,
                        tint = scheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.mx_add_manual_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.mx_add_manual_hint),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            // The text box: links are long, so it is a plain monospace field that scrolls instead of growing for ever.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp, max = 240.dp)
                    .clip(fieldShape)
                    .background(scheme.surfaceContainerLow)
                    .border(1.dp, if (failed) scheme.error else scheme.outlineVariant, fieldShape)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = stringResource(R.string.mx_add_manual_placeholder),
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                        color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        failed = false
                    },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = scheme.onSurface,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Left,
                    ),
                    cursorBrush = SolidColor(scheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(10.dp))

            // Matixray: the configs go to a new group (named here) or to a group the user already has.
            ImportTargetPicker(
                groups = groups,
                targetId = targetId,
                onTargetChange = { targetId = it; failed = false },
                name = groupName,
                onNameChange = { groupName = it },
            )
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        failed -> stringResource(R.string.mx_add_manual_invalid)
                        found > 0 -> stringResource(R.string.mx_add_manual_detected, found)
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (failed) scheme.error else scheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                )
                Spacer(Modifier.width(8.dp))
                val chip = RoundedCornerShape(50)
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(chip)
                        .border(1.dp, scheme.outlineVariant, chip)
                        .clickable(role = Role.Button) {
                            val pasted = clipboard.getText()?.text.orEmpty()
                            if (pasted.isBlank()) {
                                Toast.makeText(context, R.string.mx_add_manual_clipboard_empty, Toast.LENGTH_SHORT).show()
                            } else {
                                text = if (text.isBlank()) pasted else text.trimEnd() + "\n" + pasted
                                failed = false
                            }
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mx_paste_24dp),
                        contentDescription = null,
                        tint = scheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.mx_add_manual_paste),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurface,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            ) {
                Text(
                    text = stringResource(R.string.mx_add_manual_cancel),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(enabled = !working, role = Role.Button, onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
                val ready = text.isNotBlank() && !working
                Box(
                    modifier = Modifier
                        .clip(MatixCutShape)
                        .background(MatixButtonBrush)
                        .clickable(enabled = ready, role = Role.Button) {
                            working = true
                            failed = false
                            onSubmit(text, groupName, targetId) { added ->
                                working = false
                                if (added > 0) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.mx_add_manual_added, added),
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    onDismiss()
                                } else {
                                    failed = true
                                }
                            }
                        }
                        .padding(horizontal = 28.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (working) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = onMatixButton,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.mx_add_manual_add),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = onMatixButton.copy(alpha = if (ready) 1f else 0.45f),
                        )
                    }
                }
            }
        }
    }
}
