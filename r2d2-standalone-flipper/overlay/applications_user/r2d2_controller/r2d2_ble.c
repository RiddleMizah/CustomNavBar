#include "r2d2_ble.h"

#include <ble/ble.h>
#include <string.h>

#define TAG "R2Ble"

#define R2_SCAN_INTERVAL 0x0050
#define R2_SCAN_WINDOW   0x0050
#define R2_OWN_ADDR_TYPE 0x00

static const uint8_t R2_SERVICE_UUID[16] = {
    0xE4, 0x3B, 0x61, 0x62, 0xD5, 0xBC, 0x41, 0xB0,
    0x9C, 0xE2, 0xA1, 0xB5, 0x35, 0x14, 0xB9, 0xDA,
};

static const uint8_t R2_WRITE_UUID[16] = {
    0xE4, 0x3B, 0x61, 0x62, 0xD5, 0xBC, 0x41, 0xB0,
    0x9C, 0xE2, 0xA1, 0xB5, 0x83, 0x13, 0xB9, 0xDA,
};

static void r2_queue_event(R2Ble* ble, const R2Event* event) {
    furi_message_queue_put(ble->queue, event, 0);
}

static bool r2_name_matches(const uint8_t* data, uint8_t data_len) {
    uint8_t offset = 0;
    while(offset < data_len) {
        const uint8_t field_len = data[offset];
        if(field_len == 0) break;
        if((uint16_t)offset + field_len >= data_len + 1U) break;

        const uint8_t type = data[offset + 1];
        if((type == AD_TYPE_COMPLETE_LOCAL_NAME) || (type == AD_TYPE_SHORTENED_LOCAL_NAME)) {
            const uint8_t name_len = field_len - 1;
            const uint8_t* name = &data[offset + 2];
            if((name_len >= 5) && (memcmp(name, "Kipps", 5) == 0)) return true;
            if((name_len >= 8) && (memcmp(name, "2ndHeroD", 8) == 0)) return true;
        }
        offset += field_len + 1;
    }
    return false;
}

void r2_ble_init(R2Ble* ble, FuriMessageQueue* queue) {
    furi_check(ble);
    furi_check(queue);
    ble->queue = queue;
}

BleEventAckStatus r2_ble_event_handler(void* payload, void* context) {
    furi_check(payload);
    furi_check(context);

    R2Ble* ble = context;
    hci_event_pckt* event_pckt = (hci_event_pckt*)((hci_uart_pckt*)payload)->data;

    if(event_pckt->evt == HCI_LE_META_EVT_CODE) {
        evt_le_meta_event* meta_evt = (evt_le_meta_event*)event_pckt->data;

        if(meta_evt->subevent == HCI_LE_ADVERTISING_REPORT_SUBEVT_CODE) {
            hci_le_advertising_report_event_rp0* event =
                (hci_le_advertising_report_event_rp0*)meta_evt->data;

            for(uint8_t i = 0; i < event->Num_Reports; i++) {
                Advertising_Report_t* report = &event->Advertising_Report[i];
                if(r2_name_matches(report->Data, report->Length_Data)) {
                    R2Event app_event = {.type = R2EventDeviceFound};
                    app_event.data.device.address_type = report->Address_Type;
                    memcpy(app_event.data.device.address, report->Address, 6);
                    app_event.data.device.rssi = (int8_t)report->RSSI;
                    r2_queue_event(ble, &app_event);
                    break;
                }
            }
        } else if(meta_evt->subevent == HCI_LE_CONNECTION_COMPLETE_SUBEVT_CODE) {
            hci_le_connection_complete_event_rp0* event =
                (hci_le_connection_complete_event_rp0*)meta_evt->data;
            R2Event app_event = {.type = R2EventConnected};
            app_event.data.connected.status = event->Status;
            app_event.data.connected.connection_handle = event->Connection_Handle;
            r2_queue_event(ble, &app_event);
        }

        return BleEventAckFlowEnable;
    }

    if(event_pckt->evt == HCI_DISCONNECTION_COMPLETE_EVT_CODE) {
        hci_disconnection_complete_event_rp0* event =
            (hci_disconnection_complete_event_rp0*)event_pckt->data;
        R2Event app_event = {.type = R2EventDisconnected};
        app_event.data.disconnected.reason = event->Reason;
        r2_queue_event(ble, &app_event);
        return BleEventAckFlowEnable;
    }

    if(event_pckt->evt == HCI_VENDOR_SPECIFIC_DEBUG_EVT_CODE) {
        evt_blecore_aci* blue_evt = (evt_blecore_aci*)event_pckt->data;

        switch(blue_evt->ecode) {
        case ACI_GAP_PROC_COMPLETE_VSEVT_CODE: {
            aci_gap_proc_complete_event_rp0* event =
                (aci_gap_proc_complete_event_rp0*)blue_evt->data;
            R2Event app_event = {.type = R2EventGapProcedureComplete};
            app_event.data.gap_proc.procedure_code = event->Procedure_Code;
            app_event.data.gap_proc.status = event->Status;
            r2_queue_event(ble, &app_event);
            return BleEventAckFlowEnable;
        }

        case ACI_ATT_FIND_BY_TYPE_VALUE_RESP_VSEVT_CODE: {
            aci_att_find_by_type_value_resp_event_rp0* event =
                (aci_att_find_by_type_value_resp_event_rp0*)blue_evt->data;
            if(event->Num_of_Handle_Pair > 0) {
                R2Event app_event = {.type = R2EventServiceFound};
                app_event.data.service.start_handle =
                    event->Attribute_Group_Handle_Pair[0].Found_Attribute_Handle;
                app_event.data.service.end_handle =
                    event->Attribute_Group_Handle_Pair[0].Group_End_Handle;
                r2_queue_event(ble, &app_event);
            }
            return BleEventAckFlowEnable;
        }

        case ACI_ATT_READ_BY_TYPE_RESP_VSEVT_CODE: {
            aci_att_read_by_type_resp_event_rp0* event =
                (aci_att_read_by_type_resp_event_rp0*)blue_evt->data;
            if((event->Data_Length >= 5) && (event->Handle_Value_Pair_Length >= 5)) {
                const uint8_t* item = event->Handle_Value_Pair_Data;
                R2Event app_event = {.type = R2EventCharacteristicFound};
                app_event.data.characteristic.value_handle =
                    (uint16_t)item[3] | ((uint16_t)item[4] << 8);
                r2_queue_event(ble, &app_event);
            }
            return BleEventAckFlowEnable;
        }

        case ACI_GATT_PROC_COMPLETE_VSEVT_CODE: {
            aci_gatt_proc_complete_event_rp0* event =
                (aci_gatt_proc_complete_event_rp0*)blue_evt->data;
            R2Event app_event = {.type = R2EventGattProcedureComplete};
            app_event.data.gatt_proc.error_code = event->Error_Code;
            r2_queue_event(ble, &app_event);
            return BleEventAckFlowEnable;
        }

        case ACI_GATT_ERROR_RESP_VSEVT_CODE:
            return BleEventAckFlowEnable;

        default:
            break;
        }
    }

    return BleEventNotAck;
}

