package com.example.medialert.ui.profile;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;

import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.medialert.R;
import com.example.medialert.databinding.FragmentProfileBinding;
import com.example.medialert.data.User;

// Firebase imports
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

// IMPORTANT: Ensure these imports match your actual activity paths in your project
import com.example.medialert.HomeActivity;
// import com.example.medialert.ForgotPasswordActivity; // Consider if this is truly navigated from profile
// import com.example.medialert.SignupActivity;         // Consider if this is truly navigated from profile
import com.example.medialert.ui.auth.ChangePasswordActivity;
import com.example.medialert.LoginActivity;
// NEW IMPORT: For MedicationHistoryActivity
import com.example.medialert.MedicationHistoryActivity;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private FragmentProfileBinding binding;

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // Launchers for image selection
    private ActivityResultLauncher<String> pickImageFromGalleryLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private Uri cameraOutputUri; // URI where the camera will save the photo

    private User userProfileData;
    private Uri currentProfileImageUri; // To store the selected image URI (both from gallery and camera)

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // Initialize launcher for picking image from gallery
        pickImageFromGalleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                handleSelectedImageUri(uri);
            } else {
                Toast.makeText(getContext(), "No image selected.", Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize launcher for taking picture with camera
        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success) {
                // Image was saved to cameraOutputUri (the URI we provided)
                handleSelectedImageUri(cameraOutputUri);
            } else {
                Toast.makeText(getContext(), "Failed to capture image or image not saved.", Toast.LENGTH_SHORT).show();
                // Optionally, if the user cancels, you might want to clear cameraOutputUri or revert something
                cameraOutputUri = null; // Clear if capture failed/canceled
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(getContext(), "You are not logged in.", Toast.LENGTH_SHORT).show();
            navigateToLoginActivity();
            return root;
        }

        setupToolbar();
        loadUserProfileData();
        setupListeners();

        return root;
    }

    private void setupToolbar() {
        binding.toolbar.setTitle("My Profile");
    }

    private void loadUserProfileData() {
        if (currentUser == null) {
            return;
        }

        DocumentReference userDocRef = db.collection("users").document(currentUser.getUid());

        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    userProfileData = document.toObject(User.class);
                    if (userProfileData != null) {
                        binding.editTextName.setText(userProfileData.getName());
                        binding.editTextPhone.setText(userProfileData.getPhone());
                        binding.editTextEmail.setText(userProfileData.getEmail());

                        // Load profile image if URI string exists and is valid
                        if (userProfileData.getProfileImageUriString() != null && !userProfileData.getProfileImageUriString().isEmpty()) {
                            try {
                                Uri savedUri = Uri.parse(userProfileData.getProfileImageUriString());
                                // Attempt to set the image URI. If permissions were taken, this should work.
                                binding.imageViewProfile.setImageURI(savedUri);
                                currentProfileImageUri = savedUri; // Keep track of the loaded URI
                            } catch (Exception e) {
                                Log.e(TAG, "Error loading saved profile image URI: " + userProfileData.getProfileImageUriString(), e);
                                Toast.makeText(getContext(), "Could not load saved profile image. It might have been moved or deleted.", Toast.LENGTH_LONG).show();
                                binding.imageViewProfile.setImageResource(R.drawable.ic_default_profile); // Fallback
                                userProfileData.setProfileImageUriString(null); // Clear invalid URI in local object
                                // Optionally, save the null URI back to Firestore to clean up bad data.
                                // For simplicity, we are not adding this auto-cleanup in this example to avoid potential loops.
                            }
                        } else {
                            binding.imageViewProfile.setImageResource(R.drawable.ic_default_profile);
                        }
                    } else {
                        Log.e(TAG, "Error: User object is null after mapping from Firestore.");
                        Toast.makeText(getContext(), "Error loading profile data.", Toast.LENGTH_SHORT).show();
                        userProfileData = new User(currentUser.getUid(), "", "", currentUser.getEmail(), null);
                        binding.editTextEmail.setText(userProfileData.getEmail());
                    }
                } else {
                    Log.d(TAG, "No user profile document found for UID: " + currentUser.getUid());
                    Toast.makeText(getContext(), "Creating new profile...", Toast.LENGTH_SHORT).show();
                    userProfileData = new User(currentUser.getUid(), "", "", currentUser.getEmail(), null);
                    binding.editTextEmail.setText(userProfileData.getEmail());
                }
            } else {
                Log.e(TAG, "Error getting user profile document: ", task.getException());
                Toast.makeText(getContext(), "Failed to load profile data.", Toast.LENGTH_SHORT).show();
                userProfileData = new User(currentUser.getUid(), "", "", currentUser.getEmail(), null);
                binding.editTextEmail.setText(userProfileData.getEmail());
            }
        });
    }

    private void setupListeners() {
        binding.buttonPickImage.setOnClickListener(v -> {
            showImagePickerDialog(); // Call the new dialog method
        });

        binding.buttonChangePassword.setOnClickListener(v -> {
            // Navigate to ChangePasswordActivity
            Intent intent = new Intent(getActivity(), ChangePasswordActivity.class);
            startActivity(intent);
        });

        binding.buttonSaveProfile.setOnClickListener(v -> {
            saveUserProfileData();
        });

        // NEW Listener for Medication History
        binding.buttonMedicationHistory.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MedicationHistoryActivity.class);
            startActivity(intent);
        });

        binding.buttonLogout.setOnClickListener(v -> {
            showLogoutConfirmationDialog();
        });
    }

    /**
     * Displays an AlertDialog allowing the user to choose between taking a photo or
     * selecting one from the gallery.
     */
    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Choose Image Source");
        builder.setItems(new CharSequence[]{"Take Photo", "Choose from Gallery", "Cancel"},
                (dialog, which) -> {
                    switch (which) {
                        case 0: // Take Photo
                            dispatchTakePictureIntent();
                            break;
                        case 1: // Choose from Gallery
                            pickImageFromGalleryLauncher.launch("image/*");
                            break;
                        case 2: // Cancel
                            dialog.dismiss();
                            break;
                    }
                });
        builder.show();
    }

    /**
     * Prepares and launches the camera app to take a picture.
     * The picture will be saved to a temporary file managed by FileProvider.
     */
    private void dispatchTakePictureIntent() {
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Log.e(TAG, "Error creating image file for camera.", ex);
            Toast.makeText(getContext(), "Error: Could not prepare for photo capture.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            // Get a content:// URI for the file using FileProvider
            cameraOutputUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider", // IMPORTANT: This must match the authority in AndroidManifest.xml
                    photoFile
            );
            takePictureLauncher.launch(cameraOutputUri);
        }
    }

    /**
     * Creates a temporary image file in the app's private external storage directory.
     * @return The File object for the newly created temporary image file.
     * @throws IOException If an error occurs while creating the file.
     */
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        // Use getExternalFilesDir(null) for app-specific external storage that doesn't require WRITE_EXTERNAL_STORAGE permission on Android 10+
        File storageDir = requireContext().getExternalFilesDir(null);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return image;
    }

    /**
     * Handles the URI of the selected or captured image.
     * Takes persistable URI permissions and updates the UI and Firestore.
     * @param uri The URI of the image to handle.
     */
    private void handleSelectedImageUri(Uri uri) {
        if (uri != null) {
            try {
                // FLAG_GRANT_WRITE_URI_PERMISSION might be necessary for some older camera apps to write to the URI
                final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

                // It's good practice to ensure the URI scheme is 'content' before trying to persist permissions
                // as file:// URIs (common on older Android) don't support persistable permissions.
                // Modern camera apps and gallery pickers return content:// Uris.
                if ("content".equals(uri.getScheme())) {
                    requireContext().getContentResolver().takePersistableUriPermission(uri, takeFlags);
                }

                currentProfileImageUri = uri; // Update the class member
                binding.imageViewProfile.setImageURI(uri);
                Toast.makeText(getContext(), "Profile image updated!", Toast.LENGTH_SHORT).show();
                saveUserProfileData(); // Immediately save the new image URI to Firestore
            } catch (SecurityException e) {
                Log.e(TAG, "Failed to take persistable URI permission for " + uri, e);
                Toast.makeText(getContext(), "Error: Could not get persistent permission for image. Try again or pick a different image.", Toast.LENGTH_LONG).show();
                // Revert to default image and clear URI if permissions failed
                binding.imageViewProfile.setImageResource(R.drawable.ic_default_profile);
                currentProfileImageUri = null;
            }
        } else {
            Toast.makeText(getContext(), "No image selected.", Toast.LENGTH_SHORT).show();
            binding.imageViewProfile.setImageResource(R.drawable.ic_default_profile); // Fallback
            currentProfileImageUri = null;
        }
    }


    private void saveUserProfileData() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Not logged in to save profile.", Toast.LENGTH_SHORT).show();
            navigateToLoginActivity();
            return;
        }

        String name = binding.editTextName.getText().toString().trim();
        String phone = binding.editTextPhone.getText().toString().trim();
        String email = binding.editTextEmail.getText().toString().trim();

        if (name.isEmpty()) {
            binding.layoutName.setError("Name cannot be empty");
            return;
        } else {
            binding.layoutName.setError(null);
        }

        if (phone.isEmpty() || !Patterns.PHONE.matcher(phone).matches()) {
            binding.layoutPhone.setError("Please enter a valid phone number");
            return;
        } else {
            binding.layoutPhone.setError(null);
        }

        if (userProfileData == null) {
            userProfileData = new User(currentUser.getUid(), name, phone, email, currentProfileImageUri != null ? currentProfileImageUri.toString() : null);
        } else {
            userProfileData.setName(name);
            userProfileData.setPhone(phone);
            userProfileData.setProfileImageUriString(currentProfileImageUri != null ? currentProfileImageUri.toString() : null);
        }

        db.collection("users").document(currentUser.getUid())
                .set(userProfileData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile saved successfully!", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "DocumentSnapshot successfully written!");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error saving profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error writing document", e);
                });
    }

    private void showLogoutConfirmationDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void performLogout() {
        mAuth.signOut();
        Toast.makeText(getContext(), "You have been logged out.", Toast.LENGTH_LONG).show();
        navigateToLoginActivity();
    }

    private void navigateToLoginActivity() {
        // Ensure this points to your specific LoginActivity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}