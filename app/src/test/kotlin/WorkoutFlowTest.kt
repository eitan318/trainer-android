package com.eitan.trainer

import kotlin.test.Test
import kotlin.test.assertEquals

class WorkoutFlowTest {
    private val defaults = DefaultSettings(
        sectionRestSecs = 300,
        exerciseRestSecs = 120,
        setRestSecs = 60,
        sets = 1,
    )

    @Test
    fun zeroRestSkipsRestPhase() {
        val workout = Workout(
            name = "W",
            sections = listOf(
                WorkoutSection(
                    name = "S",
                    exerciseRestSecs = 0,
                    exercises = listOf(
                        WorkoutExercise(archiveExerciseId = "a"),
                        WorkoutExercise(archiveExerciseId = "b"),
                    ),
                )
            ),
        )
        val steps = buildFlowSteps(workout, defaults)
        FlowSession.set(FlowState(workoutId = workout.id))

        FlowSession.finishOrRest(steps, fromMillis = 1000L)

        assertEquals(
            FlowState(workoutId = workout.id, stepIndex = 1, phase = FlowPhase.Ready),
            FlowSession.state.value,
        )
    }

    @Test
    fun backUndoesTimerThenStep() {
        FlowSession.set(FlowState(workoutId = "w", stepIndex = 2, phase = FlowPhase.Resting, endsAtMillis = 99))

        FlowSession.back()
        assertEquals(FlowState("w", 2, FlowPhase.Ready), FlowSession.state.value)

        FlowSession.back()
        assertEquals(FlowState("w", 1, FlowPhase.Ready), FlowSession.state.value)
    }

    @Test
    fun restResolvesPerBoundaryThroughDefaultsChain() {
        val a = WorkoutExercise(archiveExerciseId = "a", sets = 2, setRestSecs = 45)
        val b = WorkoutExercise(archiveExerciseId = "b", restAfterSecs = 90)
        val c = WorkoutExercise(archiveExerciseId = "c")
        val workout = Workout(
            name = "W",
            sectionRestSecs = 240,
            sections = listOf(
                WorkoutSection(name = "S1", exercises = listOf(a, b)),
                WorkoutSection(name = "S2", restAfterSecs = 30, exercises = listOf(c)),
            ),
        )

        val steps = buildFlowSteps(workout, defaults)

        assertEquals(
            listOf("a" to 1, "a" to 2, "b" to 1, "c" to 1),
            steps.map { it.exercise.archiveExerciseId to it.setIndex },
        )
        // a set 1 -> a set 2: exercise's own set rest
        assertEquals(45, steps[0].restSecs)
        // a -> b: exercise rest from global defaults (neither a nor S1 override it)
        assertEquals(120, steps[1].restSecs)
        // b -> S2: b is last in S1; S1 has no restAfter, workout says 240
        assertEquals(240, steps[2].restSecs)
        // c is last overall; S2's own restAfter wins even if unused
        assertEquals(30, steps[3].restSecs)
    }

    @Test
    fun sectionSetRestBeatsGlobalDefault() {
        val workout = Workout(
            name = "W",
            sections = listOf(
                WorkoutSection(
                    name = "S",
                    setRestSecs = 15,
                    exercises = listOf(WorkoutExercise(archiveExerciseId = "a", sets = 3)),
                )
            ),
        )

        val steps = buildFlowSteps(workout, defaults)
        assertEquals(3, steps.size)
        assertEquals(listOf(15, 15), steps.dropLast(1).map { it.restSecs })
        // last set of last exercise: section rest chain, workout has none -> default 300
        assertEquals(300, steps.last().restSecs)
    }
}
