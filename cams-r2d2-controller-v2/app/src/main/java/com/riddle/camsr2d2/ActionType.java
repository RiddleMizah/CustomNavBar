package com.riddle.camsr2d2;

enum ActionType {
    FORWARD("Forward", true),
    REVERSE("Reverse", true),
    TURN_LEFT("Turn Left", true),
    TURN_RIGHT("Turn Right", true),
    STOP("Stop", false),
    HEAD_LEFT("Head Left", false),
    HEAD_CENTER("Head Center", false),
    HEAD_RIGHT("Head Right", false),
    LIGHT_RED("Red Light", false),
    LIGHT_BLUE("Blue Light", false),
    LIGHTS_OFF("Lights Off", false),
    WAKE("Wake Sound", false),
    WHISTLE("Whistle", false),
    ACHIEVEMENT("Achievement", false);

    final String label;
    final boolean motion;

    ActionType(String label, boolean motion) {
        this.label = label;
        this.motion = motion;
    }
}
