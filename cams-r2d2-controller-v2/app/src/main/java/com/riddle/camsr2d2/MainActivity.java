package com.riddle.camsr2d2;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity implements
        R2D2Client.Callback,
        RoutinePlayer.Listener {

    static final int HOME = 0;
    static final int DRIVE = 1;
    static final int DANCES = 2;
    static final int MY_MOVES = 3;
    static final int PARENT = 4;

    private static final int PERMISSION_REQUEST = 1001;
    private final RoutineRecorder recorder = new RoutineRecorder();
    private final List<Routine> routines = new ArrayList<>();

    private R2D2Client client;
    private RoutineStore store;
    private RoutinePlayer player;
    private FrameLayout content;
    private TextView statusPill;
    private TextView deviceLabel;
    private TextView recordingBanner;
    private TextView playbackBanner;
    private Button connectButton;
    private MotionController motion;
    private String latestStatus = "Not connected";
    private String latestDevice = "";

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        store = new RoutineStore(this);
        routines.addAll(store.load());
        client = new R2D2Client(this, this);
        motion = new MotionController(this, client, recorder);
        player = new RoutinePlayer(motion, this);
        setContentView(buildShell());
        showScreen(HOME);
        AppLog.add("Cam's R2-D2 Controller V2 opened.");
    }

    private View buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setBackgroundColor(Ui.color("#EAF5FF"));
        root.addView(buildNavigation(), new LinearLayout.LayoutParams(Ui.dp(this, 230), -1));

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 12));
        root.addView(main, new LinearLayout.LayoutParams(0, -1, 1f));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        main.addView(top, new LinearLayout.LayoutParams(-1, Ui.dp(this, 58)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(Ui.text(this, "Cam's R2-D2 Controller", 23f, true, "#10233B"));
        titles.addView(Ui.text(this, "Drive • Dance • Create", 13f, false, "#60758D"));
        top.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));

        statusPill = Ui.text(this, latestStatus, 14f, true, "#FFFFFF");
        statusPill.setGravity(Gravity.CENTER);
        statusPill.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        statusPill.setBackground(Ui.round(this, "#60758D", 22));
        top.addView(statusPill);

        connectButton = Ui.button(this, "Connect R2-D2", "#1677D2", "#FFFFFF", 15f);
        connectButton.setOnClickListener(v -> requestConnection());
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(Ui.dp(this, 170), Ui.dp(this, 46));
        cp.setMargins(Ui.dp(this, 10), 0, 0, 0);
        top.addView(connectButton, cp);

        recordingBanner = Ui.banner(this, "● RECORDING — tap here when finished", "#D63A46");
        recordingBanner.setVisibility(View.GONE);
        recordingBanner.setOnClickListener(v -> finishRecording());
        main.addView(recordingBanner, new LinearLayout.LayoutParams(-1, Ui.dp(this, 42)));

        playbackBanner = Ui.banner(this, "Playing routine", "#0A4D96");
        playbackBanner.setVisibility(View.GONE);
        playbackBanner.setOnClickListener(v -> player.stop());
        main.addView(playbackBanner, new LinearLayout.LayoutParams(-1, Ui.dp(this, 42)));

        content = new FrameLayout(this);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, 0, 1f);
        fp.setMargins(0, Ui.dp(this, 8), 0, 0);
        main.addView(content, fp);
        return root;
    }

    private View buildNavigation() {
        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL);
        rail.setPadding(Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16));
        rail.setBackgroundColor(Ui.color("#0A2445"));

        LinearLayout brand = new LinearLayout(this);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_cams_droid);
        brand.addView(icon, new LinearLayout.LayoutParams(Ui.dp(this, 62), Ui.dp(this, 62)));
        LinearLayout name = new LinearLayout(this);
        name.setOrientation(LinearLayout.VERTICAL);
        name.addView(Ui.text(this, "CAM'S", 13f, true, "#FFC83D"));
        name.addView(Ui.text(this, "R2-D2", 23f, true, "#FFFFFF"));
        brand.addView(name);
        rail.addView(brand);

        deviceLabel = Ui.text(this, "R2-D2 not connected", 12f, false, "#BFD6EE");
        deviceLabel.setPadding(Ui.dp(this, 4), Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 18));
        rail.addView(deviceLabel);
        rail.addView(navButton("Home", HOME));
        rail.addView(navButton("Drive", DRIVE));
        rail.addView(navButton("Dances", DANCES));
        rail.addView(navButton("My Moves", MY_MOVES));
        rail.addView(new View(this), new LinearLayout.LayoutParams(-1, 0, 1f));

        Button parent = Ui.button(this, "🔒 Parent Mode", "#12375F", "#FFFFFF", 14f);
        parent.setOnClickListener(v -> toastMessage("Press and hold for Parent Mode."));
        parent.setOnLongClickListener(v -> {
            showScreen(PARENT);
            return true;
        });
        rail.addView(parent, new LinearLayout.LayoutParams(-1, Ui.dp(this, 50)));
        return rail;
    }

    private Button navButton(String title, int screen) {
        Button button = Ui.button(this, title, "#12375F", "#FFFFFF", 16f);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setPadding(Ui.dp(this, 18), 0, Ui.dp(this, 12), 0);
        button.setOnClickListener(v -> showScreen(screen));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, Ui.dp(this, 54));
        params.setMargins(0, Ui.dp(this, 5), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    void showScreen(int screen) {
        content.removeAllViews();
        View next;
        if (screen == DRIVE) next = HomeDriveScreens.drive(this);
        else if (screen == DANCES) next = RoutineScreens.dances(this);
        else if (screen == MY_MOVES) next = RoutineScreens.myMoves(this);
        else if (screen == PARENT) next = ParentScreen.build(this);
        else next = HomeDriveScreens.home(this);
        content.addView(next, new FrameLayout.LayoutParams(-1, -1));
    }

    boolean isR2Ready() { return client.isReady(); }
    boolean isRecording() { return recorder.isRecording(); }
    List<Routine> customRoutines() { return routines; }

    void requestConnection() {
        BluetoothAdapter adapter = client.adapter();
        if (adapter == null) {
            toastMessage("Bluetooth is not available on this tablet.");
            return;
        }
        if (!adapter.isEnabled()) {
            startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            toastMessage("Turn Bluetooth on, then tap Connect again.");
            return;
        }
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        } else {
            permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode != PERMISSION_REQUEST) return;
        for (int result : results) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                toastMessage("Nearby devices permission is required to find R2-D2.");
                return;
            }
        }
        client.startScan();
    }

    void reconnect() {
        client.disconnect();
        requestConnection();
    }

    void startRecording() {
        if (player.isPlaying()) player.stop();
        recorder.start();
        recordingBanner.setVisibility(View.VISIBLE);
        showScreen(DRIVE);
        toastMessage("Recording started. Use the controls normally.");
        AppLog.add("Custom routine recording started.");
    }

    void finishRecording() {
        RoutineDialogs.finishRecording(this, recorder, routines, store, motion, recordingBanner);
    }

    void renameRoutine(Routine routine) {
        RoutineDialogs.rename(this, routine, routines, store);
    }

    void deleteRoutine(Routine routine) {
        RoutineDialogs.delete(this, routine, routines, store);
    }

    void confirmClearRoutines() {
        RoutineDialogs.clearAll(this, routines, store);
    }

    void playRoutine(Routine routine) {
        if (!client.isReady()) {
            toastMessage("Connect R2-D2 first.");
            return;
        }
        if (recorder.isRecording()) {
            toastMessage("Finish recording before playing a routine.");
            return;
        }
        player.play(routine);
    }

    void startManualMotion(ActionType action) {
        if (player.isPlaying()) player.stop();
        motion.startManual(action);
    }

    void stopManualMotion() { motion.stopManual(); }
    void performAction(ActionType action, boolean record) { motion.performAction(action, record); }
    void emergencyStop() { motion.emergencyStop(); }

    @Override
    public void onRoutineStarted(Routine routine) {
        playbackBanner.setText("▶ Playing: " + routine.name + " — tap to stop");
        playbackBanner.setVisibility(View.VISIBLE);
        AppLog.add("Routine started: " + routine.name + ".");
    }

    @Override
    public void onRoutineStep(Routine routine, int index, RoutineStep step) {
        playbackBanner.setText("▶ " + routine.name + " — " + (index + 1) + "/" +
                routine.steps.size() + "  " + step.action.label);
    }

    @Override
    public void onRoutineFinished(Routine routine, boolean cancelled) {
        playbackBanner.setVisibility(View.GONE);
        AppLog.add("Routine " + (cancelled ? "stopped: " : "finished: ") + routine.name + ".");
        if (!cancelled) toastMessage(routine.name + " finished!");
    }

    String technicalSummary() {
        String version = "unknown";
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = info.versionName;
        } catch (Exception ignored) {}
        boolean scan = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        boolean connect = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        boolean ble = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        return "App version: " + version + "\n" +
                "Package: " + getPackageName() + "\n" +
                "Android: " + Build.VERSION.RELEASE + " / API " + Build.VERSION.SDK_INT + "\n" +
                "Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n" +
                "BLE supported: " + ble + "\n" +
                "Bluetooth scan permission: " + scan + "\n" +
                "Bluetooth connect permission: " + connect + "\n" +
                "Status: " + latestStatus + "\n" +
                "Device: " + (latestDevice.isEmpty() ? "none" : latestDevice) + "\n" +
                "Saved routines: " + routines.size() + "\n" +
                "Service UUID: " + Protocol.SERVICE_UUID + "\n" +
                "Write UUID: " + Protocol.WRITE_UUID + "\n" +
                "Notify UUID: " + Protocol.NOTIFY_UUID;
    }

    void copyDiagnostics() {
        String report = technicalSummary() + "\n\nClient:\n" + client.diagnostics() +
                "\nLog:\n" + AppLog.text();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Cam's R2-D2 diagnostics", report));
            toastMessage("Diagnostics copied.");
        }
    }

    @Override
    public void onStatus(String status) {
        runOnUiThread(() -> {
            latestStatus = status;
            if (statusPill != null) {
                statusPill.setText(status);
                statusPill.setBackground(Ui.round(this, client.isReady() ? "#1D9A62" : "#60758D", 22));
            }
        });
    }

    @Override
    public void onDevice(String name, String address) {
        runOnUiThread(() -> {
            latestDevice = name + "  " + address;
            if (deviceLabel != null) deviceLabel.setText(latestDevice);
        });
    }

    @Override public void onLog(String message) {}

    @Override
    public void onConnected(boolean connected) {
        runOnUiThread(() -> {
            if (connectButton != null) connectButton.setText(connected ? "Connected" : "Connect R2-D2");
            if (!connected && deviceLabel != null) deviceLabel.setText("R2-D2 not connected");
        });
    }

    void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (player != null && player.isPlaying()) player.stop();
        if (client != null) client.disconnect();
        super.onDestroy();
    }
}
