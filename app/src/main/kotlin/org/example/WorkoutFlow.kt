package com.eitan.trainer

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FlowStep(
    val section: WorkoutSection,
    val exercise: WorkoutExercise,
    val setIndex: Int,
    val totalSets: Int,
    // Rest that follows this set: set rest within an exercise, exercise rest
    // between exercises, section rest between sections.
    val restSecs: Int,
)

fun buildFlowSteps(workout: Workout, defaults: DefaultSettings): List<FlowStep> {
    val steps = mutableListOf<FlowStep>()
    workout.sections.forEach { section ->
        section.exercises.forEachIndexed { exerciseIndex, exercise ->
            val totalSets = exercise.sets ?: defaults.sets
            for (set in 1..totalSets) {
                val restSecs = when {
                    set < totalSets ->
                        exercise.setRestSecs ?: section.setRestSecs ?: defaults.setRestSecs
                    exerciseIndex < section.exercises.lastIndex ->
                        exercise.restAfterSecs ?: section.exerciseRestSecs ?: defaults.exerciseRestSecs
                    else ->
                        section.restAfterSecs ?: workout.sectionRestSecs ?: defaults.sectionRestSecs
                }
                steps += FlowStep(section, exercise, set, totalSets, restSecs)
            }
        }
    }
    return steps
}

fun fmtClock(secs: Int) = "%d:%02d".format(secs / 60, secs % 60)

enum class FlowPhase { Ready, Exercising, Resting }

data class FlowState(
    val workoutId: String,
    val stepIndex: Int = 0,
    val phase: FlowPhase = FlowPhase.Ready,
    // End timestamp of the running exercise/rest timer; unused when Ready.
    val endsAtMillis: Long = 0,
)

// Lives outside the composable so leaving the flow screen keeps the session running.
object FlowSession {
    private val _state = MutableStateFlow<FlowState?>(null)
    val state = _state.asStateFlow()

    fun set(value: FlowState?) {
        _state.value = value
    }

    fun finishOrRest(steps: List<FlowStep>, fromMillis: Long) {
        val current = _state.value ?: return
        val step = steps.getOrNull(current.stepIndex) ?: return
        _state.value = when {
            current.stepIndex == steps.lastIndex ->
                current.copy(stepIndex = steps.size, phase = FlowPhase.Ready, endsAtMillis = 0)
            step.restSecs <= 0 ->
                current.copy(stepIndex = current.stepIndex + 1, phase = FlowPhase.Ready, endsAtMillis = 0)
            else ->
                current.copy(phase = FlowPhase.Resting, endsAtMillis = fromMillis + step.restSecs * 1000L)
        }
    }

    fun finishRest() {
        val current = _state.value ?: return
        _state.value = current.copy(stepIndex = current.stepIndex + 1, phase = FlowPhase.Ready, endsAtMillis = 0)
    }

    // Undo one visual state: a running timer goes back to the set's ready screen,
    // a ready screen goes back to the previous set.
    fun back() {
        val current = _state.value ?: return
        _state.value = when {
            current.phase != FlowPhase.Ready -> current.copy(phase = FlowPhase.Ready, endsAtMillis = 0)
            current.stepIndex > 0 -> current.copy(stepIndex = current.stepIndex - 1, endsAtMillis = 0)
            else -> current
        }
    }
}

@Composable
fun WorkoutFlowScreen(workout: Workout, onExit: () -> Unit) {
    val appData by AppRepository.state.collectAsStateWithLifecycle()
    val steps = remember(workout.id) { buildFlowSteps(workout, appData.defaults) }
    val session by FlowSession.state.collectAsStateWithLifecycle()

    LaunchedEffect(workout.id) {
        if (FlowSession.state.value?.workoutId != workout.id) {
            FlowSession.set(FlowState(workoutId = workout.id))
        }
    }

    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(steps) {
        while (true) {
            delay(250)
            now = System.currentTimeMillis()
            val state = FlowSession.state.value ?: continue
            if (state.workoutId != workout.id || state.phase == FlowPhase.Ready) continue
            if (now >= state.endsAtMillis) {
                tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
                // Advance from the timer's end, so time away from the screen still counts.
                if (state.phase == FlowPhase.Exercising) FlowSession.finishOrRest(steps, state.endsAtMillis)
                else FlowSession.finishRest()
            }
        }
    }

    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            tone.release()
        }
    }

    val state = session
    if (state == null || state.workoutId != workout.id) return
    val step = steps.getOrNull(state.stepIndex)
    val isResting = state.phase == FlowPhase.Resting
    val displayStep = if (isResting) steps.getOrNull(state.stepIndex + 1) else step
    val remainingSecs = ((state.endsAtMillis - now + 999) / 1000).toInt().coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isResting) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface
            )
            .safeDrawingPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "${workout.name}  -  ${displayStep?.section?.name ?: ""}",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onExit)
                .padding(vertical = 16.dp)
        )

        when {
            step == null -> FinishedView {
                FlowSession.set(null)
                onExit()
            }
            isResting -> RestView(
                next = displayStep!!,
                defaults = appData.defaults,
                archive = appData.archive,
                remainingSecs = remainingSecs,
                onSkip = { FlowSession.finishRest() }
            )
            state.phase == FlowPhase.Exercising -> ExerciseTimerView(
                step = step,
                defaults = appData.defaults,
                archive = appData.archive,
                remainingSecs = remainingSecs,
                onSkip = { FlowSession.finishOrRest(steps, System.currentTimeMillis()) }
            )
            else -> ReadyView(
                step = step,
                defaults = appData.defaults,
                archive = appData.archive,
                isLast = state.stepIndex == steps.lastIndex,
                onPrimary = {
                    val nowMillis = System.currentTimeMillis()
                    val effort = step.exercise.effort ?: appData.defaults.effort
                    if (effort.isDuration) {
                        FlowSession.set(
                            state.copy(phase = FlowPhase.Exercising, endsAtMillis = nowMillis + effort.amount * 1000L)
                        )
                    } else {
                        FlowSession.finishOrRest(steps, nowMillis)
                    }
                }
            )
        }
    }
}

