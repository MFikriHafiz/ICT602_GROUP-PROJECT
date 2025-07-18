package com.example.medialert.worker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.medialert.MedicationAlarmReceiver;
import com.example.medialert.model.Medication;
import com.example.medialert.repository.MedicationRepository;
import com.example.medialert.utils.AlarmSchedulerUtil; // NEW: Import AlarmSchedulerUtil
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class RescheduleAlarmsWorker extends Worker {

    private static final String TAG = "RescheduleAlarmsWorker";
    private final Context context;

    public RescheduleAlarmsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "RescheduleAlarmsWorker started.");

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            Log.d(TAG, "No user logged in. Cannot reschedule alarms.");
            return Result.failure();
        }

        String userId = currentUser.getUid();
        MedicationRepository repository = new MedicationRepository(userId);

        // No need for a null check on repository here, as the constructor throws IllegalArgumentException
        // if userId is null/empty, which would prevent the worker from even being created correctly.

        CountDownLatch latch = new CountDownLatch(1);
        final List<Medication>[] medicationsHolder = new List[]{null};
        final Exception[] errorHolder = new Exception[]{null};

        // Fetch all medications once from the repository
        repository.fetchAllMedicationsOnce(new MedicationRepository.FirestoreCallback<List<Medication>>() {
            @Override
            public void onSuccess(List<Medication> medications) {
                medicationsHolder[0] = medications;
                latch.countDown(); // Signal that data fetching is complete
            }

            @Override
            public void onFailure(Exception e) {
                errorHolder[0] = e;
                Log.e(TAG, "Failed to fetch medications for reschedule: " + e.getMessage(), e);
                latch.countDown(); // Signal completion even on failure
            }
        });

        try {
            // Wait for the data fetching to complete, with a timeout
            boolean completed = latch.await(30, TimeUnit.SECONDS);
            if (!completed) {
                Log.e(TAG, "Timeout waiting for medications to load. Retrying work.");
                return Result.retry(); // Retry if timeout occurs
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Worker interrupted while waiting for medications: " + e.getMessage(), e);
            Thread.currentThread().interrupt(); // Re-interrupt the thread
            return Result.failure(); // Fail if interrupted
        }

        if (errorHolder[0] != null) {
            Log.e(TAG, "Error fetching medications: " + errorHolder[0].getMessage());
            return Result.failure(); // Fail if there was an error fetching data
        }

        List<Medication> medications = medicationsHolder[0];
        if (medications == null || medications.isEmpty()) {
            Log.d(TAG, "No medications fetched or list is empty. No alarms to reschedule.");
            return Result.success(); // No active medications is a successful outcome for this worker
        }

        int rescheduledCount = 0;
        for (Medication medication : medications) {
            // Only reschedule active medications with defined alarm times
            if (medication.isActive() && medication.getAlarmTimes() != null && !medication.getAlarmTimes().isEmpty()) {
                Log.d(TAG, "Attempting to reschedule alarms for: " + medication.getName() + " (ID: " + medication.getId() + ")");
                scheduleMedicationAlarm(medication); // Call the helper method to schedule
                rescheduledCount++;
            } else {
                Log.d(TAG, "Skipping inactive or no-alarm medication: " + medication.getName() + " (ID: " + medication.getId() + ")");
                // Optionally, cancel any existing alarms for this medication if it became inactive
                cancelAllAlarmsForMedication(medication.getId());
            }
        }

        Log.d(TAG, "Successfully processed and rescheduled " + rescheduledCount + " medications.");
        return Result.success();
    }

    /**
     * Schedules alarms for a given medication.
     * This method is called from doWork() to reschedule alarms after boot or other triggers.
     *
     * @param medication The Medication object for which to schedule alarms.
     */
    private void scheduleMedicationAlarm(Medication medication) {
        if (medication.getAlarmTimes() == null || medication.getAlarmTimes().isEmpty()) {
            Log.w(TAG, "scheduleMedicationAlarm: No alarm times provided for " + medication.getName());
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "scheduleMedicationAlarm: AlarmManager not available. Cannot schedule alarms.");
            return;
        }

        // Check for SCHEDULE_EXACT_ALARM permission on API 31+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Exact alarm permission not granted. Alarms for " + medication.getName() + " may be delayed or not fire exactly.");
                // Since this is a Worker, we cannot directly ask the user for permission.
                // It's expected that the main Activity (e.g., HomeActivity) would have handled this prompt.
                // If not granted, the alarms will be inexact, which might be okay for some cases,
                // but for critical medication reminders, exact alarms are preferred.
                // We return here because exactness is desired.
                return;
            }
        }

        for (int i = 0; i < medication.getAlarmTimes().size(); i++) {
            long individualAlarmTimeMillisGmt08 = medication.getAlarmTimes().get(i);

            // *** CRUCIAL FIX: Use AlarmSchedulerUtil for correct time calculation ***
            // This ensures the alarm time is correctly calculated for the device's current timezone
            // based on the stored GMT+08:00 time-of-day, and is always in the future.
            long finalAlarmTime = AlarmSchedulerUtil.calculateNextAlarmTimeMillis(individualAlarmTimeMillisGmt08);

            // Generate a unique request code for the PendingIntent
            // Using medication ID hash and alarm index ensures uniqueness for each alarm instance
            int requestCode = (medication.getId().hashCode() * 1000) + i; // Max 20 alarms per medication assumed for cancellation loop

            Intent intent = new Intent(context, MedicationAlarmReceiver.class);
            // IMPORTANT: Set the explicit action for the BroadcastReceiver.
            // This MUST match the <action> filter in AndroidManifest.xml for MedicationAlarmReceiver.
            intent.setAction(MedicationAlarmReceiver.ACTION_DELIVER_MEDICATION_ALARM);

            // Put all necessary extras into the intent for the receiver
            intent.putExtra(MedicationAlarmReceiver.MEDICATION_ID_EXTRA, medication.getId());
            intent.putExtra(MedicationAlarmReceiver.MEDICATION_NAME_EXTRA, medication.getName());

            // Format dosage string
            String dosageString = String.format(Locale.getDefault(), "%.1f%s",
                    medication.getDosageQuantity(),
                    medication.getDosageUnit());
            intent.putExtra(MedicationAlarmReceiver.MEDICATION_DOSAGE_EXTRA, dosageString);

            intent.putExtra(MedicationAlarmReceiver.MEDICATION_INSTRUCTIONS_EXTRA, medication.getInstructions());
            intent.putExtra(MedicationAlarmReceiver.NOTIFICATION_ID_EXTRA, requestCode);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode, // Unique request code for this PendingIntent
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE // FLAG_IMMUTABLE is crucial for Android 6+
            );

            // Schedule the alarm based on API level
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // setExactAndAllowWhileIdle() is preferred for exact alarms that work in Doze mode
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, finalAlarmTime, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // setExact() for exact alarms on older devices
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, finalAlarmTime, pendingIntent);
            } else {
                // set() for inexact alarms on very old devices (less reliable)
                alarmManager.set(AlarmManager.RTC_WAKEUP, finalAlarmTime, pendingIntent);
            }

            Log.d(TAG, "Scheduled alarm for '" + medication.getName() +
                    "' (ID: " + medication.getId() + ") at " +
                    android.text.format.DateFormat.format("hh:mm a, dd/MM/yyyy", new java.util.Date(finalAlarmTime)) +
                    " (Request Code: " + requestCode + ")");
        }
    }

    /**
     * Cancels all potential alarms associated with a specific medication ID.
     * This is useful when a medication becomes inactive or is deleted.
     *
     * @param medicationId The ID of the medication for which alarms need to be cancelled.
     */
    private void cancelAllAlarmsForMedication(String medicationId) {
        if (medicationId == null || medicationId.isEmpty()) {
            Log.w(TAG, "cancelAllAlarmsForMedication: Medication ID is null/empty. Cannot proceed.");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "cancelAllAlarmsForMedication: AlarmManager not available. Cannot cancel alarms.");
            return;
        }

        // Loop through a reasonable number of potential request codes
        // This '20' should match the maximum number of daily alarms you allow per medication
        for (int i = 0; i < 20; i++) {
            int requestCode = (medicationId.hashCode() * 1000) + i;

            Intent intent = new Intent(context, MedicationAlarmReceiver.class);
            // IMPORTANT: The action MUST EXACTLY match the action used when setting the alarm
            intent.setAction(MedicationAlarmReceiver.ACTION_DELIVER_MEDICATION_ALARM);

            // Use FLAG_NO_CREATE to only retrieve an existing PendingIntent, not create a new one
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent); // Cancel the alarm
                pendingIntent.cancel(); // Also cancel the PendingIntent itself
                Log.d(TAG, "Cancelled alarm for medication ID: " + medicationId + " with request code: " + requestCode);
            } else {
                // This is expected for request codes that didn't have an alarm set
                // Log.d(TAG, "No existing alarm found for medication ID: " + medicationId + " with request code: " + requestCode);
            }
        }
    }
}