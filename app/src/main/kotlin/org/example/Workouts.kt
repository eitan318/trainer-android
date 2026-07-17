package com.eitan.trainer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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

@Composable
fun WorkoutListScreen(
    workouts: List<Workout>,
    onWorkoutClick: (String) -> Unit,
    onNewWorkout: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(workouts) { workout ->
                ListItem(
                    headlineContent = { Text(workout.name) },
                    supportingContent = { Text("Sections: ${workout.sections.size}") },
                    modifier = Modifier.clickable { onWorkoutClick(workout.id) }
                )
            }
            item { AddButton("New workout", onClick = onNewWorkout) }
        }
    }
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
fun WorkoutScreen(workout: Workout, onBack: () -> Unit) {
    var isEditing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        Text(workout.name, style = MaterialTheme.typography.headlineMedium)
        workout.description?.let { Text(it) }
        Text("Section rest: ${workout.sectionRestSecs?.let { "${it}s" } ?: "default"}")
        Button(onClick = { isEditing = true }) { Text("Edit") }

        SectionsList(
            workoutId = workout.id,
            sections = workout.sections,
            modifier = Modifier.weight(1f),
            onNewSection = {
                val section = WorkoutSection(
                    name = "New section",
                    exercises = emptyList(),
                    restAfterSecs = null,
                    exerciseRestSecs = null
                )

                AppRepository.addSection(workout.id, section)
            },
        )
    }

    if (isEditing) {
        WorkoutEditDialog(
            workout = workout,
            onDismiss = { isEditing = false },
            onRemove = {
                AppRepository.removeWorkout(workout.id)
                onBack()
            }
        )
    }
}

@Composable
fun WorkoutEditDialog(workout: Workout, onDismiss: () -> Unit, onRemove: () -> Unit) {
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
                TextButton(onClick = onRemove) { Text("Remove workout") }
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
fun SectionsList(
    workoutId: String,
    sections: List<WorkoutSection>,
    onNewSection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingSectionId by remember { mutableStateOf<String?>(null) }

    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(sections) { section ->
            ListItem(
                headlineContent = { Text(section.name) },
                trailingContent = {
                    TextButton(onClick = { editingSectionId = section.id }) { Text("Edit") }
                },
                supportingContent = { ExerciesesList(workoutId, section.id, section.exercises) }
            )
        }
        item { AddButton("New section", onClick = onNewSection) }
    }

    sections.find { it.id == editingSectionId }?.let { section ->
        SectionEditDialog(
            workoutId = workoutId,
            section = section,
            onDismiss = { editingSectionId = null }
        )
    }
}

@Composable
fun SectionEditDialog(workoutId: String, section: WorkoutSection, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(section.name) }
    var exerciseRestSecs by remember { mutableStateOf(section.exerciseRestSecs) }
    var restAfterSecs by remember { mutableStateOf(section.restAfterSecs) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit section") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") }
                )
                NullableIntField("Rest between exercises (s)", exerciseRestSecs) { exerciseRestSecs = it }
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
                    it.copy(name = name, exerciseRestSecs = exerciseRestSecs, restAfterSecs = restAfterSecs)
                }
                onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ExerciesesList(workoutId: String, sectionId: String, exercises: List<WorkoutExercise>) {
    var editingExerciseId by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        exercises.forEach { exercise ->
            Text(
                AppRepository.archiveExercise(exercise.archiveExerciseId)?.name ?: "Unknown",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { editingExerciseId = exercise.id }
            )
        }
        AddButton("New exercise") {
            val archived = ArchiveExercise(name = "New exercise", description = null)
            AppRepository.addArchiveExercise(archived)
            AppRepository.addExercize(
                workoutId, sectionId,
                WorkoutExercise(
                    archiveExerciseId = archived.id,
                    lastVolume = 0,
                    weightKg = null,
                    effort = null,
                    sets = null,
                    setRestSecs = null,
                    restAfterSecs = null,
                )
            )
        }
    }

    exercises.find { it.id == editingExerciseId }?.let { exercise ->
        ExerciseEditDialog(
            workoutId = workoutId,
            sectionId = sectionId,
            exercise = exercise,
            onDismiss = { editingExerciseId = null }
        )
    }
}

@Composable
fun ExerciseEditDialog(
    workoutId: String,
    sectionId: String,
    exercise: WorkoutExercise,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(AppRepository.archiveExercise(exercise.archiveExerciseId)?.name ?: "") }
    var weightKg by remember { mutableStateOf(exercise.weightKg) }
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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") }
                )
                NullableIntField("Weight (kg)", weightKg) { weightKg = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NullableIntField(
                        if (isDuration) "Duration (s)" else "Reps",
                        effortAmount
                    ) { effortAmount = it }
                    Switch(checked = isDuration, onCheckedChange = { isDuration = it })
                }
                NullableIntField("Sets", sets) { sets = it }
                NullableIntField("Set rest (s)", setRestSecs) { setRestSecs = it }
                NullableIntField("Rest after (s)", restAfterSecs) { restAfterSecs = it }
                TextButton(onClick = {
                    AppRepository.removeExercise(workoutId, sectionId, exercise.id)
                    onDismiss()
                }) { Text("Remove exercise") }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                AppRepository.updateArchiveExercise(exercise.archiveExerciseId) { it.copy(name = name) }
                AppRepository.updateExercise(workoutId, sectionId, exercise.id) {
                    it.copy(
                        weightKg = weightKg,
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
