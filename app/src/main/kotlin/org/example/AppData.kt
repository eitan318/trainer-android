package com.eitan.trainer

import java.util.UUID

data class AppData(
    val defaults: DefaultSettings = DefaultSettings(),
    val archive: List<ArchiveExercise> = emptyList(),
    val workouts: List<Workout> = emptyList(),
)

data class Effort(
    val amount: Int,
    val isDuration: Boolean,
)

data class DefaultSettings(
    val sectionRestSecs: Int = 2 * 60,
    val exerciseRestSecs: Int = 2 * 60,
    val setRestSecs: Int = 2 * 60,
    val weightKg: Int = 0,
    val sets: Int = 1,
    val effort: Effort = Effort(amount = 10, isDuration = false),
    val volumeIncrementPercent: Double = 0.1,
)

data class ArchiveExercise(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String?,
)

data class Workout(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val sectionRestSecs: Int?,
    val sections: List<WorkoutSection>,
)

data class WorkoutSection(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val exerciseRestSecs: Int?,
    val restAfterSecs: Int?,
    val exercises: List<WorkoutExercise>,
)

data class WorkoutExercise(
    val id: String = UUID.randomUUID().toString(),
    val archiveExerciseId: String,
    val lastVolume: Int,

    val weightKg: Int?,
    val effort: Effort?,
    val sets: Int?,
    val setRestSecs: Int?,
    val restAfterSecs: Int?,
)
