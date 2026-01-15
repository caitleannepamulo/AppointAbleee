package com.example.appointable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {

    private final List<StudentModel> list;
    private final String currentUserRole;
    private final boolean canGrade;

    private final Set<String> allowedRoles =
            new HashSet<>(Arrays.asList(
                    "OT Associates",
                    "Sped Teacher",
                    "Sped Coordinator",
                    "Occupational Therapist"
            ));

    public StudentAdapter(List<StudentModel> list, String currentUserRole) {
        this.list = list;
        this.currentUserRole = currentUserRole;
        this.canGrade = currentUserRole != null && allowedRoles.contains(currentUserRole);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_student_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentModel m = list.get(position);

        holder.tvID.setText(m.studentNumber);
        holder.tvName.setText(m.getFullName());
        holder.tvGrade.setText(m.grade == null || m.grade.isEmpty() ? "-" : m.grade);
        holder.tvStatus.setText(m.status);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView tvID, tvName, tvGrade, tvStatus, tvView, tvEdit;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvID = itemView.findViewById(R.id.tvID);
            tvName = itemView.findViewById(R.id.tvName);
            tvGrade = itemView.findViewById(R.id.tvGrade);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvView = itemView.findViewById(R.id.tvView);
            tvEdit = itemView.findViewById(R.id.tvEdit);

            tvView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    StudentModel m = list.get(pos);
                    showViewDialog(itemView, m);
                }
            });

            tvEdit.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    StudentModel m = list.get(pos);
                    showEditDialog(itemView, m, pos);
                }
            });
        }

        private void showViewDialog(View itemView, StudentModel m) {
            View dialogView = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.dialog_student_view, null, false);

            EditText etViewFirstname = dialogView.findViewById(R.id.etViewFirstname);
            EditText etViewLastname = dialogView.findViewById(R.id.etViewLastname);
            EditText etViewMiddlename = dialogView.findViewById(R.id.etViewMiddlename);
            EditText etViewSuffix = dialogView.findViewById(R.id.etViewSuffix);
            EditText etViewStudentNumber = dialogView.findViewById(R.id.etViewStudentNumber);
            EditText etViewGrades = dialogView.findViewById(R.id.etViewGrades);
            Spinner spViewStatus = dialogView.findViewById(R.id.spViewStatus);
            MaterialButton btnClose = dialogView.findViewById(R.id.btnClose);

            etViewFirstname.setText(m.firstName);
            etViewLastname.setText(m.lastName);
            etViewMiddlename.setText(m.middleName);
            etViewSuffix.setText(m.suffix);
            etViewStudentNumber.setText(m.studentNumber);
            etViewGrades.setText(m.grade);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    itemView.getContext(),
                    R.array.student_status_array,
                    android.R.layout.simple_spinner_dropdown_item
            );
            spViewStatus.setAdapter(adapter);
            spViewStatus.setSelection(adapter.getPosition(m.status));

            spViewStatus.setEnabled(false);
            spViewStatus.setClickable(false);
            spViewStatus.setAlpha(0.5f);

            AlertDialog dialog = new AlertDialog.Builder(itemView.getContext())
                    .setView(dialogView)
                    .create();

            btnClose.setOnClickListener(v -> dialog.dismiss());

            dialog.show();
        }

        private void showEditDialog(View itemView, StudentModel m, int position) {
            View dialogView = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.dialog_student_edit, null, false);

            EditText etFirstname = dialogView.findViewById(R.id.etFirstname);
            EditText etLastname = dialogView.findViewById(R.id.etLastname);
            EditText etMiddlename = dialogView.findViewById(R.id.etMiddlename);
            EditText etSuffix = dialogView.findViewById(R.id.etSuffix);
            EditText etStudentNumber = dialogView.findViewById(R.id.etStudentNumber);
            EditText etGrades = dialogView.findViewById(R.id.etGrades);
            Spinner spStatus = dialogView.findViewById(R.id.spStatus);
            MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancel);
            MaterialButton btnUpdate = dialogView.findViewById(R.id.btnUpdate);

            etFirstname.setText(m.firstName);
            etLastname.setText(m.lastName);
            etMiddlename.setText(m.middleName);
            etSuffix.setText(m.suffix);
            etStudentNumber.setText(m.studentNumber);
            etGrades.setText(m.grade);

            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    itemView.getContext(),
                    R.array.student_status_array,
                    android.R.layout.simple_spinner_dropdown_item
            );
            spStatus.setAdapter(adapter);
            spStatus.setSelection(adapter.getPosition(m.status));

            AlertDialog dialog = new AlertDialog.Builder(itemView.getContext())
                    .setView(dialogView)
                    .create();

            btnCancel.setOnClickListener(v -> dialog.dismiss());

            btnUpdate.setOnClickListener(v -> {
                m.firstName = etFirstname.getText().toString().trim();
                m.lastName = etLastname.getText().toString().trim();
                m.middleName = etMiddlename.getText().toString().trim();
                m.suffix = etSuffix.getText().toString().trim();
                m.studentNumber = etStudentNumber.getText().toString().trim();
                m.grade = etGrades.getText().toString().trim();
                m.status = spStatus.getSelectedItem().toString();

                notifyItemChanged(position);

                updateStudentInFirestore(m);

                if (canGrade && !m.grade.isEmpty()) {
                    saveGradeToFirestore(m);
                }

                dialog.dismiss();
            });

            dialog.show();
        }

        private void updateStudentInFirestore(StudentModel m) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            Map<String, Object> data = new HashMap<>();
            data.put("firstName", m.firstName);
            data.put("lastName", m.lastName);
            data.put("middleName", m.middleName);
            data.put("suffix", m.suffix);
            data.put("userId", m.studentNumber);
            data.put("grade", m.grade);
            data.put("status", m.status);

            db.collection("users")
                    .document(m.docId)
                    .update(data);
        }

        private void saveGradeToFirestore(StudentModel m) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            String teacherId = user.getUid();

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            Map<String, Object> data = new HashMap<>();
            data.put("teacherId", teacherId);
            data.put("studentDocId", m.docId);
            data.put("studentNumber", m.studentNumber);
            data.put("grade", m.grade);
            data.put("createdAt", FieldValue.serverTimestamp());

            db.collection("grades")
                    .add(data);
        }
    }
}
