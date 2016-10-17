package com.example.jasmine.project1;

import android.content.Intent;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by jasmine on 9/30/16.
 */

public class WearMessageListenerService extends WearableListenerService {
    private static final String START_ACTIVITY = "/start_activity";

    //check the path in the message, if start-activity, fire an intent
    //to wear MainAcivity to bring app to forefront
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equalsIgnoreCase(START_ACTIVITY)) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } else {
            super.onMessageReceived(messageEvent);
        }
    }
}
