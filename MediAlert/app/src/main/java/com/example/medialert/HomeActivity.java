package com.example.medialert;

import android.app.AlarmManager; // ✅ NEW
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings; // ✅ NEW
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog; // ✅ NEW
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.medialert.ui.home.HomeViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private NavController navController;
    private HomeViewModel homeViewModel;

    private static final int REQUEST_NOTIFICATION_PERMISSION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        BottomNavigationView navView = findViewById(R.id.nav_view);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home_fragment, R.id.navigation_map_fragment,
                R.id.emergencyContactsFragment, R.id.navigation_user_profile_fragment)
                .build();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_home);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(navView, navController);
        } else {
            System.err.println("Error: NavHostFragment with ID R.id.nav_host_fragment_activity_home not found!");
            Toast.makeText(this, "Application error: Navigation setup failed.", Toast.LENGTH_LONG).show();
            finish();
        }

        requestNotificationPermission();     // ✅ Notification for Android 13+
        requestExactAlarmPermission();       // ✅ NEW: Exact alarm for Android 12+

        homeViewModel.getDeleteSuccess().observe(this, success -> {
            if (success) {
                Toast.makeText(HomeActivity.this, "Medication deleted successfully!", Toast.LENGTH_SHORT).show();
            }
        });

        homeViewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(HomeActivity.this, "Error: " + message, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (navController != null) {
            return navController.navigateUp() || super.onSupportNavigateUp();
        }
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        if (navController != null && !navController.popBackStack()) {
            super.onBackPressed();
        } else if (navController == null) {
            super.onBackPressed();
        }
    }

    // ✅ Request POST_NOTIFICATIONS permission for Android 13+
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    // ✅ NEW: Request SCHEDULE_EXACT_ALARM permission (Android 12+)
    private void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                // Show explanation dialog before opening settings
                new AlertDialog.Builder(this)
                        .setTitle("Allow Exact Alarm")
                        .setMessage("To ensure your medication reminders are on time, please allow exact alarms.")
                        .setPositiveButton("Allow", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied. You may not receive reminders.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
