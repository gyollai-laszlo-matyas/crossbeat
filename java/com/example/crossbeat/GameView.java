package com.example.crossbeat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Picture;
import android.graphics.Rect;
import android.graphics.RectF;
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
import android.view.WindowInsets;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class GameView extends View {

    Context context;
    Handler handler;
    Runnable runnable;
    MediaPlayer mp;

    // Screen width and height
    int width;
    int height;
    float hudScale;
    RectF bgRect;

    // Basic game stats
    int gameState = 0; // 0 = gameplay, 1 = results
    int resultsDelay = 0;
    int score = 0;
    String grade = "";
    int displayScore = 0;
    int realScore = 0;
    DecimalFormat formatter = new DecimalFormat("#,###,###");
    int noteCount = 0;
    int combo = 0;
    int maxCombo = 0;
    int lastError = -999;
    int[] quadrants = {0, 0, 0, 0}; // 0 = neutral, 2 = tap, 1 = hold, top left -> top right -> bottom left -> bottom right
    int[] lastJudge = {-1, -1, -1, -1}; // last judgement hit for each quadrant, used for drawing judgement popups
    float[] lastJudgeTime = {0, 0, 0, 0}; // time last judgement was hit for each quadrant, used for drawing judgement popups
    int antiMashWindow = 160; // hitting early by up to this much before the good window will cause a miss
    int goodWindow = 120;
    int perfWindow = 70;
    int perfPlusWindow = 35;
    int[] countJudgements = {0, 0, 0, 0};

    // Song info
    float BPM = 60;
    float msPerBeat = 60000 / BPM;
    Bitmap songJacket;
    int diff = 0;

    // how many ms to look ahead for displaying notes (scroll speed)
    float lookAhead = 620;
    float offset = 105;
    float avgOffset = 0.0f;
    float circlePos = 0.675f;
    float judgePos = 0.35f;

    // Define common bitmaps
    Bitmap judge0 = BitmapFactory.decodeResource(getResources(), R.drawable.judge_0); // Perfect+
    Bitmap judge1 = BitmapFactory.decodeResource(getResources(), R.drawable.judge_1); // Perfect
    Bitmap judge1e = BitmapFactory.decodeResource(getResources(), R.drawable.judge_1e); // Perfect (early)
    Bitmap judge1l = BitmapFactory.decodeResource(getResources(), R.drawable.judge_1l); // Perfect (late)
    Bitmap judge2 = BitmapFactory.decodeResource(getResources(), R.drawable.judge_2); // Good
    Bitmap judge2e = BitmapFactory.decodeResource(getResources(), R.drawable.judge_2e); // Good (early)
    Bitmap judge2l = BitmapFactory.decodeResource(getResources(), R.drawable.judge_2l); // Good (late)
    Bitmap judge3 = BitmapFactory.decodeResource(getResources(), R.drawable.judge_3); // Miss
    Bitmap fail = BitmapFactory.decodeResource(getResources(), R.drawable.failed); // Full combo
    Bitmap clear = BitmapFactory.decodeResource(getResources(), R.drawable.cleared); // Full combo
    Bitmap fc = BitmapFactory.decodeResource(getResources(), R.drawable.fullcombo); // Full combo
    Bitmap ap = BitmapFactory.decodeResource(getResources(), R.drawable.allperfect); // All perfect
    Bitmap[] judgeBitmaps = {judge0, judge1e, judge1l, judge2e, judge2l, judge3, fc, ap};
    Bitmap[] resultBitmaps = {judge0, judge1, judge2, judge3};
    Rect src = new Rect(0, 0, 256, 64);
    RectF dest = new RectF(0, 0, 0, 0);

    // Define paints
    Paint scorePaint = new Paint();
    Paint circlePaint = new Paint();
    Paint notePaint = new Paint();
    Paint chordPaint = new Paint();
    Paint bgPaint = new Paint();
    Paint tapPaint = new Paint();
    Paint darkenPaint = new Paint();
    Paint barPaint = new Paint();

    class Note{
        float beat;
        int pos;
        boolean isChord = false;
        public Note(float beat, int pos){
            this.beat = beat;
            this.pos = pos;
        }

        public void setChord(boolean chord) {
            isChord = chord;
        }
    }

    ArrayList<Note> notes;

    public GameView(Context context, String songID, int difficulty, float scrollSpeed, int songOffset) {
        super(context);
        this.context = context;

        lookAhead = 2000 / scrollSpeed;
        offset = songOffset;
        diff = difficulty;

        // yes i will add support for chart files later thanks for asking
        // ok done
        // no im not making a chart editor for this trash
        notes = new ArrayList<Note>();
        int chartResId = 0;
        // absolute fucking monkeybrain solution but whatever
        switch(songID){
            case("powerattack"): {
                songJacket = BitmapFactory.decodeResource(getResources(), R.drawable.powerattack);
                mp = MediaPlayer.create(context, R.raw.powerattack);
                switch(difficulty) {
                    case(0): {chartResId = R.raw.powerattack_light; break;}
                    case(1): {chartResId = R.raw.powerattack_hyper; break;}
                    case(2): {chartResId = R.raw.powerattack_ultra; break;}
                }
                break;
            }
            case("teraio"): {
                songJacket = BitmapFactory.decodeResource(getResources(), R.drawable.teraio);
                mp = MediaPlayer.create(context, R.raw.teraio);
                switch(difficulty) {
                    // case(0): {chartResId = R.raw.teraio_light; break;}
                    // case(1): {chartResId = R.raw.teraio_hyper; break;}
                    // case(2): {chartResId = R.raw.teraio_ultra; break;}
                }
                break;
            }
        }

        InputStream is = this.getContext().getResources().openRawResource(chartResId);
        BufferedReader bf;
        try {
            bf = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        int seekPos = 0;
        try{
            // Set up chart info
            String line = bf.readLine();
            String[] chartAttribs = line.split(",");

            BPM = Float.parseFloat(chartAttribs[0]);
            msPerBeat = 60000 / BPM;
            seekPos = Integer.parseInt(chartAttribs[1]);
            line = bf.readLine();

            float nextBeat = 0;
            // Read note data
            while(line != null){
                if(!line.isEmpty() && line.toCharArray()[0] != '/'){
                    String[] noteAttribs = line.split(" ");
                    if(noteAttribs.length == 2){
                        nextBeat += Float.parseFloat(noteAttribs[0]);
                        notes.add(new Note(nextBeat, Integer.parseInt(noteAttribs[1])));

                        if(Float.parseFloat(noteAttribs[0]) == 0){
                            notes.get(noteCount).setChord(true);
                            notes.get(noteCount - 1).setChord(true);
                        }
                        noteCount++;
                    }
                }
                line = bf.readLine();
            }
        } catch (IOException e){
            throw new RuntimeException(e);
        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(displayMetrics);
        width = displayMetrics.widthPixels;
        hudScale = width / 1080.0f;
        height = displayMetrics.heightPixels;

        bgRect = new RectF();
        int bgSize = (Math.min(width, height));
        bgRect.left = width * 0.5f - bgSize;
        bgRect.right = width * 0.5f + bgSize;
        bgRect.top = height * 0.5f - bgSize;
        bgRect.bottom = height * 0.5f + bgSize;

        // songJacket = BitmapFactory.decodeResource(getResources(), R.drawable.powerattack);

        mp.setVolume(0.5f, 0.5f);
        mp.start();
        mp.seekTo(seekPos);

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run(){
                invalidate();
            }
        };
        Display display = ((Activity) getContext()).getWindowManager().getDefaultDisplay();

        scorePaint.setColor(Color.rgb(255, 170, 40));
        scorePaint.setShadowLayer(10, 0, 0, Color.rgb(0, 0, 0));
        scorePaint.setLetterSpacing(0.05f);
        scorePaint.setTextSize(120 * hudScale);
        scorePaint.setTextAlign(Paint.Align.CENTER);

        circlePaint.setColor(Color.argb(170, 0, 0, 0));
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setTextSize(120 * hudScale);
        circlePaint.setTextAlign(Paint.Align.CENTER);
        circlePaint.setStrokeWidth(5);

        notePaint.setColor(Color.rgb(160, 190, 255));

        chordPaint.setColor(Color.rgb(255, 190, 255));

        bgPaint.setColor(Color.argb(30,255, 255, 255));

        tapPaint.setColor(Color.argb(120, 255, 255, 255));

        darkenPaint.setColor(Color.rgb(0, 0, 0));

        barPaint.setColor(Color.argb(45, 0, 0, 0));
        barPaint.setStrokeWidth(2);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if(gameState == 0){drawGameplay(canvas);}
        else{drawResults(canvas);}

        handler.postDelayed(runnable, 8);
        for(int i = 0; i <= 3; i++){
            quadrants[i] = 0;
        }
    }

    private void drawGameplay(Canvas canvas){
        canvas.drawBitmap(songJacket, null, bgRect, bgPaint);

        float songPos = offset + mp.getCurrentPosition() - 2000;
        float finalJudge = lastJudgeTime[0];
        for(int i = 1; i <= 3; i++){
            if(lastJudgeTime[i] > finalJudge){finalJudge = lastJudgeTime[i];}
        }

        // Draw HUD elements
        tapPaint.setAlpha(255);
        canvas.drawLine(0, height / 2.0f, width, height / 2.0f, tapPaint);
        canvas.drawLine(width / 2.0f, 0, width / 2.0f, height, tapPaint);
        tapPaint.setAlpha(120);
        if(combo >= 5) {
            // circlePaint.setLetterSpacing((float)Math.max(0, 0.1 - (finalJudge - songPos) * 0.01));
            circlePaint.setLetterSpacing(0.1f - 0.1f * (float) Math.sin(Math.PI / 180 * Math.min(90.0f, 0.6f * (songPos - finalJudge))));
            circlePaint.setTextSize(120 * hudScale);
            canvas.drawText(String.valueOf(combo), width / 2f, height / 2f - 40, circlePaint);

            circlePaint.setTextSize(60 * hudScale);
            canvas.drawText(countJudgements[3] == 0 ? (countJudgements[2] == 0 ? "ALL PERFECT" : "FULL COMBO") : "COMBO", width / 2f, height / 2f + 40, circlePaint);
        }

        realScore = (int)Math.floor((double) (10000 * score) / noteCount);
        displayScore += (realScore - displayScore) * 0.7f;
        canvas.drawText(formatter.format(displayScore), width / 2f, 120, scorePaint);

        /*if(lastError != -999){
            canvas.drawText(
                ((Math.abs(lastError) < perfWindow) ? "PERFECT " : (lastError < 0) ? "EARLY " : "LATE ")
                + Integer.toString(lastError), 20, 220, testPaint
            );
        }*/

        // Draw hit receptors
        for(int i = 0; i <= 3; i++) {
            float xPos = width / 2f + width * 0.5f * circlePos * (-1 + 2 * (i % 2));
            float yPos = height / 2f + height * 0.5f * circlePos * (-1 + 2 * (int)(i / 2));
            canvas.drawArc(xPos - 100 * hudScale, yPos - 100 * hudScale, xPos + 100 * hudScale, yPos + 100 * hudScale, 0, 360, false, circlePaint);
        }

        // Draw tap highlights
        for(int i = 0; i <= 3; i++){
            if(quadrants[i] > 0){
                int xPos = i % 2;
                int yPos = i / 2;
                canvas.drawRect(width / 2f * xPos, height / 2f * yPos, width / 2f * (xPos + 1), height / 2f * (yPos + 1), tapPaint);
            }
        }

        // Draw bar lines
        float barLength = msPerBeat * 4;
        int currentBar = (int)Math.ceil(songPos / barLength);
        for(int i = -2; i <= 2; i++){
            float barPos = barLength * (i + currentBar);
            float posRate = 1 - (barPos - songPos + msPerBeat) / lookAhead;
            if(posRate < 0){continue;}
            canvas.drawLine(width / 2f - width * 0.5f * posRate * circlePos,
                    height / 2f - height * 0.5f * posRate * circlePos,
                    width / 2f + width * 0.5f * posRate * circlePos,
                    height / 2f - height * 0.5f * posRate * circlePos,
                    barPaint);
            canvas.drawLine(width / 2f - width * 0.5f * posRate * circlePos,
                    height / 2f + height * 0.5f * posRate * circlePos,
                    width / 2f + width * 0.5f * posRate * circlePos,
                    height / 2f + height * 0.5f * posRate * circlePos,
                    barPaint);
            canvas.drawLine(width / 2f - width * 0.5f * posRate * circlePos,
                    height / 2f - height * 0.5f * posRate * circlePos,
                    width / 2f - width * 0.5f * posRate * circlePos,
                    height / 2f + height * 0.5f * posRate * circlePos,
                    barPaint);
            canvas.drawLine(width / 2f + width * 0.5f * posRate * circlePos,
                    height / 2f - height * 0.5f * posRate * circlePos,
                    width / 2f + width * 0.5f * posRate * circlePos,
                    height / 2f + height * 0.5f * posRate * circlePos,
                    barPaint);
        }

        // Draw judgement popups
        for(int i = 0; i <= 3; i++){
            if(lastJudge[i] == -1){continue;}
            float xPos = width / 2f + width * 0.5f * judgePos * (-1 + 2 * (i % 2));
            float yPos = height / 2f + height * 0.5f * judgePos * (-1 + 2 * (int)(i / 2));
            float scaleRate = 2.5f - (float) Math.sin(Math.PI / 180 * Math.min(90.0f, 0.3f * (songPos - lastJudgeTime[i])));
            scaleRate *= hudScale;
            Bitmap bitmap = judgeBitmaps[lastJudge[i]];
            dest.set(xPos - (128 * scaleRate), yPos - (32 * scaleRate), xPos + (128 * scaleRate), yPos + (32 * scaleRate));

            darkenPaint.setAlpha((int)Math.max(0, (int)Math.min(255, 955 - (songPos - lastJudgeTime[i]))));
            canvas.drawBitmap(bitmap, null, dest, darkenPaint);
        }

        // Draw notes
        for(int i = 0; i < notes.size(); i++){
            Note n = notes.get(i);
            float notePos = n.beat * msPerBeat;

            // Miss window
            if(songPos - notePos > goodWindow){
                lastJudge[n.pos] = 5;
                lastJudgeTime[n.pos] = songPos;
                notes.remove(i);
                combo = 0;
                countJudgements[3] += 1;
                i--;
                continue;
            }

            if(notePos - songPos < lookAhead) {
                float posRate = 1 - (notePos - songPos) / lookAhead;
                float xPos = width / 2f + width * 0.5f * circlePos * posRate * (-1 + 2 * (n.pos % 2));
                float yPos = height / 2f + height * 0.5f * circlePos * posRate * (-1 + 2 * (int)(n.pos / 2));
                if((n.beat - 1) % 4 == 0){
                    tapPaint.setColor(Color.argb(200, 220, 220, 220));
                }
                canvas.drawCircle(
                        xPos,
                        yPos,
                        80 * hudScale,
                        tapPaint
                );
                tapPaint.setColor(Color.argb(120, 255, 255, 255));
                canvas.drawCircle(
                        xPos,
                        yPos,
                        60 * hudScale,
                        n.isChord ? chordPaint : notePaint
                );
            }else{
                break;
            }
        }

        // Draw full combo/all perfect popup
        int notesJudged = 0;
        for(int i = 0; i <= 3; i++){
            notesJudged += countJudgements[i];
        }
        if(notesJudged == noteCount && countJudgements[3] == 0){
            boolean isAP = (countJudgements[2] == 0);

            if(songPos - finalJudge < 2000) {
                darkenPaint.setAlpha((int)Math.min(100, 0.1 * (songPos - finalJudge)));
                canvas.drawRect(0, 0, width, height, darkenPaint);

                darkenPaint.setAlpha((int)Math.min(255, 0.3 * (songPos - finalJudge)));
                float scaleRate = 3.5f - 2.5f * (float) Math.sin(Math.PI / 180 * Math.min(90.0f, 0.07f * (songPos - finalJudge)));
                scaleRate *= hudScale;
                Bitmap bitmap = judgeBitmaps[isAP ? 7 : 6];
                dest.set(width / 2f - (512 * scaleRate), height / 2f - (256 * scaleRate), width / 2f + (512 * scaleRate), height / 2f + (256 * scaleRate));
                canvas.drawBitmap(bitmap, null, dest, darkenPaint);
            }else{
                darkenPaint.setAlpha((int)Math.max(0, 100 - 0.2 * (songPos - finalJudge - 2000)));
                canvas.drawRect(0, 0, width, height, darkenPaint);

                darkenPaint.setAlpha((int)Math.max(0, 255 - 0.6 * (songPos - finalJudge - 2000)));
                float scaleRate = 5.5f - 4.5f * (float) Math.sin(Math.PI / 180 * Math.max(0f, 90.0f - 0.14f * (songPos - finalJudge - 2000)));
                Bitmap bitmap = judgeBitmaps[isAP ? 7 : 6];
                dest.set(width / 2f - (512 * scaleRate), height / 2f - (256 * scaleRate), width / 2f + (512 * scaleRate), height / 2f + (256 * scaleRate));
                canvas.drawBitmap(bitmap, null, dest, darkenPaint);
            }
        }

        // White out screen for transitions
        if(!mp.isPlaying()){
            if(grade.isEmpty()){
                String[] grades = {"D", "C", "B", "A", "A+", "S", "S+", "S++"};
                int[] scores = {-1, 700000, 800000, 900000, 950000, 980000, 1000000, 1005000};
                for(int i = 0; i < grades.length; i++){
                    if(realScore >= scores[i]){
                        grade = grades[i];
                    }else{
                        break;
                    }
                }
            }
            resultsDelay += 5;
            if(resultsDelay == 255){
                gameState = 1;
                updateScore(realScore, diff);
            }
            tapPaint.setAlpha(resultsDelay);
            canvas.drawRect(0, 0, width, height, tapPaint);
        }
    }

    private void drawResults(Canvas canvas){
        canvas.drawBitmap(songJacket, null, bgRect, bgPaint);

        realScore = (int)Math.floor((double) (10000 * score) / noteCount);
        scorePaint.setTextSize(120 * hudScale);
        canvas.drawText(formatter.format(realScore), width / 2f, 300 + height * 0.1f, scorePaint);
        scorePaint.setTextSize(160 * hudScale);
        canvas.drawText(grade, width / 2f, 300 + height * 0.2f, scorePaint);

        boolean cleared = realScore >= 800000;
        circlePaint.setTextSize(60 * hudScale);

        // Draw judgement popups
        for(int i = 0; i <= 3; i++){
            float xPos = 200;
            float yPos = 300 + height * (0.4f + i * 0.05f);
            Bitmap bitmap = resultBitmaps[i];
            dest.set(xPos - (128 * hudScale), yPos - (32 * hudScale), xPos + (128 * hudScale), yPos + (32 * hudScale));
            canvas.drawBitmap(bitmap, null, dest, null);

            circlePaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(Integer.toString(countJudgements[i]), width - 200, yPos, circlePaint);
        }

        // Draw failed/cleared/full combo/all perfect popup
        if(countJudgements[3] == 0){
            Bitmap bitmap = judgeBitmaps[countJudgements[2] == 0 ? 7 : 6];
            dest.set(width / 2f - (512 * hudScale), 300 - (256 * hudScale), width / 2f + (512 * hudScale), 300 + (256 * hudScale));
            canvas.drawBitmap(bitmap, null, dest, null);
        }else{
            Bitmap bitmap = cleared ? clear : fail;
            dest.set(width / 2f - (512 * hudScale), 300 - (256 * hudScale), width / 2f + (512 * hudScale), 300 + (256 * hudScale));
            canvas.drawBitmap(bitmap, null, dest, null);
        }

        int countNotes = countJudgements[0] + countJudgements[1] + countJudgements[2];
        canvas.drawText(diff == 0 ? "LIGHT" : (diff == 1 ? "HYPER" : "ULTRA"), width * 0.9f, height - 400, circlePaint);
        canvas.drawText("MAX COMBO " + maxCombo, width * 0.9f, height - 300, circlePaint);
        canvas.drawText("Average offset: " + Float.toString(avgOffset / countNotes) + "ms", width * 0.9f, height - 200, circlePaint);

        // White out screen for transitions
        resultsDelay -= 5;
        if(resultsDelay > 0){
            tapPaint.setAlpha(resultsDelay);
            canvas.drawRect(0, 0, width, height, tapPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        for(int i = 0; i <= event.getPointerCount(); i++){
            int index = event.findPointerIndex(i);
            if(index != -1){
                int touchQuadrant = 0;
                if(event.getX(index) > width / 2f){touchQuadrant++;}
                if(event.getY(index) > height / 2f){touchQuadrant += 2;}

                if(
                        event.getActionMasked() == MotionEvent.ACTION_DOWN ||
                        event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN && event.getActionIndex() == index)
                {quadrants[touchQuadrant] = 2;}
                else{continue;}

                // Note judgment
                float songPos = offset + mp.getCurrentPosition() - 2000;
                for(int j = 0; j < notes.size(); j++){
                    Note n = notes.get(j);
                    if(n.pos != touchQuadrant){continue;}
                    float notePos = n.beat * msPerBeat;
                    float hitError = Math.abs(songPos - notePos);
                    // Good window
                    if(hitError < goodWindow){
                        int judgeType = 4;
                        if(songPos - notePos < 0){judgeType -= 1;} // distinguish early/late
                        score += 50;
                        // Perfect window
                        if(hitError < perfWindow) {
                            judgeType -= 2;
                            score += 50;
                            if(hitError < perfPlusWindow){
                                judgeType = 0;
                                score += 1;
                                countJudgements[0] += 1;
                            }
                        }
                        if(judgeType >= 3){countJudgements[2] += 1;}
                        else if(judgeType >= 1){countJudgements[1] += 1;}
                        combo += 1;
                        if(combo > maxCombo){maxCombo = combo;}
                        avgOffset += (songPos - notePos);
                        lastError = Math.round(songPos - notePos);
                        lastJudge[n.pos] = judgeType;
                        lastJudgeTime[n.pos] = songPos;
                        notes.remove(j);
                        break;
                    }else{
                        if((songPos - notePos) < (-goodWindow) && (songPos - notePos) > (-antiMashWindow)){
                            combo = 0;
                            lastError = Math.round(songPos - notePos);
                            lastJudge[n.pos] = 5;
                            lastJudgeTime[n.pos] = songPos;
                            notes.remove(j);
                            break;
                        }
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
        if(gameState == 0){
            if (visibility == View.VISIBLE) {mp.start();}
            else {mp.pause();}
        }
    }

    public void updateScore(int score, int difficulty){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference refScores = db.collection("Scores");

        refScores.whereEqualTo("userID", user.getUid()).whereEqualTo("difficulty", difficulty).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if(!task.getResult().isEmpty()){
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            //if(document.getData().get("score").equals(1)){
                                document.getReference().update("score", score);
                            //}
                        }
                    }else{
                        Map<String, Object> newScore = new HashMap<>();
                        newScore.put("userID", user.getUid());
                        newScore.put("score", score);
                        newScore.put("difficulty", difficulty);

                        refScores.add(newScore);
                    }
                }
            }
        });
    }
}
