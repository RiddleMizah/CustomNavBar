#include "r2d2_profile.h"
#include "r2d2_ble.h"

#include <furi.h>
#include <furi_hal_version.h>
#include <gap.h>
#include <ble/core/ble_defs.h>

#include <stddef.h>
#include <string.h>

typedef struct {
    FuriHalBleProfileBase base;
    GapSvcEventHandler* event_handler;
} R2BleProfile;

_Static_assert(offsetof(R2BleProfile, base) == 0, "Wrong profile layout");

static const FuriHalBleProfileTemplate profile_callbacks;

static FuriHalBleProfileBase* r2_profile_start(FuriHalBleProfileParams params) {
    furi_check(params);

    R2BleProfile* profile = malloc(sizeof(R2BleProfile));
    profile->base.config = &profile_callbacks;
    profile->event_handler = ble_event_dispatcher_register_svc_handler(
        r2_ble_event_handler,
        params);

    return &profile->base;
}

static void r2_profile_stop(FuriHalBleProfileBase* base) {
    furi_check(base);
    furi_check(base->config == &profile_callbacks);

    R2BleProfile* profile = (R2BleProfile*)base;
    ble_event_dispatcher_unregister_svc_handler(profile->event_handler);
    free(profile);
}

#define CONNECTION_INTERVAL_MIN (0x06)
#define CONNECTION_INTERVAL_MAX (0x24)

static const GapConfig r2_gap_template = {
    .adv_service =
        {
            .UUID_Type = UUID_TYPE_16,
            .Service_UUID_16 = 0x3080,
        },
    .appearance_char = 0x8600,
    .bonding_mode = false,
    .pairing_method = GapPairingNone,
    .conn_param = {
        .conn_int_min = CONNECTION_INTERVAL_MIN,
        .conn_int_max = CONNECTION_INTERVAL_MAX,
        .slave_latency = 0,
        .supervisor_timeout = 0,
    },
};

static void r2_profile_get_gap_config(
    GapConfig* config,
    FuriHalBleProfileParams params) {
    UNUSED(params);
    furi_check(config);

    memcpy(config, &r2_gap_template, sizeof(GapConfig));
    memcpy(config->mac_address, furi_hal_version_get_ble_mac(), sizeof(config->mac_address));
    strlcpy(
        config->adv_name,
        furi_hal_version_get_ble_local_device_name_ptr(),
        FURI_HAL_VERSION_DEVICE_NAME_LENGTH);
}

static const FuriHalBleProfileTemplate profile_callbacks = {
    .start = r2_profile_start,
    .stop = r2_profile_stop,
    .get_gap_config = r2_profile_get_gap_config,
};

const FuriHalBleProfileTemplate* const r2d2_ble_profile = &profile_callbacks;
