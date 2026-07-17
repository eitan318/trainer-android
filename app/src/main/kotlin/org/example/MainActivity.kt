package com.eitan.trainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                App()
            }
        }
    }
}

@Composable
fun App() {
    val appData by AppRepository.state.collectAsStateWithLifecycle()
    var openWorkoutId by remember { mutableStateOf<String?>(null) }

    val current = appData.workouts.find { it.id == openWorkoutId }
    if (current == null) {
        WorkoutListScreen(
            workouts = appData.workouts,
            onWorkoutClick = { openWorkoutId = it },
            onNewWorkout = {
                val workout = Workout(name = "New workout", sectionRestSecs = null, sections = emptyList())
                AppRepository.addWorkout(workout)
                openWorkoutId = workout.id
            }
        )
    } else {
        WorkoutScreen(
            workout = current,
            onBack = { openWorkoutId = null }
        )
    }
}

object AppRepository {
    private val _state = MutableStateFlow(AppData())
    val state = _state.asStateFlow()

    fun updateApp(transform: (AppData) -> AppData) {
        _state.update(transform)
    }

    fun addWorkout(workout: Workout) {
        updateApp { data -> data.copy(workouts = data.workouts + workout) }
    }

    fun removeWorkout(id: String) {
        updateApp { data -> data.copy(workouts = data.workouts.filter { it.id != id }) }
    }

    fun updateWorkout(workoutId: String, transform: (Workout) -> Workout) {
        updateApp { data ->
            data.copy(workouts = data.workouts.map {
                if (it.id == workoutId) transform(it) else it
            })
        }
    }

    fun addSection(workoutId: String, section: WorkoutSection) {
        updateWorkout(workoutId) { workout ->
            workout.copy(sections = workout.sections + section)
        }
    }

    fun removeSection(workoutId: String, sectionId: String) {
        updateWorkout(workoutId) { workout ->
            workout.copy(sections = workout.sections.filter { it.id != sectionId })
        }
    }

    fun updateSection(workoutId: String, sectionId: String, transform: (WorkoutSection) -> WorkoutSection) {
        updateWorkout(workoutId) { workout ->
            workout.copy(sections = workout.sections.map {
                if (it.id == sectionId) transform(it) else it
            })
        }
    }

    fun addExercize(workoutId: String, sectionId: String, exercise: WorkoutExercise) {
        updateSection(workoutId, sectionId) { section ->
            section.copy(exercises = section.exercises + exercise)
        }
    }

    fun removeExercise(workoutId: String, sectionId: String, exerciseId: String) {
        updateSection(workoutId, sectionId) { section ->
            section.copy(exercises = section.exercises.filter { it.id != exerciseId })
        }
    }

    fun updateExercise(
        workoutId: String,
        sectionId: String,
        exerciseId: String,
        transform: (WorkoutExercise) -> WorkoutExercise
    ) {
        updateSection(workoutId, sectionId) { section ->
            section.copy(exercises = section.exercises.map {
                if (it.id == exerciseId) transform(it) else it
            })
        }
    }

    fun archiveExercise(archiveExerciseId: String): ArchiveExercise? =
        _state.value.archive.firstOrNull { it.id == archiveExerciseId }

    fun addArchiveExercise(exercise: ArchiveExercise) {
        updateApp { data -> data.copy(archive = data.archive + exercise) }
    }

    fun updateArchiveExercise(id: String, transform: (ArchiveExercise) -> ArchiveExercise) {
        updateApp { data ->
            data.copy(archive = data.archive.map {
                if (it.id == id) transform(it) else it
            })
        }
    }

}
