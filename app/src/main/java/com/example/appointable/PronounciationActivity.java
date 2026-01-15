package com.example.appointable;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.VideoView;
import android.widget.MediaController;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class PronounciationActivity extends AppCompatActivity {
    private VideoView videoView;
    private RatingBar ratingBar;
    private String difficulty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pronounciation);

        ImageView btnBack = findViewById(R.id.btnBack);
        videoView = findViewById(R.id.videoView);
        ratingBar = findViewById(R.id.ratingBar);

        // Get selected difficulty
        difficulty = getIntent().getStringExtra("difficulty");
        if (difficulty == null) difficulty = "easy";

        btnBack.setOnClickListener(v -> onBackPressed());

        setupVideoPlayer();

        //Save score when the user rates
        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if (fromUser) {
                saveScoreToFirestore(rating);
            }
        });
    }

    private void setupVideoPlayer() {
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.apple);
        videoView.setVideoURI(videoUri);
        MediaController mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);
        videoView.start();
    }

    private void saveScoreToFirestore(float rating) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Convert rating (0–10) to percent (0–100)
        float percentScore = rating * 10f;

        Map<String, Object> updates = new HashMap<>();
        updates.put(difficulty, percentScore);

        db.collection("users")
                .document(userId)
                .collection("scores")
                .document("pronunciation")
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Pronunciation score saved."))
                .addOnFailureListener(e -> Log.e("Firestore", "Error saving score", e));
    }
}
