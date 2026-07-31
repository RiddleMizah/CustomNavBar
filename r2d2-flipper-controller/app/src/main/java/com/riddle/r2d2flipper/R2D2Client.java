package com.riddle.r2d2flipper;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayDeque;
import java.util.Queue;

final class R2D2Client {
    interface Callback {
        void onStatus(String status);
        void onDevice(String device);
        void onLog(String message);
        void onConnected(boolean connected);
    }

    private static final long SCAN_TIMEOUT_MS = 15_000L;
    private static final long KEEPALIVE_INTERVAL_MS = 2_000L;

    private final Context context;
    private final Callback callback;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Queue<byte[]> pendingWrites = new ArrayDeque<>();
    private final BluetoothAdapter adapter;

    private BluetoothLeScanner scanner;
    private BluetoothGatt gatt;
    private BluetoothGattCharacteristic writeCharacteristic;
    private boolean scanning;
    private boolean ready;
    private Runnable keepAliveRunnable;
    private final Runnable scanTimeoutRunnable = this::onScanTimeout;

    R2D2Client(Context context, Callback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager == null ? null : manager.getAdapter();
    }

    BluetoothAdapter adapter() {
        return adapter;
    }

    boolean isReady() {
        return ready;
    }

    @SuppressLint("MissingPermission")
    void startScan() {
        if (adapter == null) {
            callback.onStatus("Bluetooth unavailable");
            callback.onLog("This tablet does not expose a Bluetooth adapter.");
            return;
        }
        if (scanning || ready) return;

        scanner = adapter.getBluetoothLeScanner();
        if (scanner == null) {
            callback.onStatus("BLE scanner unavailable");
            return;
        }

        scanning = true;
        callback.onStatus("Scanning for R2-D2…");
        callback.onLog("Scanning for Kipps / 2ndHeroD for 15 seconds.");

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        scanner.startScan(null, settings, scanCallback);
        handler.postDelayed(scanTimeoutRunnable, SCAN_TIMEOUT_MS);
    }

    private void onScanTimeout() {
        if (!scanning) return;
        stopScan();
        callback.onStatus("R2-D2 not found");
        callback.onLog("Scan timed out. Confirm R2-D2 is switched on and nearby.");
    }

    @SuppressLint("MissingPermission")
    private void stopScan() {
        if (!scanning) return;
        scanning = false;
        handler.removeCallbacks(scanTimeoutRunnable);
        if (scanner != null) {
            try {
                scanner.stopScan(scanCallback);
            } catch (SecurityException ignored) {
                // Permission can be revoked while scanning.
            }
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        @SuppressLint("MissingPermission")
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String name = device.getName();
            if (name == null && result.getScanRecord() != null) {
                name = result.getScanRecord().getDeviceName();
            }
            if (!matchesR2D2Name(name)) return;

            stopScan();
            callback.onDevice((name == null ? "R2-D2" : name) + "  " + device.getAddress());
            callback.onStatus("Connecting…");
            callback.onLog("Found " + name + " at " + device.getAddress());
            gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
        }

        @Override
        public void onScanFailed(int errorCode) {
            scanning = false;
            callback.onStatus("BLE scan failed");
            callback.onLog("Android BLE scan error: " + errorCode);
        }
    };

    private boolean matchesR2D2Name(String name) {
        if (name == null) return false;
        for (String prefix : Protocol.DEVICE_PREFIXES) {
            if (name.startsWith(prefix)) return true;
        }
        return false;
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        @SuppressLint("MissingPermission")
        public void onConnectionStateChange(BluetoothGatt callbackGatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt = callbackGatt;
                callback.onLog("BLE connected; discovering R2-D2 services.");
                boolean mtuRequested = callbackGatt.requestMtu(512);
                if (!mtuRequested) callbackGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                callback.onLog("R2-D2 disconnected (status " + status + ").");
                closeGatt(callbackGatt);
                markDisconnected();
            }
        }

        @Override
        @SuppressLint("MissingPermission")
        public void onMtuChanged(BluetoothGatt callbackGatt, int mtu, int status) {
            callbackGatt.discoverServices();
        }

