package com.example.medialert.ui.medicalinfo;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.medialert.data.MedicalInfo;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MedicalInfoViewModel extends ViewModel {

    private static final String TAG = "MedicalInfoVM";
    private static final String COLLECTION_NAME = "medical_info";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final MutableLiveData<MedicalInfo> _medicalInfo = new MutableLiveData<>();
    public LiveData<MedicalInfo> getMedicalInfo() {
        return _medicalInfo;
    }

    private String currentUserId;

    public MedicalInfoViewModel() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            fetchMedicalInfo(currentUserId);
        } else {
            Log.e(TAG, "No authenticated user found. Cannot fetch medical info. Redirect to login should occur.");
            _medicalInfo.setValue(new MedicalInfo()); // Post an empty MedicalInfo for safety
        }
    }

    private DocumentReference getMedicalInfoDocumentRef(String userId) {
        // Medical info is stored as a document for each user directly under the 'medical_info' collection
        return db.collection(COLLECTION_NAME).document(userId);
    }

    public void fetchMedicalInfo(String userId) {
        getMedicalInfoDocumentRef(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        MedicalInfo info = documentSnapshot.toObject(MedicalInfo.class);
                        if (info != null) {
                            info.setUserId(userId); // Ensure userId is set, as it's part of the document ID
                            _medicalInfo.setValue(info);
                            Log.d(TAG, "Medical info fetched for user: " + userId);
                        } else {
                            Log.e(TAG, "Failed to parse medical info document from snapshot for user: " + userId);
                            _medicalInfo.setValue(new MedicalInfo(userId, null, null, null, null, null, null));
                        }
                    } else {
                        Log.d(TAG, "No medical info found for user " + userId + ". Initializing empty profile.");
                        // Initialize with empty lists for chips to avoid null pointer exceptions
                        _medicalInfo.setValue(new MedicalInfo(userId, null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null, null));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching medical info for user " + userId + ": " + e.getMessage());
                    _medicalInfo.setValue(new MedicalInfo(userId, null, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), null, null)); // Post empty on error
                });
    }

    public void saveMedicalInfo(MedicalInfo medicalInfo) {
        if (currentUserId == null || currentUserId.isEmpty()) {
            Log.e(TAG, "Cannot save medical info: User ID is null or empty. Re-authenticate or handle.");
            return;
        }
        medicalInfo.setUserId(currentUserId); // Ensure the userId is set on the object being saved

        getMedicalInfoDocumentRef(currentUserId).set(medicalInfo) // Use set() to create or overwrite the document
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Medical info saved successfully for user: " + currentUserId);
                    _medicalInfo.setValue(medicalInfo); // Update LiveData to reflect saved state in UI immediately
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving medical info: " + e.getMessage());
                    // Consider providing user feedback like a Toast/Snackbar here
                });
    }
}