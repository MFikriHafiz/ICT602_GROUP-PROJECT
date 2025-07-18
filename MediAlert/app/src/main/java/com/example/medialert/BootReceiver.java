package com.example.medialert;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.medialert.worker.RescheduleAlarmsWorker; // Ensure correct path to your worker

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Log the action received for debugging
        Log.d(TAG, "onReceive triggered with action: " + (intent != null ? intent.getAction() : "null"));

        // Check for BOOT_COMPLETED or QUICKBOOT_POWERON actions
        if (intent == null || (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) &&
                !"android.intent.action.QUICKBOOT_POWERON".equals(intent.getAction()))) {
            Log.w(TAG, "Received unexpected intent in BootReceiver. Ignoring.");
            return;
        }

        Log.d(TAG, "Device reboot or quick boot detected. Enqueuing RescheduleAlarmsWorker.");

        // Define constraints for the work request (optional but recommended)
        // For alarm rescheduling, we generally want it to run as soon as possible,
        // so network isn't strictly necessary if Firestore data might be cached.
        // However, if fetching fresh data is critical, uncomment the NETWORK_CONNECTED constraint.
        Constraints constraints = new Constraints.Builder()
                // .setRequiredNetworkType(NetworkType.CONNECTED) // Uncomment if network is essential for fetching medications
                .build();

        // Create a OneTimeWorkRequest for your RescheduleAlarmsWorker
        // Using a unique tag allows you to later check the status or cancel this specific work
        OneTimeWorkRequest rescheduleWorkRequest = new OneTimeWorkRequest.Builder(RescheduleAlarmsWorker.class)
                .setConstraints(constraints)
                .addTag("rescheduleAlarmsOnBoot") // Add a tag for easy identification
                .build();

        // Enqueue the work with WorkManager
        WorkManager.getInstance(context).enqueue(rescheduleWorkRequest);
        Log.d(TAG, "RescheduleAlarmsWorker enqueued successfully.");
    }
}