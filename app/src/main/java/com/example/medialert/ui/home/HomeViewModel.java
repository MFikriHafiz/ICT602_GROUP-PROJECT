package com.example.medialert.ui.home;

import android.app.Application;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.medialert.MedicationAlarmReceiver;
import com.example.medialert.model.Medication;
import com.example.medialert.repository.MedicationRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private static final String TAG = "HomeViewModel"; // Tag for logging

    private final LiveData<List<Medication>> mMedications;
    private final MedicationRepository repository;

    // LiveData for providing feedback on delete operation success/failure
    private final MutableLiveData<Boolean> _deleteSuccess = new MutableLiveData<>();
    public LiveData<Boolean> getDeleteSuccess() {
        return _deleteSuccess;
    }

    // LiveData for providing error messages to the UI
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() {
        return _errorMessage;
    }

    private final Application applicationContext; // Storing application context for system services

    /**
     * Constructor for HomeViewModel.
     * Initializes the repository and fetches medications for the current logged-in user.
     *
     * @param application The application instance, required for AndroidViewModel.
     */
    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.applicationContext = application;

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            // Initialize the repository with the current user's ID
            repository = new MedicationRepository(currentUser.getUid());
            // Get LiveData stream of all medications from the repository
            mMedications = repository.getAllMedications();
            Log.d(TAG, "MedicationRepository initialized for user: " + currentUser.getUid());
        } else {
            // Handle scenario where user is not logged in.
            // Operations will fail and LiveData will remain empty.
            repository = null; // Set to null to prevent NullPointerExceptions
            mMedications = new MutableLiveData<>(); // Return an empty LiveData list
            _errorMessage.postValue("User not logged in. Cannot fetch medications.");
            Log.e(TAG, "No current Firebase user found. Repository not initialized.");
        }
    }

    /**
     * Returns LiveData containing a list of all medications.
     * This LiveData will automatically update as changes occur in Firestore.
     *
     * @return LiveData<List<Medication>>
     */
    public LiveData<List<Medication>> getMedications() {
        return mMedications;
    }

    /**
     * Adds a new medication to the repository.
     *
     * @param newMedication The Medication object to add.
     */
    public void addMedication(Medication newMedication) {
        if (repository == null) {
            _errorMessage.postValue("User not logged in. Cannot add medication.");
            Log.e(TAG, "addMedication failed: Repository is null (user not logged in).");
            return;
        }
        repository.addMedication(newMedication, new MedicationRepository.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // LiveData update handled automatically by repository's listener
                Log.d(TAG, "Medication added successfully to Firestore.");
            }

            @Override
            public void onFailure(Exception e) {
                _errorMessage.postValue("Failed to add medication: " + e.getMessage());
                Log.e(TAG, "Failed to add medication to Firestore.", e);
            }
        });
    }

    /**
     * Updates an existing medication in the repository.
     *
     * @param updatedMedication The Medication object with updated data.
     */
    public void updateMedication(Medication updatedMedication) {
        if (repository == null) {
            _errorMessage.postValue("User not logged in. Cannot update medication.");
            Log.e(TAG, "updateMedication failed: Repository is null (user not logged in).");
            return;
        }
        repository.updateMedication(updatedMedication, new MedicationRepository.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // LiveData update handled automatically by repository's listener
                Log.d(TAG, "Medication updated successfully in Firestore.");
            }

            @Override
            public void onFailure(Exception e) {
                _errorMessage.postValue("Failed to update medication: " + e.getMessage());
                Log.e(TAG, "Failed to update medication in Firestore.", e);
            }
        });
    }

    /**
     * Deletes a medication from the repository and cancels its associated alarms.
     *
     * @param medicationId The ID of the medication to delete.
     */
    public void deleteMedication(String medicationId) {
        if (repository == null) {
            _errorMessage.postValue("User not logged in. Cannot delete medication.");
            _deleteSuccess.setValue(false); // Signal failure to the UI
            Log.e(TAG, "deleteMedication failed: Repository is null (user not logged in).");
            return;
        }

        repository.deleteMedication(medicationId, new MedicationRepository.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                // On successful deletion from Firestore, proceed to cancel associated alarms
                cancelAllAlarmsForMedication(medicationId);
                _deleteSuccess.setValue(true); // Signal success to the UI
                Log.d(TAG, "Medication deleted successfully from Firestore: " + medicationId);
            }

            @Override
            public void onFailure(Exception e) {
                _errorMessage.postValue("Failed to delete medication: " + e.getMessage());
                _deleteSuccess.setValue(false); // Signal failure to the UI
                Log.e(TAG, "Failed to delete medication from Firestore: " + medicationId, e);
            }
        });
    }

    /**
     * Cancels all potential alarms associated with a specific medication ID.
     * It iterates through a range of request codes to cover all possible alarms
     * that might have been set for that medication.
     *
     * @param medicationId The ID of the medication for which alarms need to be cancelled.
     */
    private void cancelAllAlarmsForMedication(String medicationId) {
        if (applicationContext == null || medicationId == null || medicationId.isEmpty()) {
            Log.w(TAG, "cancelAllAlarmsForMedication: Context or medication ID is null/empty. Cannot proceed.");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) applicationContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "cancelAllAlarmsForMedication: AlarmManager not available. Cannot cancel alarms.");
            return;
        }

        // Loop through a reasonable number of potential alarm IDs.
        // This '20' should match the maximum number of daily alarms you allow per medication
        // (e.g., if a medication can have up to 20 daily alarm times).
        for (int i = 0; i < 20; i++) {
            // Reconstruct the request code used when setting the alarm
            int requestCode = (medicationId.hashCode() * 1000) + i;

            Intent intent = new Intent(applicationContext, MedicationAlarmReceiver.class);
            // IMPORTANT: The action MUST EXACTLY match the action used when setting the alarm
            intent.setAction(MedicationAlarmReceiver.ACTION_DELIVER_MEDICATION_ALARM);

            // Use FLAG_NO_CREATE to only retrieve an existing PendingIntent, not create a new one
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    applicationContext,
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

    /**
     * Called when the ViewModel is no longer used and will be destroyed.
     * This is the place to clean up resources, like removing Firestore listeners,
     * to prevent memory leaks.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        if (repository != null) {
            repository.removeMedicationsListener(); // Remove the Firestore real-time listener
            Log.d(TAG, "Firestore listener removed from repository during onCleared.");
        }
    }
}