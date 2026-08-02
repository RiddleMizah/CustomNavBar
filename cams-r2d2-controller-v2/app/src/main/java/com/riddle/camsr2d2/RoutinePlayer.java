package com.riddle.camsr2d2;

import android.os.Handler;
import android.os.Looper;

final class RoutinePlayer {
    interface Executor {
        void execute(RoutineStep step, Runnable complete);
        void emergencyStop();
    }

    interface Listener {
        void onRoutineStarted(Routine routine);
        void onRoutineStep(Routine routine, int index, RoutineStep step);
        void onRoutineFinished(Routine routine, boolean cancelled);
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Executor executor;
    private final Listener listener;
    private Routine current;
    private int index;
    private boolean cancelled;

    RoutinePlayer(Executor executor, Listener listener) {
        this.executor = executor;
        this.listener = listener;
    }

    boolean isPlaying() {
        return current != null;
    }

    void play(Routine routine) {
        stop();
        current = routine;
        index = 0;
        cancelled = false;
        listener.onRoutineStarted(routine);
        scheduleNext();
    }

    void stop() {
        if (current == null) return;
        cancelled = true;
        handler.removeCallbacksAndMessages(null);
        executor.emergencyStop();
        Routine finished = current;
        current = null;
        listener.onRoutineFinished(finished, true);
    }

    private void scheduleNext() {
        Routine routine = current;
        if (routine == null || cancelled) return;
        if (index >= routine.steps.size()) {
            current = null;
            executor.emergencyStop();
            listener.onRoutineFinished(routine, false);
            return;
        }

        RoutineStep step = routine.steps.get(index);
        int stepIndex = index;
        handler.postDelayed(() -> {
            if (current != routine || cancelled) return;
            listener.onRoutineStep(routine, stepIndex, step);
            executor.execute(step, () -> {
                if (current != routine || cancelled) return;
                index++;
                scheduleNext();
            });
        }, step.delayBeforeMs);
    }
}
