#pragma once

#include <furi.h>
#include <input/input.h>

typedef enum {
    R2EventInput,
    R2EventDeviceFound,
    R2EventGapProcedureComplete,
    R2EventConnected,
    R2EventDisconnected,
    R2EventServiceFound,
    R2EventCharacteristicFound,
    R2EventGattProcedureComplete,
} R2EventType;

typedef struct {
    R2EventType type;
    union {
        InputEvent input;
        struct {
            uint8_t address_type;
            uint8_t address[6];
            int8_t rssi;
        } device;
        struct {
            uint8_t procedure_code;
            uint8_t status;
        } gap_proc;
        struct {
            uint8_t status;
            uint16_t connection_handle;
        } connected;
        struct {
            uint8_t reason;
        } disconnected;
        struct {
            uint16_t start_handle;
            uint16_t end_handle;
        } service;
        struct {
            uint16_t value_handle;
        } characteristic;
        struct {
            uint8_t error_code;
        } gatt_proc;
    } data;
} R2Event;
