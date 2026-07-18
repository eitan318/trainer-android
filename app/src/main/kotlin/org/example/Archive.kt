package com.eitan.trainer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

private val archiveListSerializer = kotlinx.serialization.builtins.ListSerializer(ArchiveExercise.serializer())

fun exportArchive(archive: List<ArchiveExercise>): String =
    transferJson.encodeToString(archiveListSerializer, archive)

fun importArchive(text: String) {
    val entries = runCatching {
        transferJson.decodeFromString(archiveListSerializer, text)
    }.getOrNull() ?: return
    val existing = AppRepository.state.value.archive.map { it.name.lowercase() }.toSet()
    entries
        .distinctBy { it.name.lowercase() }
        .filter { it.name.lowercase() !in existing }
        .forEach { AppRepository.addArchiveExercise(it) }
}

fun List<ArchiveExercise>.matching(query: String) = filter {
    it.name.contains(query, ignoreCase = true) ||
        it.description?.contains(query, ignoreCase = true) == true
}

@Composable
fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Search") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    )
}

@Composable
fun ArchiveScreen(archive: List<ArchiveExercise>, onBack: () -> Unit) {
    var isAdding by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Archive",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { isImporting = true }) { Text("Import") }
            TextButton(onClick = { isExporting = true }) { Text("Export") }
            IconButton(onClick = { isAdding = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New archive exercise")
            }
        }
        SearchField(query) { query = it }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(archive.matching(query)) { archived ->
                ListItem(
                    headlineContent = { Text(archived.name) },
                    supportingContent = { archived.description?.let { Text(it) } },
                    trailingContent = {
                        TextButton(onClick = { AppRepository.removeArchiveExercise(archived.id) }) {
                            Text("Remove")
                        }
                    }
                )
            }
        }
    }

    if (isAdding) {
        ArchiveAddDialog(onDismiss = { isAdding = false })
    }
    if (isImporting) {
        ImportTextDialog(
            title = "Import archive",
            hint = "Paste exported JSON",
            onImport = ::importArchive,
            onDismiss = { isImporting = false }
        )
    }
    if (isExporting) {
        ExportTextDialog(
            title = "Export archive",
            text = exportArchive(archive),
            onDismiss = { isExporting = false }
        )
    }
}

@Composable
fun ArchiveAddDialog(onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New archive exercise") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") }
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                AppRepository.addArchiveExercise(
                    ArchiveExercise(name = name, description = description.ifBlank { null })
                )
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ImportTextDialog(title: String, hint: String, onImport: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(hint) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onImport(text)
                onDismiss()
            }) { Text("Import") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ExportTextDialog(title: String, text: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TextField(
                value = text,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(text))
                onDismiss()
            }) { Text("Copy") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
