package com.example.medialert;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignupActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput, confirmPasswordInput;
    private ImageView showPasswordBtn, showConfirmPasswordBtn;
    private TextView errorMsg;
    private Button signupBtn;
    private ProgressBar progressBar;
    private TextView loginLink;
    private FirebaseAuth mAuth;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // Your signup layout is named activity_register

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components using your XML IDs
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        showPasswordBtn = findViewById(R.id.showPasswordBtn);
        showConfirmPasswordBtn = findViewById(R.id.showConfirmPasswordBtn);
        errorMsg = findViewById(R.id.errorMsg);
        signupBtn = findViewById(R.id.signupBtn);
        progressBar = findViewById(R.id.progressBar);
        loginLink = findViewById(R.id.loginLink);

        // Set up click listeners
        signupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        showPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility(passwordInput, showPasswordBtn, true);
            }
        });

        showConfirmPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility(confirmPasswordInput, showConfirmPasswordBtn, false);
            }
        });

        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to Login Screen
                finish(); // Simply finish this activity to go back to Login
            }
        });
    }

    private void togglePasswordVisibility(EditText editText, ImageView toggleButton, boolean isMainPassword) {
        if ((isMainPassword && isPasswordVisible) || (!isMainPassword && isConfirmPasswordVisible)) {
            // Hide password
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleButton.setImageResource(R.drawable.ic_visibility); // Set to "show" icon
        } else {
            // Show password
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleButton.setImageResource(R.drawable.ic_visibility_off); // Set to "hide" icon
        }
        editText.setSelection(editText.getText().length()); // Keep cursor at the end
        if (isMainPassword) {
            isPasswordVisible = !isPasswordVisible;
        } else {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
        }
    }

    private void registerUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Hide previous error message
        errorMsg.setVisibility(View.GONE);

        // Basic validation
        if (TextUtils.isEmpty(email)) {
            errorMsg.setText("Email is required.");
            errorMsg.setVisibility(View.VISIBLE);
            return;
        }
        if (TextUtils.isEmpty(password)) {
            errorMsg.setText("Password is required.");
            errorMsg.setVisibility(View.VISIBLE);
            return;
        }
        if (password.length() < 6) {
            errorMsg.setText("Password must be at least 6 characters.");
            errorMsg.setVisibility(View.VISIBLE);
            return;
        }
        if (TextUtils.isEmpty(confirmPassword)) {
            errorMsg.setText("Confirm password is required.");
            errorMsg.setVisibility(View.VISIBLE);
            return;
        }
        if (!password.equals(confirmPassword)) {
            errorMsg.setText("Passwords do not match.");
            errorMsg.setVisibility(View.VISIBLE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE); // Show progress bar
        signupBtn.setEnabled(false); // Disable button during registration

        // Firebase registration
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE); // Hide progress bar
                    signupBtn.setEnabled(true); // Re-enable button

                    if (task.isSuccessful()) {
                        // Sign in success
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(SignupActivity.this, "Registration successful.",
                                Toast.LENGTH_SHORT).show();
                        // Optionally, send email verification
                        // if (user != null) {
                        //     user.sendEmailVerification();
                        // }

                        // Navigate to Home Activity
                        // Navigate to LoginActivity after successful registration
                        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish(); // Close SignupActivity

                    } else {
                        // If sign in fails, display a message to the user.
                        String errorMessage = task.getException().getMessage();
                        if (errorMessage != null) {
                            if (errorMessage.contains("email address is already in use")) {
                                errorMsg.setText("This email is already registered. Try logging in.");
                            } else if (errorMessage.contains("badly formatted")) {
                                errorMsg.setText("Invalid email format.");
                            } else {
                                errorMsg.setText("Registration failed: " + errorMessage);
                            }
                        } else {
                            errorMsg.setText("Registration failed. Please try again.");
                        }
                        errorMsg.setVisibility(View.VISIBLE);
                    }
                });
    }
}