package com.eitan.trainer

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkoutIoTest {
    @BeforeTest
    fun reset() {
        AppRepository.updateApp { AppData() }
    }

    @Test
    fun exportImportRoundTrips() {
        val archived = ArchiveExercise(name = "pull_ups", description = "chin over bar")
        val workout = Workout(
            name = "W",
            description = "leg day",
            sectionRestSecs = 120,
            sections = listOf(
                WorkoutSection(
                    name = "Power",
                    description = "heavy stuff",
                    setRestSecs = 60,
                    exerciseRestSecs = 90,
                    restAfterSecs = 180,
                    exercises = listOf(
                        WorkoutExercise(
                            archiveExerciseId = archived.id,
                            lastVolume = 250,
                            weightKg = 82.5,
                            effort = Effort(10),
                            sets = 3,
                            setRestSecs = 45,
                            restAfterSecs = 30,
                        )
                    ),
                )
            ),
        )

        importWorkouts(exportWorkouts(listOf(workout), listOf(archived)))

        val state = AppRepository.state.value
        assertEquals(listOf("pull_ups"), state.archive.map { it.name })
        val imported = state.workouts.single()
        assertEquals(workout.copy(id = imported.id, sections = imported.sections), imported)
        val section = imported.sections.single()
        assertEquals(
            workout.sections.single().copy(id = section.id, exercises = section.exercises),
            section,
        )
        val exercise = section.exercises.single()
        assertEquals(
            workout.sections.single().exercises.single().copy(id = exercise.id),
            exercise,
        )
    }

    @Test
    fun importReusesExistingArchiveEntriesByName() {
        val existing = ArchiveExercise(name = "pull_ups", description = "kept")
        AppRepository.addArchiveExercise(existing)

        val bundled = ArchiveExercise(name = "Pull_Ups", description = "incoming")
        val workout = Workout(
            name = "W",
            sections = listOf(
                WorkoutSection(
                    name = "S",
                    exercises = listOf(WorkoutExercise(archiveExerciseId = bundled.id)),
                )
            ),
        )
        importWorkouts(exportWorkouts(listOf(workout), listOf(bundled)))

        val state = AppRepository.state.value
        assertEquals(listOf(existing), state.archive)
        assertEquals(
            existing.id,
            state.workouts.single().sections.single().exercises.single().archiveExerciseId,
        )
    }

    @Test
    fun importsWorkout1Fixture() {
        val text = javaClass.getResource("/workout1.json")!!.readText()
        importWorkouts(text)

        val state = AppRepository.state.value
        assertEquals(44, state.archive.size)
        val workout = state.workouts.single()
        assertEquals("Workout1", workout.name)
        assertEquals(
            listOf("Warmup", "Power", "Explosive", "Burnout", "Post"),
            workout.sections.map { it.name },
        )
        assertEquals(listOf(23, 12, 3, 2, 4), workout.sections.map { it.exercises.size })

        val byName = { name: String ->
            val id = state.archive.first { it.name == name }.id
            workout.sections.flatMap { it.exercises }.first { it.archiveExerciseId == id }
        }
        assertEquals(82.5, byName("hip_thrust").weightKg)
        assertEquals(Effort(20, isDuration = true), byName("hang").effort)
        assertEquals(0.0, byName("fast_squats").weightKg)
        assertEquals(2, byName("90deg").sets)
    }
}
