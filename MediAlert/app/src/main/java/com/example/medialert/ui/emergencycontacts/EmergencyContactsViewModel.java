package com.example.medialert.ui.emergencycontacts;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.medialert.data.EmergencyContact;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class EmergencyContactsViewModel extends ViewModel {

    private static final String TAG = "EmergencyContactsVM";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private CollectionReference contactsCollection;

    private final MutableLiveData<List<EmergencyContact>> _emergencyContacts = new MutableLiveData<>();
    public LiveData<List<EmergencyContact>> getEmergencyContacts() {
        return _emergencyContacts;
    }

    private ListenerRegistration firestoreListenerRegistration;

    public EmergencyContactsViewModel() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Authenticated user ID: " + currentUser.getUid());
            contactsCollection = db.collection("users").document(currentUser.getUid()).collection("emergency_contacts");
            listenForContactsChanges();
        } else {
            Log.e(TAG, "No authenticated user found. Emergency contacts will be empty.");
            _emergencyContacts.setValue(new ArrayList<>()); // Set empty list if no user
        }
    }

    private void listenForContactsChanges() {
        if (contactsCollection == null) {
            Log.e(TAG, "Contacts collection is not initialized. Cannot listen for changes.");
            return;
        }

        Query query = contactsCollection.orderBy("name", Query.Direction.ASCENDING);

        firestoreListenerRegistration = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.w(TAG, "Emergency contacts listen failed.", e);
                _emergencyContacts.setValue(new ArrayList<>()); // Set empty list on error
                return;
            }

            List<EmergencyContact> contacts = new ArrayList<>();
            if (snapshots != null) {
                for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots) {
                    EmergencyContact contact = doc.toObject(EmergencyContact.class);
                    if (contact != null) {
                        contact.setId(doc.getId()); // Set the Firestore document ID to the contact object
                        contacts.add(contact);
                    }
                }
            }
            Log.d(TAG, "Emergency contacts fetched from Firestore. Count: " + contacts.size());
            _emergencyContacts.setValue(contacts);
        });
    }

    public void addContact(EmergencyContact contact) {
        if (contactsCollection == null) {
            Log.e(TAG, "Contacts collection not initialized. Cannot add contact.");
            return;
        }

        // If the new contact is primary, unmark existing primary contacts first
        if (contact.isPrimary()) {
            contactsCollection.whereEqualTo("primary", true)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (com.google.firebase.firestore.QueryDocumentSnapshot document : task.getResult()) {
                                // Ensure we don't unmark the contact if it's the one being updated (shouldn't happen on add though)
                                if (!document.getId().equals(contact.getId())) {
                                    contactsCollection.document(document.getId())
                                            .update("primary", false)
                                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Previous primary contact un-marked for add operation."))
                                            .addOnFailureListener(e -> Log.e(TAG, "Error un-marking previous primary on add: " + e.getMessage()));
                                }
                            }
                        }
                        addNewContactToFirestore(contact);
                    });
        } else {
            addNewContactToFirestore(contact);
        }
    }

    private void addNewContactToFirestore(EmergencyContact contact) {
        contactsCollection.add(contact)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Emergency contact added with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding emergency contact", e);
                });
    }

    public void updateContact(EmergencyContact updatedContact) {
        if (contactsCollection == null) {
            Log.e(TAG, "Contacts collection not initialized. Cannot update contact.");
            return;
        }
        if (updatedContact.getId() == null || updatedContact.getId().isEmpty()) {
            Log.e(TAG, "Cannot update contact: ID is null or empty.");
            return;
        }

        // If the updated contact is marked primary, unmark any other primary contacts
        if (updatedContact.isPrimary()) {
            contactsCollection.whereEqualTo("primary", true)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (com.google.firebase.firestore.QueryDocumentSnapshot document : task.getResult()) {
                                // Only unmark if it's a different document than the one being updated
                                if (!document.getId().equals(updatedContact.getId())) {
                                    contactsCollection.document(document.getId())
                                            .update("primary", false)
                                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Previous primary contact un-marked during update."))
                                            .addOnFailureListener(e -> Log.e(TAG, "Error un-marking previous primary on update: " + e.getMessage()));
                                }
                            }
                        }
                        updateContactInFirestore(updatedContact);
                    });
        } else {
            updateContactInFirestore(updatedContact);
        }
    }

    private void updateContactInFirestore(EmergencyContact updatedContact) {
        contactsCollection.document(updatedContact.getId())
                .set(updatedContact) // Use set() to overwrite or create if ID exists
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Emergency contact updated successfully: " + updatedContact.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating emergency contact: " + updatedContact.getId(), e);
                });
    }

    public void deleteContact(String contactId) {
        if (contactsCollection == null) {
            Log.e(TAG, "Contacts collection not initialized. Cannot delete contact.");
            return;
        }
        if (contactId == null || contactId.isEmpty()) {
            Log.e(TAG, "Cannot delete contact: ID is null or empty.");
            return;
        }

        contactsCollection.document(contactId).delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Emergency contact deleted successfully: " + contactId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting emergency contact: " + contactId, e);
                });
    }

    // Removed getContactById(String id) method from ViewModel,
    // as we now pass the full EmergencyContact object via Bundle.

    @Override
    protected void onCleared() {
        super.onCleared();
        if (firestoreListenerRegistration != null) {
            firestoreListenerRegistration.remove();
            Log.d(TAG, "Firestore listener removed for EmergencyContactsViewModel.");
        }
    }
}