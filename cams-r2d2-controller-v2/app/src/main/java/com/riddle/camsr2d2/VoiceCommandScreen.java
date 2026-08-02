package com.riddle.camsr2d2;

import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

final class VoiceCommandScreen {
    private VoiceCommandScreen() {}

    static View build(MainActivity app) {
        ScrollView scroll = new ScrollView(app);
        LinearLayout page = Ui.page(app);
        scroll.addView(page);

        page.addView(Ui.text(app, "Voice Commands", 25f, true, "#10233B"));
        page.addView(Ui.text(app,
                "Tap the microphone, say one command, and R2-D2 will respond. Movement commands stop automatically.",
                14f, false, "#60758D"), Ui.matchWrap(app, 0, 4, 0, 12));

        LinearLayout console = Ui.card(app, "#0A2445");
        console.setGravity(Gravity.CENTER_HORIZONTAL);
        console.setPadding(Ui.dp(app, 22), Ui.dp(app, 22), Ui.dp(app, 22), Ui.dp(app, 22));

        TextView heard = Ui.text(app, "Ready for a command", 24f, true, "#FFC83D");
        heard.setGravity(Gravity.CENTER);
        console.addView(heard, new LinearLayout.LayoutParams(-1, -2));

        TextView status = Ui.text(app, "Tap the microphone to begin", 15f, false, "#EAF5FF");
        status.setGravity(Gravity.CENTER);
        console.addView(status, Ui.matchWrap(app, 0, 8, 0, 16));

        Button microphone = Ui.button(app, "🎤  Speak to R2-D2", "#1677D2", "#FFFFFF", 20f);
        console.addView(microphone, new LinearLayout.LayoutParams(Ui.dp(app, 330), Ui.dp(app, 68)));
        page.addView(console, Ui.matchWrap(app, 0, 0, 0, 12));

        LinearLayout examples = Ui.card(app, "#FFFFFF");
        examples.setPadding(Ui.dp(app, 18), Ui.dp(app, 16), Ui.dp(app, 18), Ui.dp(app, 16));
        examples.addView(Ui.text(app, "Try saying", 19f, true, "#10233B"));
        examples.addView(Ui.text(app,
                "“Go forward”  •  “Turn left”  •  “Reverse”  •  “Stop”\n" +
                        "“Look left”  •  “Center your head”  •  “Blue light”\n" +
                        "“Whistle”  •  “Celebrate”  •  “Do a happy dance”  •  “Patrol mode”",
                15f, false, "#60758D"), Ui.matchWrap(app, 0, 10, 0, 0));
        page.addView(examples, Ui.matchWrap(app, 0, 0, 0, 12));

        LinearLayout safety = Ui.card(app, "#FFF8E4");
        safety.setPadding(Ui.dp(app, 16), Ui.dp(app, 14), Ui.dp(app, 16), Ui.dp(app, 14));
        safety.addView(Ui.text(app, "Parent safety setting", 17f, true, "#7A5700"));
        safety.addView(Ui.text(app,
                app.preferences().voiceMotionEnabled()
                        ? "Movement commands are enabled for " + app.preferences().voiceMotionDurationMs() + " ms."
                        : "Movement commands are disabled. Sounds, lights, head controls, and routines still work.",
                14f, false, "#7A5700"), Ui.matchWrap(app, 0, 6, 0, 0));
        page.addView(safety);

        VoiceCommandController.Listener listener = new VoiceCommandController.Listener() {
            @Override
            public void onListeningState(String message, boolean listening) {
                status.setText(message);
                microphone.setText(listening ? "■  Cancel listening" : "🎤  Speak to R2-D2");
                microphone.setBackground(Ui.round(app, listening ? "#D63A46" : "#1677D2", 14));
            }

            @Override
            public void onPhrase(String phrase) {
                heard.setText("“" + phrase + "”");
                String result = app.executeVoicePhrase(phrase);
                status.setText(result);
            }

            @Override
            public void onError(String message) {
                heard.setText("Try that again");
                status.setText(message);
                app.toastMessage(message);
            }
        };

        microphone.setOnClickListener(v -> {
            if (app.isVoiceListening()) {
                app.stopVoiceListening();
                listener.onListeningState("Listening cancelled", false);
            } else {
                app.startVoiceListening(listener);
            }
        });

        return scroll;
    }
}
