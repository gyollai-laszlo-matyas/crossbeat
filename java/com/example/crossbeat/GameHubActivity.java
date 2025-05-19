package com.example.crossbeat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class GameHubActivity extends AppCompatActivity {
    private static final String PREF_KEY = GameHubActivity.class.getPackage().toString();
    private static final int SECRET_KEY = 616;
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference refUsers = db.collection("Usernames");
    TextView usernameText;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_game_hub);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user == null){
            finish();
        }

        usernameText = findViewById(R.id.usernameText);

        refUsers = db.collection("Usernames");
        refUsers.whereEqualTo("userID", user.getUid()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        usernameText.setText("Welcome, " + document.getString("userName"));
                    }
                }
            }
        });

        sharedPreferences = getSharedPreferences("GameSettings", MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void start0(View view){
        editor.putInt("difficulty", 0);
        editor.apply();
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }

    public void start1(View view){
        editor.putInt("difficulty", 1);
        editor.apply();
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }

    public void start2(View view){
        editor.putInt("difficulty", 2);
        editor.apply();
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }

    public void logout(View view) {
        // FirebaseAuth.getInstance().getCurrentUser().delete();
        finish();
    }

    public void viewLeaderboards(View view){
        Intent intent = new Intent(this, LeaderboardActivity.class);
        startActivity(intent);
    }

    public void updateScore(){
        refUsers = db.collection("Scores");
        refUsers.whereEqualTo("userID", user.getUid()).whereEqualTo("difficulty", 2).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if(!task.getResult().isEmpty()){
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            document.getReference().update("score", 900000);
                        }
                    }else{
                        Map<String, Object> newUser = new HashMap<>();
                        newUser.put("userID", user.getUid());
                        newUser.put("score", 800000);
                        newUser.put("difficulty", 2);

                        refUsers.add(newUser);
                    }
                }
            }
        });
    }
}