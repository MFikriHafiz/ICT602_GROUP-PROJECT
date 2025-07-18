package com.example.medialert; // Ensure this matches your package

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes; // For custom sound in channels (API 21+)
import android.media.RingtoneManager; // For default sound URI
import android.net.Uri; // For Uri
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

// Make sure your HomeActivity.java exists at the root of your package if you want the notification to open it
// e.g., import com.example.medialert.HomeActivity;

public class MedicationAlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "MedicationAlarmReceiver";
    public static final String CHANNEL_ID = "medialert_reminders_channel"; // Unique and constant channel ID
    private static final String CHANNEL_NAME = "Medialert Reminders";
    private static final String CHANNEL_DESCRIPTION = "Notifications for your medication reminders.";

    // Constants for Intent extras (already good, just for completeness)
    public static final String MEDICATION_ID_EXTRA = "medicationId";
    public static final String MEDICATION_NAME_EXTRA = "medicationName";
    public static final String MEDICATION_DOSAGE_EXTRA = "medicationDosage";
    public static final String MEDICATION_INSTRUCTIONS_EXTRA = "medicationInstructions";
    public static final String NOTIFICATION_ID_EXTRA = "notificationId";

    // Action string MUST match what's set in your PendingIntents in AddEditMedicationActivity and RescheduleAlarmsWorker
    public static final String ACTION_DELIVER_MEDICATION_ALARM = "com.example.medialert.ACTION_DELIVER_MEDICATION_ALARM";


    @Override
    public void onReceive(Context context, Intent intent) {
        // Essential: Check if the intent's action matches what we expect from AlarmManager
        // This helps filter out other broadcasts the receiver might get (e.g., BOOT_COMPLETED if combined)
        if (intent == null || !ACTION_DELIVER_MEDICATION_ALARM.equals(intent.getAction())) {
            Log.w(TAG, "Received unexpected intent action: " + (intent != null ? intent.getAction() : "null"));
            return;
        }

        Log.d(TAG, "MedicationAlarmReceiver received alarm.");

        // Create Notification Channel (required for Android 8.0 Oreo and above)
        // It's safe to call this method repeatedly; it only creates the channel if it doesn't exist.
        createNotificationChannel(context);

        // Retrieve data from the intent
        String medicationId = intent.getStringExtra(MEDICATION_ID_EXTRA);
        String medicationName = intent.getStringExtra(MEDICATION_NAME_EXTRA);
        String medicationDosage = intent.getStringExtra(MEDICATION_DOSAGE_EXTRA);
        String medicationInstructions = intent.getStringExtra(MEDICATION_INSTRUCTIONS_EXTRA);
        // Use a default value in case extra is missing; unique ID is crucial for notification management
        int notificationId = intent.getIntExtra(NOTIFICATION_ID_EXTRA, 0);


        Log.d(TAG, "Alarm details - Name: " + medicationName + ", Dosage: " + medicationDosage + ", Notif ID: " + notificationId);

        if (medicationName == null || medicationName.isEmpty()) {
            Log.e(TAG, "Medication name is null or empty. Cannot display notification.");
            return; // Essential data is missing, so don't show notification
        }

        // Create an Intent to open your HomeActivity when the notification is tapped
        // Ensure HomeActivity exists and is properly declared in AndroidManifest.xml
        Intent notificationTapIntent = new Intent(context, HomeActivity.class);
        // Flags to clear existing activities and start HomeActivity clean
        notificationTapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // Pass medication ID if HomeActivity needs it to display specific details or highlight
        notificationTapIntent.putExtra(MEDICATION_ID_EXTRA, medicationId);

        // Create a PendingIntent for the notification tap action
        // Use a unique request code (notificationId) to ensure distinct PendingIntents
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                notificationId, // Use the unique notification ID as the request code
                notificationTapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE // FLAG_IMMUTABLE is mandatory for API 23+
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_medialert_notification) // **IMPORTANT: Ensure this drawable exists and is a monochrome icon**
                .setContentTitle("Time for your medication!")
                .setContentText(medicationName + " - " + medicationDosage)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority helps with heads-up notification
                .setAutoCancel(true) // Dismiss notification when tapped
                .setCategory(NotificationCompat.CATEGORY_REMINDER) // Suggests it's a reminder
                .setContentIntent(contentIntent); // Set the tap action

        // Add instructions if available using BigTextStyle for expandable content
        if (medicationInstructions != null && !medicationInstructions.isEmpty()) {
            builder.setStyle(new NotificationCompat.BigTextStyle()
                    .bigText(medicationName + " - " + medicationDosage + "\nInstructions: " + medicationInstructions)
                    .setBigContentTitle("Time for your medication!")
                    .setSummaryText("Instructions: " + medicationInstructions)); // Smaller text below big content
        }

        // Set notification color (optional, requires a color in colors.xml)
        // Make sure R.color.primary exists or choose another color
        try {
            builder.setColor(ContextCompat.getColor(context, R.color.primary));
        } catch (Exception e) {
            Log.w(TAG, "Error setting notification color (R.color.primary might be missing): " + e.getMessage());
            // Fallback to default or just omit color
        }

        // Get the NotificationManager and show the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            // Check if notifications are enabled for the app
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !notificationManager.areNotificationsEnabled()) {
                Log.e(TAG, "Notifications are globally disabled for this app!");
                return; // Do not proceed if notifications are disabled by the user
            }
            // Corrected line here:
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Changed VERSION_INT to VERSION.SDK_INT
                NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID);
                if (channel == null) {
                    Log.e(TAG, "Notification channel '" + CHANNEL_ID + "' does not exist!");
                    // This case indicates createNotificationChannel was not effective or ran too late
                    return;
                }
                if (channel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                    Log.e(TAG, "Notification channel '" + CHANNEL_ID + "' is set to IMPORTANCE_NONE by user!");
                    return; // Channel is muted by user
                }
            }

            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "Notification shown for ID: " + notificationId + " for " + medicationName);
        } else {
            Log.e(TAG, "NotificationManager is null. Cannot show notification.");
        }
    }

    /**
     * Creates or updates the notification channel for Android 8.0 (API 26) and higher.
     * This method is idempotent, meaning it can be called multiple times safely.
     *
     * @param context The application context.
     */
    private void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH // Use IMPORTANCE_HIGH for timely, pop-up reminders
            );
            channel.setDescription(CHANNEL_DESCRIPTION);
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE); // Optional: notification light color
            channel.enableVibration(true);
            // Optional: Set a custom vibration pattern (e.g., strong then light)
            channel.setVibrationPattern(new long[]{500, 500, 500, 500, 500}); // Vibrate for 0.5s, pause 0.5s, etc.

            // Optional: Set a custom notification sound (e.g., default system notification sound)
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM); // Use alarm type for medication
            if (alarmSound == null) {
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            if (alarmSound != null) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM) // Important for alarm-like behavior
                        .build();
                channel.setSound(alarmSound, audioAttributes);
            }


            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this from code (user can change it)
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                // Check if channel already exists before creating
                NotificationChannel existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
                if (existingChannel == null) {
                    notificationManager.createNotificationChannel(channel);
                    Log.d(TAG, "Notification Channel '" + CHANNEL_ID + "' created.");
                } else {
                    // Log if it exists, no need to recreate
                    Log.d(TAG, "Notification Channel '" + CHANNEL_ID + "' already exists.");
                }
            } else {
                Log.e(TAG, "NotificationManager is null when trying to create channel.");
            }
        }
    }
}