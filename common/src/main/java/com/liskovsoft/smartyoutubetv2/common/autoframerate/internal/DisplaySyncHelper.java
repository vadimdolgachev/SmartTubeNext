package com.liskovsoft.smartyoutubetv2.common.autoframerate.internal;

import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION;
import android.view.Window;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.autoframerate.internal.DisplayHolder.Mode;
import com.liskovsoft.smartyoutubetv2.common.prefs.AppPrefs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

// Source: https://developer.amazon.com/docs/fire-tv/4k-apis-for-hdmi-mode-switch.html#amazonextension

public class DisplaySyncHelper implements UhdHelperListener {
    private static final String TAG = DisplaySyncHelper.class.getSimpleName();
    private static final int STATE_ORIGINAL = 1;
    protected Context mContext;
    private boolean mDisplaySyncInProgress = false;
    private UhdHelper mUhdHelper;
    protected Mode mOriginalMode;
    private Mode mNewMode;
    // switch not only framerate but resolution too
    private boolean mIsResolutionSwitchEnabled;
    private int mModeLength = -1;
    private AutoFrameRateListener mListener;

    public interface AutoFrameRateListener {
        void onModeStart(Mode newMode);
    }

    public DisplaySyncHelper(Context context) {
        mContext = context;
    }

    public Mode getOriginalMode() {
        return mOriginalMode;
    }

    public Mode getNewMode() {
        return mNewMode;
    }

    private List<Mode> filterSameResolutionModes(Mode[] oldModes, Mode currentMode) {
        if (currentMode == null) {
            return Collections.emptyList();
        }

        ArrayList<Mode> newModes = new ArrayList<>();
        int oldModesLen = oldModes.length;

        for (int i = 0; i < oldModesLen; ++i) {
            Mode mode = oldModes[i];
            if (mode == null) {
                continue;
            }

            if (mode.getPhysicalHeight() == currentMode.getPhysicalHeight() && mode.getPhysicalWidth() == currentMode.getPhysicalWidth()) {
                newModes.add(mode);
            }
        }

        return newModes;
    }

    private ArrayList<Mode> filterModesByWidth(Mode[] allModes, int videoWidth) {
        ArrayList<Mode> newModes = new ArrayList<>();

        if (videoWidth == -1) {
            return newModes;
        }

        for (Mode mode : allModes) {
            int width = mode.getPhysicalWidth();
            if (width >= (videoWidth - 100) && width <= (videoWidth + 100)) {
                newModes.add(mode);
            }
        }

        if (newModes.isEmpty()) {
            Log.i(TAG, "MODE CANDIDATES NOT FOUND!! Old modes: " + Arrays.asList(allModes));
        } else {
            Log.i(TAG, "FOUND MODE CANDIDATES! New modes: " + newModes);
        }

        return newModes;
    }

    private ArrayList<Mode> filterModes(Mode[] oldModes, int minHeight, int maxHeight) {
        ArrayList<Mode> newModes = new ArrayList<>();

        if (minHeight == -1 || maxHeight == -1) {
            return newModes;
        }

        int modesNum = oldModes.length;

        for (int i = 0; i < modesNum; ++i) {
            Mode mode = oldModes[i];
            int height = mode.getPhysicalHeight();
            if (height >= minHeight && height <= maxHeight) {
                newModes.add(mode);
            }
        }

        if (newModes.isEmpty()) {
            Log.i(TAG, "MODE CANDIDATES NOT FOUND!! Old modes: " + Arrays.asList(oldModes));
        } else {
            Log.i(TAG, "FOUND MODE CANDIDATES! New modes: " + newModes);
        }

        return newModes;
    }

    protected Mode findCloserMode(Mode[] modes, float videoFramerate) {
        if (modes == null) {
            return null;
        }

        return findCloserMode(Arrays.asList(modes), videoFramerate);
    }

    private Mode findCloserMode(List<Mode> modes, float videoFramerate) {
        HashMap<Integer, int[]> relatedRates;

        relatedRates = getRateMapping();

        int myRate = (int) (videoFramerate * 100.0F);

        if (myRate >= 2300 && myRate <= 2399) {
            myRate = 2397;
        }

        if (relatedRates.containsKey(myRate)) {
            HashMap<Integer, Mode> rateAndMode = new HashMap<>();
            Iterator modeIterator = modes.iterator();

            while (modeIterator.hasNext()) {
                Mode mode = (Mode) modeIterator.next();
                rateAndMode.put((int) (mode.getRefreshRate() * 100.0F), mode);
            }

            int[] rates = relatedRates.get(myRate);
            int ratesLen = rates.length;

            for (int i = 0; i < ratesLen; ++i) {
                int newRate = rates[i];
                if (rateAndMode.containsKey(newRate)) {
                    return rateAndMode.get(newRate);
                }
            }
        }

        return null;
    }

