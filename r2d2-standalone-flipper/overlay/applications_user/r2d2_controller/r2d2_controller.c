#include "r2d2_ble.h"
#include "r2d2_events.h"
#include "r2d2_profile.h"

#include <bt/bt_service/bt.h>
#include <furi.h>
#include <furi_hal.h>
#include <gui/gui.h>
#include <gui/view_port.h>
#include <input/input.h>

#include <stdio.h>
#include <string.h>

#define TAG "R2Controller"

#define R2_INVALID_HANDLE 0xFFFF
#define R2_KEEPALIVE_MS   2000
#define R2_CAM_DELAY_MS   400
#define R2_RECONNECT_MS   750

typedef enum {
    R2PhaseIdle,
    R2PhaseScanning,
    R2PhaseStoppingScan,
    R2PhaseConnecting,
    R2PhaseDiscoverService,
    R2PhaseDiscoverCharacteristic,
    R2PhaseReady,
    R2PhaseError,
} R2Phase;

typedef enum {
    R2ModeDrive,
    R2ModeHeadSound,
    R2ModeLights,
    R2ModeCount,
} R2Mode;

typedef struct {
    Gui* gui;
    ViewPort* view_port;
    FuriMessageQueue* queue;
    FuriMutex* mutex;
    Bt* bt;
    FuriHalBleProfileBase* profile;
    R2Ble ble;

    R2Phase phase;
    R2Mode mode;
    char status[32];

    uint8_t device_address_type;
    uint8_t device_address[6];
    bool device_found;

    uint16_t connection_handle;
    uint16_t service_start_handle;
    uint16_t service_end_handle;
    uint16_t write_handle;

    bool motion_pending;
    uint8_t pending_motor_direction;
    uint32_t motion_due_tick;
    uint32_t keepalive_due_tick;
    uint32_t reconnect_due_tick;
    bool running;
} R2App;

static const uint8_t CMD_KEEPALIVE[] = {80, 0x8D};
static const uint8_t CMD_END_APP[] = {80, 0x8C};
static const uint8_t CMD_MOTOR_STOP[] = {20, 0};
static const uint8_t CMD_STOP_SEQUENCES[] = {24, 0x3F};

static const char* r2_mode_name(R2Mode mode) {
    switch(mode) {
    case R2ModeDrive:
        return "DRIVE";
    case R2ModeHeadSound:
        return "HEAD / SOUND";
    case R2ModeLights:
        return "LIGHTS";
    default:
        return "";
    }
}

static void r2_set_status(R2App* app, R2Phase phase, const char* status) {
    furi_mutex_acquire(app->mutex, FuriWaitForever);
    app->phase = phase;
    strlcpy(app->status, status, sizeof(app->status));
    furi_mutex_release(app->mutex);
    view_port_update(app->view_port);
}

static void r2_draw_callback(Canvas* canvas, void* context) {
    R2App* app = context;
    furi_mutex_acquire(app->mutex, FuriWaitForever);

    canvas_clear(canvas);
    canvas_set_font(canvas, FontPrimary);
    canvas_draw_str(canvas, 2, 11, "R2-D2 DIRECT");
    canvas_draw_line(canvas, 0, 14, 127, 14);

    canvas_set_font(canvas, FontSecondary);
    canvas_draw_str(canvas, 2, 27, app->status);

    if(app->phase == R2PhaseReady) {
        char mode_line[30];
        snprintf(mode_line, sizeof(mode_line), "Mode: %s", r2_mode_name(app->mode));
        canvas_draw_str(canvas, 2, 39, mode_line);

        if(app->mode == R2ModeDrive) {
            canvas_draw_str(canvas, 2, 50, "D-pad drive | OK stop");
        } else if(app->mode == R2ModeHeadSound) {
            canvas_draw_str(canvas, 2, 50, "L/R head | U/D sounds");
        } else {
            canvas_draw_str(canvas, 2, 50, "L red | R blue | D off");
        }
    } else if(app->phase == R2PhaseError) {
        canvas_draw_str(canvas, 2, 42, "Back exits; retry after fix");
    } else {
        canvas_draw_str(canvas, 2, 42, "Power on R2-D2 nearby");
    }

    canvas_draw_line(canvas, 0, 54, 127, 54);
    canvas_draw_str(canvas, 2, 63, "Hold OK: mode   Back: exit");

    furi_mutex_release(app->mutex);
}

