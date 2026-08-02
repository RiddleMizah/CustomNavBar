package com.riddle.camsr2d2;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity implements
        R2D2Client.Callback,
        RoutinePlayer.Listener {

    static final int HOME = 0;
    static final int DRIVE = 1;
    static final int DANCES = 2;
    static final int MY_MOVES = 3;
    static final int TILT = 4;
    static final int BUILDER = 5;
    static final int VOICE = 6;
    static final int PARENT = 7;

    private static final int BLUETOOTH_PERMISSION_REQUEST = 1001;
    private static final int MICROPHONE_PERMISSION_REQUEST = 1002;

    private final RoutineRecorder recorder = new RoutineRecorder();
    private final List<Routine> routines = new ArrayList<>();
    private final List<RoutineStep> builderSteps = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private AppPreferences appPreferences;
    private R2D2Client client;
    private RoutineStore store;
    private RoutinePlayer player;
    private FrameLayout content;
    private TextView statusPill;
    private TextView deviceLabel;
    private TextView recordingBanner;
    private TextView playbackBanner;
    private Button connectButton;
    private Button disconnectButton;
    private MotionController motion;
    private TiltController tiltController;
    private VoiceCommandController voiceController;
    private VoiceCommandController.Listener pendingVoiceListener;
    private Runnable voiceStopRunnable;
    private ActionType tiltAction = ActionType.STOP;
    private String latestStatus = "Not connected";
    private String latestDevice = "";
    private int currentScreen = HOME;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        appPreferences = new AppPreferences(this);
        store = new RoutineStore(this);
        routines.addAll(store.load());
        client = new R2D2Client(this, this);
        motion = new MotionController(this, client, recorder);
        player = new RoutinePlayer(motion, this);
        setContentView(buildShell());
        showScreen(HOME);
        AppLog.add(controllerTitle() + " opened.");
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
        titles.addView(Ui.text(this, controllerTitle(), 23f, true, "#10233B"));
        titles.addView(Ui.text(this, "Drive • Tilt • Build • Voice", 13f, false, "#60758D"));
        top.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));

        statusPill = Ui.text(this, latestStatus, 14f, true, "#FFFFFF");
        statusPill.setGravity(Gravity.CENTER);
        statusPill.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        statusPill.setBackground(Ui.round(this, "#60758D", 22));
        top.addView(statusPill);

        connectButton = Ui.button(this, "Connect R2-D2", "#1677D2", "#FFFFFF", 15f);
        connectButton.setOnClickListener(v -> requestConnection());
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(Ui.dp(this, 155), Ui.dp(this, 46));
        cp.setMargins(Ui.dp(this, 10), 0, 0, 0);
        top.addView(connectButton, cp);

        disconnectButton = Ui.button(this, "Disconnect", "#D63A46", "#FFFFFF", 14f);
        disconnectButton.setEnabled(false);
        disconnectButton.setAlpha(0.45f);
        disconnectButton.setOnClickListener(v -> disconnectR2D2());
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(Ui.dp(this, 125), Ui.dp(this, 46));
        dp.setMargins(Ui.dp(this, 8), 0, 0, 0);
        top.addView(disconnectButton, dp);

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
        name.addView(Ui.text(this, controllerPossessive().toUpperCase(Locale.US), 13f, true, "#FFC83D"));
        name.addView(Ui.text(this, "R2-D2", 23f, true, "#FFFFFF"));
        brand.addView(name);
        rail.addView(brand);

        deviceLabel = Ui.text(this, "R2-D2 not connected", 12f, false, "#BFD6EE");
        deviceLabel.setPadding(Ui.dp(this, 4), Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 12));
        rail.addView(deviceLabel);
        rail.addView(navButton("Home", HOME));
        rail.addView(navButton("Drive", DRIVE));
        if (appPreferences.tiltEnabled()) rail.addView(navButton("Tilt Drive", TILT));
        rail.addView(navButton("Dances", DANCES));
        rail.addView(navButton("My Moves", MY_MOVES));
        if (appPreferences.builderEnabled()) rail.addView(navButton("Routine Builder", BUILDER));
        if (appPreferences.voiceEnabled()) rail.addView(navButton("Voice Commands", VOICE));
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
        Button button = Ui.button(this, title, "#12375F", "#FFFFFF", 15f);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        button.setPadding(Ui.dp(this, 18), 0, Ui.dp(this, 12), 0);
        button.setOnClickListener(v -> showScreen(screen));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, Ui.dp(this, 48));
        params.setMargins(0, Ui.dp(this, 4), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    void showScreen(int screen) {
        stopFeatureSessions();
        if (screen == TILT && !appPreferences.tiltEnabled()) screen = HOME;
        if (screen == BUILDER && !appPreferences.builderEnabled()) screen = HOME;
        if (screen == VOICE && !appPreferences.voiceEnabled()) screen = HOME;
        currentScreen = screen;
        content.removeAllViews();
        View next;
        if (screen == DRIVE) next = HomeDriveScreens.drive(this);
        else if (screen == DANCES) next = RoutineScreens.dances(this);
        else if (screen == MY_MOVES) next = RoutineScreens.myMoves(this);
        else if (screen == TILT) next = TiltDriveScreen.build(this);
        else if (screen == BUILDER) next = RoutineBuilderScreen.build(this);
        else if (screen == VOICE) next = VoiceCommandScreen.build(this);
        else if (screen == PARENT) next = ParentScreen.build(this);
        else next = HomeDriveScreens.home(this);
        content.addView(next, new FrameLayout.LayoutParams(-1, -1));
    }

    void refreshInterface() {
        recreate();
    }

    AppPreferences preferences() { return appPreferences; }
    boolean isR2Ready() { return client.isReady(); }
    boolean isRecording() { return recorder.isRecording(); }
    List<Routine> customRoutines() { return routines; }
    String controllerName() { return appPreferences.controllerName(); }
    String controllerPossessive() { return appPreferences.possessiveName(); }
    String controllerTitle() { return appPreferences.controllerTitle(); }

    void editControllerName() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(controllerName());
        input.setSelectAllOnFocus(true);
        input.setHint("Enter a name");

        new AlertDialog.Builder(this)
                .setTitle("Change controller name")
                .setMessage("This changes the name shown inside the app. The installed launcher name stays Cam's R2-D2 Controller.")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String oldName = controllerName();
                    appPreferences.setControllerName(input.getText().toString());
                    AppLog.add("Controller name changed from " + oldName + " to " + controllerName() + ".");
                    toastMessage("Name changed to " + controllerName() + ".");
                    recreate();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    void requestConnection() {
        if (client.isReady()) {
            toastMessage("R2-D2 is already connected.");
            return;
        }
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
                requestPermissions(permissions, BLUETOOTH_PERMISSION_REQUEST);
                return;
            }
        }
        client.startScan();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == BLUETOOTH_PERMISSION_REQUEST) {
            for (int result : results) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    toastMessage("Nearby devices permission is required to find R2-D2.");
                    return;
                }
            }
            client.startScan();
            return;
        }
        if (requestCode == MICROPHONE_PERMISSION_REQUEST) {
            boolean granted = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED;
            VoiceCommandController.Listener listener = pendingVoiceListener;
            pendingVoiceListener = null;
            if (!granted) {
                toastMessage("Microphone permission is required for voice commands.");
                if (listener != null) listener.onError("Microphone permission was not granted.");
            } else if (listener != null && currentScreen == VOICE) {
                startVoiceController(listener);
            }
        }
    }

    void disconnectR2D2() {
        stopFeatureSessions();
        cancelVoiceMotion();
        if (player != null && player.isPlaying()) player.stop();
        if (recorder.isRecording()) {
            recorder.cancel();
            if (recordingBanner != null) recordingBanner.setVisibility(View.GONE);
        }
        if (motion != null) motion.emergencyStop();
        client.disconnect();
        latestDevice = "";
        if (deviceLabel != null) deviceLabel.setText("R2-D2 not connected");
        AppLog.add("R2-D2 disconnected by the user.");
        toastMessage("R2-D2 disconnected.");
        showScreen(HOME);
    }

    void reconnect() {
        stopFeatureSessions();
        client.disconnect();
        requestConnection();
    }

    void startRecording() {
        stopFeatureSessions();
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
        stopFeatureSessions();
        cancelVoiceMotion();
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
        cancelVoiceMotion();
        if (player.isPlaying()) player.stop();
        motion.startManual(action);
    }

    void stopManualMotion() { motion.stopManual(); }
    void performAction(ActionType action, boolean record) { motion.performAction(action, record); }

    void emergencyStop() {
        cancelVoiceMotion();
        disarmTilt();
        if (player != null && player.isPlaying()) player.stop();
        else if (motion != null) motion.emergencyStop();
    }

    boolean armTilt(TiltController.Listener screenListener) {
        if (!appPreferences.tiltEnabled()) return false;
        if (!client.isReady()) {
            toastMessage("Connect R2-D2 before arming Tilt Drive.");
            return false;
        }
        if (recorder.isRecording()) {
            toastMessage("Finish recording before using Tilt Drive.");
            return false;
        }
        if (player.isPlaying()) player.stop();
        cancelVoiceMotion();
        disarmTilt();
        tiltAction = ActionType.STOP;
        tiltController = new TiltController(this, appPreferences, new TiltController.Listener() {
            @Override
            public void onSensorState(boolean available, String sensorName) {
                screenListener.onSensorState(available, sensorName);
            }

            @Override
            public void onReading(float forwardDegrees, float steeringDegrees, ActionType action) {
                applyTiltAction(action);
                screenListener.onReading(forwardDegrees, steeringDegrees, action);
            }
        });
        boolean started = tiltController.start();
        if (started) AppLog.add("Tilt Drive armed.");
        else tiltController = null;
        return started;
    }

    void disarmTilt() {
        if (tiltController != null) {
            tiltController.stop();
            tiltController = null;
            AppLog.add("Tilt Drive disarmed.");
        }
        if (tiltAction != ActionType.STOP && motion != null) motion.stopManual();
        tiltAction = ActionType.STOP;
    }

    private void applyTiltAction(ActionType action) {
        if (!client.isReady()) {
            disarmTilt();
            return;
        }
        if (action == tiltAction) return;
        if (tiltAction != ActionType.STOP) motion.stopManual();
        tiltAction = action;
        if (action != ActionType.STOP) motion.startContinuous(action, false);
    }

    void startVoiceListening(VoiceCommandController.Listener listener) {
        if (!appPreferences.voiceEnabled()) {
            listener.onError("Voice Commands are disabled in Parent Mode.");
            return;
        }
        if (!client.isReady()) {
            listener.onError("Connect R2-D2 first.");
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingVoiceListener = listener;
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MICROPHONE_PERMISSION_REQUEST);
            return;
        }
        startVoiceController(listener);
    }

    private void startVoiceController(VoiceCommandController.Listener listener) {
        stopVoiceListening();
        voiceController = new VoiceCommandController(this, listener);
        if (!voiceController.start()) voiceController = null;
    }

    boolean isVoiceListening() {
        return voiceController != null && voiceController.isListening();
    }

    void stopVoiceListening() {
        if (voiceController != null) {
            voiceController.stop();
            voiceController = null;
        }
    }

    String executeVoicePhrase(String phrase) {
        String command = phrase.toLowerCase(Locale.US).trim();
        AppLog.add("Voice command heard: " + phrase + ".");

        if (containsAny(command, "emergency stop", "stop", "freeze", "halt")) {
            emergencyStop();
            return "Emergency stop sent.";
        }

        Routine routine = routineForVoice(command);
        if (routine != null) {
            playRoutine(routine);
            return "Playing " + routine.name + ".";
        }

        ActionType action = null;
        if (containsAny(command, "look left", "head left", "turn your head left")) action = ActionType.HEAD_LEFT;
        else if (containsAny(command, "look right", "head right", "turn your head right")) action = ActionType.HEAD_RIGHT;
        else if (containsAny(command, "center your head", "head center", "look center", "look straight")) action = ActionType.HEAD_CENTER;
        else if (containsAny(command, "red light", "lights red", "turn red")) action = ActionType.LIGHT_RED;
        else if (containsAny(command, "blue light", "lights blue", "turn blue")) action = ActionType.LIGHT_BLUE;
        else if (containsAny(command, "lights off", "light off", "turn off the lights")) action = ActionType.LIGHTS_OFF;
        else if (containsAny(command, "whistle", "beep")) action = ActionType.WHISTLE;
        else if (containsAny(command, "wake up", "wake sound", "hello r2")) action = ActionType.WAKE;
        else if (containsAny(command, "celebrate", "victory sound", "achievement")) action = ActionType.ACHIEVEMENT;
        else if (containsAny(command, "go forward", "move forward", "forward", "go ahead")) action = ActionType.FORWARD;
        else if (containsAny(command, "go backward", "move backward", "reverse", "back up", "backward")) action = ActionType.REVERSE;
        else if (containsAny(command, "turn left", "spin left", "go left", "left")) action = ActionType.TURN_LEFT;
        else if (containsAny(command, "turn right", "spin right", "go right", "right")) action = ActionType.TURN_RIGHT;

        if (action == null) return "Command not recognized. Try “go forward,” “whistle,” or “happy dance.”";
        if (action.motion) {
            if (!appPreferences.voiceMotionEnabled()) {
                return "Movement voice commands are disabled in Parent Mode.";
            }
            if (player.isPlaying()) player.stop();
            disarmTilt();
            cancelVoiceMotion();
            motion.startContinuous(action, false);
            voiceStopRunnable = () -> {
                motion.stopManual();
                voiceStopRunnable = null;
            };
            handler.postDelayed(voiceStopRunnable, appPreferences.voiceMotionDurationMs());
            return action.label + " for " + appPreferences.voiceMotionDurationMs() + " ms, then stop.";
        }
        performAction(action, false);
        return action.label + " sent.";
    }

    private Routine routineForVoice(String command) {
        String id = null;
        if (containsAny(command, "happy dance", "dance party", "do a dance", "dance")) id = "happy-dance";
        else if (containsAny(command, "victory spin", "spin celebration")) id = "victory-spin";
        else if (containsAny(command, "patrol mode", "start patrol", "patrol")) id = "patrol";
        else if (containsAny(command, "silly wiggle", "wiggle")) id = "silly-wiggle";
        else if (containsAny(command, "nervous droid", "act nervous")) id = "nervous-droid";
        else if (containsAny(command, "bedtime", "go to sleep", "sleep mode")) id = "bedtime";
        if (id == null) return null;
        for (Routine routine : PresetRoutines.all()) {
            if (routine.id.equals(id)) return routine;
        }
        return null;
    }

    private boolean containsAny(String value, String... options) {
        for (String option : options) if (value.contains(option)) return true;
        return false;
    }

    private void cancelVoiceMotion() {
        if (voiceStopRunnable != null) {
            handler.removeCallbacks(voiceStopRunnable);
            voiceStopRunnable = null;
            if (motion != null) motion.stopManual();
        }
    }

    List<RoutineStep> builderSteps() {
        return Collections.unmodifiableList(builderSteps);
    }

    void addBuilderStep(ActionType action) {
        long delay = builderSteps.isEmpty() ? 0L : appPreferences.builderDefaultDelayMs();
        long duration = action.motion ? appPreferences.builderMotionDurationMs() : 0L;
        builderSteps.add(new RoutineStep(action, delay, duration));
        showScreen(BUILDER);
    }

    void removeBuilderStep(int index) {
        if (index >= 0 && index < builderSteps.size()) builderSteps.remove(index);
        showScreen(BUILDER);
    }

    void moveBuilderStep(int index, int direction) {
        int target = index + direction;
        if (index >= 0 && index < builderSteps.size() && target >= 0 && target < builderSteps.size()) {
            Collections.swap(builderSteps, index, target);
        }
        showScreen(BUILDER);
    }

    void updateBuilderStep(int index, long delayMs, long durationMs) {
        if (index >= 0 && index < builderSteps.size()) {
            RoutineStep old = builderSteps.get(index);
            builderSteps.set(index, new RoutineStep(old.action, delayMs, durationMs));
        }
        showScreen(BUILDER);
    }

    void previewBuilderRoutine() {
        if (builderSteps.isEmpty()) {
            toastMessage("Add at least one block first.");
            return;
        }
        playRoutine(Routine.custom("Builder Preview", new ArrayList<>(builderSteps)));
    }

    void saveBuilderRoutine() {
        if (builderSteps.isEmpty()) {
            toastMessage("Add at least one block first.");
            return;
        }
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(controllerPossessive() + " Built Routine");
        new AlertDialog.Builder(this)
                .setTitle("Name this routine")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) name = controllerPossessive() + " Built Routine " + (routines.size() + 1);
                    routines.add(0, Routine.custom(name, new ArrayList<>(builderSteps)));
                    store.save(routines);
                    builderSteps.clear();
                    AppLog.add("Saved visual routine: " + name + ".");
                    showScreen(MY_MOVES);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    void confirmClearBuilder() {
        if (builderSteps.isEmpty()) return;
        new AlertDialog.Builder(this)
                .setTitle("Clear the routine builder?")
                .setMessage("The unsaved action blocks will be removed.")
                .setPositiveButton("Clear", (dialog, which) -> {
                    builderSteps.clear();
                    showScreen(BUILDER);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void stopFeatureSessions() {
        disarmTilt();
        stopVoiceListening();
        pendingVoiceListener = null;
    }

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
        boolean mic = checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean ble = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
        return "Controller name: " + controllerName() + "\n" +
                "App version: " + version + "\n" +
                "Package: " + getPackageName() + "\n" +
                "Android: " + Build.VERSION.RELEASE + " / API " + Build.VERSION.SDK_INT + "\n" +
                "Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n" +
                "BLE supported: " + ble + "\n" +
                "Bluetooth scan permission: " + scan + "\n" +
                "Bluetooth connect permission: " + connect + "\n" +
                "Microphone permission: " + mic + "\n" +
                "Status: " + latestStatus + "\n" +
                "Device: " + (latestDevice.isEmpty() ? "none" : latestDevice) + "\n" +
                "Saved routines: " + routines.size() + "\n" +
                "Tilt / Builder / Voice visible: " + appPreferences.tiltEnabled() + " / " +
                        appPreferences.builderEnabled() + " / " + appPreferences.voiceEnabled() + "\n" +
                "Tilt deadzone / smoothing: " + appPreferences.tiltDeadzoneDegrees() + "° / " +
                        appPreferences.tiltSmoothingPercent() + "%\n" +
                "Voice movement duration: " + appPreferences.voiceMotionDurationMs() + " ms\n" +
                "Service UUID: " + Protocol.SERVICE_UUID + "\n" +
                "Write UUID: " + Protocol.WRITE_UUID + "\n" +
                "Notify UUID: " + Protocol.NOTIFY_UUID;
    }

    void copyDiagnostics() {
        String report = technicalSummary() + "\n\nClient:\n" + client.diagnostics() +
                "\nLog:\n" + AppLog.text();
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("R2-D2 Controller diagnostics", report));
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
            if (connectButton != null) connectButton.setEnabled(!connected);
            if (disconnectButton != null) {
                disconnectButton.setEnabled(connected);
                disconnectButton.setAlpha(connected ? 1f : 0.45f);
            }
            if (!connected) {
                stopFeatureSessions();
                cancelVoiceMotion();
                if (deviceLabel != null) deviceLabel.setText("R2-D2 not connected");
                latestDevice = "";
            }
        });
    }

    void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        stopFeatureSessions();
        cancelVoiceMotion();
        handler.removeCallbacksAndMessages(null);
        if (player != null && player.isPlaying()) player.stop();
        if (client != null) client.disconnect();
        super.onDestroy();
    }
}
