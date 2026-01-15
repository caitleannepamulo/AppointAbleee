package com.example.appointable;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FollowInstructionActivity extends AppCompatActivity {

    private TextView tvInstruction;
    private Button btnNext, btnRepeat;
    private RatingBar ratingBar;
    private ImageView btnBack;
    private String[] instructions;
    private int currentIndex = 0;
    private TextToSpeech textToSpeech;
    private String difficulty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow_instruction);

        tvInstruction = findViewById(R.id.tvInstruction);
        btnNext = findViewById(R.id.btnNext);
        btnRepeat = findViewById(R.id.btnRepeat);
        btnBack = findViewById(R.id.btnBack);
        ratingBar = findViewById(R.id.ratingBar);

        // Get difficulty from previous screen
        difficulty = getIntent().getStringExtra("difficulty");
        if (difficulty == null) difficulty = "easy";

        instructions = new String[]{
                "Clap your hands twice!",
                "Touch your nose!",
                "Jump two times!",
                "Spin around once!",
                "Raise your hands up high!",
                "Stomp your feet three times!",
                "Pat your head and rub your tummy!"
        };

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.US);
                speakInstruction(instructions[currentIndex]);
            }
        });

        btnNext.setOnClickListener(v -> {
            currentIndex = (currentIndex + 1) % instructions.length;
            speakInstruction(instructions[currentIndex]);
        });

        btnRepeat.setOnClickListener(v -> speakInstruction(instructions[currentIndex]));

        btnBack.setOnClickListener(v -> finish());

        // ⭐ Save score when the user rates
        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if (fromUser) {
                saveScoreToFirestore(rating);
            }
        });
    }

    private void speakInstruction(String text) {
        tvInstruction.setText(text);
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void saveScoreToFirestore(float rating) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        float percentScore = rating * 10f;
        Map<String, Object> updates = new HashMap<>();
        updates.put(difficulty, percentScore);

        db.collection("users")
                .document(userId)
                .collection("scores")
                .document("followInstruction")
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Follow Instruction score saved."))
                .addOnFailureListener(e -> Log.e("Firestore", "Error saving score", e));
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
