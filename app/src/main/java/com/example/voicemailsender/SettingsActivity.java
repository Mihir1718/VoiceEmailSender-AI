package com.example.voicemailsender;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.gms.auth.api.signin.*;
import com.google.firebase.auth.*;

public class SettingsActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNav;
    private Switch switchNotifications, switchTheme;
    private LinearLayout btnVisitWebsite, btnContactSupport, btnManagePermissions, logoutBtn;
    private TextView nameText, emailText;
    private ImageView profileImage;

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // ✅ Apply Dark Mode Early (Prevents Blinking)
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                darkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // -------- Toolbar and Drawer Setup --------
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.app_name3));

        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
            } else if (id == R.id.nav_app) {
                startActivity(new Intent(this, AboutAppActivity.class));
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setSelectedItemId(R.id.nav_settings); // ✅ Highlight this tab

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_settings) {
                return true; // Already here
            } else if (itemId == R.id.nav_home) {
                startActivity(new Intent(SettingsActivity.this, MainActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_voice) {
                startActivity(new Intent(SettingsActivity.this, VoiceActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });


        // -------- Views --------
        switchNotifications = findViewById(R.id.switchNotifications);
        switchTheme = findViewById(R.id.switchTheme);
        btnVisitWebsite = findViewById(R.id.btnVisitWebsite);
        btnContactSupport = findViewById(R.id.btnContactSupport);
        btnManagePermissions = findViewById(R.id.btnManagePermissions);
        logoutBtn = findViewById(R.id.logoutBtn);
        nameText = findViewById(R.id.nameText);
        emailText = findViewById(R.id.emailText);
        profileImage = findViewById(R.id.profileImage);

        // -------- Dark Mode Toggle --------
        switchTheme.setChecked(darkMode);
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = getSharedPreferences("settings", MODE_PRIVATE).edit();
            editor.putBoolean("dark_mode", isChecked);
            editor.apply();

            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            recreate(); // Redraw UI with new theme
        });

        // -------- Notifications Switch --------
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this,
                    isChecked ? "Notifications Enabled" : "Notifications Disabled",
                    Toast.LENGTH_SHORT).show();
        });

        // -------- Visit Website --------
        btnVisitWebsite.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://deep-2510.github.io/A-W-devloper/"));
            startActivity(intent);
        });

        // -------- Contact Support --------
        btnContactSupport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:vbes2025@gmail.com"));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Support Request");
            intent.putExtra(Intent.EXTRA_TEXT, "Hello, I need help with...");
            try {
                intent.setPackage("com.google.android.gm"); // Open in Gmail
                startActivity(intent);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "Gmail app not found", Toast.LENGTH_SHORT).show();
            }
        });

        // -------- Manage Permissions --------
        btnManagePermissions.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });

        // -------- Firebase Auth + Google Profile --------
        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            nameText.setText(user.getDisplayName());
            emailText.setText(user.getEmail());
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .placeholder(R.drawable.profile_placeholder)
                    .circleCrop()
                    .into(profileImage);
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // -------- Logout --------
        logoutBtn.setOnClickListener(v -> {
            auth.signOut();
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, GoogleLoginActivity.class));
                finish();
            });
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
