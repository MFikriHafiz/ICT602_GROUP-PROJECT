package com.example.medialert.viewmodel;

import android.app.Application;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.medialert.MedicationAlarmReceiver;
import com.example.medialert.model.Medication;
import com.example.medialert.repository.MedicationRepository;
import com.example.medialert.utils.AlarmSchedulerUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddEditMedicationViewModel extends AndroidViewModel {

    private static final String TAG = "AddEditMedicationVM";

    private final MedicationRepository medicationRepository;
    private final FirebaseAuth firebaseAuth;
    private final MutableLiveData<Boolean> _saveSuccess = new MutableLiveData<>();
    public LiveData<Boolean> getSaveSuccess() {
        return _saveSuccess;
    }
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() {
        return _errorMessage;
    }

    private final Application applicationContext;

    public AddEditMedicationViewModel(@NonNull Application application) {
        super(application);
        this.applicationContext = application;

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            this.medicationRepository = new MedicationRepository(currentUser.getUid());
            Log.d(TAG, "MedicationRepository initialized for user: " + currentUser.getUid());
        } else {
            _errorMessage.postValue("User not logged in. Cannot access medication data.");
            this.medicationRepository = null;
            Log.e(TAG, "User not logged in. MedicationRepository not initialized.");
        }
    }

    public LiveData<Medication> getMedicationById(String medicationId) {
        MutableLiveData<Medication> liveData = new MutableLiveData<>();
        if (medicationRepository != null) {
            medicationRepository.getMedicationById(medicationId, new MedicationRepository.FirestoreCallback<Medication>() {
                @Override
                public void onSuccess(Medication medication) {
                    liveData.setValue(medication);
                    Log.d(TAG, "Medication loaded: " + (medication != null ? medication.getName() : "null"));
                }

                @Override
                public void onFailure(Exception e) {
                    _errorMessage.postValue("Failed to load medication: " + e.getMessage());
                    liveData.setValue(null);
                    Log.e(TAG, "Failed to load medication: " + e.getMessage());
                }
            });
        } else {
            liveData.setValue(null);
            Log.e(TAG, "Medication repository not initialized when trying to get by ID.");
        }
        return liveData;
    }

    public void saveMedication(Medication medication) {
        if (medicationRepository == null) {
            _errorMessage.postValue("Medication repository not initialized. User must be logged in.");
            _saveSuccess.setValue(false);
            Log.e(TAG, "Save failed: Medication repository not initialized.");
            return;
        }
        if (medication == null) {
            _errorMessage.postValue("Medication object is null.");
            _saveSuccess.setValue(false);
            Log.e(TAG, "Save failed: Medication object is null.");
            return;
        }

        MedicationRepository.FirestoreCallback<Void> callback = new MedicationRepository.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Medication saved/updated successfully: " + medication.getName());
                // Always cancel all existing alarms for this medication before (re)scheduling
                cancelAllAlarmsForMedication(medication.getId());

                if (medication.isActive() && medication.getAlarmTimes() != null && !medication.getAlarmTimes().isEmpty()) {
                    scheduleMedicationAlarm(medication);
                } else {
                    Log.d(TAG, "No alarms scheduled for medication " + medication.getName() +
                            " (Active: " + medication.isActive() + ", AlarmTimes Empty: " +
                            (medication.getAlarmTimes() == null || medication.getAlarmTimes().isEmpty()) + ")");
                }
                _saveSuccess.setValue(true);
            }

            @Override
            public void onFailure(Exception e) {
                _errorMessage.setValue("Failed to save medication: " + e.getMessage());
                _saveSuccess.setValue(false);
                Log.e(TAG, "Failed to save medication: " + e.getMessage(), e);
            }
        };

        if (medication.getId() == null || medication.getId().isEmpty()) {
            medicationRepository.addMedication(medication, callback);
            Log.d(TAG, "Adding new medication: " + medication.getName());
        } else {
            medicationRepository.updateMedication(medication, callback);
            Log.d(TAG, "Updating existing medication: " + medication.getName() + " (ID: " + medication.getId() + ")");
        }
    }

    // This method is called from saveMedication and BootReceiver
    public void scheduleMedicationAlarm(Medication medication) {
        if (applicationContext == null || medication.getAlarmTimes() == null || medication.getAlarmTimes().isEmpty()) {
            Log.w(TAG, "Cannot schedule alarm: missing context, alarm times, or empty alarm times list.");
            return;
        }
        if (!medication.isActive()) {
            Log.d(TAG, "Medication " + medication.getName() + " is inactive. Not scheduling alarms.");
            cancelAllAlarmsForMedication(medication.getId()); // Ensure alarms are off if it became inactive
            return;
        }

        AlarmManager alarmManager = (AlarmManager) applicationContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager not available. Cannot schedule alarm.");
            return;
        }

        // Check for exact alarm permission on Android 12 (API 31) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                _errorMessage.postValue("Exact alarm permission not granted. Reminders may be delayed or not fire.");
                Log.w(TAG, "Exact alarm permission not granted for API " + Build.VERSION.SDK_INT + ". Alarm may not fire exactly on time.");
                return; // Return if exactness is required and permission is not granted
            }
        }

        for (int i = 0; i < medication.getAlarmTimes().size(); i++) {
            long individualAlarmTimeMillisGmt08 = medication.getAlarmTimes().get(i);

            // Using AlarmSchedulerUtil for correct time calculation across timezones
            long finalAlarmTime = AlarmSchedulerUtil.calculateNextAlarmTimeMillis(individualAlarmTimeMillisGmt08);

            // Generate a unique request code for the PendingIntent
            // Using medication ID hash and alarm index ensures uniqueness
            int requestCode = (medication.getId().hashCode() * 1000) + i; // Max 20 alarms per medication assumed for cancellation loop

            Intent intent = new Intent(applicationContext, MedicationAlarmReceiver.class);
            // IMPORTANT: Set the explicit action for the BroadcastReceiver.
            // This MUST match the <action> filter in AndroidManifest.xml for MedicationAlarmReceiver.
            intent.setAction("com.example.medialert.ACTION_DELIVER_MEDICATION_ALARM");

            intent.putExtra(MedicationAlarmReceiver.MEDICATION_ID_EXTRA, medication.getId());
            intent.putExtra(MedicationAlarmReceiver.MEDICATION_NAME_EXTRA, medication.getName());

            // Ensure dosage is formatted correctly
            String dosageString = String.format(Locale.getDefault(), "%.1f%s",
                    medication.getDosageQuantity(),
                    medication.getDosageUnit());
            intent.putExtra(MedicationAlarmReceiver.MEDICATION_DOSAGE_EXTRA, dosageString);

            intent.putExtra(MedicationAlarmReceiver.MEDICATION_INSTRUCTIONS_EXTRA, medication.getInstructions());
            intent.putExtra(MedicationAlarmReceiver.NOTIFICATION_ID_EXTRA, requestCode);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    applicationContext,
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
                    new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(new java.util.Date(finalAlarmTime)) +
                    " (Request Code: " + requestCode + ")");
        }
    }

    public void cancelAllAlarmsForMedication(String medicationId) {
        if (applicationContext == null || medicationId == null || medicationId.isEmpty()) {
            Log.w(TAG, "Cannot cancel alarms: missing context or medication ID.");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) applicationContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager not available for canceling alarms.");
            return;
        }

        // Loop through a reasonable number of potential request codes
        // This must match the range used when scheduling (e.g., up to 20 alarms per medication)
        for (int i = 0; i < 20; i++) {
            int requestCode = (medicationId.hashCode() * 1000) + i;

            Intent intent = new Intent(applicationContext, MedicationAlarmReceiver.class);
            // IMPORTANT: The action MUST match the action used when setting the alarm
            intent.setAction("com.example.medialert.ACTION_DELIVER_MEDICATION_ALARM");
            // FLAG_NO_CREATE is important here: it only retrieves if the PendingIntent already exists
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    applicationContext,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel(); // Also cancel the PendingIntent itself to free resources
                Log.d(TAG, "Cancelled alarm for medication ID: " + medicationId + " with request code: " + requestCode);
            } else {
                // Log.d(TAG, "No existing alarm found for medication ID: " + medicationId + " with request code: " + requestCode);
            }
        }
    }
}