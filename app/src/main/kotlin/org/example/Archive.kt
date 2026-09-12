package com.eitan.trainer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val archiveListSerializer = kotlinx.serialization.builtins.ListSerializer(ArchiveExercise.serializer())

object ExerciseImages {
    private const val MAX_DIMENSION = 1280

    private fun dir(context: Context) = File(context.filesDir, "exercise-images").apply { mkdirs() }

    fun file(context: Context, name: String) = File(dir(context), name)

    fun import(context: Context, uri: Uri): String? =
        context.contentResolver.openInputStream(uri)
            ?.use { BitmapFactory.decodeStream(it) }
            ?.let { save(context, it) }

    fun import(context: Context, bytes: ByteArray): String? =
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { save(context, it) }

    // Downscaled on import so decoding stays cheap for list thumbnails.
    private fun save(context: Context, bitmap: Bitmap): String {
        val scale = MAX_DIMENSION.toFloat() / maxOf(bitmap.width, bitmap.height)
        val scaled =
            if (scale >= 1) bitmap
            else Bitmap.createScaledBitmap(
                bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true
            )
        val name = "${UUID.randomUUID()}.jpg"
        file(context, name).outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return name
    }

    fun delete(context: Context, name: String) {
        file(context, name).delete()
    }
}

@Composable
fun ExerciseImage(fileName: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Fit) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, fileName) {
        value = withContext(Dispatchers.IO) {
            BitmapFactory.decodeFile(ExerciseImages.file(context, fileName).path)?.asImageBitmap()
        }
    }
    bitmap?.let {
        Image(it, contentDescription = null, contentScale = contentScale, modifier = modifier)
    }
}

@Composable
fun VideoLink(url: String) {
    val context = LocalContext.current
    TextButton(onClick = {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }) { Text("Watch video") }
}

fun exportArchive(archive: List<ArchiveExercise>): String =
    transferJson.encodeToString(archiveListSerializer, archive)

fun importArchive(text: String, materialize: (ArchiveExercise) -> ArchiveExercise = { it }) {
    val entries = runCatching {
        transferJson.decodeFromString(archiveListSerializer, text)
    }.getOrNull() ?: return
    val existing = AppRepository.state.value.archive.map { it.name.lowercase() }.toSet()
    entries
        .distinctBy { it.name.lowercase() }
        .filter { it.name.lowercase() !in existing }
        .forEach { AppRepository.addArchiveExercise(materialize(it)) }
}

// Turns a transfer entry's embedded base64 picture into a local image file.
fun ArchiveExercise.materialized(context: Context): ArchiveExercise {
    val bytes = imageBase64?.let {
        runCatching { java.util.Base64.getDecoder().decode(it) }.getOrNull()
    }
    return copy(
        imageFile = bytes?.let { ExerciseImages.import(context, it) } ?: imageFile,
        imageBase64 = null,
    )
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
    val context = LocalContext.current
    var isAdding by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
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
            TopBarIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onClick = onBack)
            Text(
                "Archive",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            TopBarButton("Import") { isImporting = true }
            TopBarButton("Export") { isExporting = true }
            TopBarIconButton(Icons.Filled.Add, "New archive exercise") { isAdding = true }
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
                    leadingContent = archived.imageFile?.let { file ->
                        { ExerciseImage(file, Modifier.size(48.dp), ContentScale.Crop) }
                    },
                    trailingContent = {
                        TextButton(onClick = {
                            archived.imageFile?.let { ExerciseImages.delete(context, it) }
                            AppRepository.removeArchiveExercise(archived.id)
                        }) {
                            Text("Remove")
                        }
                    },
                    modifier = Modifier.clickable { editingId = archived.id }
                )
            }
        }
    }

    if (isAdding) {
        ArchiveExerciseDialog(existing = null, onDismiss = { isAdding = false })
    }
    archive.find { it.id == editingId }?.let { archived ->
        ArchiveExerciseDialog(existing = archived, onDismiss = { editingId = null })
    }
    if (isImporting) {
        ImportTextDialog(
            title = "Import archive",
            hint = "Paste exported JSON",
            onImport = { text -> importArchive(text) { it.materialized(context) } },
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
fun ArchiveExerciseDialog(existing: ArchiveExercise?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var videoUrl by remember { mutableStateOf(existing?.videoUrl ?: "") }
    var imageFile by remember { mutableStateOf(existing?.imageFile) }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pickedUri = uri
    }
    val hasPicture = pickedUri != null || imageFile != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New archive exercise" else "Edit archive exercise") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                TextField(
                    value = videoUrl,
                    onValueChange = { videoUrl = it },
                    label = { Text("Video link") }
                )
                Row {
                    TextButton(onClick = {
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text(if (hasPicture) "Change picture" else "Pick picture") }
                    if (hasPicture) {
                        TextButton(onClick = {
                            pickedUri = null
                            imageFile = null
                        }) { Text("Remove picture") }
                    }
                }
                if (pickedUri == null) {
                    imageFile?.let { ExerciseImage(it, Modifier.size(96.dp), ContentScale.Crop) }
                } else {
                    Text("New picture selected")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val newImage = pickedUri?.let { ExerciseImages.import(context, it) } ?: imageFile
                if (existing == null) {
                    AppRepository.addArchiveExercise(
                        ArchiveExercise(
                            name = name,
                            description = description.ifBlank { null },
                            videoUrl = videoUrl.ifBlank { null },
                            imageFile = newImage,
                        )
                    )
                } else {
                    AppRepository.updateArchiveExercise(existing.id) {
                        it.copy(
                            name = name,
                            description = description.ifBlank { null },
                            videoUrl = videoUrl.ifBlank { null },
                            imageFile = newImage,
                        )
                    }
                    if (existing.imageFile != null && existing.imageFile != newImage) {
                        ExerciseImages.delete(context, existing.imageFile)
                    }
                }
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
