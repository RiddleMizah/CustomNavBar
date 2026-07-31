#pragma once

#include <furi.h>
#include <app_common.h>
#include <ble/ble.h>
#include <furi_ble/event_dispatcher.h>
#include "r2d2_events.h"

typedef struct {
    FuriMessageQueue* queue;
} R2Ble;

void r2_ble_init(R2Ble* ble, FuriMessageQueue* queue);
BleEventAckStatus r2_ble_event_handler(void* event, void* context);

bool r2_ble_start_scan(void);
bool r2_ble_stop_scan(void);
bool r2_ble_connect(uint8_t address_type, const uint8_t address[6]);
bool r2_ble_discover_service(uint16_t connection_handle);
bool r2_ble_discover_write_characteristic(
    uint16_t connection_handle,
    uint16_t start_handle,
    uint16_t end_handle);
bool r2_ble_write(
    uint16_t connection_handle,
    uint16_t characteristic_handle,
    const uint8_t* data,
    uint8_t length);
void r2_ble_disconnect(uint16_t connection_handle);
