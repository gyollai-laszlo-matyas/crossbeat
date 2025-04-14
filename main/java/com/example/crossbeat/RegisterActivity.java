package com.example.crossbeat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {
    private static final String PREF_KEY = RegisterActivity.class.getPackage().toString();
    private static final int RC_SIGN_IN = 123;
    private static final int SECRET_KEY = 616;

    private FirebaseAuth fbAuth = FirebaseAuth.getInstance();

    EditText usernameET;
    EditText emailET;
    EditText passwordET;
    EditText passwordConfirmET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        usernameET = findViewById(R.id.usernameET);
        emailET = findViewById(R.id.emailET);
        passwordET = findViewById(R.id.passwordET);
        passwordConfirmET = findViewById(R.id.passwordConfirmET);

        int secret_key = getIntent().getIntExtra("SECRET_KEY", 0);
        if (secret_key != 616) {
            finish();
        }
    }

    public void register(View view){
        String username = usernameET.getText().toString();
        String email = emailET.getText().toString();
        String password = passwordET.getText().toString();
        String passwordConfirm = passwordConfirmET.getText().toString();

        if(!password.equals(passwordConfirm)){
            Toast.makeText(RegisterActivity.this, "Passwords do not match.", Toast.LENGTH_LONG).show();
            return;
        }

        if(username.isEmpty() || email.isEmpty() || password.isEmpty()){
            return;
        }

        fbAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if(task.isSuccessful()){
                Intent intent = new Intent(this, GameHubActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(RegisterActivity.this, "ERROR: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public void cancel(View view) {
        finish();
    }
}