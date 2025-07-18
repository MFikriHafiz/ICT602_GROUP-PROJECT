package com.example.medialert.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log; // Added for debugging logs
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medialert.AddEditMedicationActivity;
import com.example.medialert.R;
import com.example.medialert.adapters.MedicationAdapter;
import com.example.medialert.model.Medication;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.List; // Ensure List is imported

public class HomeFragment extends Fragment implements MedicationAdapter.OnMedicationActionsListener {

    private static final String TAG = "HomeFragment"; // Tag for logging

    private HomeViewModel homeViewModel;
    private FloatingActionButton fabAddMedication;
    private RecyclerView recyclerViewMedications;
    private MedicationAdapter medicationAdapter;

    private TextView textViewCurrentDateTime;
    private TextView textViewHints; // To display hints when medication list is empty

    private Handler handler;
    private Runnable updateTimeRunnable;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // Initialize ViewModel using ViewModelProvider
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize UI elements
        textViewCurrentDateTime = root.findViewById(R.id.textViewCurrentDateTime);
        textViewHints = root.findViewById(R.id.textViewHints); // Make sure this ID exists in your fragment_home.xml

        recyclerViewMedications = root.findViewById(R.id.recyclerViewMedications);
        recyclerViewMedications.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewMedications.setHasFixedSize(true); // Optimization for RecyclerView performance

        medicationAdapter = new MedicationAdapter();
        // Set 'this' fragment as the listener for medication actions (edit, delete, toggle)
        medicationAdapter.setOnMedicationActionsListener(this);
        recyclerViewMedications.setAdapter(medicationAdapter);

        // Observe the list of medications from the ViewModel
        homeViewModel.getMedications().observe(getViewLifecycleOwner(), medications -> {
            Log.d(TAG, "Medications LiveData updated. Count: " + (medications != null ? medications.size() : 0));
            // Update the adapter with the new list of medications
            medicationAdapter.setMedications(medications);

            // Update hints based on whether medications are present
            if (medications == null || medications.isEmpty()) {
                textViewHints.setText("No medications added yet. Tap '+' to add your first reminder!");
                textViewHints.setVisibility(View.VISIBLE);
            } else {
                textViewHints.setVisibility(View.GONE);
            }
        });

        // Observe delete success events from the ViewModel
        homeViewModel.getDeleteSuccess().observe(getViewLifecycleOwner(), success -> {
            if (success != null) {
                if (success) {
                    // Deletion success is primarily handled by the medication LiveData observer (it removes the item).
                    // We can log it or show a subtle feedback if needed, but Toast is already in AlertDialog.
                    Log.d(TAG, "Medication deletion reported as successful by ViewModel.");
                } else {
                    // Error message will be handled by the errorMessage observer
                    Log.e(TAG, "Medication deletion reported as failed by ViewModel.");
                }
            }
        });

