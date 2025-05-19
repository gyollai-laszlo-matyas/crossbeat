package com.example.crossbeat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity_backup extends AppCompatActivity {
    private static final String PREF_KEY = MainActivity_backup.class.getPackage().toString();
    private static final int RC_SIGN_IN = 123;
    private static final int SECRET_KEY = 616;
    TextView welcomeText;
    TextView speedText;
    SeekBar speedSB;
    TextView offsetText;
    SeekBar offsetSB;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        welcomeText = findViewById(R.id.welcomeText);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.float_up);
        welcomeText.startAnimation(animation);

        speedText = findViewById(R.id.speedText);
        speedSB = findViewById(R.id.speedSeekBar);
        offsetText = findViewById(R.id.offsetText);
        offsetSB = findViewById(R.id.offsetSeekBar);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // set default game settings
        sharedPreferences = getSharedPreferences("GameSettings", MODE_PRIVATE);
        editor = sharedPreferences.edit();
        if(!sharedPreferences.contains("scrollSpeed")){
            editor.putFloat("scrollSpeed", 1.0f);
            editor.apply();
            editor.putInt("offset", 0);
            editor.apply();
        }

        speedSB.setMax(516);
        speedSB.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener()
                {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                                                  boolean fromUser)
                    {
                        speedText.setText("Note Speed | x" + String.format("%.2f", (1.0f + progress / 100.0f)));
                    }
                }
        );
        speedSB.setProgress((int)(sharedPreferences.getFloat("scrollSpeed", 1.0f) * 100.0f) - 100);
        speedText.setText("Note Speed | x" + String.format("%.2f", (1.0f + speedSB.getProgress() / 100.0f)));
        offsetSB.setMax(1000);
        offsetSB.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener()
                {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {}

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress,
                                                  boolean fromUser)
                    {
                        offsetText.setText("Audio Offset | " + (progress - 500) + "ms (increase if hitting early)");
                    }
                }
        );
        offsetSB.setProgress(sharedPreferences.getInt("offset", 0) + 500);
        offsetText.setText("Audio Offset | " + (offsetSB.getProgress() - 500) + "ms (increase if hitting early)");
    }

    public void gotologin(View view) {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
    }

    public void gotoregister(View view) {
        Intent intent = new Intent(this, RegisterActivity.class);
        intent.putExtra("SECRET_KEY", SECRET_KEY);
        startActivity(intent);
    }

    public void gotodebug(View view) {
        editor.putFloat("scrollSpeed", 1.0f + speedSB.getProgress() / 100.0f);
        editor.apply();
        editor.putInt("offset", offsetSB.getProgress() - 500);
        editor.apply();
        Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }
}