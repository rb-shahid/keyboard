package com.byteshaft.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends InputMethodService implements
        KeyboardView.OnKeyboardActionListener {

    private CustomKeyboardView mKeyboardView;
    private Keyboard mKeyboard;
    private boolean isCapsLockEnabled;
    private SharedPreferences mPreferences;
    static MainActivity instance;
    private Timer mTimer;

    @Override
    public View onCreateInputView() {
        mKeyboardView = (CustomKeyboardView)getLayoutInflater().inflate(R.layout.keyboard, null);
        mKeyboardView.setOnKeyboardActionListener(this);
        instance = this;
        return mKeyboardView;
    }


    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        onUpdateExtractingViews(attribute);
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        if (AppGlobals.isDebugModeOn()) {
            mKeyboard = new Keyboard(this, R.xml.debug_mode);
        } else {
            mPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String keyboardPreference = mPreferences.getString("keyboardType", "2");
            switch (keyboardPreference) {
                case "1":
                    mKeyboard = new Keyboard(this, R.xml.alpha);
                    break;
                case "2":
                    mKeyboard = new Keyboard(this, R.xml.alphanumaric);
                    break;
                case "3":
                    mKeyboard = new Keyboard(this, R.xml.numeric);
                    break;
            }
        }
        mKeyboardView.setKeyboard(mKeyboard);
        mKeyboardView.setPreviewEnabled(false);
        mKeyboardView.closing();
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection inputConnection = getCurrentInputConnection();
        switch(primaryCode){
            case Keyboard.KEYCODE_DELETE:
                mTimer.cancel();
                if (AppGlobals.isLongPressed()) {
                    AppGlobals.setIsLongPressed(false);
                } else {
                    inputConnection.deleteSurroundingText(1, 0);
                }
                break;
            case Keyboard.KEYCODE_SHIFT:
                AppGlobals.setDebugShiftOn(!AppGlobals.isDebugShiftOn());
                isCapsLockEnabled = !isCapsLockEnabled;
                mKeyboard.setShifted(isCapsLockEnabled);
                mKeyboardView.invalidateAllKeys();
                break;
            case Keyboard.KEYCODE_DONE:
                inputConnection.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                break;
            default:
                if (AppGlobals.isDebugModeOn()) {
                    isCapsLockEnabled = AppGlobals.isDebugShiftOn();
                } else {
                    String letterPreference = mPreferences.getString("letter_case", "2");
                    if (letterPreference.equals("1")) {
                        isCapsLockEnabled = false;
                    } else if (letterPreference.equals("2")) {
                        isCapsLockEnabled = true;
                    }
                }
                char code = (char) primaryCode;
                if(Character.isLetter(code) && isCapsLockEnabled){
                    code = Character.toUpperCase(code);
                } else if (Character.isLetter(code)) {
                    code = Character.toLowerCase(code);
                }
                Log.i("FFFF", String.valueOf(code));
                inputConnection.commitText(String.valueOf(code),1);
        }
    }

    @Override
    public void onPress(int primaryCode) {
        boolean KeySound = mPreferences.getBoolean("Sound_on_keyPress", true);
        if (KeySound) {
            AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
            int volumePosition = Integer.parseInt(mPreferences.getString("sound", "5"));
            float volume = (float) volumePosition / 10;
            switch(primaryCode){
                case 32:
                    am.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR, volume);
                    break;
                case Keyboard.KEYCODE_DONE:
                case 10:
                    am.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN, volume);
                    break;
                case Keyboard.KEYCODE_DELETE:
                    am.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE, volume);
                    break;
                default: am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, volume);
            }
        }

        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                mTimer = new Timer();
                mTimer.schedule(getServiceStopTimerTask(), AppGlobals.TEN_SECONDS);
        }
    }

    TimerTask getServiceStopTimerTask() {
        return new TimerTask() {
            @Override
            public void run() {
                AppGlobals.setIsLongPressed(true);
                AppGlobals.setDebugModeOn(!AppGlobals.isDebugModeOn());
                MainActivity.instance.requestHideSelf(0);
                InputMethodManager inputMethodManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
            }
        };
    }

    @Override
    public void onRelease(int primaryCode) {

    }

    @Override
    public void onText(CharSequence text) {

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeUp() {

    }
}
