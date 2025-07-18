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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import com.example.medialert.HomeActivity;
import com.example.medialert.ui.auth.ChangePasswordActivity;
import com.example.medialert.LoginActivity;
import com.example.medialert.MedicationHistoryActivity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private FragmentProfileBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private ActivityResultLauncher<String[]> pickImageFromGalleryLauncher;
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private Uri cameraOutputUri;
    private Uri currentProfileImageUri;
    private User userProfileData;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        pickImageFromGalleryLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        try {
                            requireContext().getContentResolver().takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            );
                            handleSelectedImageUri(uri);
                        } catch (SecurityException e) {
                            Log.e(TAG, "Failed to persist permission for gallery image", e);
                            Toast.makeText(getContext(), "Cannot access image. Try another one.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "No image selected.", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (success) {
                handleSelectedImageUri(cameraOutputUri);
            } else {
                Toast.makeText(getContext(), "Failed to capture image.", Toast.LENGTH_SHORT).show();
                cameraOutputUri = null;
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

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
        if (currentUser == null) return;

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

                        if (userProfileData.getProfileImageUriString() != null && !userProfileData.getProfileImageUriString().isEmpty()) {
                            try {
                                Uri savedUri = Uri.parse(userProfileData.getProfileImageUriString());
                                binding.imageViewProfile.setImageURI(savedUri);
                                currentProfileImageUri = savedUri;
                            } catch (Exception e) {
                                Log.e(TAG, "Invalid saved image URI", e);
                                Toast.makeText(getContext(), "Could not load saved image.", Toast.LENGTH_LONG).show();
                                binding.imageViewProfile.setImageResource(R.drawable.ic_default_profile);
                                userProfileData.setProfileImageUriString(null);
                            }
                        } else {
                            binding.imageViewProfile.setImageResource(R.drawable.ic_default_profile);
                        }
                    } else {
                        userProfileData = new User(currentUser.getUid(), "", "", currentUser.getEmail(), null);
                        binding.editTextEmail.setText(userProfileData.getEmail());
                    }
                } else {
                    userProfileData = new User(currentUser.getUid(), "", "", currentUser.getEmail(), null);
                    binding.editTextEmail.setText(userProfileData.getEmail());
                }
            } else {
                userProfileData = new User(currentUser.getUid(), "", "", currentUser.getEmail(), null);
                binding.editTextEmail.setText(userProfileData.getEmail());
            }
        });
    }

    private void setupListeners() {
        binding.buttonPickImage.setOnClickListener(v -> showImagePickerDialog());
        binding.buttonChangePassword.setOnClickListener(v -> startActivity(new Intent(getActivity(), ChangePasswordActivity.class)));
        binding.buttonSaveProfile.setOnClickListener(v -> saveUserProfileData());
        binding.buttonLogout.setOnClickListener(v -> showLogoutConfirmationDialog());
    }

    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Choose Image Source");
        builder.setItems(new CharSequence[]{"Take Photo", "Choose from Gallery", "Cancel"}, (dialog, which) -> {
            switch (which) {
                case 0:
                    dispatchTakePictureIntent();
                    break;
                case 1:
                    pickImageFromGalleryLauncher.launch(new String[]{"image/*"});
                    break;
                default:
                    dialog.dismiss();
                    break;
            }
        });
        builder.show();
    }

    private void dispatchTakePictureIntent() {
        File photoFile;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Log.e(TAG, "Error creating image file", ex);
            Toast.makeText(getContext(), "Camera error.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            cameraOutputUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    photoFile
            );
            takePictureLauncher.launch(cameraOutputUri);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(null);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private boolean isFromFileProvider(Uri uri) {
        String authority = uri.getAuthority();
        return authority != null && authority.equals(requireContext().getPackageName() + ".fileprovider");
    }

    private void handleSelectedImageUri(Uri uri) {
        if (uri != null) {
            try {
                currentProfileImageUri = uri;
                binding.imageViewProfile.setImageURI(uri);
                saveUserProfileData();
            } catch (SecurityException e) {
                Log.e(TAG, "No access to image: " + uri, e);
                Toast.makeText(getContext(), "Cannot access selected image.", Toast.LENGTH_LONG).show();
                currentProfileImageUri = null;
                binding.imageViewProfile.setImageResource(R.drawable.ic_default_profile);
            }
        } else {
            binding.imageViewProfile.setImageResource(R.drawable.ic_default_profile);
            currentProfileImageUri = null;
        }
    }

    private void saveUserProfileData() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "Not logged in.", Toast.LENGTH_SHORT).show();
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
            binding.layoutPhone.setError("Enter valid phone number");
            return;
        } else {
            binding.layoutPhone.setError(null);
        }

        if (userProfileData == null) {
            userProfileData = new User(currentUser.getUid(), name, phone, email,
                    currentProfileImageUri != null ? currentProfileImageUri.toString() : null);
        } else {
            userProfileData.setName(name);
            userProfileData.setPhone(phone);
            userProfileData.setProfileImageUriString(currentProfileImageUri != null ? currentProfileImageUri.toString() : null);
        }

        db.collection("users").document(currentUser.getUid())
                .set(userProfileData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile saved successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error saving profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
        Toast.makeText(getContext(), "Logged out.", Toast.LENGTH_LONG).show();
        navigateToLoginActivity();
    }

    private void navigateToLoginActivity() {
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
