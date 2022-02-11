package com.liskovsoft.smartyoutubetv2.common.misc;

import android.view.KeyEvent;
import com.liskovsoft.sharedutils.mylogger.Log;

import java.util.Map;

public abstract class KeyTranslator {
    private static final String TAG = KeyTranslator.class.getSimpleName();

    public KeyEvent translate(KeyEvent event) {
        Map<Integer, Integer> keyMapping = getKeyMapping();
        Integer newKeyCode = null;

        if (keyMapping != null) {
            newKeyCode = keyMapping.get(event.getKeyCode());
        }

        Map<Integer, Runnable> actionMapping = getActionMapping();

        if (actionMapping != null) {
            Runnable action = actionMapping.get(event.getKeyCode());
            if (action != null) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    action.run();
                }
                newKeyCode = KeyEvent.KEYCODE_UNKNOWN; // disable original mapping
            }
        }

        return translate(event, newKeyCode);
    }

    private KeyEvent translate(KeyEvent origin, Integer newKeyCode) {
        if (newKeyCode == null) {
            return origin;
        }

        KeyEvent newKey = new KeyEvent(
                origin.getDownTime(),
                origin.getEventTime(),
                origin.getAction(),
                newKeyCode,
                origin.getRepeatCount(),
                origin.getMetaState(),
                origin.getDeviceId(),
                origin.getScanCode(),
                origin.getFlags(),
                origin.getSource()
        );

        Log.d(TAG, "Translating %s to %s", origin, newKey);

        return newKey;
    }

    protected abstract Map<Integer, Integer> getKeyMapping();

    protected abstract Map<Integer, Runnable> getActionMapping();
}
