package com.example.medialert;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.medialert.adapters.MedicationHistoryAdapter;
import com.example.medialert.model.Medication;
import com.example.medialert.model.MedicationHistoryViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MedicationHistoryActivity extends AppCompatActivity implements
        MedicationHistoryAdapter.OnItemClickListener,
        MedicationHistoryAdapter.OnDeleteClickListener {

    private MedicationHistoryViewModel viewModel;
    private MedicationHistoryAdapter adapter;
    private RecyclerView recyclerView;
    private TextView textViewNoHistory;
    private ProgressBar progressBar;
    private EditText editTextSearch;

    private List<Medication> fullHistoryList = new ArrayList<>(); // Store the full list for filtering

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medication_history);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_history);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Show back button
            getSupportActionBar().setTitle(""); // Set title empty as we have a custom TextView
        }

        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerViewMedicationHistory);
        textViewNoHistory = findViewById(R.id.textViewNoHistory);
        progressBar = findViewById(R.id.progressBarHistory);
        editTextSearch = findViewById(R.id.editTextSearchHistory);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MedicationHistoryAdapter(this);
        adapter.setOnItemClickListener(this); // Set this activity as item click listener
        adapter.setOnDeleteClickListener(this); // Set this activity as delete click listener
        recyclerView.setAdapter(adapter);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(MedicationHistoryViewModel.class);

        // Observe LiveData from ViewModel
        viewModel.inactiveMedications.observe(this, medications -> {
            progressBar.setVisibility(View.GONE);
            if (medications != null && !medications.isEmpty()) {
                fullHistoryList = new ArrayList<>(medications); // Store full list
                adapter.setMedicationList(medications); // Display all initially
                textViewNoHistory.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            } else {
                adapter.setMedicationList(new ArrayList<>()); // Clear adapter
                textViewNoHistory.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });

        viewModel.errorMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                progressBar.setVisibility(View.GONE);
            }
        });

        viewModel.deleteSuccess.observe(this, isSuccess -> {
            if (isSuccess != null) {
                if (isSuccess) {
                    Toast.makeText(this, "Medication deleted from history.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to delete medication from history.", Toast.LENGTH_SHORT).show();
                }
                // Reset the delete success status to prevent re-triggering on rotation/config change
                viewModel.deleteSuccess.getValue();
            }
        });

        // Search functionality
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Not used */ }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { /* Not used */ }
        });

        // Initially fetch history
        progressBar.setVisibility(View.VISIBLE);
        viewModel.fetchInactiveMedications();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-fetch data every time the activity resumes to ensure fresh data
        // (e.g., if a medication was marked inactive from another screen)
        progressBar.setVisibility(View.VISIBLE);
        viewModel.fetchInactiveMedications();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed(); // Handle back button click
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // --- Adapter Click Listeners Implementation ---

    @Override
    public void onItemClick(Medication medication) {
        // TODO: Implement what happens when a history item is clicked.
        // For example, navigate to a detail view (read-only AddEditMedicationActivity or new DetailActivity)
        Toast.makeText(this, "Clicked: " + medication.getName() + " (ID: " + medication.getId() + ")", Toast.LENGTH_SHORT).show();
        // Example: Intent to MedicationDetailActivity (you'd need to create this)
        // Intent intent = new Intent(this, MedicationDetailActivity.class);
        // intent.putExtra("medication_id", medication.getId());
        // startActivity(intent);
    }

    @Override
    public void onDeleteClick(Medication medication) {
        // Show a confirmation dialog before deleting
        new AlertDialog.Builder(this)
                .setTitle("Delete Medication from History")
                .setMessage("Are you sure you want to permanently delete '" + medication.getName() + "' from your history? This action cannot be undone.")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        progressBar.setVisibility(View.VISIBLE); // Show progress during deletion
                        viewModel.deleteMedicationFromHistory(medication.getId());
                    }
                })
                .setNegativeButton("Cancel", null) // Do nothing on cancel
                .show();
    }

    // --- Filtering Logic ---
    private void filter(String text) {
        List<Medication> filteredList = new ArrayList<>();
        if (TextUtils.isEmpty(text)) {
            filteredList.addAll(fullHistoryList); // If search bar is empty, show full list
        } else {
            String lowerCaseText = text.toLowerCase(Locale.getDefault());
            for (Medication medication : fullHistoryList) {
                if (medication.getName().toLowerCase(Locale.getDefault()).contains(lowerCaseText) ||
                        medication.getInstructions().toLowerCase(Locale.getDefault()).contains(lowerCaseText) ||
                        medication.getFrequency().toLowerCase(Locale.getDefault()).contains(lowerCaseText) ||
                        medication.getDosageUnit().toLowerCase(Locale.getDefault()).contains(lowerCaseText) ||
                        String.valueOf(medication.getDosageQuantity()).contains(lowerCaseText)) { // Also search dosage quantity
                    filteredList.add(medication);
                }
            }
        }
        adapter.setMedicationList(filteredList);
        textViewNoHistory.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(filteredList.isEmpty() ? View.GONE : View.VISIBLE);
    }
}