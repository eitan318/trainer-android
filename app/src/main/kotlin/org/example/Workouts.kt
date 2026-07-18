package com.eitan.trainer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun WorkoutListScreen(
    workouts: List<Workout>,
    onWorkoutClick: (String) -> Unit,
    onNewWorkout: () -> Unit,
    onOpenArchive: () -> Unit,
    onStartWorkout: (String) -> Unit,
) {
    var isImporting by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var confirmingRemoveId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Workouts",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )
            TextButton(onClick = { isImporting = true }) { Text("Import") }
            TextButton(onClick = { isExporting = true }) { Text("Export") }
            TextButton(onClick = onOpenArchive) { Text("Archive") }
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(workouts) { workout ->
                ListItem(
                    headlineContent = { Text(workout.name) },
                    supportingContent = {
                        Column {
                            workout.description?.let { Text(it) }
                            if (workout.sections.isNotEmpty()) {
                                Text(
                                    workout.sections.joinToString(" · ") { it.name },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    trailingContent = {
                        Row {
                            StartWorkoutIcon(workout.id) { onStartWorkout(workout.id) }
                            IconButton(onClick = { confirmingRemoveId = workout.id }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Remove workout")
                            }
                        }
                    },
                    modifier = Modifier.clickable { onWorkoutClick(workout.id) }
                )
            }
            item { AddButton("New workout", onClick = onNewWorkout) }
        }
    }

    if (isImporting) {
        ImportTextDialog(
            title = "Import workouts",
            hint = "Paste exported JSON",
            onImport = ::importWorkouts,
            onDismiss = { isImporting = false }
        )
    }
    if (isExporting) {
        WorkoutsExportDialog(workouts = workouts, onDismiss = { isExporting = false })
    }
    workouts.find { it.id == confirmingRemoveId }?.let { workout ->
        AlertDialog(
            onDismissRequest = { confirmingRemoveId = null },
            title = { Text("Remove ${workout.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    AppRepository.removeWorkout(workout.id)
                    confirmingRemoveId = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmingRemoveId = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun WorkoutsExportDialog(workouts: List<Workout>, onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf(emptySet<String>()) }
    var showText by remember { mutableStateOf(false) }

    if (showText) {
        ExportTextDialog(
            title = "Export workouts",
            text = exportWorkouts(
                workouts.filter { it.id in selected },
                AppRepository.state.value.archive,
            ),
            onDismiss = onDismiss
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export workouts") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                workouts.forEach { workout ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = if (workout.id in selected) selected - workout.id else selected + workout.id
                            }
                    ) {
                        Checkbox(
                            checked = workout.id in selected,
                            onCheckedChange = null
                        )
                        Text(workout.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected.isNotEmpty(),
                onClick = { showText = true }
            ) { Text("Export") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddButton(contentDescription: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(Icons.Filled.Add, contentDescription = contentDescription)
        }
    }
}

// Play arrow, or two "running" bars when this workout's session is active.
@Composable
fun StartWorkoutIcon(workoutId: String, onStart: () -> Unit) {
    val session by FlowSession.state.collectAsStateWithLifecycle()
    IconButton(onClick = onStart) {
        if (session?.workoutId == workoutId) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .width(5.dp)
                            .height(18.dp)
                            .background(LocalContentColor.current)
                    )
                }
            }
        } else {
            Icon(Icons.Filled.PlayArrow, contentDescription = "Start workout")
        }
    }
}

fun fmtSecs(secs: Int) = if (secs % 60 == 0) "${secs / 60}m" else "${secs}s"

fun fmtWeight(weightKg: Double) =
    if (weightKg % 1.0 == 0.0) weightKg.toInt().toString() else weightKg.toString()


class WorkoutState(private val workout: Workout) {
    var name by mutableStateOf(workout.name)
    var description by mutableStateOf(workout.description ?: "")
    var sectionRestSecs by mutableStateOf(workout.sectionRestSecs?.toString() ?: "")

    fun toWorkout() = workout.copy(
        name = name,
        description = description.ifBlank { null },
        sectionRestSecs = sectionRestSecs.toIntOrNull(),
    )
}

@Composable
fun NullableIntField(label: String, value: Int?, onValueChange: (Int?) -> Unit) {
    TextField(
        value = value?.toString() ?: "",
        onValueChange = { onValueChange(it.toIntOrNull()) },
        label = { Text(label) }
    )
}

@Composable
fun WorkoutScreen(workout: Workout, onBack: () -> Unit, onStart: () -> Unit) {
    var isEditing by remember { mutableStateOf(false) }

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
                workout.name,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .weight(1f)
                    .clickable { isEditing = true }
            )
            StartWorkoutIcon(workout.id, onStart = onStart)
            IconButton(onClick = {
                AppRepository.addSection(workout.id, WorkoutSection(name = "New section"))
            }) {
                Icon(Icons.Filled.Add, contentDescription = "New section")
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(workout.sections) { section ->
                SectionCard(workout = workout, section = section)
            }
        }
    }

    if (isEditing) {
        WorkoutEditDialog(workout = workout, onDismiss = { isEditing = false })
    }
}

@Composable
fun WorkoutEditDialog(workout: Workout, onDismiss: () -> Unit) {
    val form = remember { WorkoutState(workout) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit workout") },
        text = {
            Column {
                TextField(
                    value = form.name,
                    onValueChange = { form.name = it },
                    label = { Text("Name") }
                )
                TextField(
                    value = form.description,
                    onValueChange = { form.description = it },
                    label = { Text("Description") }
                )
                TextField(
                    value = form.sectionRestSecs,
                    onValueChange = { form.sectionRestSecs = it },
                    label = { Text("Section rest (secs)") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                AppRepository.updateWorkout(workout.id) { form.toWorkout() }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SectionCard(workout: Workout, section: WorkoutSection) {
    val appData by AppRepository.state.collectAsStateWithLifecycle()
    var isEditing by remember { mutableStateOf(false) }
    var isPicking by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                section.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .clickable { isEditing = true }
                    .padding(8.dp)
            )
            Text(
                "${fmtSecs(section.restAfterSecs ?: workout.sectionRestSecs ?: appData.defaults.sectionRestSecs)} after",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { isPicking = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New exercise")
            }
        }
        section.description?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        ExerciseTable(workoutId = workout.id, section = section, appData = appData)
    }

    if (isEditing) {
        SectionEditDialog(
            workoutId = workout.id,
            section = section,
            onDismiss = { isEditing = false }
        )
    }
    if (isPicking) {
        ArchivePickerDialog(
            onPick = { archived ->
                AppRepository.addExercize(
                    workout.id, section.id,
                    WorkoutExercise(archiveExerciseId = archived.id)
                )
                isPicking = false
            },
            onDismiss = { isPicking = false }
        )
    }
}

@Composable
fun SectionEditDialog(workoutId: String, section: WorkoutSection, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(section.name) }
    var description by remember { mutableStateOf(section.description ?: "") }
    var setRestSecs by remember { mutableStateOf(section.setRestSecs) }
    var exerciseRestSecs by remember { mutableStateOf(section.exerciseRestSecs) }
    var restAfterSecs by remember { mutableStateOf(section.restAfterSecs) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit section") },
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
                NullableIntField("Default set rest (s)", setRestSecs) { setRestSecs = it }
                NullableIntField("Default exercise rest (s)", exerciseRestSecs) { exerciseRestSecs = it }
                NullableIntField("Rest after section (s)", restAfterSecs) { restAfterSecs = it }
                TextButton(onClick = {
                    AppRepository.removeSection(workoutId, section.id)
                    onDismiss()
                }) { Text("Remove section") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                AppRepository.updateSection(workoutId, section.id) {
                    it.copy(
                        name = name,
                        description = description.ifBlank { null },
                        setRestSecs = setRestSecs,
                        exerciseRestSecs = exerciseRestSecs,
                        restAfterSecs = restAfterSecs,
                    )
                }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ExerciseTable(workoutId: String, section: WorkoutSection, appData: AppData) {
    val defaults = appData.defaults
    var editingExerciseId by remember { mutableStateOf<String?>(null) }
    var infoArchiveId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            val style = MaterialTheme.typography.labelSmall
            Text("name", style = style, modifier = Modifier.weight(2f))
            Text("sets", style = style, modifier = Modifier.weight(0.8f))
            Text("reps", style = style, modifier = Modifier.weight(0.8f))
            Text("weight", style = style, modifier = Modifier.weight(1f))
            Text("set-rest", style = style, modifier = Modifier.weight(1f))
            Text("ex-rest", style = style, modifier = Modifier.weight(1f))
            Text("info", style = style, modifier = Modifier.weight(0.6f))
        }
        section.exercises.forEach { exercise ->
            val archived = appData.archive.firstOrNull { it.id == exercise.archiveExerciseId }
            val effort = exercise.effort ?: defaults.effort
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { editingExerciseId = exercise.id }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(archived?.name ?: "Unknown", modifier = Modifier.weight(2f))
                Text("${exercise.sets ?: defaults.sets}", modifier = Modifier.weight(0.8f))
                Text(if (effort.isDuration) fmtSecs(effort.amount) else "${effort.amount}", modifier = Modifier.weight(0.8f))
                Text(fmtWeight(exercise.weightKg ?: defaults.weightKg), modifier = Modifier.weight(1f))
                Text(
                    fmtSecs(exercise.setRestSecs ?: section.setRestSecs ?: defaults.setRestSecs),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    fmtSecs(exercise.restAfterSecs ?: section.exerciseRestSecs ?: defaults.exerciseRestSecs),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "?",
                    modifier = Modifier
                        .weight(0.6f)
                        .clickable { infoArchiveId = exercise.archiveExerciseId }
                )
            }
        }
    }

    section.exercises.find { it.id == editingExerciseId }?.let { exercise ->
        ExerciseEditDialog(
            workoutId = workoutId,
            sectionId = section.id,
            exercise = exercise,
            onDismiss = { editingExerciseId = null }
        )
    }
    appData.archive.firstOrNull { it.id == infoArchiveId }?.let { archived ->
        AlertDialog(
            onDismissRequest = { infoArchiveId = null },
            title = { Text(archived.name) },
            text = { Text(archived.description ?: "No description") },
            confirmButton = { TextButton(onClick = { infoArchiveId = null }) { Text("Close") } }
        )
    }
}

@Composable
fun ArchivePickerDialog(onPick: (ArchiveExercise) -> Unit, onDismiss: () -> Unit) {
    val appData by AppRepository.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose exercise") },
        text = {
            if (appData.archive.isEmpty()) {
                Text("Archive is empty. Add exercises on the archive page first.")
            } else {
                Column {
                    SearchField(query) { query = it }
                    LazyColumn {
                        items(appData.archive.matching(query)) { archived ->
                            ListItem(
                                headlineContent = { Text(archived.name) },
                                supportingContent = { archived.description?.let { Text(it) } },
                                modifier = Modifier.clickable { onPick(archived) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ExerciseEditDialog(
    workoutId: String,
    sectionId: String,
    exercise: WorkoutExercise,
    onDismiss: () -> Unit,
) {
    var weightKg by remember { mutableStateOf(exercise.weightKg?.let(::fmtWeight) ?: "") }
    var effortAmount by remember { mutableStateOf(exercise.effort?.amount) }
    var isDuration by remember { mutableStateOf(exercise.effort?.isDuration ?: false) }
    var sets by remember { mutableStateOf(exercise.sets) }
    var setRestSecs by remember { mutableStateOf(exercise.setRestSecs) }
    var restAfterSecs by remember { mutableStateOf(exercise.restAfterSecs) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit exercise") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                TextField(
                    value = weightKg,
                    onValueChange = { weightKg = it },
                    label = { Text("Weight (kg)") }
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NullableIntField(
                        if (isDuration) "Duration (s)" else "Reps",
                        effortAmount
                    ) { effortAmount = it }
                    Switch(checked = isDuration, onCheckedChange = { isDuration = it })
                }
                NullableIntField("Sets", sets) { sets = it }
                NullableIntField("Set rest (s)", setRestSecs) { setRestSecs = it }
                NullableIntField("Exercise rest (s)", restAfterSecs) { restAfterSecs = it }
                TextButton(onClick = {
                    AppRepository.removeExercise(workoutId, sectionId, exercise.id)
                    onDismiss()
                }) { Text("Remove exercise") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                AppRepository.updateExercise(workoutId, sectionId, exercise.id) {
                    it.copy(
                        weightKg = weightKg.toDoubleOrNull(),
                        effort = effortAmount?.let { amount -> Effort(amount, isDuration) },
                        sets = sets,
                        setRestSecs = setRestSecs,
                        restAfterSecs = restAfterSecs,
                    )
                }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
