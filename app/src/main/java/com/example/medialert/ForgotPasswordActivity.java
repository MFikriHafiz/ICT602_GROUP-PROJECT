package com.example.medialert;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
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
        setContentView(R.layout.forgotpassword); // Make sure this layout exists

        mAuth = FirebaseAuth.getInstance();

        emailInput = findViewById(R.id.emailInput);
        sendResetBtn = findViewById(R.id.sendResetBtn);
        progressBar = findViewById(R.id.progressBar);
        errorMsg = findViewById(R.id.errorMsg);
        successMsg = findViewById(R.id.successMsg);
        loginLink = findViewById(R.id.loginLink);

        sendResetBtn.setOnClickListener(v -> sendPasswordResetEmail());

        loginLink.setOnClickListener(v -> finish()); // Go back to login
    }

    private void sendPasswordResetEmail() {
        String email = emailInput.getText().toString().trim();

        errorMsg.setVisibility(View.GONE);
        successMsg.setVisibility(View.GONE);

        if (TextUtils.isEmpty(email)) {
            errorMsg.setText("Please enter your email.");
            errorMsg.setVisibility(View.VISIBLE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        sendResetBtn.setEnabled(false);

        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            progressBar.setVisibility(View.GONE);
            sendResetBtn.setEnabled(true);

            if (task.isSuccessful()) {
                successMsg.setText("Password reset link sent to: " + email);
                successMsg.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Check your email to reset password.", Toast.LENGTH_LONG).show();
            } else {
                Exception e = task.getException();
                String error = (e != null) ? e.getMessage() : "Unknown error";

                if (error.contains("no user record")) {
                    errorMsg.setText("No account found with this email.");
                } else if (error.contains("badly formatted")) {
                    errorMsg.setText("Invalid email format.");
                } else {
                    errorMsg.setText("Failed to send reset email: " + error);
                }

                errorMsg.setVisibility(View.VISIBLE);
            }
        });
    }
}
