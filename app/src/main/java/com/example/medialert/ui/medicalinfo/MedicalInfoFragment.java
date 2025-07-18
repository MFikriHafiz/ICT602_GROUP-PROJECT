package com.example.medialert.ui.medicalinfo;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.medialert.R;
import com.example.medialert.data.MedicalInfo;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MedicalInfoFragment extends Fragment {

    private MedicalInfoViewModel medicalInfoViewModel;

    private CircleImageView imageViewUserProfile;
    private TextView textViewUserName;

    private AutoCompleteTextView actBloodType;
    private ChipGroup chipGroupConditions;
    private TextInputLayout tilAddCondition;
    private TextInputEditText editTextAddCondition;
    private ChipGroup chipGroupAllergies;
    private TextInputLayout tilAddAllergy;
    private TextInputEditText editTextAddAllergy;
    private ChipGroup chipGroupMedications;
    private TextInputLayout tilAddMedication;
    private TextInputEditText editTextAddMedication;
    private TextInputEditText editTextEmergencyNotes;
    private MaterialSwitch switchOrganDonor;
    private MaterialButton buttonSaveMedicalInfo;

    // Data for dropdown and chips
    private final String[] BLOOD_TYPES = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Apply Material 3 theme
        Context contextThemeWrapper = new ContextThemeWrapper(getContext(), R.style.Theme_MediAlert_EmergencyContacts);
        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

        View root = localInflater.inflate(R.layout.fragment_medical_info, container, false);

        medicalInfoViewModel = new ViewModelProvider(this).get(MedicalInfoViewModel.class);

        // Initialize Profile Views
        imageViewUserProfile = root.findViewById(R.id.imageViewUserProfile);
        textViewUserName = root.findViewById(R.id.textViewUserName);

        // Initialize Medical Info Views
        actBloodType = root.findViewById(R.id.act_blood_type);
        chipGroupConditions = root.findViewById(R.id.chip_group_conditions);
        tilAddCondition = root.findViewById(R.id.til_add_condition);
        editTextAddCondition = root.findViewById(R.id.edit_text_add_condition);
        chipGroupAllergies = root.findViewById(R.id.chip_group_allergies);
        tilAddAllergy = root.findViewById(R.id.til_add_allergy);
        editTextAddAllergy = root.findViewById(R.id.edit_text_add_allergy);
        chipGroupMedications = root.findViewById(R.id.chip_group_medications);
        tilAddMedication = root.findViewById(R.id.til_add_medication);
        editTextAddMedication = root.findViewById(R.id.edit_text_add_medication);
        editTextEmergencyNotes = root.findViewById(R.id.edit_text_emergency_notes);
        switchOrganDonor = root.findViewById(R.id.switch_organ_donor);
        buttonSaveMedicalInfo = root.findViewById(R.id.button_save_medical_info);

        // --- Populate User Profile Info ---
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userName = currentUser.getDisplayName();
            if (userName != null && !userName.isEmpty()) {
                textViewUserName.setText(userName);
            } else {
                textViewUserName.setText("My Medical Profile"); // Default if no display name
            }

            Uri photoUrl = currentUser.getPhotoUrl();
            if (photoUrl != null) {
                Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_default_profile)
                        .error(R.drawable.ic_default_profile)
                        .into(imageViewUserProfile);
            } else {
                imageViewUserProfile.setImageResource(R.drawable.ic_default_profile);
            }
        } else {
            textViewUserName.setText("My Medical Profile");
            imageViewUserProfile.setImageResource(R.drawable.ic_default_profile);
        }
        // --- End Populate User Profile Info ---

        // Setup Blood Type Dropdown
        ArrayAdapter<String> bloodTypeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, BLOOD_TYPES);
        actBloodType.setAdapter(bloodTypeAdapter);

        // Observe MedicalInfo LiveData
        medicalInfoViewModel.getMedicalInfo().observe(getViewLifecycleOwner(), medicalInfo -> {
            if (medicalInfo != null) {
                actBloodType.setText(medicalInfo.getBloodType(), false); // false to not trigger selection listener

                populateChipGroup(chipGroupConditions, medicalInfo.getMedicalConditions());
                populateChipGroup(chipGroupAllergies, medicalInfo.getAllergies());
                populateChipGroup(chipGroupMedications, medicalInfo.getMedications());

                editTextEmergencyNotes.setText(medicalInfo.getEmergencyNotes());
                if (medicalInfo.getOrganDonor() != null) {
                    switchOrganDonor.setChecked(medicalInfo.getOrganDonor());
                } else {
                    switchOrganDonor.setChecked(false); // Default to false if not set
                }
            }
        });

        // Setup Add Chip functionality
        setupAddChipInput(tilAddCondition, editTextAddCondition, chipGroupConditions);
        setupAddChipInput(tilAddAllergy, editTextAddAllergy, chipGroupAllergies);
        setupAddChipInput(tilAddMedication, editTextAddMedication, chipGroupMedications);

        // Save button click listener
        buttonSaveMedicalInfo.setOnClickListener(v -> saveMedicalInfo());

        return root;
    }

    private void setupAddChipInput(TextInputLayout textInputLayout, TextInputEditText editText, ChipGroup chipGroup) {
        // Set end icon click listener to add chip
        textInputLayout.setEndIconOnClickListener(v -> addChip(editText, chipGroup));

        // Set EditorActionListener to add chip on 'Done' or 'Next' keyboard action
        editText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                addChip(editText, chipGroup);
                return true;
            }
            return false;
        });
    }

    private void addChip(TextInputEditText editText, ChipGroup chipGroup) {
        String text = editText.getText().toString().trim();
        if (!TextUtils.isEmpty(text)) {
            Chip chip = new Chip(requireContext());
            chip.setText(text);
            chip.setCloseIconVisible(true);
            chip.setCheckable(false); // Chips added programmatically usually aren't checkable
            chip.setClickable(false); // Chips added programmatically usually aren't clickable

            // Set a listener to remove the chip when its close icon is clicked
            chip.setOnCloseIconClickListener(v -> chipGroup.removeView(chip));

            chipGroup.addView(chip); // Add the chip to the ChipGroup
            editText.setText(""); // Clear the input field
        }
    }

    // Helper method to populate a ChipGroup from a List of strings
    private void populateChipGroup(ChipGroup chipGroup, List<String> items) {
        chipGroup.removeAllViews(); // Clear existing chips first
        if (items != null) {
            for (String item : items) {
                Chip chip = new Chip(requireContext());
                chip.setText(item);
                chip.setCloseIconVisible(true);
                chip.setCheckable(false);
                chip.setClickable(false);
                chip.setOnCloseIconClickListener(v -> chipGroup.removeView(chip));
                chipGroup.addView(chip);
            }
        }
    }

    // Helper method to get all text values from a ChipGroup
    private List<String> getChipGroupItems(ChipGroup chipGroup) {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            items.add(chip.getText().toString());
        }
        return items;
    }

    private void saveMedicalInfo() {
        String bloodType = actBloodType.getText().toString().trim();
        List<String> conditions = getChipGroupItems(chipGroupConditions);
        List<String> allergies = getChipGroupItems(chipGroupAllergies);
        List<String> medications = getChipGroupItems(chipGroupMedications);
        String emergencyNotes = editTextEmergencyNotes.getText().toString().trim();
        boolean organDonor = switchOrganDonor.isChecked();

        // Get the current user ID. This should ideally already be set in ViewModel.
        // If ViewModel.getMedicalInfo() returns a value, it should have the userId.
        // Otherwise, get it from FirebaseAuth.
        String userId = medicalInfoViewModel.getMedicalInfo().getValue() != null ?
                medicalInfoViewModel.getMedicalInfo().getValue().getUserId() :
                FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null || userId.isEmpty()) {
            Toast.makeText(getContext(), "User not logged in. Cannot save info.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new MedicalInfo object with updated values
        MedicalInfo medicalInfo = new MedicalInfo(
                userId,
                bloodType.isEmpty() ? null : bloodType,
                conditions.isEmpty() ? null : conditions,
                allergies.isEmpty() ? null : allergies,
                medications.isEmpty() ? null : medications,
                emergencyNotes.isEmpty() ? null : emergencyNotes,
                organDonor
        );

        // Call the ViewModel to save the data
        medicalInfoViewModel.saveMedicalInfo(medicalInfo);
        Toast.makeText(getContext(), "Medical information saved!", Toast.LENGTH_SHORT).show();
    }
}