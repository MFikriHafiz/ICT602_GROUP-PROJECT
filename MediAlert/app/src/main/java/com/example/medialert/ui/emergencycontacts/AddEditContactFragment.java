package com.example.medialert.ui.emergencycontacts;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts; // Corrected import
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.ContextCompat; // Added this import
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.medialert.R;
import com.example.medialert.data.EmergencyContact;

import de.hdodenhof.circleimageview.CircleImageView;

public class AddEditContactFragment extends Fragment {

    private static final String TAG = "AddEditContactFrag";

    private EmergencyContactsViewModel emergencyContactsViewModel;
    private NavController navController;

    private EmergencyContact currentContact = null;
    private Uri currentImageUri = null;

    // UI Elements
    private MaterialToolbar toolbar;
    private CircleImageView imageViewContactPicturePreview;
    private ImageButton buttonPickImage;
    private TextInputLayout textInputLayoutName;
    private TextInputEditText editTextName;
    private TextInputLayout textInputLayoutRelationship;
    private TextInputEditText editTextRelationship;
    private TextInputLayout textInputLayoutPhoneNumber;
    private TextInputEditText editTextPhoneNumber;
    private MaterialSwitch switchPrimaryContact;
    private MaterialButton buttonDelete;
    private MaterialButton buttonCancel;
    private MaterialButton buttonSave;

