package dev.nheox.mintal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.nheox.mintal.data.db.entity.TagEntity
import dev.nheox.mintal.data.repository.TagRepository

/**
 * Choose a tag, or make one on the spot. Shared by the hub, before a session, and the
 * running screen, during one — two copies of a picker drift apart one button at a time.
 *
 * [onManageTags] is null where leaving for the tag manager would be wrong: in the middle
 * of a session, navigating away from the running screen is not something a picker should
 * offer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagPickerDialog(
    tags: List<TagEntity>,
    onPick: (Long?) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
    onManageTags: (() -> Unit)? = null,
) {
    var newTagName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a tag") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TextButton(onClick = { onPick(null) }) { Text("No tag") }
                tags.forEach { tag ->
                    TextButton(onClick = { onPick(tag.id) }) { Text(tag.name) }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                OutlinedTextField(
                    value = newTagName,
                    onValueChange = {
                        if (it.length <= TagRepository.MAX_NAME_LENGTH) newTagName = it
                    },
                    label = { Text("New tag") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        enabled = newTagName.isNotBlank(),
                        onClick = {
                            onCreate(newTagName)
                            newTagName = ""
                        },
                    ) { Text("Create and use") }
                    if (onManageTags != null) {
                        TextButton(onClick = onManageTags) { Text("Manage tags") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