static void r2_input_callback(InputEvent* input_event, void* context) {
    R2App* app = context;
    R2Event event = {.type = R2EventInput};
    event.data.input = *input_event;
    furi_message_queue_put(app->queue, &event, 0);
}

static bool r2_send(R2App* app, const uint8_t* data, uint8_t length) {
    if(app->phase != R2PhaseReady) return false;
    return r2_ble_write(app->connection_handle, app->write_handle, data, length);
}

static void r2_send_motor_stop(R2App* app) {
    app->motion_pending = false;
    if(app->phase == R2PhaseReady) {
        r2_send(app, CMD_MOTOR_STOP, sizeof(CMD_MOTOR_STOP));
    }
}

static void r2_emergency_stop(R2App* app) {
    r2_send_motor_stop(app);
    if(app->phase == R2PhaseReady) {
        r2_send(app, CMD_STOP_SEQUENCES, sizeof(CMD_STOP_SEQUENCES));
    }
}

static void r2_start_motion(R2App* app, uint8_t cam_position, uint8_t motor_direction) {
    if(app->phase != R2PhaseReady) return;
    const uint8_t cam_command[] = {18, 2, cam_position};
    if(r2_send(app, cam_command, sizeof(cam_command))) {
        app->motion_pending = true;
        app->pending_motor_direction = motor_direction;
        app->motion_due_tick = furi_get_tick() + furi_ms_to_ticks(R2_CAM_DELAY_MS);
    }
}

static void r2_set_head(R2App* app, uint8_t position) {
    const uint8_t command[] = {19, position};
    r2_send(app, command, sizeof(command));
}

static void r2_set_led(R2App* app, uint8_t red, uint8_t blue) {
    const uint8_t command[] = {21, red, blue};
    r2_send(app, command, sizeof(command));
}

static void r2_play_audio(R2App* app, uint16_t index) {
    const uint8_t command[] = {16, (uint8_t)(index & 0xFF), (uint8_t)(index >> 8)};
    r2_send(app, command, sizeof(command));
}

static void r2_reset_connection_data(R2App* app) {
    app->connection_handle = R2_INVALID_HANDLE;
    app->service_start_handle = 0;
    app->service_end_handle = 0;
    app->write_handle = 0;
    app->device_found = false;
    app->motion_pending = false;
}

static void r2_begin_scan(R2App* app) {
    r2_reset_connection_data(app);
    if(r2_ble_start_scan()) {
        r2_set_status(app, R2PhaseScanning, "Scanning for R2-D2...");
    } else {
        r2_set_status(app, R2PhaseError, "BLE scan failed");
    }
}

static void r2_begin_connect(R2App* app) {
    if(!app->device_found) {
        r2_begin_scan(app);
        return;
    }

    if(r2_ble_connect(app->device_address_type, app->device_address)) {
        r2_set_status(app, R2PhaseConnecting, "Connecting...");
    } else {
        app->reconnect_due_tick = furi_get_tick() + furi_ms_to_ticks(R2_RECONNECT_MS);
        r2_set_status(app, R2PhaseIdle, "Connect failed; retrying");
    }
}

