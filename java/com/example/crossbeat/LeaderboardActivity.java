package com.example.crossbeat;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.crossbeat.model.Score;
import com.example.crossbeat.model.User;
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

public class LeaderboardActivity extends AppCompatActivity {
    private FirebaseUser user;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference refUsers = db.collection("Usernames");
    private CollectionReference refScores = db.collection("Scores");
    TextView titleText;
    TextView scoreText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_leaderboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        user = FirebaseAuth.getInstance().getCurrentUser();
        if(user == null){
            finish();
        }

        titleText = findViewById(R.id.titleText);
        scoreText = findViewById(R.id.scoreText);
    }

    public void showScores0(View view){
        showScores(0, view);
    }

    public void showScores1(View view){
        showScores(1, view);
    }

    public void showScores2(View view){
        showScores(2, view);
    }

    private void showScores(double difficulty, View view){
        final String[] fullText = {""};
        refScores.whereEqualTo("difficulty", difficulty).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        fullText[0] = fullText[0] + document.getString("userID") + " - \n";
                    }
                }
            }
        });
        if(difficulty == 0){titleText.setText("Leaderboards - LIGHT");}
        else if(difficulty == 1){titleText.setText("Leaderboards - HYPER");}
        else{titleText.setText("Leaderboards - ULTRA");}
        scoreText.setText(fullText[0]);
    }

    private String findUsernameById(String id){
        final String[] username = {""};
        refUsers.whereEqualTo("userID", id).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        username[0] = document.getString("userName");
                    }
                }
            }
        });
        return username[0];
    }

    public void closeLeaderboard(View view){
        finish();
    }
}