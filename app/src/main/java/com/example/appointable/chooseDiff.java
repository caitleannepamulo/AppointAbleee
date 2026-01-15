package com.example.appointable;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class chooseDiff extends Fragment {

    private FirebaseFirestore db;
    private String taskType; // "pronunciation" or "followInstruction"
    private String userId;

    public chooseDiff() {}

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.choose_diff, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        taskType = getArguments() != null ? getArguments().getString("taskType") : "pronunciation";

        Button btnEasy = view.findViewById(R.id.btnEasy);
        Button btnMedium = view.findViewById(R.id.btnMedium);
        Button btnHard = view.findViewById(R.id.btnHard);
        ImageView btnBack = view.findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        // Always enable Easy
        btnEasy.setOnClickListener(v -> launchGame("easy"));

        // Load difficulty unlock status
        db.collection("users").document(userId)
                .collection("scores").document(taskType)
                .get().addOnSuccessListener(doc -> {
                    double easyScore = doc.getDouble("easy") != null ? doc.getDouble("easy") : 0.0;
                    double mediumScore = doc.getDouble("medium") != null ? doc.getDouble("medium") : 0.0;

                    if (easyScore >= 70) {
                        btnMedium.setEnabled(true);
                    } else {
                        btnMedium.setEnabled(false);
                        btnMedium.setAlpha(0.5f);
                    }

                    if (mediumScore >= 70) {
                        btnHard.setEnabled(true);
                    } else {
                        btnHard.setEnabled(false);
                        btnHard.setAlpha(0.5f);
                    }
                });

        btnMedium.setOnClickListener(v -> {
            if (btnMedium.isEnabled()) launchGame("medium");
            else Toast.makeText(getContext(), "Unlock Medium by scoring 70% or higher in Easy!", Toast.LENGTH_SHORT).show();
        });

        btnHard.setOnClickListener(v -> {
            if (btnHard.isEnabled()) launchGame("hard");
            else Toast.makeText(getContext(), "Unlock Hard by scoring 70% or higher in Medium!", Toast.LENGTH_SHORT).show();
        });
    }

    private void launchGame(String difficulty) {
        Bundle args = new Bundle();
        args.putString("difficulty", difficulty);

        if ("pronunciation".equals(taskType)) {
            Intent intent = new Intent(requireContext(), PronounciationActivity.class);
            intent.putExtra("difficulty", difficulty);
            startActivity(intent);
        } else if ("followInstruction".equals(taskType)) {
            Intent intent = new Intent(requireContext(), FollowInstructionActivity.class);
            intent.putExtra("difficulty", difficulty);
            startActivity(intent);
        }
    }

}