@Composable
private fun ExerciseInfo(step: FlowStep, defaults: DefaultSettings, archive: List<ArchiveExercise>) {
    val archived = archive.firstOrNull { it.id == step.exercise.archiveExerciseId }
    val effort = step.exercise.effort ?: defaults.effort
    val weight = step.exercise.weightKg ?: defaults.weightKg
    var expanded by remember(step.exercise.id) { mutableStateOf(true) }

    Text(
        archived?.name ?: "Unknown",
        style = MaterialTheme.typography.displaySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.clickable { expanded = !expanded }
    )
    if (expanded && archived != null) {
        archived.imageFile?.let {
            ExerciseImage(
                it,
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
            )
        }
        archived.description?.let { description ->
            Text(
                description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable { expanded = false }
            )
        }
        archived.videoUrl?.let { VideoLink(it) }
    }
    Spacer(Modifier.height(24.dp))
    Text(
        if (effort.isDuration) "Duration: ${effort.amount}s" else "Reps: ${effort.amount}",
        style = MaterialTheme.typography.headlineLarge
    )
    if (weight > 0) {
        Spacer(Modifier.height(8.dp))
        Text("Weight: ${fmtWeight(weight)} Kg", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun SetProgress(totalSets: Int, done: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalSets) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .background(
                        if (index < done) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
        Text(
            "$done/$totalSets",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun TimerBox(label: String, secs: Int) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(fmtClock(secs), style = MaterialTheme.typography.displayLarge, fontSize = 84.sp)
        }
    }
}

@Composable
private fun FlowControls(onBack: () -> Unit, onSkip: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onBack) { Text("BACK") }
        if (onSkip != null) {
            TextButton(onClick = onSkip) { Text("SKIP") }
        }
    }
}

@Composable
private fun ReadyView(
    step: FlowStep,
    defaults: DefaultSettings,
    archive: List<ArchiveExercise>,
    isLast: Boolean,
    onPrimary: () -> Unit,
) {
    val effort = step.exercise.effort ?: defaults.effort

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ExerciseInfo(step, defaults, archive)
        }
        SetProgress(step.totalSets, done = step.setIndex)
        OutlinedButton(
            onClick = onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
        ) {
            Text(
                when {
                    effort.isDuration -> "Start"
                    isLast -> "Finish workout"
                    else -> "Done set"
                },
                style = MaterialTheme.typography.headlineLarge
            )
        }
        FlowControls(onBack = { FlowSession.back() })
    }
}

@Composable
private fun ExerciseTimerView(
    step: FlowStep,
    defaults: DefaultSettings,
    archive: List<ArchiveExercise>,
    remainingSecs: Int,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ExerciseInfo(step, defaults, archive)
        }
        SetProgress(step.totalSets, done = step.setIndex)
        TimerBox("EXERCISE", remainingSecs)
        FlowControls(onBack = { FlowSession.back() }, onSkip = onSkip)
    }
}

@Composable
private fun RestView(
    next: FlowStep,
    defaults: DefaultSettings,
    archive: List<ArchiveExercise>,
    remainingSecs: Int,
    onSkip: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TimerBox("REST", remainingSecs)
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Next is", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ExerciseInfo(next, defaults, archive)
        }
        SetProgress(next.totalSets, done = next.setIndex - 1)
        FlowControls(onBack = { FlowSession.back() }, onSkip = onSkip)
    }
}

@Composable
private fun FinishedView(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Workout finished", style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(48.dp))
            OutlinedButton(
                onClick = onDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
            ) {
                Text("Done", style = MaterialTheme.typography.headlineLarge)
            }
        }
        FlowControls(onBack = { FlowSession.back() })
    }
}
