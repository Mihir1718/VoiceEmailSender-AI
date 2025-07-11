package com.example.voicemailsender;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.gmail.GmailScopes;
import com.google.firebase.auth.*;
import com.google.android.gms.common.api.Scope;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class GoogleLoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123;
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;
    private Button googleSignInBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_login);

        googleSignInBtn = findViewById(R.id.googleSignInBtn);
        firebaseAuth = FirebaseAuth.getInstance();

        // Auto-redirect if already signed in
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        // ✅ Configure sign-in with Gmail API scope


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken("399502980503-2e0mfpe4kgf13danb7i75cd78d2c2ia9.apps.googleusercontent.com")
                .requestScopes(new Scope("https://www.googleapis.com/auth/gmail.send"))
                .requestScopes(new Scope(GmailScopes.GMAIL_MODIFY))
                .build();



        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInBtn.setOnClickListener(view -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = firebaseAuth.getCurrentUser();

                if (user != null) {
                    // Store user info in Firestore
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("uid", user.getUid());
                    userData.put("name", user.getDisplayName());
                    userData.put("email", user.getEmail());
                    userData.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");

                    FirebaseFirestore.getInstance().collection("users")
                            .document(user.getUid())
                            .set(userData)
                            .addOnSuccessListener(aVoid -> Log.d("Firestore", "User saved"))
                            .addOnFailureListener(e -> Log.e("Firestore", "Error saving user", e));
                }

                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(GoogleLoginActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
