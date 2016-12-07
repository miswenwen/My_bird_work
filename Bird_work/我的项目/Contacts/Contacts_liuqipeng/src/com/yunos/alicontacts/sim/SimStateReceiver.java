
package com.yunos.alicontacts.sim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.yunos.alicontacts.ContactsUtils;

import java.util.ArrayList;

public class SimStateReceiver extends BroadcastReceiver {
    private static final String TAG = "SimStateReceiver";

    /**
     * Monitor the action android.intent.action.SIM_STATE_CHANGED and android.intent.action.AIRPLANE_MODE.
     * This will affect dialer and SIM contacts.
     */
    public interface SimStateListener {
        public void onSimStateChanged(int slot, String state);
    }

    private static final ArrayList<SimStateListener> sSimStateListeners = new ArrayList<SimStateListener>();

    public static void registSimStateListener(SimStateListener listener) {
        synchronized (sSimStateListeners) {
            sSimStateListeners.add(listener);
        }
    }

    public static void unregistSimStateListener(SimStateListener listener) {
        synchronized (sSimStateListeners) {
            sSimStateListeners.remove(listener);
        }
    }

    private void notifySimStateChanged(int slot, String state) {
        if (slot == SimUtil.INVALID_SLOT_ID) {
            Log.i(TAG, "notifySimStateChanged: can NOT read subscription from intent. quit.");
            return;
        }
        SimStateListener[] listeners;
        synchronized (sSimStateListeners) {
            listeners = sSimStateListeners.toArray(new SimStateListener[sSimStateListeners.size()]);
        }
        for (int i = listeners.length - 1; i >= 0; i--) {
            final SimStateListener listener = listeners[i];
            if (listener != null) {
                listener.onSimStateChanged(slot, state);
            }
        }
    }

    /**
     * *************************************************************************
     * MTK platform dual card:
     * 1. airplane mode from off to on, SIM state change: LOADED-> NOT_READY.
     *
     * 2. airplane mode from on to off, SIM state change: NOT_READY-> UNKNOWN->
     * READY-> IMSI-> LOADED.
     *
     * 3. hot eject SIM card, SIM state change: LOADED-> ABSENT.
     *
     * 4. hot inject SIM card, SIM state change: ABSENT-> READY-> IMSI-> LOADED.
     *
     * 5. deactivate SIM card, SIM state change: LOADED-> NOT_READY.
     *
     * 6. activate SIM card, SIM state change: NOT_READY-> UNKNOWN-> READY->
     * IMSI-> LOADED.
     *
     * ------------------------------------------------------------------------
     * Qualcomm platform single card:
     * 1. airplane mode from off to on, SIM state change: LOADED-> ABSENT.
     *
     * 2. airplane mode from on to off, SIM state change: ABSENT-> NOT_READY->
     * UNKNOWN-> READY-> IMSI-> LOADED.
     *
     * 3. hot eject SIM card, SIM state change: LOADED-> ABSENT.
     *
     * 4. hot inject SIM card, SIM state change: ABSENT-> READY-> IMSI-> LOADED.
     * *************************************************************************
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        Log.i(TAG, "onReceive: action="+action);
        if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
            //when switch airPlane mode on>off>on fast,isAirplaneMode will get result false>false which should be false>true
            //so get the airmode from intent,rather than from SimUtil.isAirplaneModeOn(context);
            boolean airMode = intent.getExtras().getBoolean("state");
            if (airMode) {
                notifySimStateChanged(SimUtil.DUMMY_SLOT_FOR_AIRPLANE_MODE_ON, null);
            } else {
                notifySimStateChanged(SimUtil.DUMMY_SLOT_FOR_AIRPLANE_MODE_OFF, null);
            }
        } else if (SimContactUtils.ACTION_SIM_STATE_CHANGED.equals(action)) {
            String state = intent.getStringExtra(SimContactUtils.INTENT_KEY_ICC_STATE);
            //int slotId = intent.getIntExtra(SimUtil.SIM_STATE_CHANGE_SLOT_KEY, SimUtil.INVALID_SLOT_ID);
            int slotId = SimContactUtils.getSlotIdFromSimStateChangedIntent(intent);

            final boolean isAirplaneMode = SimUtil.isAirplaneModeOn(context);
            Log.d(TAG, "onReceive() action:" + action + ", isAirplaneMode:" + isAirplaneMode
                    + ", state:" + state + " on slot " + slotId);
            if (SimContactUtils.INTENT_VALUE_ICC_LOADED.equals(state)
                    || (!isAirplaneMode && (SimContactUtils.INTENT_VALUE_ICC_NOT_READY
                            .equals(state) || SimContactUtils.INTENT_VALUE_ICC_ABSENT.equals(state)))) {
                notifySimStateChanged(slotId, state);

                // TODO: When eject SIM card,
                // the country detector might not get the correct country immediately.
                // If the countryIso impacts high priority cases,
                // we might have to call updateCurrentCountryIso in other events.
                ContactsUtils.updateCurrentCountryIso(context);
            }
        }
    }

}