static void r2_handle_input(R2App* app, const InputEvent* input) {
    if((input->key == InputKeyBack) &&
       ((input->type == InputTypeShort) || (input->type == InputTypeLong))) {
        app->running = false;
        return;
    }

    if((input->key == InputKeyOk) && (input->type == InputTypeLong)) {
        r2_emergency_stop(app);
        app->mode = (R2Mode)((app->mode + 1) % R2ModeCount);
        view_port_update(app->view_port);
        return;
    }

    if(app->phase != R2PhaseReady) return;

    if(app->mode == R2ModeDrive) {
        if(input->type == InputTypePress) {
            switch(input->key) {
            case InputKeyUp:
                r2_start_motion(app, 7, 1);
                break;
            case InputKeyDown:
                r2_start_motion(app, 7, 2);
                break;
            case InputKeyLeft:
                r2_start_motion(app, 8, 1);
                break;
            case InputKeyRight:
                r2_start_motion(app, 6, 1);
                break;
            case InputKeyOk:
                r2_emergency_stop(app);
                break;
            default:
                break;
            }
        } else if(input->type == InputTypeRelease) {
            if((input->key == InputKeyUp) || (input->key == InputKeyDown) ||
               (input->key == InputKeyLeft) || (input->key == InputKeyRight)) {
                r2_send_motor_stop(app);
            }
        }
    } else if((input->type == InputTypeShort) && (app->mode == R2ModeHeadSound)) {
        switch(input->key) {
        case InputKeyLeft:
            r2_set_head(app, 0);
            break;
        case InputKeyRight:
            r2_set_head(app, 2);
            break;
        case InputKeyOk:
            r2_set_head(app, 1);
            break;
        case InputKeyUp:
            r2_play_audio(app, 146);
            break;
        case InputKeyDown:
            r2_play_audio(app, 152);
            break;
        default:
            break;
        }
    } else if((input->type == InputTypeShort) && (app->mode == R2ModeLights)) {
        switch(input->key) {
        case InputKeyLeft:
            r2_set_led(app, 255, 0);
            break;
        case InputKeyRight:
            r2_set_led(app, 0, 255);
            break;
        case InputKeyDown:
        case InputKeyOk:
            r2_set_led(app, 0, 0);
            break;
        case InputKeyUp:
            r2_play_audio(app, 157);
            break;
        default:
            break;
        }
    }
}

static void r2_handle_ble_event(R2App* app, const R2Event* event) {
    switch(event->type) {
    case R2EventDeviceFound:
        if(app->phase == R2PhaseScanning) {
            app->device_found = true;
            app->device_address_type = event->data.device.address_type;
            memcpy(app->device_address, event->data.device.address, 6);
            if(r2_ble_stop_scan()) {
                r2_set_status(app, R2PhaseStoppingScan, "R2-D2 found");
            } else {
                r2_begin_connect(app);
            }
        }
        break;

    case R2EventGapProcedureComplete:
        if(event->data.gap_proc.procedure_code == GAP_GENERAL_DISCOVERY_PROC) {
            if((app->phase == R2PhaseStoppingScan) && app->device_found) {
                r2_begin_connect(app);
            } else if(app->phase == R2PhaseScanning) {
                r2_begin_scan(app);
            }
        }
        break;

    case R2EventConnected:
        if((app->phase == R2PhaseConnecting) &&
           (event->data.connected.status == BLE_STATUS_SUCCESS)) {
            app->connection_handle = event->data.connected.connection_handle;
            if(r2_ble_discover_service(app->connection_handle)) {
                r2_set_status(app, R2PhaseDiscoverService, "Finding R2 service...");
            } else {
                r2_set_status(app, R2PhaseError, "Service search failed");
            }
        } else if(event->data.connected.status != BLE_STATUS_SUCCESS) {
            app->reconnect_due_tick = furi_get_tick() + furi_ms_to_ticks(R2_RECONNECT_MS);
            r2_set_status(app, R2PhaseIdle, "Connection rejected");
        }
        break;

    case R2EventServiceFound:
        app->service_start_handle = event->data.service.start_handle;
        app->service_end_handle = event->data.service.end_handle;
        break;

    case R2EventCharacteristicFound:
        app->write_handle = event->data.characteristic.value_handle;
        break;

    case R2EventGattProcedureComplete:
        if(app->phase == R2PhaseDiscoverService) {
            if((event->data.gatt_proc.error_code == BLE_STATUS_SUCCESS) &&
               app->service_start_handle && app->service_end_handle &&
               r2_ble_discover_write_characteristic(
                   app->connection_handle,
                   app->service_start_handle,
                   app->service_end_handle)) {
                r2_set_status(app, R2PhaseDiscoverCharacteristic, "Finding command channel...");
            } else {
                r2_set_status(app, R2PhaseError, "R2 service not found");
            }
        } else if(app->phase == R2PhaseDiscoverCharacteristic) {
            if((event->data.gatt_proc.error_code == BLE_STATUS_SUCCESS) && app->write_handle) {
                r2_set_status(app, R2PhaseReady, "Connected - ready");
                app->keepalive_due_tick = furi_get_tick();
            } else {
                r2_set_status(app, R2PhaseError, "Command channel missing");
            }
        }
        break;

    case R2EventDisconnected:
        if(app->running) {
            r2_reset_connection_data(app);
            app->reconnect_due_tick = furi_get_tick() + furi_ms_to_ticks(R2_RECONNECT_MS);
            r2_set_status(app, R2PhaseIdle, "Disconnected; retrying");
        }
        break;

    default:
        break;
    }
}

