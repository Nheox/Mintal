package dev.nheox.mintal.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.nheox.mintal.core.time.MintalClock
import dev.nheox.mintal.data.db.entity.TagEntity
import dev.nheox.mintal.data.repository.TagRepository
import dev.nheox.mintal.ui.components.MintalTopBar
import dev.nheox.mintal.ui.theme.mintalPalette
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    private val clock: MintalClock,
) : ViewModel() {

    val tags: StateFlow<List<TagEntity>> = tagRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, colourIndex: Int) = viewModelScope.launch {
        tagRepository.create(name, colourIndex, clock.nowMillis())
    }

    fun rename(tag: TagEntity, name: String) = viewModelScope.launch {
        tagRepository.rename(tag, name)
    }

    fun recolour(tag: TagEntity, colourIndex: Int) = viewModelScope.launch {
        tagRepository.recolour(tag, colourIndex)
    }

    fun delete(tag: TagEntity) = viewModelScope.launch { tagRepository.delete(tag) }
}

/** Tag manager (spec §5.8). Deleting a tag untags its sessions; it never deletes them. */
@Composable
fun TagsScreen(onBack: () -> Unit) {
    val viewModel: TagsViewModel = hiltViewModel()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val palette = mintalPalette
    var editing by remember { mutableStateOf<TagEntity?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { MintalTopBar(title = "Tags", onBack = onBack) },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Create a tag")
            }
        },
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(items = tags, key = { it.id }) { tag ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editing = tag }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(palette.tagColour(tag.colorIndex), CircleShape),
                    )
                    Text(
                        text = tag.name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Edit",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider()
            }
        }
    }

    if (creating) {
        TagEditorDialog(
            initialName = "",
            initialColour = 0,
            title = "New tag",
            onDismiss = { creating = false },
            onSave = { name, colour ->
                viewModel.create(name, colour)
                creating = false
            },
        )
    }

    editing?.let { tag ->
        TagEditorDialog(
            initialName = tag.name,
            initialColour = tag.colorIndex,
            title = "Edit tag",
            onDismiss = { editing = null },
            onSave = { name, colour ->
                viewModel.rename(tag, name)
                viewModel.recolour(tag, colour)
                editing = null
            },
            onDelete = {
                viewModel.delete(tag)
                editing = null
            },
        )
    }
}

@Composable
private fun TagEditorDialog(
    initialName: String,
    initialColour: Int,
    title: String,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val palette = mintalPalette
    var name by remember { mutableStateOf(initialName) }
    var colour by remember { mutableIntStateOf(initialColour) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= TagRepository.MAX_NAME_LENGTH) name = it },
                    label = { Text("Name") },
                    supportingText = {
                        Text("${name.length}/${TagRepository.MAX_NAME_LENGTH}")
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    (0 until 6).forEach { index ->
                        ColourSwatch(
                            colourIndex = index,
                            selected = colour == index,
                            onClick = { colour = index },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    (6 until 12).forEach { index ->
                        ColourSwatch(
                            colourIndex = index,
                            selected = colour == index,
                            onClick = { colour = index },
                        )
                    }
                }
                if (onDelete != null) {
                    TextButton(onClick = onDelete, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Delete tag")
                    }
                    Text(
                        text = "Its sessions stay, untagged.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, colour) },
                enabled = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ColourSwatch(colourIndex: Int, selected: Boolean, onClick: () -> Unit) {
    val palette = mintalPalette
    Box(
        modifier = Modifier
            .size(if (selected) 34.dp else 28.dp)
            .background(palette.tagColour(colourIndex), CircleShape)
            .clickable(onClick = onClick),
    )
}