        // Observe error messages from the ViewModel
        homeViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                Log.e(TAG, "ViewModel Error: " + errorMessage);
            }
        });

        // Set up Floating Action Button for adding new medications
        fabAddMedication = root.findViewById(R.id.fab_add_medication);
        fabAddMedication.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddEditMedicationActivity.class);
            startActivity(intent);
        });

        // Setup for swipe-to-delete functionality
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) { // Allow swipe left and right
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false; // We don't support drag & drop
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Medication swipedMedication = medicationAdapter.getMedicationAt(position);
                if (swipedMedication != null) {
                    // Show a confirmation dialog before actually deleting
                    showDeleteConfirmationDialog(swipedMedication, position);
                } else {
                    // Fallback: If medication is somehow null, restore the item and show an error
                    medicationAdapter.notifyItemChanged(position); // Restore the swiped item
                    Toast.makeText(getContext(), "Error: Medication not found for deletion.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "onSwiped: Attempted to delete null medication at position " + position);
                }
            }
        }).attachToRecyclerView(recyclerViewMedications); // Attach the ItemTouchHelper to the RecyclerView

        // Setup Handler and Runnable for updating current date/time
        handler = new Handler(Looper.getMainLooper());
        updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                updateCurrentDateTime();
                handler.postDelayed(this, 1000); // Update every second
            }
        };

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Start updating the time when the fragment is resumed
        if (handler != null && updateTimeRunnable != null) {
            handler.post(updateTimeRunnable);
            Log.d(TAG, "Time update runnable started.");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop updating the time when the fragment is paused
        if (handler != null && updateTimeRunnable != null) {
            handler.removeCallbacks(updateTimeRunnable);
            Log.d(TAG, "Time update runnable stopped.");
        }
    }

    /**
     * Updates the TextView with the current date and time formatted for Malaysia's timezone.
     */
    private void updateCurrentDateTime() {
        // Use a specific timezone for Malaysia
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMMM yyyy, hh:mm:ss a z", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kuala_Lumpur")); // Shah Alam, Selangor is in Asia/Kuala_Lumpur

        String formattedDateTime = sdf.format(new Date());
        textViewCurrentDateTime.setText(formattedDateTime);
    }

    // --- Implementation of MedicationAdapter.OnMedicationActionsListener ---

    @Override
    public void onItemClick(Medication medication) {
        // Handle general item click, e.g., to view full details or navigate to edit
        Log.d(TAG, "Medication item clicked: " + medication.getName());
        // Navigate to AddEditMedicationActivity for editing
        Intent intent = new Intent(getActivity(), AddEditMedicationActivity.class);
        // Pass the entire Medication object (must be Parcelable)
        intent.putExtra(AddEditMedicationActivity.EXTRA_MEDICATION, medication);
        startActivity(intent);
    }

    @Override
    public void onEditClick(Medication medication) {
        Log.d(TAG, "Edit icon clicked for: " + medication.getName());
        // Navigate to AddEditMedicationActivity for editing
        Intent intent = new Intent(getActivity(), AddEditMedicationActivity.class);
        // Pass the entire Medication object (must be Parcelable)
        intent.putExtra(AddEditMedicationActivity.EXTRA_MEDICATION, medication);
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Medication medication) {
        Log.d(TAG, "Delete icon clicked for: " + medication.getName());
        // Show the same delete confirmation dialog as swipe-to-delete
        showDeleteConfirmationDialog(medication, -1); // -1 indicates no specific position to restore for button click
    }

    @Override
    public void onToggleActive(Medication medication, boolean isActive) {
        Log.d(TAG, "Toggle active status for " + medication.getName() + " to: " + isActive);
        // Update the medication's active status in the database via ViewModel
        medication.setActive(isActive);
        homeViewModel.updateMedication(medication);
        String status = isActive ? "activated" : "deactivated";
        Toast.makeText(getContext(), medication.getName() + " " + status + ".", Toast.LENGTH_SHORT).show();
        // The LiveData observer for medications will automatically update the UI after database change
    }

    /**
     * Shows a confirmation dialog before deleting a medication.
     *
     * @param medicationToDelete The Medication object to be deleted.
     * @param adapterPosition    The adapter position of the item if deletion was triggered by swipe; -1 otherwise.
     */
    private void showDeleteConfirmationDialog(Medication medicationToDelete, int adapterPosition) {
        if (getContext() == null) {
            Log.e(TAG, "Context is null, cannot show delete dialog.");
            return;
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Delete Medication")
                .setMessage("Are you sure you want to delete '" + medicationToDelete.getName() + "'? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Call ViewModel to delete the medication from the database and cancel alarms
                    homeViewModel.deleteMedication(medicationToDelete.getId());
                    Toast.makeText(getContext(), "Medication '" + medicationToDelete.getName() + "' deleted!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "User confirmed deletion for: " + medicationToDelete.getName());
                    // The LiveData observer for medications will automatically remove the item from RecyclerView
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // User cancelled deletion
                    if (adapterPosition != -1) {
                        // If it was a swipe, restore the item's position in the RecyclerView
                        medicationAdapter.notifyItemChanged(adapterPosition);
                        Log.d(TAG, "Deletion cancelled by user. Item restored at position: " + adapterPosition);
                    } else {
                        Log.d(TAG, "Deletion cancelled by user.");
                    }
                    Toast.makeText(getContext(), "Deletion cancelled.", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false) // User must choose an option
                .show();
    }
}