        @Override
        @SuppressLint("MissingPermission")
        public void onServicesDiscovered(BluetoothGatt callbackGatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                callback.onLog("Service discovery failed: " + status);
                disconnect();
                return;
            }

            BluetoothGattService service = callbackGatt.getService(Protocol.SERVICE_UUID);
            if (service == null) {
                callback.onLog("Hasbro R2-D2 BLE service was not found.");
                disconnect();
                return;
            }

            writeCharacteristic = service.getCharacteristic(Protocol.WRITE_UUID);
            BluetoothGattCharacteristic notifyCharacteristic = service.getCharacteristic(Protocol.NOTIFY_UUID);
            if (writeCharacteristic == null) {
                callback.onLog("R2-D2 write characteristic is missing.");
                disconnect();
                return;
            }

            if (notifyCharacteristic != null) enableNotifications(callbackGatt, notifyCharacteristic);

            ready = true;
            callback.onStatus("Connected");
            callback.onConnected(true);
            callback.onLog("R2-D2 is ready.");
            startKeepAlive();
            send(Protocol.KEEP_ALIVE);
            drainPendingWrites();
        }

        @Override
        public void onCharacteristicWrite(
                BluetoothGatt callbackGatt,
                BluetoothGattCharacteristic characteristic,
                int status
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                callback.onLog("BLE write failed: " + status);
            }
        }
    };

    @SuppressLint("MissingPermission")
    private void enableNotifications(BluetoothGatt callbackGatt, BluetoothGattCharacteristic characteristic) {
        if (!callbackGatt.setCharacteristicNotification(characteristic, true)) {
            callback.onLog("Could not enable R2-D2 notifications; control may still work.");
            return;
        }
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Protocol.CCCD_UUID);
        if (descriptor == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            callbackGatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            callbackGatt.writeDescriptor(descriptor);
        }
    }

    private void startKeepAlive() {
        stopKeepAlive();
        keepAliveRunnable = new Runnable() {
            @Override
            public void run() {
                if (!ready) return;
                send(Protocol.KEEP_ALIVE);
                handler.postDelayed(this, KEEPALIVE_INTERVAL_MS);
            }
        };
        handler.postDelayed(keepAliveRunnable, KEEPALIVE_INTERVAL_MS);
    }

    private void stopKeepAlive() {
        if (keepAliveRunnable != null) {
            handler.removeCallbacks(keepAliveRunnable);
            keepAliveRunnable = null;
        }
    }

    synchronized boolean send(byte[] data) {
        if (!ready || gatt == null || writeCharacteristic == null) {
            if (pendingWrites.size() < 32) pendingWrites.add(data.clone());
            return false;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                int result = gatt.writeCharacteristic(
                        writeCharacteristic,
                        data,
                        BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                );
                return result == BluetoothStatusCodes.SUCCESS;
            }
            writeCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            writeCharacteristic.setValue(data);
            return gatt.writeCharacteristic(writeCharacteristic);
        } catch (SecurityException ex) {
            callback.onLog("Bluetooth write permission error: " + ex.getMessage());
            return false;
        }
    }

    private synchronized void drainPendingWrites() {
        while (ready && !pendingWrites.isEmpty()) {
            byte[] data = pendingWrites.poll();
            if (data != null) send(data);
        }
    }

    @SuppressLint("MissingPermission")
    void disconnect() {
        stopScan();
        stopKeepAlive();
        ready = false;
        pendingWrites.clear();
        writeCharacteristic = null;

        BluetoothGatt current = gatt;
        gatt = null;
        if (current != null) {
            try {
                current.disconnect();
                current.close();
            } catch (SecurityException ignored) {
                // Continue UI cleanup.
            }
        }
        markDisconnected();
    }

    private void markDisconnected() {
        ready = false;
        stopKeepAlive();
        callback.onStatus("Disconnected");
        callback.onConnected(false);
    }

    @SuppressLint("MissingPermission")
    private void closeGatt(BluetoothGatt callbackGatt) {
        try {
            callbackGatt.close();
        } catch (Exception ignored) {
            // Already closed.
        }
        if (gatt == callbackGatt) gatt = null;
        writeCharacteristic = null;
    }
}