    protected HashMap<Integer, int[]> getRateMapping() {
        HashMap<Integer, int[]> relatedRates = new HashMap<>();
        relatedRates.put(1500, new int[]{3000, 6000});
        relatedRates.put(2397, new int[]{2397, 2400, 3000, 6000});
        relatedRates.put(2400, new int[]{2400, 3000, 6000});
        relatedRates.put(2500, new int[]{2500, 5000});
        relatedRates.put(2997, new int[]{2997, 3000, 6000});
        relatedRates.put(3000, new int[]{3000, 6000});
        relatedRates.put(5000, new int[]{5000, 2500});
        relatedRates.put(5994, new int[]{5994, 6000, 3000});
        relatedRates.put(6000, new int[]{6000, 3000});
        return relatedRates;
    }

    /**
     * Utility method to check if device is Amazon Fire TV device
     * @return {@code true} true if device is Amazon Fire TV device.
     */
    public static boolean isAmazonFireTVDevice(){
        String deviceName = Build.MODEL;
        String manufacturerName = Build.MANUFACTURER;
        return (deviceName.startsWith("AFT")
                && "Amazon".equalsIgnoreCase(manufacturerName));
    }

    public boolean supportsDisplayModeChangeComplex() {
        if (mModeLength == -1) {
            Mode[] supportedModes = null;

            if (VERSION.SDK_INT >= 21) {
                supportedModes = getUhdHelper().getSupportedModes();
            }

            mModeLength = supportedModes == null ? 0 : supportedModes.length;
        }

        return mModeLength > 1 && supportsDisplayModeChange();
    }

    /**
     * Check whether device supports mode change. Also shows toast if no
     * @return mode change supported
     */
    public static boolean supportsDisplayModeChange() {
        boolean supportedDevice = true;

        //We fail for following conditions
        if(VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            supportedDevice = false;
        } else {
            switch (VERSION.SDK_INT) {
                case Build.VERSION_CODES.LOLLIPOP:
                case Build.VERSION_CODES.LOLLIPOP_MR1:
                    if (!isAmazonFireTVDevice()) {
                        supportedDevice = false;
                    }
                    break;
            }
        }

        if (!supportedDevice) {
            Log.i(TAG, "Device doesn't support display mode change");
        }

        return supportedDevice;
    }

    public boolean displayModeSyncInProgress() {
        return mDisplaySyncInProgress;
    }

    @Override
    public void onModeChanged(Mode mode) {
        mDisplaySyncInProgress = false;

        Mode currentMode = getUhdHelper().getCurrentMode();

        if (mode == null && currentMode == null) {
            String msg = "Mode change failure. Internal error occurred.";
            Log.w(TAG, msg);

            // NOTE: changed
            //CommonApplication.getPreferences().setCurrentDisplayMode(msg);
        } else {
            int modeId = mNewMode != null ? mNewMode.getModeId() : -1;

            if (currentMode.getModeId() != modeId) {
                // Once onDisplayChangedListener sends proper callback, the above if condition
                // need to changed to mode.getModeId() != modeId
                String msg = String.format("Mode change failure. Current mode id is %s. Expected mode id is %s", currentMode.getModeId(), modeId);
                Log.w(TAG, msg);

                String newMsg = String.format("Expected %s", UhdHelper.formatMode(mNewMode));

                // NOTE: changed
                //CommonApplication.getPreferences().setCurrentDisplayMode(String.format("%s (%s)", UhdHelper.formatMode(currentMode), newMsg));
            } else {
                Log.i(TAG, "Mode changed successfully");
            }

            AppPrefs.instance(mContext).setCurrentDisplayMode(UhdHelper.formatMode(currentMode));
        }
    }

    // switch frame rate only
    private boolean getNeedDisplaySync() {
        return true;
    }

    public boolean syncDisplayMode(Window window, int videoWidth, float videoFramerate) {
        return syncDisplayMode(window, videoWidth, videoFramerate, false);
    }

