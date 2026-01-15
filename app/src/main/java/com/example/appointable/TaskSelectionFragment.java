package com.example.appointable;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TaskSelectionFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.task_selection_fragment, container, false);

        ImageView btnBack = view.findViewById(R.id.btnBack);
        // Find game options
        LinearLayout pronunciationGame = view.findViewById(R.id.cardPronunciation);
        LinearLayout followInstruc = view.findViewById(R.id.cardFollow);


        btnBack.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new TeacherHomeFragment())
                    .commit();
        });


        pronunciationGame.setOnClickListener(v -> {
            chooseDiff diffFragment = new chooseDiff();
            Bundle args = new Bundle();
            args.putString("taskType", "pronunciation");
            diffFragment.setArguments(args);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, diffFragment)
                    .addToBackStack(null)
                    .commit();
        });

        followInstruc.setOnClickListener(v -> {
            chooseDiff diffFragment = new chooseDiff();
            Bundle args = new Bundle();
            args.putString("taskType", "followInstruction");
            diffFragment.setArguments(args);

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, diffFragment)
                    .addToBackStack(null)
                    .commit();
        });


        return view;
    }
}
