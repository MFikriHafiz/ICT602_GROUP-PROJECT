package com.example.medialert.model;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.medialert.model.Medication;
import com.example.medialert.repository.MedicationRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class MedicationHistoryViewModel extends AndroidViewModel {

    private static final String TAG = "MedicationHistoryVM";

    private final MedicationRepository medicationRepository;
    private final MutableLiveData<List<Medication>> _inactiveMedications = new MutableLiveData<>();
    public LiveData<List<Medication>> inactiveMedications = _inactiveMedications;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<Boolean> _deleteSuccess = new MutableLiveData<>();
    public LiveData<Boolean> deleteSuccess = _deleteSuccess;

    public MedicationHistoryViewModel(@NonNull Application application) {
        super(application);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            this.medicationRepository = new MedicationRepository(currentUser.getUid());
        } else {
            // Handle case where user is not logged in (e.g., redirect to login or show error)
            Log.e(TAG, "No user logged in. Cannot initialize MedicationRepository.");
            this.medicationRepository = null; // Or throw an exception, depending on desired behavior
            _errorMessage.setValue("No user logged in. Please log in to view history.");
        }
    }

    /**
     * Fetches all inactive medications from the repository.
     */
    public void fetchInactiveMedications() {
        if (medicationRepository == null) {
            Log.e(TAG, "MedicationRepository is not initialized. Cannot fetch inactive medications.");
            _errorMessage.setValue("Error: User not authenticated.");
            return;
        }

        medicationRepository.fetchInactiveMedicationsOnce(new MedicationRepository.FirestoreCallback<List<Medication>>() {
            @Override
            public void onSuccess(List<Medication> medications) {
                _inactiveMedications.setValue(medications);
                Log.d(TAG, "Inactive medications fetched successfully: " + (medications != null ? medications.size() : 0));
            }

            @Override
            public void onFailure(Exception e) {
                _errorMessage.setValue("Failed to load medication history: " + e.getMessage());
                Log.e(TAG, "Error fetching inactive medications: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Deletes a medication from history (Firebase).
     * @param medicationId The ID of the medication to delete.
     */
    public void deleteMedicationFromHistory(String medicationId) {
        if (medicationRepository == null) {
            Log.e(TAG, "MedicationRepository is not initialized. Cannot delete medication.");
            _errorMessage.setValue("Error: User not authenticated.");
            _deleteSuccess.setValue(false);
            return;
        }

        medicationRepository.deleteMedication(medicationId, new MedicationRepository.FirestoreCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Medication deleted from history successfully: " + medicationId);
                _deleteSuccess.setValue(true);
                // After deletion, re-fetch the list to update the UI
                fetchInactiveMedications();
            }

            @Override
            public void onFailure(Exception e) {
                _errorMessage.setValue("Failed to delete medication from history: " + e.getMessage());
                Log.e(TAG, "Error deleting medication from history: " + e.getMessage(), e);
                _deleteSuccess.setValue(false);
            }
        });
    }

    // You can add more methods here if you want search/filter logic within the ViewModel
    // For example:
    /*
    public void searchInactiveMedications(String query) {
        if (_inactiveMedications.getValue() == null) {
            return;
        }
        List<Medication> currentList = _inactiveMedications.getValue();
        List<Medication> filteredList = new ArrayList<>();
        for (Medication med : currentList) {
            if (med.getName().toLowerCase(Locale.getDefault()).contains(query.toLowerCase(Locale.getDefault()))) {
                filteredList.add(med);
            }
        }
        _inactiveMedications.setValue(filteredList); // This will update the UI with filtered results
    }
    */
}