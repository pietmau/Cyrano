/**
 * CLASS: STTListener
 *   This is the listener for speech to text
 */

package com.cjcornell.samplebluetooth;

import java.util.ArrayList;

import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.util.Log;

public class STTListener implements RecognitionListener {
    private static String TAG = "STTListener";
    private SpeechRecognizer recognizer;
    private STTResultListener resultListener;

    public interface STTResultListener {
        public void gotResults(ArrayList<String> results);
        public void gotError(int error);
    }

    public STTListener(SpeechRecognizer r, STTResultListener l) {
        recognizer = r;
        resultListener = l;
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.v(TAG, "Detected user speech");
    }

    @Override
    public void onBufferReceived(byte[] buffer) {}

    @Override
    public void onEndOfSpeech() {
        recognizer.stopListening();
    }

    @Override
    public void onError(int error) {
        Log.e("STT", "Error occurred: " + error);
        resultListener.gotError(error);
    }

    @Override
    public void onEvent(int eventType, Bundle params) {}

    @Override
    public void onPartialResults(Bundle partialResults) {}

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.v(TAG, "The user can now speak successfully");}

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> spokenWords = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        resultListener.gotResults(spokenWords);
    }

    @Override
    public void onRmsChanged(float rmsdB) {}

}
