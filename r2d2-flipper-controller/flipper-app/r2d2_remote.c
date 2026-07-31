#include <furi.h>
#include <furi_hal_bt.h>
#include <extra_profiles/hid_profile.h>
#include <bt/bt_service/bt.h>
#include <gui/gui.h>
#include <gui/view.h>
#include <gui/view_dispatcher.h>
#include <storage/storage.h>

#define R2D2_BT_KEYS_STORAGE_NAME ".r2d2_hid.keys"
#define R2D2_VIEW_ID 0

/* USB HID keyboard usage IDs. */
#define HID_KEY_A 0x04
#define HID_KEY_B 0x05
#define HID_KEY_C 0x06
#define HID_KEY_E 0x08
#define HID_KEY_O 0x12
#define HID_KEY_Q 0x14
#define HID_KEY_R 0x15
#define HID_KEY_1 0x1E
#define HID_KEY_2 0x1F
#define HID_KEY_3 0x20
#define HID_KEY_SPACE 0x2C
#define HID_KEY_RIGHT 0x4F
#define HID_KEY_LEFT 0x50
#define HID_KEY_DOWN 0x51
#define HID_KEY_UP 0x52

typedef enum {
    R2D2ModeDrive = 0,
    R2D2ModeHead,
    R2D2ModeExtras,
    R2D2ModeCount,
} R2D2Mode;

typedef struct {
    bool connected;
    R2D2Mode mode;
    InputKey pressed_key;
} R2D2RemoteModel;

typedef struct {
    Bt* bt;
    Gui* gui;
    ViewDispatcher* view_dispatcher;
    View* view;
    FuriHalBleProfileBase* ble_hid_profile;
} R2D2Remote;

static const char* r2d2_mode_name(R2D2Mode mode) {
    switch(mode) {
    case R2D2ModeDrive:
        return "DRIVE";
    case R2D2ModeHead:
        return "HEAD / SOUND";
    case R2D2ModeExtras:
        return "LIGHTS / SOUND";
    default:
        return "UNKNOWN";
    }
}

static uint16_t r2d2_key_for_input(R2D2Mode mode, InputKey input) {
    switch(mode) {
    case R2D2ModeDrive:
        switch(input) {
        case InputKeyUp:
            return HID_KEY_UP;
        case InputKeyDown:
            return HID_KEY_DOWN;
        case InputKeyLeft:
            return HID_KEY_LEFT;
        case InputKeyRight:
            return HID_KEY_RIGHT;
        case InputKeyOk:
            return HID_KEY_SPACE;
        default:
            return 0;
        }

    case R2D2ModeHead:
        switch(input) {
        case InputKeyLeft:
            return HID_KEY_Q;
        case InputKeyRight:
            return HID_KEY_E;
        case InputKeyOk:
            return HID_KEY_C;
        case InputKeyUp:
            return HID_KEY_1;
        case InputKeyDown:
            return HID_KEY_2;
        default:
            return 0;
        }

    case R2D2ModeExtras:
        switch(input) {
        case InputKeyUp:
            return HID_KEY_1;
        case InputKeyDown:
            return HID_KEY_2;
        case InputKeyLeft:
            return HID_KEY_R;
        case InputKeyRight:
            return HID_KEY_B;
        case InputKeyOk:
            return HID_KEY_O;
        default:
            return 0;
        }

    default:
        return 0;
    }
}

static void r2d2_draw_centered(Canvas* canvas, uint8_t y, Font font, const char* text) {
    canvas_set_font(canvas, font);
    canvas_draw_str_aligned(canvas, 64, y, AlignCenter, AlignTop, text);
}

static void r2d2_remote_draw(Canvas* canvas, void* context) {
    furi_assert(context);
    R2D2RemoteModel* model = context;

    canvas_clear(canvas);
    r2d2_draw_centered(canvas, 1, FontPrimary, "R2-D2 Remote");

    canvas_set_font(canvas, FontSecondary);
    canvas_draw_str(canvas, 2, 14, model->connected ? "BT: CONNECTED" : "BT: PAIR TABLET");
    canvas_draw_str(canvas, 91, 14, r2d2_mode_name(model->mode));
    canvas_draw_line(canvas, 0, 17, 127, 17);

    canvas_set_font(canvas, FontSecondary);
    switch(model->mode) {
    case R2D2ModeDrive:
        canvas_draw_str(canvas, 4, 28, "D-pad: drive / turn");
        canvas_draw_str(canvas, 4, 40, "Release: automatic stop");
        canvas_draw_str(canvas, 4, 52, "OK: emergency stop");
        break;
    case R2D2ModeHead:
        canvas_draw_str(canvas, 4, 28, "Left/Right: head");
        canvas_draw_str(canvas, 4, 40, "OK: center head");
        canvas_draw_str(canvas, 4, 52, "Up: wake  Down: whistle");
        break;
    case R2D2ModeExtras:
        canvas_draw_str(canvas, 4, 28, "Left: red  Right: blue");
        canvas_draw_str(canvas, 4, 40, "OK: lights off");
        canvas_draw_str(canvas, 4, 52, "Up: wake  Down: whistle");
        break;
    default:
        break;
    }

    canvas_draw_line(canvas, 0, 55, 127, 55);
    canvas_set_font(canvas, FontSecondary);
    canvas_draw_str(canvas, 2, 63, "Back: mode    Hold Back: exit");
}

