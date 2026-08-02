package com.riddle.camsr2d2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

final class VoiceCommandController implements RecognitionListener {
    interface Listener {
        void onListeningState(String status, boolean listening);
        void onPhrase(String phrase);
        void onError(String message);
    }

    private final Context context;
    private final Listener listener;
    private SpeechRecognizer recognizer;
    private boolean listening;

    VoiceCommandController(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    boolean start() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            listener.onError("Speech recognition is not available on this tablet.");
            return false;
        }
        stop();
        recognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizer.setRecognitionListener(this);

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell R2-D2 what to do");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        recognizer.startListening(intent);
        listening = true;
        listener.onListeningState("Listening…", true);
        return true;
    }

    void stop() {
        listening = false;
        if (recognizer != null) {
            try {
                recognizer.cancel();
                recognizer.destroy();
            } catch (Exception ignored) {}
            recognizer = null;
        }
    }

    boolean isListening() {
        return listening;
    }

    @Override public void onReadyForSpeech(Bundle params) {
        listener.onListeningState("Listening — speak now", true);
    }

    @Override public void onBeginningOfSpeech() {
        listener.onListeningState("I hear you…", true);
    }

    @Override public void onRmsChanged(float rmsdB) {}
    @Override public void onBufferReceived(byte[] buffer) {}

    @Override public void onEndOfSpeech() {
        listener.onListeningState("Understanding…", true);
    }

    @Override public void onError(int error) {
        listening = false;
        String message;
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO: message = "Microphone error."; break;
            case SpeechRecognizer.ERROR_CLIENT: message = "Listening was cancelled."; break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: message = "Microphone permission is required."; break;
            case SpeechRecognizer.ERROR_NETWORK:
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: message = "Speech service could not reach the network."; break;
            case SpeechRecognizer.ERROR_NO_MATCH: message = "I did not understand that command."; break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: message = "Speech recognition is busy. Try again."; break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: message = "I did not hear anything."; break;
            default: message = "Speech recognition error " + error + "."; break;
        }
        listener.onListeningState("Tap the microphone to try again", false);
        if (error != SpeechRecognizer.ERROR_CLIENT) listener.onError(message);
        destroyRecognizerOnly();
    }

    @Override public void onResults(Bundle results) {
        listening = false;
        ArrayList<String> phrases = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String best = phrases == null || phrases.isEmpty() ? "" : phrases.get(0);
        listener.onListeningState("Tap the microphone for another command", false);
        if (best.trim().isEmpty()) listener.onError("I did not understand that command.");
        else listener.onPhrase(best.trim());
        destroyRecognizerOnly();
    }

    @Override public void onPartialResults(Bundle partialResults) {}
    @Override public void onEvent(int eventType, Bundle params) {}

    private void destroyRecognizerOnly() {
        if (recognizer != null) {
            try { recognizer.destroy(); } catch (Exception ignored) {}
            recognizer = null;
        }
    }
}