bool r2_ble_start_scan(void) {
    const tBleStatus status = aci_gap_start_general_discovery_proc(
        R2_SCAN_INTERVAL,
        R2_SCAN_WINDOW,
        R2_OWN_ADDR_TYPE,
        0x00);
    FURI_LOG_I(TAG, "scan start: %u", status);
    return status == BLE_STATUS_SUCCESS;
}

bool r2_ble_stop_scan(void) {
    const tBleStatus status = aci_gap_terminate_gap_proc(GAP_GENERAL_DISCOVERY_PROC);
    FURI_LOG_I(TAG, "scan stop: %u", status);
    return status == BLE_STATUS_SUCCESS;
}

bool r2_ble_connect(uint8_t address_type, const uint8_t address[6]) {
    const tBleStatus status = aci_gap_create_connection(
        0x0060,
        0x0030,
        address_type,
        address,
        R2_OWN_ADDR_TYPE,
        0x0006,
        0x0018,
        0x0000,
        0x0190,
        0x0000,
        0x0000);
    FURI_LOG_I(TAG, "connect: %u", status);
    return status == BLE_STATUS_SUCCESS;
}

bool r2_ble_discover_service(uint16_t connection_handle) {
    UUID_t uuid = {0};
    memcpy(uuid.UUID_128, R2_SERVICE_UUID, sizeof(R2_SERVICE_UUID));
    const tBleStatus status =
        aci_gatt_disc_primary_service_by_uuid(connection_handle, UUID_TYPE_128, &uuid);
    FURI_LOG_I(TAG, "service discovery: %u", status);
    return status == BLE_STATUS_SUCCESS;
}

bool r2_ble_discover_write_characteristic(
    uint16_t connection_handle,
    uint16_t start_handle,
    uint16_t end_handle) {
    UUID_t uuid = {0};
    memcpy(uuid.UUID_128, R2_WRITE_UUID, sizeof(R2_WRITE_UUID));
    const tBleStatus status = aci_gatt_disc_char_by_uuid(
        connection_handle,
        start_handle,
        end_handle,
        UUID_TYPE_128,
        &uuid);
    FURI_LOG_I(TAG, "characteristic discovery: %u", status);
    return status == BLE_STATUS_SUCCESS;
}

bool r2_ble_write(
    uint16_t connection_handle,
    uint16_t characteristic_handle,
    const uint8_t* data,
    uint8_t length) {
    if((connection_handle == 0xFFFF) || (characteristic_handle == 0)) return false;
    const tBleStatus status = aci_gatt_write_without_resp(
        connection_handle,
        characteristic_handle,
        length,
        data);
    return status == BLE_STATUS_SUCCESS;
}

void r2_ble_disconnect(uint16_t connection_handle) {
    if(connection_handle != 0xFFFF) {
        aci_gap_terminate(connection_handle, 0x13);
    }
}
