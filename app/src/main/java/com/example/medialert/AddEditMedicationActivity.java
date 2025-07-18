package com.example.medialert;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface; // Import for AlertDialog.Builder
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem; // Import for onSupportNavigateUp
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.medialert.model.Medication;
import com.example.medialert.viewmodel.AddEditMedicationViewModel;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class AddEditMedicationActivity extends AppCompatActivity {

    public static final String EXTRA_MEDICATION = "com.example.medialert.EXTRA_MEDICATION";
    private static final String TAG = "AddEditMedicationAct";
    private static final TimeZone MALAYSIA_TIMEZONE = TimeZone.getTimeZone("Asia/Kuala_Lumpur");

    private EditText editTextMedicationName;
    private Spinner spinnerMedicationType;
    private Spinner spinnerDosageUnit;
    private EditText editTextDosageQuantity;
    private Spinner spinnerFrequency;
    private EditText editTextInstructions;
    private EditText editTextStartDate;
    private EditText editTextEndDate;
    private Button buttonSaveMedication;
    // NEW: Stop Medication Button
    private Button buttonStopMedication;
    private TextView addEditTitle;
    private SwitchCompat switchIsActive;
    private Button buttonAddAlarmTime;
    private LinearLayout linearLayoutAlarmTimesContainer;
    private TextView noAlarmTimesText;
    private ImageView imageViewMedicationPhoto;
    private Button buttonSelectPhoto;

    private List<Long> selectedAlarmTimes = new ArrayList<>();
    private Uri currentImageUri;
    private String currentPhotoPath;
    private Medication currentMedication;
    private AddEditMedicationViewModel addEditViewModel;
    private Calendar startCalendar;
    private Calendar endCalendar;

    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;
    private ActivityResultLauncher<String[]> requestPermissionsLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_medication);
        initializeViews();
        setupViewModels();
        setupSpinners();
        setupDatePickers();
        setupAlarmTimeUI();
        setupImageHandlers();
        setupSaveButton();
        // NEW: Setup Stop Medication Button
        setupStopMedicationButton();
        setupActionBar();
        loadMedicationData();
    }

    private void initializeViews() {
        addEditTitle = findViewById(R.id.addEditTitle);
        editTextMedicationName = findViewById(R.id.editTextMedicationName);
        spinnerMedicationType = findViewById(R.id.spinnerMedicationType);
        spinnerDosageUnit = findViewById(R.id.spinnerDosageUnit);
        editTextDosageQuantity = findViewById(R.id.editTextDosageQuantity);
        spinnerFrequency = findViewById(R.id.spinnerFrequency);
        editTextInstructions = findViewById(R.id.editTextInstructions);
        editTextStartDate = findViewById(R.id.editTextStartDate);
        editTextEndDate = findViewById(R.id.editTextEndDate);
        buttonSaveMedication = findViewById(R.id.buttonSaveMedication);
        // NEW: Initialize Stop Medication Button
        buttonStopMedication = findViewById(R.id.buttonStopMedication);
        switchIsActive = findViewById(R.id.switchIsActive);
        buttonAddAlarmTime = findViewById(R.id.buttonAddAlarmTime);
        linearLayoutAlarmTimesContainer = findViewById(R.id.linearLayoutAlarmTimesContainer);
        noAlarmTimesText = findViewById(R.id.noAlarmTimesText);
        imageViewMedicationPhoto = findViewById(R.id.imageViewMedicationPhoto);
        buttonSelectPhoto = findViewById(R.id.buttonSelectPhoto);

        startCalendar = Calendar.getInstance(MALAYSIA_TIMEZONE);
        endCalendar = Calendar.getInstance(MALAYSIA_TIMEZONE);
    }

    private void setupViewModels() {
        addEditViewModel = new ViewModelProvider(this).get(AddEditMedicationViewModel.class);

        addEditViewModel.getSaveSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, "Medication saved!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        addEditViewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, "Error: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupSpinners() {
        setupSpinner(spinnerMedicationType, R.array.medication_types);
        setupSpinner(spinnerDosageUnit, R.array.dosage_units);
        setupSpinner(spinnerFrequency, R.array.frequency);
    }

    private void setupSpinner(Spinner spinner, int arrayResId) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, arrayResId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void setupDatePickers() {
        editTextStartDate.setInputType(InputType.TYPE_NULL);
        editTextEndDate.setInputType(InputType.TYPE_NULL);

        View.OnClickListener datePickerListener = v -> {
            if (v == editTextStartDate) showDatePickerDialog(editTextStartDate, startCalendar);
            else showDatePickerDialog(editTextEndDate, endCalendar);
        };

        editTextStartDate.setOnClickListener(datePickerListener);
        editTextEndDate.setOnClickListener(datePickerListener);

        editTextStartDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showDatePickerDialog(editTextStartDate, startCalendar);
        });
        editTextEndDate.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showDatePickerDialog(editTextEndDate, endCalendar);
        });
    }

    private void setupAlarmTimeUI() {
        buttonAddAlarmTime.setOnClickListener(v -> showTimePickerDialog());
        updateAlarmTimesUI();
    }

    private void setupImageHandlers() {
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> handleImageCaptureResult(result));

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::handleSelectedImageUri);

        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                this::handlePermissionResult);

        buttonSelectPhoto.setOnClickListener(v -> requestImagePermissions());
    }

    private void setupSaveButton() {
        buttonSaveMedication.setOnClickListener(v -> saveMedication());
    }

    // NEW: Setup Stop Medication Button
    private void setupStopMedicationButton() {
        buttonStopMedication.setOnClickListener(v -> showStopMedicationConfirmationDialog());
    }

    private void setupActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
    }

    private void loadMedicationData() {
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_MEDICATION)) {
            currentMedication = intent.getParcelableExtra(EXTRA_MEDICATION);
            addEditTitle.setText("Edit Medication");
            if (currentMedication != null) {
                populateFields(currentMedication);
                // NEW: Show Stop Medication button only if medication is currently active
                if (currentMedication.isActive()) {
                    buttonStopMedication.setVisibility(View.VISIBLE);
                } else {
                    buttonStopMedication.setVisibility(View.GONE);
                }
            } else {
                Log.e(TAG, "Received null Medication object");
                Toast.makeText(this, "Error: Could not load medication details.", Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            addEditTitle.setText("Add New Medication");
            updateDateInView(editTextStartDate, startCalendar);
            switchIsActive.setChecked(true);
            // NEW: Hide Stop Medication button for new medications
            buttonStopMedication.setVisibility(View.GONE);
        }
    }

    private void showDatePickerDialog(EditText editText, Calendar calendar) {
        Calendar pickerCalendar = Calendar.getInstance(MALAYSIA_TIMEZONE);
        pickerCalendar.setTimeInMillis(calendar.getTimeInMillis());

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    updateDateInView(editText, calendar);
                },
                pickerCalendar.get(Calendar.YEAR),
                pickerCalendar.get(Calendar.MONTH),
                pickerCalendar.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private void updateDateInView(EditText editText, Calendar calendar) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
        sdf.setTimeZone(MALAYSIA_TIMEZONE);
        editText.setText(sdf.format(calendar.getTime()));
    }

    private void showTimePickerDialog() {
        Calendar currentTime = Calendar.getInstance(MALAYSIA_TIMEZONE);
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> addAlarmTime(selectedHour, selectedMinute),
                currentTime.get(Calendar.HOUR_OF_DAY),
                currentTime.get(Calendar.MINUTE),
                false);

        timePickerDialog.show();
    }

    private void addAlarmTime(int hour, int minute) {
        Calendar selectedTime = Calendar.getInstance(MALAYSIA_TIMEZONE);
        selectedTime.set(1970, Calendar.JANUARY, 1, hour, minute, 0);
        selectedTime.set(Calendar.MILLISECOND, 0);

        long timeInMillis = selectedTime.getTimeInMillis();

        if (selectedAlarmTimes.contains(timeInMillis)) {
            Toast.makeText(this, "This alarm time already exists", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedAlarmTimes.add(timeInMillis);
        updateAlarmTimesUI();
        Toast.makeText(this, "Alarm time added", Toast.LENGTH_SHORT).show();
    }

    private void updateAlarmTimesUI() {
        linearLayoutAlarmTimesContainer.removeAllViews();

        if (selectedAlarmTimes.isEmpty()) {
            noAlarmTimesText.setVisibility(View.VISIBLE);
            if (noAlarmTimesText.getParent() == null) {
                linearLayoutAlarmTimesContainer.addView(noAlarmTimesText);
            }
            return;
        }

        noAlarmTimesText.setVisibility(View.GONE);
        Collections.sort(selectedAlarmTimes);
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        sdf.setTimeZone(MALAYSIA_TIMEZONE);

        for (int i = 0; i < selectedAlarmTimes.size(); i++) {
            long alarmTime = selectedAlarmTimes.get(i);
            Calendar tempCalendar = Calendar.getInstance(MALAYSIA_TIMEZONE);
            tempCalendar.setTimeInMillis(alarmTime);

            LinearLayout alarmItem = createAlarmItemView(i, sdf.format(tempCalendar.getTime()), alarmTime);
            linearLayoutAlarmTimesContainer.addView(alarmItem);
        }
    }

    private LinearLayout createAlarmItemView(int index, String formattedTime, long alarmTime) {
        LinearLayout alarmItem = new LinearLayout(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        if (index > 0) {
            params.topMargin = (int) getResources().getDimension(R.dimen.default_margin_small);
        }

        alarmItem.setLayoutParams(params);
        alarmItem.setOrientation(LinearLayout.HORIZONTAL);
        alarmItem.setPadding(
                (int) getResources().getDimension(R.dimen.default_padding_small),
                (int) getResources().getDimension(R.dimen.default_padding_small),
                (int) getResources().getDimension(R.dimen.default_padding_small),
                (int) getResources().getDimension(R.dimen.default_padding_small));
        alarmItem.setBackgroundResource(R.drawable.rounded_edittext_no_border);

        TextView timeView = new TextView(this);
        timeView.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        timeView.setText(formattedTime);
        timeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        timeView.setTextColor(ContextCompat.getColor(this, R.color.black));
        timeView.setPadding(0, 0, (int) getResources().getDimension(R.dimen.default_padding_small), 0);

        Button removeButton = createRemoveButton(alarmTime);

        alarmItem.addView(timeView);
        alarmItem.addView(removeButton);
        return alarmItem;
    }

    private Button createRemoveButton(long alarmTime) {
        Button button = new Button(this);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                (int) getResources().getDimension(R.dimen.remove_button_width),
                (int) getResources().getDimension(R.dimen.remove_button_height)));
        button.setText("X");
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        button.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        button.setBackgroundResource(R.drawable.rounded_red_button);
        button.setTag(alarmTime);
        button.setOnClickListener(v -> removeAlarmTime(v.getTag()));
        return button;
    }

    private void removeAlarmTime(Object tag) {
        selectedAlarmTimes.remove(tag);
        updateAlarmTimesUI();
        Toast.makeText(this, "Alarm time removed", Toast.LENGTH_SHORT).show();
    }

    private void populateFields(Medication medication) {
        editTextMedicationName.setText(medication.getName());
        setSpinnerSelection(spinnerMedicationType, medication.getType(), R.array.medication_types);
        setSpinnerSelection(spinnerDosageUnit, medication.getDosageUnit(), R.array.dosage_units);
        editTextDosageQuantity.setText(String.valueOf(medication.getDosageQuantity()));
        setSpinnerSelection(spinnerFrequency, medication.getFrequency(), R.array.frequency);
        editTextInstructions.setText(medication.getInstructions());

        if (medication.getStartDate() > 0) {
            startCalendar.setTimeInMillis(medication.getStartDate());
            updateDateInView(editTextStartDate, startCalendar);
        } else {
            // This should ideally not happen for an existing medication, but defensively set to now
            startCalendar = Calendar.getInstance(MALAYSIA_TIMEZONE);
            updateDateInView(editTextStartDate, startCalendar);
        }

        if (medication.getEndDate() > 0) {
            endCalendar.setTimeInMillis(medication.getEndDate());
            updateDateInView(editTextEndDate, endCalendar);
        } else {
            editTextEndDate.setText("");
            // Reset endCalendar if no end date set
            endCalendar = Calendar.getInstance(MALAYSIA_TIMEZONE);
            endCalendar.clear(); // Clear all fields to ensure it's "empty"
        }

        switchIsActive.setChecked(medication.isActive());
        selectedAlarmTimes.clear();

        if (medication.getAlarmTimes() != null) {
            selectedAlarmTimes.addAll(medication.getAlarmTimes());
        }

        updateAlarmTimesUI();
        loadMedicationImage(medication.getImageUrl());
    }

    private void loadMedicationImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            currentImageUri = Uri.parse(imageUrl);
            Glide.with(getApplicationContext())
                    .load(currentImageUri)
                    .placeholder(R.drawable.ic_medication_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(imageViewMedicationPhoto);
            imageViewMedicationPhoto.setVisibility(View.VISIBLE);
        } else {
            imageViewMedicationPhoto.setVisibility(View.GONE);
            imageViewMedicationPhoto.setImageDrawable(null);
            currentImageUri = null;
        }
    }

    private void setSpinnerSelection(Spinner spinner, String value, int arrayResId) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, arrayResId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (value != null) {
            int position = adapter.getPosition(value);
            if (position >= 0) spinner.setSelection(position);
        }
    }

    private void saveMedication() {
        // ... (existing input validation) ...

        String name = editTextMedicationName.getText().toString().trim();
        String type = spinnerMedicationType.getSelectedItem().toString();
        String dosageQuantityStr = editTextDosageQuantity.getText().toString().trim();
        String dosageUnit = spinnerDosageUnit.getSelectedItem().toString();
        String frequency = spinnerFrequency.getSelectedItem().toString();
        String instructions = editTextInstructions.getText().toString().trim();
        long startDate = startCalendar.getTimeInMillis();
        // Ensure endDate is 0 if empty string, not just based on endCalendar
        long endDate = TextUtils.isEmpty(editTextEndDate.getText().toString().trim()) ? 0 : endCalendar.getTimeInMillis();
        boolean isActive = switchIsActive.isChecked();
        String imageUrl = currentImageUri != null ? currentImageUri.toString() : null;

        if (!validateInputs(name, dosageQuantityStr, startDate, endDate, isActive)) {
            return;
        }

        double dosageQuantity = Double.parseDouble(dosageQuantityStr);

        if (currentMedication == null) {
            // New medication
            currentMedication = new Medication(
                    name, dosageQuantity, dosageUnit, instructions,
                    selectedAlarmTimes, isActive, type, frequency,
                    startDate, endDate, imageUrl);
        } else {
            // Existing medication
            currentMedication.setName(name);
            currentMedication.setDosageQuantity(dosageQuantity);
            currentMedication.setDosageUnit(dosageUnit);
            currentMedication.setInstructions(instructions);
            currentMedication.setAlarmTimes(selectedAlarmTimes);
            currentMedication.setActive(isActive);
            currentMedication.setType(type);
            currentMedication.setFrequency(frequency);
            currentMedication.setStartDate(startDate);
            currentMedication.setEndDate(endDate);
            currentMedication.setImageUrl(imageUrl);
        }

        addEditViewModel.saveMedication(currentMedication);
    }

    private boolean validateInputs(String name, String dosageQuantityStr,
                                   long startDate, long endDate, boolean isActive) {

        // ... (existing validation logic) ...
        if (TextUtils.isEmpty(name)) {
            showError(editTextMedicationName, "Medication name required");
            return false;
        }

        if (TextUtils.isEmpty(dosageQuantityStr)) {
            showError(editTextDosageQuantity, "Dosage quantity required");
            return false;
        }

        try {
            double dosageQuantity = Double.parseDouble(dosageQuantityStr);
            if (dosageQuantity <= 0) {
                showError(editTextDosageQuantity, "Must be positive");
                return false;
            }
        } catch (NumberFormatException e) {
            showError(editTextDosageQuantity, "Invalid number");
            return false;
        }

        // Validate selected spinner items are not empty (if they can be)
        if (spinnerMedicationType.getSelectedItem() == null || spinnerMedicationType.getSelectedItem().toString().isEmpty()) {
            Toast.makeText(this, "Medication type required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (spinnerDosageUnit.getSelectedItem() == null || spinnerDosageUnit.getSelectedItem().toString().isEmpty()) {
            Toast.makeText(this, "Dosage unit required", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (spinnerFrequency.getSelectedItem() == null || spinnerFrequency.getSelectedItem().toString().isEmpty()) {
            Toast.makeText(this, "Frequency required", Toast.LENGTH_SHORT).show();
            return false;
        }


        if (TextUtils.isEmpty(editTextStartDate.getText().toString().trim())) {
            showError(editTextStartDate, "Start date required");
            return false;
        }

        // If end date is set, validate it
        if (endDate != 0 && endDate < startDate) {
            Toast.makeText(this, "End date cannot be before start date", Toast.LENGTH_LONG).show();
            return false;
        }

        // If medication is active, require alarm times.
        // If it's being made inactive, alarm times are not strictly required.
        if (isActive && selectedAlarmTimes.isEmpty()) {
            Toast.makeText(this, "Add at least one alarm time for active medication", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void showError(EditText field, String message) {
        field.setError(message);
        field.requestFocus();
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // --- NEW: Stop Medication Logic ---
    private void showStopMedicationConfirmationDialog() {
        if (currentMedication == null || !currentMedication.isActive()) {
            Toast.makeText(this, "Medication is already inactive.", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Stop Medication")
                .setMessage("Are you sure you want to stop taking " + currentMedication.getName() + "? It will be marked as inactive and moved to your history.")
                .setPositiveButton("Stop", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        stopMedication();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void stopMedication() {
        if (currentMedication != null) {
            // Set medication to inactive
            currentMedication.setActive(false);
            // Set end date to current time
            currentMedication.setEndDate(System.currentTimeMillis());

            // Save the updated medication
            addEditViewModel.saveMedication(currentMedication);
            Toast.makeText(this, currentMedication.getName() + " has been stopped and moved to history.", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity
        }
    }
    // --- END NEW: Stop Medication Logic ---

    private void requestImagePermissions() {
        List<String> permissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addPermissionIfNeeded(permissions, Manifest.permission.CAMERA);
            addPermissionIfNeeded(permissions, Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            addPermissionIfNeeded(permissions, Manifest.permission.CAMERA);
            addPermissionIfNeeded(permissions, Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (!permissions.isEmpty()) {
            requestPermissionsLauncher.launch(permissions.toArray(new String[0]));
        } else {
            showImageSourceDialog();
        }
    }

    private void addPermissionIfNeeded(List<String> permissions, String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(permission);
        }
    }

    private void handlePermissionResult(Map<String, Boolean> permissions) {
        boolean allGranted = !permissions.values().contains(false);
        if (allGranted) {
            showImageSourceDialog();
        } else {
            Toast.makeText(this, "Required permissions denied", Toast.LENGTH_LONG).show();
        }
    }

    private void showImageSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Select Image Source")
                .setItems(new CharSequence[]{"Take Photo", "Choose from Gallery"}, (dialog, which) -> {
                    if (which == 0) takePhoto();
                    else selectFromGallery();
                })
                .create()
                .show();
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                File photoFile = createImageFile();
                currentImageUri = FileProvider.getUriForFile(
                        this,
                        getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentImageUri);
                takePictureLauncher.launch(currentImageUri);
            } catch (IOException ex) {
                Log.e(TAG, "Error creating image file", ex);
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No camera app found", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs();
        }

        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void selectFromGallery() {
        pickImageLauncher.launch("image/*");
    }

    private void handleImageCaptureResult(boolean success) {
        if (success) {
            handleSelectedImageUri(currentImageUri);
        } else {
            Log.d(TAG, "Image capture cancelled");
            currentImageUri = null;
            currentPhotoPath = null;
            Toast.makeText(this, "Image capture cancelled", Toast.LENGTH_SHORT).show();
            imageViewMedicationPhoto.setVisibility(View.GONE);
            imageViewMedicationPhoto.setImageDrawable(null);
        }
    }

    private void handleSelectedImageUri(Uri uri) {
        if (uri == null) {
            handleNoImageSelected();
            return;
        }

        currentImageUri = uri;
        takePersistableUriPermission(uri);
        loadImageWithGlide(uri);
    }

    private void takePersistableUriPermission(Uri uri) {
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try {
                getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException | UnsupportedOperationException e) {
                Log.w(TAG, "Could not get persistable URI permission", e);
                Toast.makeText(this, "Image permission may be temporary", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadImageWithGlide(Uri uri) {
        try {
            Glide.with(getApplicationContext())
                    .load(uri)
                    .placeholder(R.drawable.ic_medication_placeholder)
                    .error(R.drawable.ic_image_error)
                    .into(imageViewMedicationPhoto);
            imageViewMedicationPhoto.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
            handleImageLoadError();
        }
    }

    private void handleNoImageSelected() {
        Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
        imageViewMedicationPhoto.setVisibility(View.GONE);
        imageViewMedicationPhoto.setImageDrawable(null);
        currentImageUri = null;
    }

    private void handleImageLoadError() {
        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
        imageViewMedicationPhoto.setVisibility(View.GONE);
        imageViewMedicationPhoto.setImageDrawable(null);
        currentImageUri = null;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}