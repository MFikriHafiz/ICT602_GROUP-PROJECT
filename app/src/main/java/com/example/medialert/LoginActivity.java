package com.example.medialert; // Ensure this matches your project's root package

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

// Import your HomeActivity
import com.example.medialert.HomeActivity; // Assuming HomeActivity is directly under com.example.medialert
// If HomeActivity is in a sub-package, e.g., ui.home, then it would be:
// import com.example.medialert.ui.home.HomeActivity;

// Import your ForgotPasswordActivity and SignupActivity
import com.example.medialert.ForgotPasswordActivity; // Adjust path if needed
import com.example.medialert.SignupActivity;       // Adjust path if needed


public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private ImageView showPasswordBtn;
    private TextView errorMsg;
    private Button loginBtn;
    private ProgressBar progressBar;
    private TextView forgotPasswordLink, signupLink;
    private FirebaseAuth mAuth;

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components using your XML IDs
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        showPasswordBtn = findViewById(R.id.showPasswordBtn);
        errorMsg = findViewById(R.id.errorMsg);
        loginBtn = findViewById(R.id.loginBtn);
        progressBar = findViewById(R.id.progressBar);
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink);
        signupLink = findViewById(R.id.signupLink);

        // Optional: Check if user is already logged in
        // If you want to skip login screen when user is already logged in:
        // if (mAuth.getCurrentUser() != null) {
        //     Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        //     startActivity(intent);
        //     finish();
        //     return; // Important: finish() and return to prevent further setup if already logged in
        // }


        // Set up click listeners
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        showPasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePasswordVisibility(passwordInput, showPasswordBtn);
            }
        });

        forgotPasswordLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Forgot Password Screen
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });

        signupLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Sign Up Screen
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });
    }

    private void togglePasswordVisibility(EditText editText, ImageView toggleButton) {
        if (isPasswordVisible) {
            // Hide password
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleButton.setImageResource(R.drawable.ic_visibility); // Assuming this is your "show" icon
        } else {
            // Show password
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleButton.setImageResource(R.drawable.ic_visibility_off); // Assuming this is your "hide" icon
        }
        editText.setSelection(editText.getText().length()); // Keep cursor at the end
        isPasswordVisible = !isPasswordVisible;
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Hide previous error message
        errorMsg.setVisibility(View.GONE);

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

        progressBar.setVisibility(View.VISIBLE); // Show progress bar
        loginBtn.setEnabled(false); // Disable button during login

        // Firebase login
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE); // Hide progress bar
                    loginBtn.setEnabled(true); // Re-enable button

                    if (task.isSuccessful()) {
                        // Sign in success
                        Toast.makeText(LoginActivity.this, "Login successful.", Toast.LENGTH_SHORT).show();
                        // Navigate to Home Activity
                        Intent intent = new Intent(LoginActivity.this, HomeActivity.class); // Changed to HomeActivity
                        startActivity(intent);
                        finish(); // Close LoginActivity
                    } else {
                        // If sign in fails, display a message to the user.
                        String errorMessage = task.getException().getMessage();
                        if (errorMessage != null) {
                            if (errorMessage.contains("no user record")) {
                                errorMsg.setText("No account found with this email. Please sign up.");
                            } else if (errorMessage.contains("badly formatted")) {
                                errorMsg.setText("Invalid email format.");
                            } else if (errorMessage.contains("wrong password")) {
                                errorMsg.setText("Incorrect password.");
                            } else {
                                errorMsg.setText("Authentication failed: " + errorMessage);
                            }
                        } else {
                            errorMsg.setText("Authentication failed. Please try again.");
                        }
                        errorMsg.setVisibility(View.VISIBLE);
                    }
                });
    }
}