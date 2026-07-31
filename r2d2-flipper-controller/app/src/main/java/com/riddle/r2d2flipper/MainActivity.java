package com.riddle.r2d2flipper;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public final class MainActivity extends Activity implements R2D2Client.Callback {
    private static final int PERMISSION_REQUEST = 1001;
    private static final long CAM_SETTLE_MS = 400L;

    private enum Motion { NONE, FORWARD, BACKWARD, LEFT, RIGHT }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private R2D2Client client;
    private Motion activeMotion = Motion.NONE;
    private Runnable pendingMotionRunnable;

    private TextView statusView;
    private TextView deviceView;
    private TextView keyView;
    private TextView logView;
    private Button connectButton;
    private Button disconnectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        client = new R2D2Client(this, this);
        setContentView(buildUi());
        onStatus("Disconnected");
        onLog("Pair the Flipper as a Bluetooth remote, then connect to R2-D2.");
    }

    private View buildUi() {
        int pad = dp(10);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.rgb(235, 239, 244));
        root.setFocusableInTouchMode(true);
        root.requestFocus();

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.VERTICAL);
        controls.setPadding(0, 0, pad, 0);
        root.addView(controls, new LinearLayout.LayoutParams(0, -1, 1.25f));

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        root.addView(info, new LinearLayout.LayoutParams(0, -1, 0.75f));

        TextView title = new TextView(this);
        title.setText("R2-D2 + Flipper Zero");
        title.setTextSize(26f);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.rgb(18, 45, 77));
        controls.addView(title);

        LinearLayout connectionRow = horizontalRow();
        connectButton = button("Connect R2-D2");
        disconnectButton = button("Disconnect");
        disconnectButton.setEnabled(false);
        connectionRow.addView(connectButton, weighted());
        connectionRow.addView(disconnectButton, weighted());
        controls.addView(connectionRow);
        connectButton.setOnClickListener(v -> requestPermissionsAndScan());
        disconnectButton.setOnClickListener(v -> disconnect());

        controls.addView(sectionLabel("Hold to drive — release to stop"));
        GridLayout dpad = new GridLayout(this);
        dpad.setColumnCount(3);
        dpad.setRowCount(3);
        dpad.setAlignmentMode(GridLayout.ALIGN_BOUNDS);

        addGridSpacer(dpad);
        Button forward = largeButton("▲\nForward");
        bindMotionButton(forward, Motion.FORWARD);
        dpad.addView(forward, gridCell());
        addGridSpacer(dpad);

        Button left = largeButton("◀\nLeft");
        bindMotionButton(left, Motion.LEFT);
        dpad.addView(left, gridCell());
        Button stop = largeButton("■\nSTOP");
        stop.setOnClickListener(v -> stopMotion());
        dpad.addView(stop, gridCell());
        Button right = largeButton("▶\nRight");
        bindMotionButton(right, Motion.RIGHT);
        dpad.addView(right, gridCell());

        addGridSpacer(dpad);
        Button backward = largeButton("▼\nReverse");
        bindMotionButton(backward, Motion.BACKWARD);
        dpad.addView(backward, gridCell());
        addGridSpacer(dpad);
        controls.addView(dpad, new LinearLayout.LayoutParams(-1, 0, 1f));

        controls.addView(sectionLabel("Head"));
        LinearLayout headRow = horizontalRow();
        headRow.addView(actionButton("Head Left", () -> client.send(Protocol.head(0))), weighted());
        headRow.addView(actionButton("Center", () -> client.send(Protocol.head(1))), weighted());
        headRow.addView(actionButton("Head Right", () -> client.send(Protocol.head(2))), weighted());
        controls.addView(headRow);

        controls.addView(sectionLabel("Lights and sounds"));
        LinearLayout extrasRow = horizontalRow();
        extrasRow.addView(actionButton("Red", () -> client.send(Protocol.led(255, 0))), weighted());
        extrasRow.addView(actionButton("Blue", () -> client.send(Protocol.led(0, 255))), weighted());
        extrasRow.addView(actionButton("Lights Off", () -> client.send(Protocol.led(0, 0))), weighted());
        extrasRow.addView(actionButton("Wake", () -> client.send(Protocol.audio(146))), weighted());
        extrasRow.addView(actionButton("Whistle", () -> client.send(Protocol.audio(152))), weighted());
        controls.addView(extrasRow);

        statusView = infoText(20f, true);
        deviceView = infoText(14f, false);
        keyView = infoText(16f, true);
        keyView.setText("Last Flipper key: none");

        info.addView(sectionLabel("Connection"));
        info.addView(statusView);
        info.addView(deviceView);
        info.addView(sectionLabel("Flipper input"));
        info.addView(keyView);

        TextView mapping = infoText(13f, false);
        mapping.setText(
                "Mapped keys:\n" +
                "Arrows / WASD / numpad = drive\n" +
                "Enter / OK / Space / Play-Pause = stop\n" +
                "Volume Up or Q = head left\n" +
                "Volume Down or E = head right\n" +
                "1 = wake, 2 = whistle, 3 = achievement\n" +
                "R / B / O = red / blue / off\n\n" +
                "The last-key display identifies what the chosen Flipper remote mode sends."
        );
        info.addView(mapping);

        info.addView(sectionLabel("Log"));
        logView = infoText(12f, false);
        logView.setTypeface(Typeface.MONOSPACE);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(logView);
        info.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        return root;
    }

    private void bindMotionButton(Button button, Motion motion) {
        button.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                startMotion(motion);
                view.setPressed(true);
                return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_UP ||
                    event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                stopMotion();
                view.setPressed(false);
                view.performClick();
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (!isMappedKey(keyCode)) return super.dispatchKeyEvent(event);

        keyView.setText("Last Flipper key: " + KeyEvent.keyCodeToString(keyCode));
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getRepeatCount() == 0) handleMappedKeyDown(keyCode);
        } else if (event.getAction() == KeyEvent.ACTION_UP && isMotionKey(keyCode)) {
            stopMotion();
        }
        return true;
    }

    private boolean isMappedKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_W:
            case KeyEvent.KEYCODE_A:
            case KeyEvent.KEYCODE_S:
            case KeyEvent.KEYCODE_D:
            case KeyEvent.KEYCODE_NUMPAD_8:
            case KeyEvent.KEYCODE_NUMPAD_2:
            case KeyEvent.KEYCODE_NUMPAD_4:
            case KeyEvent.KEYCODE_NUMPAD_6:
            case KeyEvent.KEYCODE_PAGE_UP:
            case KeyEvent.KEYCODE_PAGE_DOWN:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_STOP:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_Q:
            case KeyEvent.KEYCODE_E:
            case KeyEvent.KEYCODE_C:
            case KeyEvent.KEYCODE_H:
            case KeyEvent.KEYCODE_1:
            case KeyEvent.KEYCODE_2:
            case KeyEvent.KEYCODE_3:
            case KeyEvent.KEYCODE_R:
            case KeyEvent.KEYCODE_B:
            case KeyEvent.KEYCODE_O:
                return true;
            default:
                return false;
        }
    }

    private boolean isMotionKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_W:
            case KeyEvent.KEYCODE_A:
            case KeyEvent.KEYCODE_S:
            case KeyEvent.KEYCODE_D:
            case KeyEvent.KEYCODE_NUMPAD_8:
            case KeyEvent.KEYCODE_NUMPAD_2:
            case KeyEvent.KEYCODE_NUMPAD_4:
            case KeyEvent.KEYCODE_NUMPAD_6:
            case KeyEvent.KEYCODE_PAGE_UP:
            case KeyEvent.KEYCODE_PAGE_DOWN:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                return true;
            default:
                return false;
        }
    }

    private void handleMappedKeyDown(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_W:
            case KeyEvent.KEYCODE_NUMPAD_8:
            case KeyEvent.KEYCODE_PAGE_UP:
                startMotion(Motion.FORWARD); break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_S:
            case KeyEvent.KEYCODE_NUMPAD_2:
            case KeyEvent.KEYCODE_PAGE_DOWN:
                startMotion(Motion.BACKWARD); break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_A:
            case KeyEvent.KEYCODE_NUMPAD_4:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                startMotion(Motion.LEFT); break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_D:
            case KeyEvent.KEYCODE_NUMPAD_6:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                startMotion(Motion.RIGHT); break;
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_SPACE:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_STOP:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                stopMotion(); break;
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_Q:
                client.send(Protocol.head(0)); break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_E:
                client.send(Protocol.head(2)); break;
            case KeyEvent.KEYCODE_C:
            case KeyEvent.KEYCODE_H:
                client.send(Protocol.head(1)); break;
            case KeyEvent.KEYCODE_1:
                client.send(Protocol.audio(146)); break;
            case KeyEvent.KEYCODE_2:
                client.send(Protocol.audio(152)); break;
            case KeyEvent.KEYCODE_3:
                client.send(Protocol.audio(157)); break;
            case KeyEvent.KEYCODE_R:
                client.send(Protocol.led(255, 0)); break;
            case KeyEvent.KEYCODE_B:
                client.send(Protocol.led(0, 255)); break;
            case KeyEvent.KEYCODE_O:
                client.send(Protocol.led(0, 0)); break;
            default: break;
        }
    }

    private void startMotion(Motion motion) {
        if (!client.isReady()) {
            toast("Connect to R2-D2 first.");
            return;
        }
        cancelPendingMotion();
        if (activeMotion != Motion.NONE && activeMotion != motion) client.send(Protocol.motor(Protocol.MOTOR_STOP));
        activeMotion = motion;

        byte cam;
        byte direction;
        switch (motion) {
            case FORWARD:
                cam = Protocol.CAM_DRIVE; direction = Protocol.MOTOR_FORWARD; break;
            case BACKWARD:
                cam = Protocol.CAM_DRIVE; direction = Protocol.MOTOR_BACKWARD; break;
            case LEFT:
                cam = Protocol.CAM_PIVOT_LEFT; direction = Protocol.MOTOR_FORWARD; break;
            case RIGHT:
                cam = Protocol.CAM_PIVOT_RIGHT; direction = Protocol.MOTOR_FORWARD; break;
            default:
                return;
        }

        client.send(Protocol.cam(cam));
        pendingMotionRunnable = () -> {
            pendingMotionRunnable = null;
            if (activeMotion == motion) client.send(Protocol.motor(direction));
        };
        handler.postDelayed(pendingMotionRunnable, CAM_SETTLE_MS);
        onLog("Motion: " + motion.name().toLowerCase(Locale.US));
    }

    private void stopMotion() {
        cancelPendingMotion();
        activeMotion = Motion.NONE;
        client.send(Protocol.motor(Protocol.MOTOR_STOP));
        client.send(Protocol.stopAll());
        onLog("Motion: stop");
    }

    private void cancelPendingMotion() {
        if (pendingMotionRunnable != null) {
            handler.removeCallbacks(pendingMotionRunnable);
            pendingMotionRunnable = null;
        }
    }

    private void requestPermissionsAndScan() {
        BluetoothAdapter adapter = client.adapter();
        if (adapter == null) {
            toast("Bluetooth is not available on this tablet.");
            return;
        }
        if (!adapter.isEnabled()) {
            startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            toast("Turn Bluetooth on, then tap Connect again.");
            return;
        }

        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        } else {
            permissions = new String[0];
        }

        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(permissions, PERMISSION_REQUEST);
                return;
            }
        }
        client.startScan();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != PERMISSION_REQUEST) return;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                toast("Bluetooth permissions are required.");
                return;
            }
        }
        client.startScan();
    }

    private void disconnect() {
        cancelPendingMotion();
        activeMotion = Motion.NONE;
        client.disconnect();
    }

    @Override
    protected void onDestroy() {
        disconnect();
        super.onDestroy();
    }

    @Override
    public void onStatus(String status) {
        runOnUiThread(() -> statusView.setText(status));
    }

    @Override
    public void onDevice(String device) {
        runOnUiThread(() -> deviceView.setText(device));
    }

    @Override
    public void onLog(String message) {
        runOnUiThread(() -> {
            if (logView == null) return;
            String next = logView.getText() + message + "\n";
            if (next.length() > 5000) next = next.substring(next.length() - 5000);
            logView.setText(next);
        });
    }

    @Override
    public void onConnected(boolean connected) {
        runOnUiThread(() -> {
            connectButton.setEnabled(!connected);
            disconnectButton.setEnabled(connected);
            if (!connected) deviceView.setText("");
        });
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private LinearLayout horizontalRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));
        return row;
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(52), 1f);
        params.setMargins(dp(3), dp(3), dp(3), dp(3));
        return params;
    }

    private GridLayout.LayoutParams gridCell() {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = 0;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        return params;
    }

    private void addGridSpacer(GridLayout grid) {
        grid.addView(new View(this), gridCell());
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(15f);
        button.setAllCaps(false);
        return button;
    }

    private Button largeButton(String text) {
        Button button = button(text);
        button.setTextSize(19f);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        return button;
    }

    private Button actionButton(String text, Runnable action) {
        Button button = button(text);
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private TextView sectionLabel(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(15f);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(Color.rgb(38, 78, 120));
        view.setPadding(0, dp(7), 0, dp(2));
        return view;
    }

    private TextView infoText(float size, boolean bold) {
        TextView view = new TextView(this);
        view.setTextSize(size);
        view.setTextColor(Color.rgb(25, 25, 25));
        view.setPadding(dp(4), dp(2), dp(4), dp(2));
        if (bold) view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
