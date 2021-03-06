package br.com.luisleao.things.smiledispenser;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.util.Log;
import java.util.Locale;

/**
 * Created by Nilanchala
 * http://www.stacktips.com
 */
public class TTSManager {

    private TextToSpeech mTts = null;
    private boolean isLoaded = false;

    public void init(Context context) {
        try {
            mTts = new TextToSpeech(context, onInitListener);
//
//            mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
//                @Override
//                public void onStart(String utteranceId) {
//                    // Speaking started.
//                    Log.i("TTS", "*** TTS ON START: " + utteranceId);
//                }
//
//                @Override
//                public void onDone(String utteranceId) {
//                    // Speaking stopped.
//                    Log.i("TTS", "*** TTS COMPLETED: " + utteranceId);
//
//                }
//
//                @Override
//                public void onError(String utteranceId) {
//
//                }
//
//            });

            mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                public void onStart(String utteranceId) {
                    Log.i("TTS", "progressListener onStart: " + utteranceId);
                }

                public void onError(String utteranceId) {
                    Log.i("TTS", "progressListener onError: " + utteranceId);
                    //if (utteranceId.equals(ID))
                    //    done();
                    return;
                }

                public void onDone(String utteranceId) {
                    Log.i("TTS", "progressListener onDone: " + utteranceId);
                    //if (utteranceId.equals(ID))
                    //    done();
                    return;
                }
            });



        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TODO: include UtteranceCompleted callback



    private TextToSpeech.OnInitListener onInitListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                int result = mTts.setLanguage(Locale.US);

//                Object voices[] = mTts.getVoices().toArray();
//                Log.i("*** VOICES ***", "" + voices.length);
//                for (Voice voice : mTts.getVoices()) {
//                    Log.i("** VOICE **", voice.getName());
//                    if (voice.getName().equals("en-US-locale")) {
//                        mTts.setVoice(voice);
//                        break;
//                    }
//                }

                isLoaded = true;


                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("error", "This Language is not supported");
                }
            } else {
                Log.e("error", "Initialization Failed!");
            }
        }
    };

    public void shutDown() {
        mTts.shutdown();
    }


    public void addQueue(String text) {
        addQueue(text, null);
    }

    public void addQueue(String text, String utteranceId) {
        if (isLoaded)
            mTts.speak(text, TextToSpeech.QUEUE_ADD, null);
        else
            Log.e("error", "TTS Not Initialized");
    }

    public void initQueue(String text) {

        if (isLoaded)
            mTts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
        else
            Log.e("error", "TTS Not Initialized");
    }

    public void setLocale(Locale new_locale) {
        mTts.setLanguage(new_locale);
    }

}