static void r2_tick(R2App* app) {
    const uint32_t now = furi_get_tick();

    if(app->motion_pending && ((int32_t)(now - app->motion_due_tick) >= 0)) {
        app->motion_pending = false;
        const uint8_t motor_command[] = {20, app->pending_motor_direction};
        r2_send(app, motor_command, sizeof(motor_command));
    }

    if((app->phase == R2PhaseReady) && ((int32_t)(now - app->keepalive_due_tick) >= 0)) {
        r2_send(app, CMD_KEEPALIVE, sizeof(CMD_KEEPALIVE));
        app->keepalive_due_tick = now + furi_ms_to_ticks(R2_KEEPALIVE_MS);
    }

    if((app->phase == R2PhaseIdle) && app->reconnect_due_tick &&
       ((int32_t)(now - app->reconnect_due_tick) >= 0)) {
        app->reconnect_due_tick = 0;
        r2_begin_scan(app);
    }
}

int32_t r2d2_controller_app(void* p) {
    UNUSED(p);

    R2App* app = malloc(sizeof(R2App));
    memset(app, 0, sizeof(R2App));

    app->queue = furi_message_queue_alloc(16, sizeof(R2Event));
    app->mutex = furi_mutex_alloc(FuriMutexTypeNormal);
    app->view_port = view_port_alloc();
    app->gui = furi_record_open(RECORD_GUI);
    app->bt = furi_record_open(RECORD_BT);
    app->connection_handle = R2_INVALID_HANDLE;
    app->mode = R2ModeDrive;
    app->running = true;
    strlcpy(app->status, "Starting Bluetooth...", sizeof(app->status));

    view_port_draw_callback_set(app->view_port, r2_draw_callback, app);
    view_port_input_callback_set(app->view_port, r2_input_callback, app);
    gui_add_view_port(app->gui, app->view_port, GuiLayerFullscreen);

    r2_ble_init(&app->ble, app->queue);

    if(furi_hal_bt_get_radio_stack() != FuriHalBtStackFull) {
        r2_set_status(app, R2PhaseError, "BLE Full stack required");
    } else {
        app->profile = bt_profile_start(app->bt, r2d2_ble_profile, &app->ble);
        if(app->profile) {
            furi_hal_bt_stop_advertising();
            r2_begin_scan(app);
        } else {
            r2_set_status(app, R2PhaseError, "R2 Bluetooth profile failed");
        }
    }

    while(app->running) {
        R2Event event;
        if(furi_message_queue_get(app->queue, &event, 50) == FuriStatusOk) {
            if(event.type == R2EventInput) {
                r2_handle_input(app, &event.data.input);
            } else {
                r2_handle_ble_event(app, &event);
            }
        }
        r2_tick(app);
    }

    r2_emergency_stop(app);
    if(app->phase == R2PhaseReady) {
        r2_send(app, CMD_END_APP, sizeof(CMD_END_APP));
    }
    r2_ble_disconnect(app->connection_handle);
    furi_delay_ms(150);

    if(app->profile) {
        bt_profile_restore_default(app->bt);
    }

    gui_remove_view_port(app->gui, app->view_port);
    view_port_free(app->view_port);
    furi_record_close(RECORD_GUI);
    furi_record_close(RECORD_BT);
    furi_mutex_free(app->mutex);
    furi_message_queue_free(app->queue);
    free(app);

    return 0;
}
