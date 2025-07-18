package com.example.medialert.ui.auth; // Adjust package name

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.medialert.R; // Ensure this is correct
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText currentPasswordEditText;
    private EditText newPasswordEditText;
    private EditText confirmNewPasswordEditText;
    private Button changePasswordButton;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password); // You'll need to create this layout

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "You need to be logged in to change password.", Toast.LENGTH_SHORT).show();
            finish(); // Close this activity
            return;
        }

        // Initialize views (ensure these IDs exist in activity_change_password.xml)
        currentPasswordEditText = findViewById(R.id.editTextCurrentPassword);
        newPasswordEditText = findViewById(R.id.editTextNewPassword);
        confirmNewPasswordEditText = findViewById(R.id.editTextConfirmNewPassword);
        changePasswordButton = findViewById(R.id.buttonChangePassword);


        changePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeUserPassword();
            }
        });
    }

    private void changeUserPassword() {
        String currentPassword = currentPasswordEditText.getText().toString().trim();
        String newPassword = newPasswordEditText.getText().toString().trim();
        String confirmNewPassword = confirmNewPasswordEditText.getText().toString().trim();

        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
            Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "New password must be at least 6 characters long.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmNewPassword)) {
            Toast.makeText(this, "New passwords do not match.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Re-authenticate user with their current password first
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);

        currentUser.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // User re-authenticated, now update the password
                        currentUser.updatePassword(newPassword)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(ChangePasswordActivity.this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                                        finish(); // Close activity after successful change
                                    } else {
                                        Toast.makeText(ChangePasswordActivity.this, "Failed to update password: " + updateTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        Toast.makeText(ChangePasswordActivity.this, "Authentication failed: Incorrect current password.", Toast.LENGTH_LONG).show();
                    }
                });
    }
}