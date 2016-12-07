
package com.yunos.alicontacts.sim;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SimContactLoadReceiver extends BroadcastReceiver {
    private static final String TAG = "SimContactLoadReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SimUtil.IS_YUNOS) {
            final String action = intent.getAction();
            int slotId = SimContactUtils.getSlotIdFromSimStateChangedIntent(intent);
            Log.d(TAG, "onReceive() action:" + action + ", dualSimMode:" + SimUtil.MULTISIM_ENABLE + "; slotId="+slotId);
            if (SimContactUtils.ACTION_SIM_STATE_CHANGED.equals(action)) {
                SimContactUtils.handleSimState(intent);
            }

            String handleSimContactsAction = null;
            int op = SimContactUtils.needToLoadSimContactsToPhone(intent, action);
            if (op == SimContactUtils.OPERATION_LOAD_SIM_CONTACTS_TO_PHONE_DB) {
                // If the app is launched by a broadcast to this receiver,
                // then the App.onCreate might start the service.
                // If it is this case, then the loaded count will be SCHEDULE_LOAD_COUNT,
                // we need to skip start service again.
                if (SimContactLoadService.getSimLoadedCount(slotId) == SimContactLoadService.SCHEDULE_LOAD_COUNT) {
                    Log.i(TAG, "onReceive: already scheduled loading. quit.");
                    return;
                }
                handleSimContactsAction = SimContactLoadService.ACTION_LOAD_SIM_CONTACTS_TO_PHONE_DB;
            } else if (op == SimContactUtils.OPERATION_DELETE_SIM_CONTACTS_FROM_PHONE_DB) {
                handleSimContactsAction = SimContactLoadService.ACTION_DELETE_SIM_CONTACTS_FROM_PHONE_DB;
            } else {
                Log.d(TAG, "onReceive: nothing to do, quit.");
                return;
            }
            handleSimContacts(context, action, handleSimContactsAction, slotId);
        }
    }

    private void handleSimContacts(Context context, String receiverAction, String handleSimContactsAction, int slotId) {
        if (slotId == SimUtil.INVALID_SLOT_ID) {
            Log.i(TAG, "handleSimContacts: got invalid slot id. receiverAction="+receiverAction);
            if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(receiverAction)) {
                handleSimContactsForSlot(context, handleSimContactsAction, SimUtil.SLOT_ID_1);
                if (SimUtil.MULTISIM_ENABLE) {
                    handleSimContactsForSlot(context, handleSimContactsAction, SimUtil.SLOT_ID_2);
                }
            } else {
                Log.e(TAG, "handleSimContacts: can NOT determine which slot to handle. quit.");
                return;
            }
        } else {
            Log.i(TAG, "handleSimContacts: slotId="+slotId+"; action="+handleSimContactsAction);
            handleSimContactsForSlot(context, handleSimContactsAction, slotId);
        }
    }

    private void handleSimContactsForSlot(Context context, String action, int slotId) {
        // If the receiver gets broadcast during app running,
        // then we don't need to change the count.
        if (SimContactLoadService.getSimLoadedCount(slotId) == SimContactLoadService.NOT_LOADED_COUNT) {
            Log.i(TAG, "handleSimContactsForSlot: set sim loaded count to schedule loading for slot "+slotId);
            SimContactLoadService.setSimScheduleLoading(slotId);
        }
        Intent handleIntent = new Intent(action);
        handleIntent.setClass(context.getApplicationContext(), SimContactLoadService.class);
        handleIntent.putExtra(SimContactLoadService.INTENT_KEY_SLOT_ID, slotId);
        Log.i(TAG, "handleSimContactsForSlot: send intent "+handleIntent+" for slot "+slotId);
        context.startService(handleIntent);
    }

}
