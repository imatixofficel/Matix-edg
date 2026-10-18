package com.v2ray.ang.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.v2ray.ang.AppConfig
import com.v2ray.ang.R
import com.v2ray.ang.dto.GroupMapItem

/**
 * Matixray: where configs that are added by hand go: a new group (with a name the user types) or one of the groups
 * the user already has. The free group is not offered, because "get configs" empties it.
 *
 * @param targetId the id of the chosen group; null means a new group.
 */
@Composable
internal fun ImportTargetPicker(
    groups: List<GroupMapItem>,
    targetId: String?,
    onTargetChange: (String?) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val options = groups.filter { it.id.isNotEmpty() && it.id != AppConfig.FREE_SUBSCRIPTION_ID }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.mx_import_target_title),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TargetChip(stringResource(R.string.mx_import_target_new), selected = targetId == null) { onTargetChange(null) }
            options.forEach { group ->
                TargetChip(group.remarks, selected = targetId == group.id) { onTargetChange(group.id) }
            }
        }
        if (targetId == null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.mx_import_target_empty_note),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            val fieldShape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(fieldShape)
                    .background(scheme.surfaceContainerLow)
                    .border(1.dp, scheme.outlineVariant, fieldShape)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                if (name.isEmpty()) {
                    Text(
                        text = stringResource(R.string.mx_import_name_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
                BasicTextField(
                    value = name,
                    onValueChange = onNameChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = scheme.onSurface),
                    cursorBrush = SolidColor(scheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun TargetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) scheme.primary else scheme.surfaceContainerHigh)
            .border(BorderStroke(1.dp, if (selected) scheme.primary else scheme.outlineVariant), shape)
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) scheme.onPrimary else scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
