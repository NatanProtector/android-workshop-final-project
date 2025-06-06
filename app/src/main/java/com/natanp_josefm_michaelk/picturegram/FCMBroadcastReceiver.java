package com.natanp_josefm_michaelk.picturegram;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;

public class FCMBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "FCMBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.hasExtra("message")) {
            RemoteMessage message = intent.getParcelableExtra("message");
            if (message != null) {
                // Forward the message to BackgroundNotificationService
                Intent serviceIntent = new Intent(context, BackgroundNotificationService.class);
                serviceIntent.putExtra("message", message);
                context.startService(serviceIntent);
            }
        }
    }
} 