static void r2d2_remote_press(R2D2Remote* app, uint16_t key) {
    if(key && app->ble_hid_profile) {
        ble_profile_hid_kb_press(app->ble_hid_profile, key);
    }
}

static void r2d2_remote_release(R2D2Remote* app, uint16_t key) {
    if(key && app->ble_hid_profile) {
        ble_profile_hid_kb_release(app->ble_hid_profile, key);
    }
}

static void r2d2_remote_release_all(R2D2Remote* app) {
    if(app->ble_hid_profile) {
        ble_profile_hid_kb_release_all(app->ble_hid_profile);
    }
}

static bool r2d2_remote_input(InputEvent* event, void* context) {
    furi_assert(event);
    furi_assert(context);
    R2D2Remote* app = context;

    if(event->key == InputKeyBack && event->type == InputTypeLong) {
        r2d2_remote_release_all(app);
        view_dispatcher_stop(app->view_dispatcher);
        return true;
    }

    if(event->key == InputKeyBack && event->type == InputTypeShort) {
        r2d2_remote_release_all(app);
        with_view_model(
            app->view,
            R2D2RemoteModel * model,
            {
                model->mode = (R2D2Mode)((model->mode + 1) % R2D2ModeCount);
                model->pressed_key = InputKeyMAX;
            },
            true);
        return true;
    }

    if(event->key == InputKeyBack) return true;

    R2D2Mode mode = R2D2ModeDrive;
    with_view_model(
        app->view, R2D2RemoteModel * model, { mode = model->mode; }, false);
    const uint16_t hid_key = r2d2_key_for_input(mode, event->key);
    if(!hid_key) return true;

    if(event->type == InputTypePress) {
        with_view_model(
            app->view,
            R2D2RemoteModel * model,
            { model->pressed_key = event->key; },
            true);
        r2d2_remote_press(app, hid_key);
    } else if(event->type == InputTypeRelease) {
        r2d2_remote_release(app, hid_key);
        with_view_model(
            app->view,
            R2D2RemoteModel * model,
            { model->pressed_key = InputKeyMAX; },
            true);
    }

    return true;
}

static void r2d2_bt_status_changed(BtStatus status, void* context) {
    furi_assert(context);
    R2D2Remote* app = context;
    const bool connected = status == BtStatusConnected;
    with_view_model(
        app->view,
        R2D2RemoteModel * model,
        { model->connected = connected; },
        true);
}

static R2D2Remote* r2d2_remote_alloc(void) {
    R2D2Remote* app = malloc(sizeof(R2D2Remote));
    furi_check(app);

    app->gui = furi_record_open(RECORD_GUI);
    app->bt = furi_record_open(RECORD_BT);
    app->ble_hid_profile = NULL;

    app->view_dispatcher = view_dispatcher_alloc();
    view_dispatcher_attach_to_gui(app->view_dispatcher, app->gui, ViewDispatcherTypeFullscreen);

    app->view = view_alloc();
    view_set_context(app->view, app);
    view_allocate_model(app->view, ViewModelTypeLocking, sizeof(R2D2RemoteModel));
    view_set_draw_callback(app->view, r2d2_remote_draw);
    view_set_input_callback(app->view, r2d2_remote_input);
    with_view_model(
        app->view,
        R2D2RemoteModel * model,
        {
            model->connected = false;
            model->mode = R2D2ModeDrive;
            model->pressed_key = InputKeyMAX;
        },
        false);

    view_dispatcher_add_view(app->view_dispatcher, R2D2_VIEW_ID, app->view);
    view_dispatcher_switch_to_view(app->view_dispatcher, R2D2_VIEW_ID);

    return app;
}

static void r2d2_remote_free(R2D2Remote* app) {
    furi_assert(app);

    view_dispatcher_remove_view(app->view_dispatcher, R2D2_VIEW_ID);
    view_free(app->view);
    view_dispatcher_free(app->view_dispatcher);

    furi_record_close(RECORD_BT);
    furi_record_close(RECORD_GUI);
    free(app);
}

int32_t r2d2_remote_app(void* context) {
    UNUSED(context);
    R2D2Remote* app = r2d2_remote_alloc();

    bt_disconnect(app->bt);
    furi_delay_ms(200);

    bt_keys_storage_set_storage_path(app->bt, APP_DATA_PATH(R2D2_BT_KEYS_STORAGE_NAME));
    app->ble_hid_profile = bt_profile_start(app->bt, ble_profile_hid, NULL);
    furi_check(app->ble_hid_profile);

    furi_hal_bt_start_advertising();
    bt_set_status_changed_callback(app->bt, r2d2_bt_status_changed, app);

    view_dispatcher_run(app->view_dispatcher);

    r2d2_remote_release_all(app);
    bt_set_status_changed_callback(app->bt, NULL, NULL);
    bt_disconnect(app->bt);
    furi_delay_ms(200);
    bt_keys_storage_set_default_path(app->bt);
    furi_check(bt_profile_restore_default(app->bt));

    r2d2_remote_free(app);
    return 0;
}
