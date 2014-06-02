/** 
 * CLASS: AudioMethods
 *   This class contains methods specific to playing audio in the application.
 */

package com.cjcornell.samplebluetooth;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Build;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener;
import android.speech.tts.TextToSpeech;
import android.util.Log;

@SuppressWarnings("deprecation")
public class AudioMethods {
    private static final String AUDIO_TAG = "Audio";
    private static final String TTS_TAG = "TTS";
    
    // For text-to-speech
    private static TextToSpeech tts;
    private static MediaPlayer mp; //Used when playing/streaming audio
    private static boolean isPreparing = false;

    /**
     * playSound
     *   Plays a sound file (in R.raw.<soundfile-no-extension>) in a provided activity.
     */
    public static void playSound(Activity activity, int audioFile) {
        // TODO: Test with all applicable file types
        MediaPlayer newmp = MediaPlayer.create(activity, audioFile);
        
        newmp.setOnCompletionListener(new OnCompletionListener() {
            // Ensure we release the allocated resources back to the device
            @Override
            public void onCompletion(MediaPlayer mp) {
                if (AudioMethods.mp == mp) {
                    shutdownMediaPlayer();
                }
                Log.v(AUDIO_TAG, "MediaPlayer successfully released");
            }
        });
        
        newmp.start();
        shutdownMediaPlayer();
        mp = newmp;
    }
    
    public interface AudioCompletionNotifiable {
        public void audioCompleted();
    }
    /**
     * Streams audio from the internet, playing it in a provided activity.
     * @param uri      the uri of the stream
     * @param n        action to take when stream completes
     */
    public static void streamAudio(String uri, final AudioCompletionNotifiable n) {
        if (mp != null) {
            mp.start();
        } else {
            
            try {
                MediaPlayer newmp = new MediaPlayer();
                newmp.setDataSource(uri);
                Log.v(AUDIO_TAG, "Streaming " + uri);
                newmp.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC);
    
                newmp.setOnCompletionListener(new OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        if (AudioMethods.mp == mp) {
                            shutdownMediaPlayer();
                        }
                        if (n != null) {
                            n.audioCompleted();
                        }
                        Log.v(AUDIO_TAG, "MediaPlayer successfully released");
                    }
                });
                newmp.setOnPreparedListener(new OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        isPreparing = false;
                        mp.start();
                        Log.v(AUDIO_TAG, "MediaPlayer started");
                    }
                });
    
                newmp.prepareAsync();
                shutdownMediaPlayer();
                mp = newmp;
                isPreparing = true;
            } catch (IOException e) {
                Log.w(AUDIO_TAG, "Unable to open " + uri, e);
            }
        }
    }
    public static void streamAudio(String uri) {
        streamAudio(uri, null);
    }


    private interface TTSAction {
        public void run(TextToSpeech tts);
    }
    @SuppressLint("NewApi")
    private static void performTextToSpeech(Context context, final TTSAction callback,
            final AudioCompletionNotifiable n) {
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (tts == null) return;
                /*
                 * We need to deal with the tts object once it is done speaking - namely, we should
                 * free the resources it is using. There is a deprecated way to do this, so we must
                 * include both if we are to be flexible with device versions.
                 */
                if (Build.VERSION.SDK_INT >= 15) {
                    // The newer way
                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onDone(String utteranceId) {
                            Log.v(TTS_TAG, "Completed successfully");
                            stopTextToSpeech();
                            if (n != null) {
                                n.audioCompleted();
                            }
                        }
                        
                        @Override
                        public void onError(String utteranceId) {
                            Log.v(TTS_TAG, "Error id: " + utteranceId);
                            stopTextToSpeech();
                            if (n != null) {
                                n.audioCompleted();
                            }
                        }
                        
                        @Override
                        public void onStart(String utteranceId) {
                            Log.v(TTS_TAG, "TTS Started");
                        }
                    });
                } else {
                    // This covers the deprecated way to handle the object upon completion
                    tts.setOnUtteranceCompletedListener(new OnUtteranceCompletedListener() {
                        @Override
                        public void onUtteranceCompleted(String utteranceId) {
                            Log.v(TTS_TAG, "Completed successfully");
                            stopTextToSpeech();
                            if (n != null) {
                                n.audioCompleted();
                            }
                        }
                    });
                }
                
                if (status == TextToSpeech.SUCCESS) {
                    // Do nothing if no TTS object was created
                    if (tts != null) {
                        // Set the language appropriately
                        // TODO: Make options for different languages
                        int result = tts.setLanguage(Locale.US);
                        
                        // Ensure the language is supported
                        if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.v(TTS_TAG, "Language not supported");
                            
                        // Actually read out the message
                        } else {
                           callback.run(tts);
                        }
                    } else {
                        Log.v(TTS_TAG, "The TextToSpeech variable is null");
                    }
                }
            }
        });
    }
    
    /**
     * Text-to-speech (TTS)
     *   Use the built in text-to-speech engine to speak text.
     */
    public static void textToSpeech(Context context, final String text,
            final AudioCompletionNotifiable n) {
        performTextToSpeech(context, new TTSAction() {
            public void run(TextToSpeech tts) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
                Log.v(TTS_TAG, "Message read out successfully: " + text);
            }
        }, n);
    }
    public static void textToSpeech(Context context, final String text) {
        textToSpeech(context, text, null);
    }
    
    /**
     * Text-to-speech (TTS)
     *   Stop the current TTS utterance.
     */
    public static void stopTextToSpeech() {
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
            tts.shutdown();
        }
        tts = null;
    }
    
    /**
     * Pause the current playing MediaPlayer.
     */
    public static void pauseMediaPlayer() {
        if (mp != null && mp.isPlaying()) {
            mp.pause();
        }
    }
    
    /**
     * playInstructions
     *   Plays a sequence of instructions with delays
     *   specified in between
     */
    public static void playInstructions(Context context, final List<String> instructions,
            final List<Integer> delays, final AudioCompletionNotifiable n) {
        performTextToSpeech(context, new TTSAction() {
            public void run(TextToSpeech tts) {
                for (int i = 0; i < instructions.size(); i++) {
                    tts.speak(instructions.get(i), TextToSpeech.QUEUE_ADD, null);
                    tts.playSilence(1000 * delays.get(i), TextToSpeech.QUEUE_ADD, null);
                }
                Log.v(TTS_TAG, "Instructions read out successfully!");
            }
        }, n);
    }
    
    public static void playInstructions(Context context, final List<String> instructions,
            final List<Integer> delays) {
        playInstructions(context, instructions, delays, null);
    }

    
    /**
     * shutdownMediaPlayer
     *   A simple helper method to shut down the MediaPlayer object.
     */
    public static void shutdownMediaPlayer() {
        if (mp != null) {
            // fix "stop called in state 4" error
            if (!isPreparing) {
                mp.stop();
            }
            mp.release();
            mp = null;
        }
    }
}