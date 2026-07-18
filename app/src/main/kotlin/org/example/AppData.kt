package com.eitan.trainer

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class AppData(
    val defaults: DefaultSettings = DefaultSettings(),
    val archive: List<ArchiveExercise> = emptyList(),
    val workouts: List<Workout> = emptyList(),
)

@Serializable
data class Effort(
    val amount: Int,
    val isDuration: Boolean = false,
)

@Serializable
data class DefaultSettings(
    val sectionRestSecs: Int = 2 * 60,
    val exerciseRestSecs: Int = 2 * 60,
    val setRestSecs: Int = 2 * 60,
    val weightKg: Double = 0.0,
    val sets: Int = 1,
    val effort: Effort = Effort(amount = 10, isDuration = false),
    val volumeIncrementPercent: Double = 0.1,
)

@Serializable
data class ArchiveExercise(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
)

@Serializable
data class Workout(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val sectionRestSecs: Int? = null,
    val sections: List<WorkoutSection> = emptyList(),
)

@Serializable
data class WorkoutSection(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    // Defaults for this section's exercises; each exercise may override them.
    val setRestSecs: Int? = null,
    val exerciseRestSecs: Int? = null,
    // Rest after the whole section; overrides the workout's sectionRestSecs.
    val restAfterSecs: Int? = null,
    val exercises: List<WorkoutExercise> = emptyList(),
)

@Serializable
data class WorkoutExercise(
    val id: String = UUID.randomUUID().toString(),
    val archiveExerciseId: String,
    val lastVolume: Int = 0,

    val weightKg: Double? = null,
    val effort: Effort? = null,
    val sets: Int? = null,
    val setRestSecs: Int? = null,
    val restAfterSecs: Int? = null,
)
