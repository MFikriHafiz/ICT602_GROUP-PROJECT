package com.example.medialert;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText emailInput;
    private Button sendResetBtn;
    private ProgressBar progressBar;
    private TextView errorMsg, successMsg, loginLink;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.forgotpassword); // Your forgot password layout is named forgotpassword

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components using your XML IDs
        emailInput = findViewById(R.id.emailInput);
        sendResetBtn = findViewById(R.id.sendResetBtn);
        progressBar = findViewById(R.id.progressBar);
        errorMsg = findViewById(R.id.errorMsg);
        successMsg = findViewById(R.id.successMsg);
        loginLink = findViewById(R.id.loginLink);

        // Set up click listeners
        sendResetBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendPasswordResetEmail();
            }
        });

        loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Go back to the previous activity (Login Screen)
            }
        });
    }

    private void sendPasswordResetEmail() {
        String email = emailInput.getText().toString().trim();

        // Hide previous messages
        errorMsg.setVisibility(View.GONE);
        successMsg.setVisibility(View.GONE);

        if (TextUtils.isEmpty(email)) {
            errorMsg.setText("Email is required.");
            errorMsg.setVisibility(View.VISIBLE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE); // Show progress bar
        sendResetBtn.setEnabled(false); // Disable button

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE); // Hide progress bar
                    sendResetBtn.setEnabled(true); // Re-enable button

                    if (task.isSuccessful()) {
                        successMsg.setText("Password reset link sent to " + email);
                        successMsg.setVisibility(View.VISIBLE);
                        Toast.makeText(ForgotPasswordActivity.this, "Check your email for reset link.", Toast.LENGTH_LONG).show();
                        // Optionally, you might want to automatically go back to the login screen after a short delay
                        // new Handler(Looper.getMainLooper()).postDelayed(() -> finish(), 3000);
                    } else {
                        String errorMessage = task.getException().getMessage();
                        if (errorMessage != null) {
                            if (errorMessage.contains("no user record")) {
                                errorMsg.setText("No account found with this email.");
                            } else if (errorMessage.contains("badly formatted")) {
                                errorMsg.setText("Invalid email format.");
                            } else {
                                errorMsg.setText("Failed to send reset email: " + errorMessage);
                            }
                        } else {
                            errorMsg.setText("Failed to send reset email. Please try again.");
                        }
                        errorMsg.setVisibility(View.VISIBLE);
                    }
                });
    }
}