    /**
     * Tries to find best suited display params for the video
     * @param window window object
     * @param videoWidth width of the video material
     * @param videoFramerate framerate of the video
     * @return
     */
    public boolean syncDisplayMode(Window window, int videoWidth, float videoFramerate, boolean force) {
        if (supportsDisplayModeChange() && videoWidth >= 10) {
            if (mUhdHelper == null) {
                mUhdHelper = new UhdHelper(mContext);
                mUhdHelper.registerModeChangeListener(this);
            }

            Mode[] modes = mUhdHelper.getSupportedModes();

            Log.d(TAG, "Modes supported by device:");
            Log.d(TAG, Arrays.asList(modes));

            boolean needResolutionSwitch = false;

            List<Mode> resultModes = new ArrayList<>();

            if (mIsResolutionSwitchEnabled) {
                resultModes = filterModesByWidth(modes, Math.max(videoWidth, 1200));
            }

            //int minHeight = -1;
            //int maxHeight = -1;

            //if (mSwitchToUHD) { // switch not only framerate but resolution too
            //    if (videoWidth > 1920) {
            //        minHeight = 2160;
            //        maxHeight = 5000;
            //    }
            //}
            //
            //if (mSwitchToFHD) { // switch not only framerate but resolution too
            //    if (videoWidth <= 1920) {
            //        minHeight = 1080;
            //        maxHeight = 1080;
            //    }
            //}

            //resultModes = filterModes(modes, minHeight, maxHeight);

            if (!resultModes.isEmpty()) {
                needResolutionSwitch = true;
            }

            Log.i(TAG, "Need resolution switch: " + needResolutionSwitch);

            Mode currentMode = mUhdHelper.getCurrentMode();

            if (!needResolutionSwitch) {
                resultModes = filterSameResolutionModes(modes, currentMode);
            }

            Mode closerMode = findCloserMode(resultModes, videoFramerate);

            if (closerMode == null) {
                String msg = "Could not find closer refresh rate for " + videoFramerate + "fps";
                Log.i(TAG, msg);
                return false;
            }

            Log.i(TAG, "Found closer mode: " + closerMode + " for fps " + videoFramerate);
            Log.i(TAG, "Current mode: " + currentMode);

            if (!force && closerMode.equals(currentMode)) {
                Log.i(TAG, "Do not need to change mode.");
                return false;
            }

            mNewMode = closerMode;
            mUhdHelper.setPreferredDisplayModeId(window, mNewMode.getModeId(), true);
            mDisplaySyncInProgress = true;

            if (mListener != null) {
                mListener.onModeStart(mNewMode);
            }

            return true;
        }

        return false;
    }

    public void resetMode(Window window) {
        getUhdHelper().setPreferredDisplayModeId(window, 0, true);
    }

    /**
     * Lazy init of uhd helper.<br/>
     * Convenient when user doesn't use a afr at all.
     * @return helper
     */
    protected UhdHelper getUhdHelper() {
        if (mUhdHelper == null) {
            mUhdHelper = new UhdHelper(mContext);
            mUhdHelper.registerModeChangeListener(this);
        }

        return mUhdHelper;
    }

    public void saveOriginalState() {
        saveState(STATE_ORIGINAL);
    }

    public boolean restoreOriginalState(Window window, boolean force) {
        return restoreState(window, STATE_ORIGINAL, force);
    }

    public boolean restoreOriginalState(Window window) {
        return restoreOriginalState(window, false);
    }

    private void saveState(int state) {
        Mode mode = getUhdHelper().getCurrentMode();

        Log.d(TAG, "Saving mode: " + mode);

        if (mode != null) {
            switch (state) {
                case STATE_ORIGINAL:
                    mOriginalMode = mode;

                    AppPrefs.instance(mContext).setDefaultDisplayMode(UhdHelper.formatMode(mode));
                    break;
            }
        }
    }

    private boolean restoreState(Window window, int state, boolean force) {
        Log.d(TAG, "Beginning to restore state...");

        Mode modeTmp = null;

        switch (state) {
            case STATE_ORIGINAL:
                modeTmp = mOriginalMode;
                break;
        }

        if (modeTmp == null) {
            Log.d(TAG, "Can't restore state. Mode is null.");
            return false;
        }

        Mode mode = getUhdHelper().getCurrentMode();

        if (!force && modeTmp.equals(mode)) {
            Log.d(TAG, "Do not need to restore mode. Current mode is the same as new.");
            return false;
        }

        Log.d(TAG, "Restoring mode: " + modeTmp);
        
        getUhdHelper().setPreferredDisplayModeId(
                window,
                modeTmp.getModeId(),
                true);

        return true;
    }

    public void resetStats() {
        mModeLength = -1;
    }

    public void setListener(AutoFrameRateListener listener) {
        mListener = listener;
    }

    /**
     * Set default mode to 1920x1080@50<br/>
     * Because switch not work with some devices running at 60HZ. Like: UGOOS
     */
    public void applyModeChangeFix(Window window) {
        if (mOriginalMode != null) {
            if (mOriginalMode.getRefreshRate() > 55) {
                setDefaultMode(window, mOriginalMode.getPhysicalWidth(), 50);
            } else {
                setDefaultMode(window, mOriginalMode.getPhysicalWidth(), 60);
            }
        } else {
            setDefaultMode(window, 1080, 50);
        }
    }

    private void setDefaultMode(Window window, int width, float frameRate) {
        syncDisplayMode(window, width, frameRate);

        if (mNewMode != null) {
            mOriginalMode = mNewMode;
            AppPrefs.instance(mContext).setDefaultDisplayMode(UhdHelper.formatMode(mOriginalMode));
        }
    }

    public void setResolutionSwitchEnabled(boolean enabled) {
        mIsResolutionSwitchEnabled = enabled;
    }

    public boolean isResolutionSwitchEnabled() {
        return mIsResolutionSwitchEnabled;
    }

    public void setContext(Context context) {
        if (context != null) {
            mContext = context.getApplicationContext();
        }

        mUhdHelper = null; // uhd helper uses context, so do re-init
    }
}