    // Activity Result Launchers for Camera/Gallery and Permissions
    private ActivityResultLauncher<String> requestCameraPermissionLauncher;
    private ActivityResultLauncher<String> requestGalleryPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        setupActivityResultLaunchers();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey("emergencyContact")) {
            currentContact = (EmergencyContact) getArguments().getSerializable("emergencyContact");
            if (currentContact != null && currentContact.getImageUrl() != null && !currentContact.getImageUrl().isEmpty()) {
                currentImageUri = Uri.parse(currentContact.getImageUrl());
            }
            Log.d(TAG, "Edit mode: Loaded contact ID " + (currentContact != null ? currentContact.getId() : "null"));
        } else {
            Log.d(TAG, "Add mode: No contact object provided.");
            currentContact = new EmergencyContact();
        }
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context contextThemeWrapper = new ContextThemeWrapper(getContext(), R.style.Theme_MediAlert);
        LayoutInflater localInflater = inflater.cloneInContext(contextThemeWrapper);

        View root = localInflater.inflate(R.layout.fragment_add_edit_contact, container, false);

        emergencyContactsViewModel = new ViewModelProvider(requireActivity()).get(EmergencyContactsViewModel.class);

        // Initialize UI elements
        toolbar = root.findViewById(R.id.toolbar);
        imageViewContactPicturePreview = root.findViewById(R.id.imageViewContactPicturePreview);
        buttonPickImage = root.findViewById(R.id.buttonPickImage);
        textInputLayoutName = root.findViewById(R.id.textInputLayoutName);
        editTextName = root.findViewById(R.id.editTextName);
        textInputLayoutRelationship = root.findViewById(R.id.textInputLayoutRelationship);
        editTextRelationship = root.findViewById(R.id.editTextRelationship);
        textInputLayoutPhoneNumber = root.findViewById(R.id.textInputLayoutPhoneNumber);
        editTextPhoneNumber = root.findViewById(R.id.editTextPhoneNumber);
        switchPrimaryContact = root.findViewById(R.id.switchPrimaryContact);
        buttonDelete = root.findViewById(R.id.buttonDelete);
        buttonCancel = root.findViewById(R.id.buttonCancel);
        buttonSave = root.findViewById(R.id.buttonSave);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(view);

        toolbar.setNavigationOnClickListener(v -> navController.navigateUp());

        populateUI();

        buttonPickImage.setOnClickListener(v -> showImagePickerDialog());
        buttonSave.setOnClickListener(v -> saveContact());
        buttonCancel.setOnClickListener(v -> navController.navigateUp());
        buttonDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    private void populateUI() {
        if (currentContact != null && currentContact.getId() != null) {
            // Edit mode
            toolbar.setTitle("Edit Contact");
            buttonDelete.setVisibility(View.VISIBLE);

            editTextName.setText(currentContact.getName());
            editTextRelationship.setText(currentContact.getRelationship());
            editTextPhoneNumber.setText(currentContact.getPhoneNumber());
            switchPrimaryContact.setChecked(currentContact.isPrimary());

            if (currentImageUri != null) {
                Glide.with(this).load(currentImageUri).into(imageViewContactPicturePreview);
            } else {
                imageViewContactPicturePreview.setImageResource(R.drawable.ic_default_profile);
            }
        } else {
            // Add mode
            toolbar.setTitle("Add New Contact");
            buttonDelete.setVisibility(View.GONE);
            imageViewContactPicturePreview.setImageResource(R.drawable.ic_default_profile);
        }
    }

    private void setupActivityResultLaunchers() {
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                currentImageUri = result.getData().getData();
                if (currentImageUri != null) {
                    Glide.with(this).load(currentImageUri).into(imageViewContactPicturePreview);
                }
            } else {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Image selection cancelled.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> { // Corrected line
            if (success) {
                if (currentImageUri != null) {
                    Glide.with(this).load(currentImageUri).into(imageViewContactPicturePreview);
                }
            } else {
                currentImageUri = null;
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to capture image or capture cancelled.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        requestCameraPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                launchCamera();
            } else {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Camera permission is required to take photos.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        requestGalleryPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                launchGallery();
            } else {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Storage permission is required to pick photos from gallery.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void showImagePickerDialog() {
        if (getContext() == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Choose Image Source");
        String[] options = {"Take Photo", "Choose from Gallery"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Take Photo
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        launchCamera();
                    } else {
                        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA);
                    }
                    break;
                case 1: // Choose from Gallery
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                            launchGallery();
                        } else {
                            requestGalleryPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
                        }
                    } else {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            launchGallery();
                        } else {
                            requestGalleryPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                        }
                    }
                    break;
            }
        });
        builder.show();
    }

    private void launchCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From MediAlert Camera");
        currentImageUri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (currentImageUri != null) {
            takePictureLauncher.launch(currentImageUri);
        } else {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Failed to create image file URI.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void saveContact() {
        String name = editTextName.getText().toString().trim();
        String relationship = editTextRelationship.getText().toString().trim();
        String phoneNumber = editTextPhoneNumber.getText().toString().trim();
        boolean isPrimary = switchPrimaryContact.isChecked();
        String imageUrl = (currentImageUri != null) ? currentImageUri.toString() : null;

        // Input validation
        if (TextUtils.isEmpty(name)) {
            textInputLayoutName.setError("Name is required.");
            return;
        } else {
            textInputLayoutName.setError(null);
        }
        if (TextUtils.isEmpty(phoneNumber)) {
            textInputLayoutPhoneNumber.setError("Phone Number is required.");
            return;
        } else {
            textInputLayoutPhoneNumber.setError(null);
        }

        if (currentContact == null) {
            currentContact = new EmergencyContact();
        }

        currentContact.setName(name);
        currentContact.setRelationship(relationship);
        currentContact.setPhoneNumber(phoneNumber);
        currentContact.setImageUrl(imageUrl);
        currentContact.setPrimary(isPrimary);

        if (currentContact.getId() == null) {
            emergencyContactsViewModel.addContact(currentContact);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Contact added!", Toast.LENGTH_SHORT).show();
            }
        } else {
            emergencyContactsViewModel.updateContact(currentContact);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Contact updated!", Toast.LENGTH_SHORT).show();
            }
        }
        navController.navigateUp();
    }

    private void showDeleteConfirmationDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Contact")
                .setMessage("Are you sure you want to delete " + editTextName.getText().toString() + "? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteContact())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteContact() {
        if (currentContact != null && currentContact.getId() != null) {
            emergencyContactsViewModel.deleteContact(currentContact.getId());
            if (getContext() != null) {
                Toast.makeText(getContext(), "Contact deleted!", Toast.LENGTH_SHORT).show();
            }
            navController.navigateUp();
        } else {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error: No contact selected for deletion.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}