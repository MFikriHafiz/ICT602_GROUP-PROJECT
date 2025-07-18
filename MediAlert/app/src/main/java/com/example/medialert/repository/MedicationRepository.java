package com.example.medialert.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.medialert.model.Medication;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
// Removed unused imports from your previous versions
// import com.google.firebase.firestore.DocumentReference; // Not strictly needed for the operations you perform here

import java.util.ArrayList;
import java.util.List;

public class MedicationRepository {

    private static final String TAG = "MedicationRepository"; // For logging
    private final CollectionReference medicationsRef;
    private ListenerRegistration medicationsListener;

    public MedicationRepository(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty for MedicationRepository.");
        }
        medicationsRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("medications");
    }

    /**
     * Callback interface for asynchronous Firestore operations.
     * @param <T> The type of the result on success.
     */
    public interface FirestoreCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    /**
     * Provides a LiveData stream of all medications for the current user,
     * sorted by name. It sets up a real-time Firestore listener.
     *
     * @return LiveData containing a list of Medication objects.
     */
    public LiveData<List<Medication>> getAllMedications() {
        MutableLiveData<List<Medication>> liveData = new MutableLiveData<>();

        // Remove any existing listener to avoid multiple listeners or leaks
        if (medicationsListener != null) {
            medicationsListener.remove();
        }

        // Attach a real-time listener to the Firestore collection
        medicationsListener = medicationsRef
                .orderBy("name", Query.Direction.ASCENDING) // Order results by medication name
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        System.err.println("MedicationRepository Listen failed: " + error);
                        // Post null or an empty list, and log the error for debugging
                        liveData.postValue(null);
                        return;
                    }

                    List<Medication> medications = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            // Convert each document to a Medication object
                            Medication medication = doc.toObject(Medication.class);
                            if (medication != null) {
                                medication.setId(doc.getId()); // Set the Firestore document ID
                                medications.add(medication);
                            }
                        }
                    }
                    liveData.postValue(medications); // Update LiveData with the new list
                });
        return liveData;
    }

    /**
     * Removes the active Firestore real-time listener.
     * This should be called when the associated ViewModel or component is no longer active
     * to prevent memory leaks.
     */
    public void removeMedicationsListener() {
        if (medicationsListener != null) {
            medicationsListener.remove();
            medicationsListener = null;
            System.out.println("Medications listener removed."); // For debugging
        }
    }

    /**
     * Fetches a single medication by its ID.
     *
     * @param medicationId The ID of the medication to retrieve.
     * @param callback Callback to receive the Medication object or an error.
     */
    public void getMedicationById(String medicationId, FirestoreCallback<Medication> callback) {
        medicationsRef.document(medicationId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Medication medication = documentSnapshot.toObject(Medication.class);
                        if (medication != null) {
                            medication.setId(documentSnapshot.getId()); // Set the Firestore document ID
                            callback.onSuccess(medication);
                        } else {
                            callback.onFailure(new Exception("Medication object is null after conversion for ID: " + medicationId));
                        }
                    } else {
                        callback.onSuccess(null); // Medication not found
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Adds a new medication to Firestore.
     * Note: Firestore will automatically generate an ID. If you need this ID *before*
     * the onSuccess callback, you'd need to manually generate it with `document().getId()`
     * and use `set()` instead of `add()`.
     *
     * @param medication The Medication object to add.
     * @param callback Callback to receive success or failure.
     */
    public void addMedication(Medication medication, FirestoreCallback<Void> callback) {
        medicationsRef.add(medication)
                .addOnSuccessListener(documentReference -> {
                    // documentReference.getId() holds the newly generated ID
                    // If you need to immediately set the ID back to the medication object:
                    // medication.setId(documentReference.getId());
                    System.out.println("Medication added with ID: " + documentReference.getId()); // For debugging
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    System.err.println("Error adding medication: " + e.getMessage()); // For debugging
                    callback.onFailure(e);
                });
    }

    /**
     * Updates an existing medication in Firestore.
     * The medication object MUST have a valid ID.
     *
     * @param medication The Medication object with updated data and its ID.
     * @param callback Callback to receive success or failure.
     */
    public void updateMedication(Medication medication, FirestoreCallback<Void> callback) {
        if (medication.getId() == null || medication.getId().isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Medication ID cannot be null or empty for update."));
            return;
        }
        medicationsRef.document(medication.getId()).set(medication) // Use set() to update (or create if ID doesn't exist)
                .addOnSuccessListener(aVoid -> {
                    System.out.println("Medication updated: " + medication.getId()); // For debugging
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    System.err.println("Error updating medication " + medication.getId() + ": " + e.getMessage()); // For debugging
                    callback.onFailure(e);
                });
    }

    /**
     * Deletes a medication from Firestore by its ID.
     *
     * @param medicationId The ID of the medication to delete.
     * @param callback Callback to receive success or failure.
     */
    public void deleteMedication(String medicationId, FirestoreCallback<Void> callback) {
        if (medicationId == null || medicationId.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Medication ID cannot be null or empty for deletion."));
            return;
        }
        medicationsRef.document(medicationId).delete()
                .addOnSuccessListener(aVoid -> {
                    System.out.println("Medication deleted: " + medicationId); // For debugging
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    System.err.println("Error deleting medication " + medicationId + ": " + e.getMessage()); // For debugging
                    callback.onFailure(e);
                });
    }

    /**
     * Fetches all medications a single time without setting up a continuous listener.
     * Useful for one-off operations like rescheduling alarms on boot.
     *
     * @param callback Callback to receive the list of Medication objects or an error.
     */
    public void fetchAllMedicationsOnce(FirestoreCallback<List<Medication>> callback) {
        medicationsRef.get() // Perform a one-time get operation
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Medication> allMedications = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Medication medication = document.toObject(Medication.class);
                        if (medication != null) {
                            medication.setId(document.getId()); // Ensure ID is set from Firestore document
                            allMedications.add(medication);
                        }
                    }
                    callback.onSuccess(allMedications);
                })
                .addOnFailureListener(callback::onFailure);
    }

    /**
     * Fetches all active medications a single time.
     * This is useful for getting the current set of active medications without a continuous listener.
     *
     * @param callback Callback to receive the list of active Medication objects or an error.
     */
    public void getAllActiveMedications(FirestoreCallback<List<Medication>> callback) {
        medicationsRef.whereEqualTo("active", true) // Query for documents where 'active' field is true
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Medication> activeMedications = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Medication medication = document.toObject(Medication.class);
                        if (medication != null) {
                            medication.setId(document.getId()); // Ensure ID is set
                            activeMedications.add(medication);
                        }
                        else {
                            System.err.println("Warning: Document " + document.getId() + " converted to null Medication object.");
                        }
                    }
                    System.out.println("Fetched " + activeMedications.size() + " active medications."); // For debugging
                    callback.onSuccess(activeMedications);
                })
                .addOnFailureListener(e -> {
                    System.err.println("Error fetching active medications: " + e.getMessage()); // For debugging
                    callback.onFailure(e);
                });
    }

    /**
     * NEW METHOD: Fetches all inactive medications for the current user once.
     * Orders them by endDate in descending order (most recent inactive first).
     *
     * @param callback A FirestoreCallback to handle success or failure.
     */
    public void fetchInactiveMedicationsOnce(FirestoreCallback<List<Medication>> callback) {
        medicationsRef.whereEqualTo("active", false) // Query for documents where 'active' field is false
                .orderBy("endDate", Query.Direction.DESCENDING) // Order by endDate, most recent first
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Medication> inactiveMedications = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Medication medication = document.toObject(Medication.class);
                        if (medication != null) {
                            medication.setId(document.getId()); // Ensure ID is set
                            inactiveMedications.add(medication);
                        }
                        else {
                            System.err.println("Warning: Document " + document.getId() + " converted to null inactive Medication object.");
                        }
                    }
                    System.out.println("Fetched " + inactiveMedications.size() + " inactive medications."); // For debugging
                    callback.onSuccess(inactiveMedications);
                })
                .addOnFailureListener(e -> {
                    System.err.println("Error fetching inactive medications: " + e.getMessage()); // For debugging
                    callback.onFailure(e);
                });
    }
}