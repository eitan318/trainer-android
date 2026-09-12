package com.eitan.trainer

import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class WorkoutExport(
    val archive: List<ArchiveExercise>,
    val workouts: List<Workout>,
)

val transferJson = Json { ignoreUnknownKeys = true }

fun exportWorkouts(workouts: List<Workout>, archive: List<ArchiveExercise>): String {
    val referenced = workouts
        .flatMap { it.sections }
        .flatMap { it.exercises }
        .map { it.archiveExerciseId }
        .toSet()
    return transferJson.encodeToString(
        WorkoutExport.serializer(),
        WorkoutExport(archive.filter { it.id in referenced }, workouts),
    )
}

fun importWorkouts(text: String, materialize: (ArchiveExercise) -> ArchiveExercise = { it }) {
    val bundle = runCatching {
        transferJson.decodeFromString(WorkoutExport.serializer(), text)
    }.getOrNull() ?: return

    // Bundled archive entries are matched to existing ones by name; missing ones are added.
    val idMap = mutableMapOf<String, String>()
    val existing = AppRepository.state.value.archive
    bundle.archive.forEach { entry ->
        val match = existing.firstOrNull { it.name.equals(entry.name, ignoreCase = true) }
        if (match != null) idMap[entry.id] = match.id
        else AppRepository.addArchiveExercise(materialize(entry))
    }

    bundle.workouts.forEach { workout ->
        AppRepository.addWorkout(
            workout.copy(
                id = UUID.randomUUID().toString(),
                sections = workout.sections.map { section ->
                    section.copy(
                        id = UUID.randomUUID().toString(),
                        exercises = section.exercises.map { exercise ->
                            exercise.copy(
                                id = UUID.randomUUID().toString(),
                                archiveExerciseId = idMap[exercise.archiveExerciseId]
                                    ?: exercise.archiveExerciseId,
                            )
                        },
                    )
                },
            )
        )
    }
}
