package com.riddle.r2d2flipper;

import java.util.UUID;

final class Protocol {
    static final String[] DEVICE_PREFIXES = {"Kipps", "2ndHeroD"};

    static final UUID SERVICE_UUID = UUID.fromString("DAB91435-B5A1-E29C-B041-BCD562613BE4");
    static final UUID NOTIFY_UUID = UUID.fromString("DAB91382-B5A1-E29C-B041-BCD562613BE4");
    static final UUID WRITE_UUID = UUID.fromString("DAB91383-B5A1-E29C-B041-BCD562613BE4");
    static final UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    private static final byte CMD_PLAY_AUDIO = 16;
    private static final byte CMD_CAM_POSITION = 18;
    private static final byte CMD_HEAD_POSITION = 19;
    private static final byte CMD_MOTOR = 20;
    private static final byte CMD_LED = 21;
    private static final byte CMD_STOP_SEQUENCE = 24;

    static final byte CAM_PIVOT_RIGHT = 6;
    static final byte CAM_DRIVE = 7;
    static final byte CAM_PIVOT_LEFT = 8;

    static final byte MOTOR_STOP = 0;
    static final byte MOTOR_FORWARD = 1;
    static final byte MOTOR_BACKWARD = 2;

    static final byte[] KEEP_ALIVE = {(byte) 80, (byte) 0x8D};

    private Protocol() {}

    static byte[] head(int position) {
        return new byte[]{CMD_HEAD_POSITION, (byte) position};
    }

    static byte[] cam(byte position) {
        return new byte[]{CMD_CAM_POSITION, 2, position};
    }

    static byte[] motor(byte direction) {
        return new byte[]{CMD_MOTOR, direction};
    }

    static byte[] led(int red, int blue) {
        return new byte[]{CMD_LED, (byte) red, (byte) blue};
    }

    static byte[] audio(int index) {
        return new byte[]{CMD_PLAY_AUDIO, (byte) (index & 0xFF), (byte) ((index >> 8) & 0xFF)};
    }

    static byte[] stopAll() {
        return new byte[]{CMD_STOP_SEQUENCE, 0x3F};
    }
}
