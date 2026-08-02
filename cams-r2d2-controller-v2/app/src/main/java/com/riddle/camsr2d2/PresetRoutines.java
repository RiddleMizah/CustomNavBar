package com.riddle.camsr2d2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class PresetRoutines {
    private PresetRoutines() {}

    static List<Routine> all() {
        List<Routine> routines = new ArrayList<>();

        routines.add(Routine.preset("happy-dance", "Happy Droid Dance", Arrays.asList(
                s(ActionType.WAKE, 0, 0),
                s(ActionType.LIGHT_BLUE, 250, 0),
                s(ActionType.TURN_LEFT, 250, 650),
                s(ActionType.TURN_RIGHT, 180, 650),
                s(ActionType.HEAD_LEFT, 220, 0),
                s(ActionType.HEAD_RIGHT, 220, 0),
                s(ActionType.WHISTLE, 250, 0),
                s(ActionType.HEAD_CENTER, 250, 0)
        )));

        routines.add(Routine.preset("victory-spin", "Victory Spin", Arrays.asList(
                s(ActionType.ACHIEVEMENT, 0, 0),
                s(ActionType.LIGHT_BLUE, 150, 0),
                s(ActionType.TURN_LEFT, 180, 1300),
                s(ActionType.TURN_RIGHT, 220, 1300),
                s(ActionType.WHISTLE, 250, 0),
                s(ActionType.LIGHTS_OFF, 400, 0)
        )));

        routines.add(Routine.preset("patrol", "Patrol Mode", Arrays.asList(
                s(ActionType.LIGHT_BLUE, 0, 0),
                s(ActionType.FORWARD, 200, 1600),
                s(ActionType.HEAD_LEFT, 300, 0),
                s(ActionType.HEAD_RIGHT, 500, 0),
                s(ActionType.HEAD_CENTER, 350, 0),
                s(ActionType.FORWARD, 350, 900),
                s(ActionType.WHISTLE, 300, 0)
        )));

        routines.add(Routine.preset("silly-wiggle", "Silly Wiggle", Arrays.asList(
                s(ActionType.TURN_LEFT, 0, 300),
                s(ActionType.TURN_RIGHT, 80, 300),
                s(ActionType.TURN_LEFT, 80, 300),
                s(ActionType.TURN_RIGHT, 80, 300),
                s(ActionType.HEAD_LEFT, 120, 0),
                s(ActionType.HEAD_RIGHT, 180, 0),
                s(ActionType.WHISTLE, 120, 0)
        )));

        routines.add(Routine.preset("nervous-droid", "Nervous Droid", Arrays.asList(
                s(ActionType.LIGHT_RED, 0, 0),
                s(ActionType.REVERSE, 150, 650),
                s(ActionType.TURN_LEFT, 100, 350),
                s(ActionType.TURN_RIGHT, 100, 350),
                s(ActionType.WHISTLE, 100, 0),
                s(ActionType.LIGHTS_OFF, 500, 0)
        )));

        routines.add(Routine.preset("bedtime", "Bedtime R2", Arrays.asList(
                s(ActionType.HEAD_CENTER, 0, 0),
                s(ActionType.LIGHT_BLUE, 200, 0),
                s(ActionType.WHISTLE, 300, 0),
                s(ActionType.LIGHTS_OFF, 1000, 0),
                s(ActionType.STOP, 100, 0)
        )));

        return routines;
    }

    private static RoutineStep s(ActionType action, long delayMs, long durationMs) {
        return new RoutineStep(action, delayMs, durationMs);
    }
}
