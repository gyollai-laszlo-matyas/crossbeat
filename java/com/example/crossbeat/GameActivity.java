package com.example.crossbeat;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class GameActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        // setContentView(R.layout.activity_game);

        SharedPreferences sharedPreferences = getSharedPreferences("GameSettings", MODE_PRIVATE);
        float scrollSpeed = sharedPreferences.getFloat("scrollSpeed", 1.0f);
        int offset = sharedPreferences.getInt("offset", 0);
        int difficulty = sharedPreferences.getInt("difficulty", 0);
        GameView view = new GameView(this, "powerattack", difficulty, scrollSpeed, offset);
        setContentView(view);
        /*ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        }); */
    }
}