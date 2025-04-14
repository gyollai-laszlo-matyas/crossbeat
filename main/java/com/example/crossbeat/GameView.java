package com.example.crossbeat;

import android.media.MediaPlayer;
import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedList;

public class GameView extends View {

    Context context;
    Handler handler;
    Runnable runnable;

    Paint testPaint = new Paint();
    Paint circlePaint = new Paint();
    Paint notePaint = new Paint();
    MediaPlayer mp;

    // Screen width and height
    int width;
    int height;

    // Basic game stats
    int score = 0;
    int[] quadrants = {0, 0, 0, 0}; // 0 = neutral, 2 = tap, 1 = hold, top left -> top right -> bottom left -> bottom right
    int goodWindow = 120;
    int perfWindow = 60;

    // Song info
    float BPM = 180;
    float msPerBeat = 60000 / BPM;
    float offset = 90;

    // how many ms to look ahead for displaying notes
    float lookAhead = 500;

    class Note{
        float beat;
        int pos;
        boolean active;
        public Note(float beat, int pos){
            this.beat = beat;
            this.pos = pos;
            this.active = true;
        }
    }

    ArrayList<Note> notes;

    public GameView(Context context) {
        super(context);
        this.context = context;

        // yes i will add support for chart files later thanks for asking
        notes = new ArrayList<Note>();
        notes.add(new Note(0, 2));
        notes.add(new Note(1, 2));
        notes.add(new Note(1.5f, 3));
        notes.add(new Note(2, 2));
        notes.add(new Note(3, 2));

        notes.add(new Note(4, 2));
        notes.add(new Note(4.5f, 3));
        notes.add(new Note(5, 2));
        notes.add(new Note(6, 2));
        notes.add(new Note(7, 3));
        notes.add(new Note(7.5f, 3));

        notes.add(new Note(8, 2));
        notes.add(new Note(9, 2));
        notes.add(new Note(9.5f, 3));
        notes.add(new Note(10, 2));
        notes.add(new Note(11, 2));
        notes.add(new Note(12, 2));
        notes.add(new Note(12.5f, 3));

        notes.add(new Note(13, 0));
        notes.add(new Note(14, 1));
        notes.add(new Note(15, 0));
        notes.add(new Note(16, 1));

        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        width = displayMetrics.widthPixels;
        height = displayMetrics.heightPixels;

        mp = MediaPlayer.create(context, R.raw.powerattack);
        mp.start();

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run(){
                invalidate();
            }
        };
        Display display = ((Activity) getContext()).getWindowManager().getDefaultDisplay();

        testPaint.setColor(Color.rgb(170, 255, 190));
        testPaint.setTextSize(120);
        testPaint.setTextAlign(Paint.Align.LEFT);

        circlePaint.setColor(Color.argb(170, 0, 0, 0));
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(5);

        notePaint.setColor(Color.rgb(160, 190, 255));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        // Draw HUD elements
        canvas.drawText(Integer.toString(score), 20, 120, testPaint);
        canvas.drawText(width + "x" + height, 600, 120, testPaint);

        // Draw hit receptors
        canvas.drawArc(50, 250, 250, 450, 0, 360, false, circlePaint);
        canvas.drawArc(width-250, 250, width-50, 450, 0, 360, false, circlePaint);
        canvas.drawArc(50, height-250, 250, height-450, 0, 360, false, circlePaint);
        canvas.drawArc(width-50, height-250, width-250, height-450, 0, 360, false, circlePaint);

        // Draw notes
        float songPos = offset + mp.getCurrentPosition() - 2000;
        for(Note n : notes){
            if(!n.active){continue;}
            float notePos = n.beat * msPerBeat;

            // Miss window
            if(songPos - notePos > goodWindow){
                n.active = false;
                continue;
            }

            if(notePos - songPos < lookAhead) {
                float posRate = 1 - (notePos - songPos) / lookAhead;
                int xRate = -1 + (n.pos % 2) * 2;
                int yRate = -1 + (n.pos / 2) * 2;
                canvas.drawCircle(
                        width / 2 + xRate * posRate * (width / 2 - 150),
                        height / 2 + yRate * posRate * (height / 2 - 350),
                        90,
                        notePaint
                );
            }else{
                break;
            }
        }

        // Draw tap highlights
        for(int i = 0; i <= 3; i++){
            if(quadrants[i] > 0){
                int xPos = i % 2;
                int yPos = i / 2;
                Paint p = new Paint();
                p.setColor(Color.argb(quadrants[i] * 40, 0, 0, 0));
                canvas.drawRect(width / 2 * xPos, height / 2 * yPos, width / 2 * (xPos + 1), height / 2 * (yPos + 1), p);
            }
        }

        handler.postDelayed(runnable, 10);
        for(int i = 0; i <= 3; i++){
            quadrants[i] = 0;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        for(int i = 0; i <= event.getPointerCount(); i++){
            int index = event.findPointerIndex(i);
            if(index != -1){
                int touchQuadrant = 0;
                if(event.getX(index) > width / 2){touchQuadrant++;}
                if(event.getY(index) > height / 2){touchQuadrant += 2;}

                if(
                        event.getAction() == MotionEvent.ACTION_DOWN ||
                        event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN && event.findPointerIndex(event.getPointerId(event.getActionIndex())) == index)
                {quadrants[touchQuadrant] = 2;}

                // Note judgment
                float songPos = offset + mp.getCurrentPosition() - 2000;
                for(Note n : notes){
                    if(!n.active){continue;}
                    if(n.pos != touchQuadrant){continue;}
                    float notePos = n.beat * msPerBeat;
                    if(Math.abs(songPos - notePos) < goodWindow){
                        score += 50;

                        // Perfect window
                        if(Math.abs(songPos - notePos) < perfWindow) {
                            score += 50;
                        }
                        n.active = false;
                        break;
                    }
                }
            }
        }
        return true;
    }

    // Pause/resume the song when exiting/reopening the app
    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == View.VISIBLE) {mp.start();}
        else {mp.pause();}
    }
}
