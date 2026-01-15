package com.example.appointable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class Student_TeacherFragment extends Fragment {

    private RecyclerView rvStudents;
    private TextView tvRemainingCount;

    private final List<StudentModel> students = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String currentUserRole;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_student_teacher, container, false);

        rvStudents = view.findViewById(R.id.rvStudents);
        tvRemainingCount = view.findViewById(R.id.tvRemainingCount);

        rvStudents.setLayoutManager(new LinearLayoutManager(getContext()));
        rvStudents.setHasFixedSize(true);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        fetchCurrentUserRoleAndStudents();

        return view;
    }

    private void fetchCurrentUserRoleAndStudents() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        currentUserRole = doc.getString("role");
                    }
                    fetchStudents();
                })
                .addOnFailureListener(e -> {
                    currentUserRole = null;
                    fetchStudents();
                });
    }

    private void fetchStudents() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String currentUserUid = user.getUid();

        Query query = db.collection("users")
                .whereEqualTo("role", "Student");

        if (currentUserRole == null ||
                !currentUserRole.equals("Sped Coordinator")) {
            query = query.whereEqualTo("assignedTeacherId", currentUserUid);
        }

        query.get()
                .addOnSuccessListener(querySnapshot -> {
                    students.clear();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {

                        String docId = doc.getId();
                        String studentNumber = doc.getString("userId"); // your Student #
                        String firstName = doc.getString("firstName");
                        String lastName = doc.getString("lastName");
                        String middleName = doc.getString("middleName");
                        String suffix = doc.getString("suffix");
                        String status = doc.getString("status");
                        String grade = doc.getString("grade"); // latest grade (may be null)

                        if (studentNumber == null) studentNumber = "";
                        if (firstName == null) firstName = "";
                        if (lastName == null) lastName = "";
                        if (middleName == null) middleName = "";
                        if (suffix == null) suffix = "";
                        if (status == null) status = "Active";
                        if (grade == null) grade = "";

                        students.add(new StudentModel(
                                docId,
                                studentNumber,
                                firstName,
                                lastName,
                                middleName,
                                suffix,
                                grade,
                                status
                        ));
                    }

                    StudentAdapter adapter = new StudentAdapter(students, currentUserRole);
                    rvStudents.setAdapter(adapter);

                    tvRemainingCount.setText(String.valueOf(students.size()));
                });
    }
}
