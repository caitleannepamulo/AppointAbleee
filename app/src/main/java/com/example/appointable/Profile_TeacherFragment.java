package com.example.appointable;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Profile_TeacherFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private ImageView imgProfile;
    private EditText etFirstname, etLastname, etMiddlename, etSuffix;
    private EditText etBirthdate, etAge, etEmail, etUsername, etContactNumber;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale;
    private Spinner spRole;
    private Button btnLogout;

    public Profile_TeacherFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_teacher, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews(view);

        // ✅ Add adapter back so Spinner can display items
        setupRoleSpinner();

        // ✅ Make everything read-only (since you removed edit feature)
        setAllFieldsReadOnly();

        loadUserData();

        btnLogout.setOnClickListener(v -> showLogoutDialog());
    }

    private void initViews(View view) {
        imgProfile = view.findViewById(R.id.imgProfile);

        etFirstname = view.findViewById(R.id.etFirstname);
        etLastname = view.findViewById(R.id.etLastname);
        etMiddlename = view.findViewById(R.id.etMiddlename);
        etSuffix = view.findViewById(R.id.etSuffix);

        etBirthdate = view.findViewById(R.id.etBirthdate);
        etAge = view.findViewById(R.id.etAge);
        etEmail = view.findViewById(R.id.etEmail);
        etUsername = view.findViewById(R.id.etUsername);
        etContactNumber = view.findViewById(R.id.etContactNumber);

        rgGender = view.findViewById(R.id.rgGender);
        rbMale = view.findViewById(R.id.rbMale);
        rbFemale = view.findViewById(R.id.rbFemale);

        spRole = view.findViewById(R.id.spRole);
        btnLogout = view.findViewById(R.id.btnLogout);
    }

    private void setupRoleSpinner() {
        if (getContext() == null) return;

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.role_list,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRole.setAdapter(adapter);
    }

    private void setAllFieldsReadOnly() {
        etFirstname.setEnabled(false);
        etLastname.setEnabled(false);
        etMiddlename.setEnabled(false);
        etSuffix.setEnabled(false);

        etBirthdate.setEnabled(false);
        etAge.setEnabled(false);
        etEmail.setEnabled(false);

        etUsername.setEnabled(false);
        etContactNumber.setEnabled(false);

        rgGender.setEnabled(false);
        rbMale.setEnabled(false);
        rbFemale.setEnabled(false);

        spRole.setEnabled(false);

        // Profile image click disabled (no edit mode)
        imgProfile.setEnabled(false);
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String firstName = doc.getString("firstName");
                    String lastName = doc.getString("lastName");
                    String middleName = doc.getString("middleName");
                    String suffix = doc.getString("suffix");

                    String birthdate = doc.getString("birthdate");
                    String age = doc.getString("age");
                    String email = doc.getString("email");
                    String username = doc.getString("username");
                    String contact = doc.getString("contact");

                    String gender = doc.getString("gender");
                    String role = doc.getString("role");
                    String profileImageUrl = doc.getString("profileImageUrl");

                    etFirstname.setText(firstName != null ? firstName : "");
                    etLastname.setText(lastName != null ? lastName : "");
                    etMiddlename.setText(middleName != null ? middleName : "");
                    etSuffix.setText(suffix != null ? suffix : "");

                    etBirthdate.setText(birthdate != null ? birthdate : "");
                    etAge.setText(age != null ? age : "");
                    etEmail.setText(email != null ? email : "");
                    etUsername.setText(username != null ? username : "");
                    etContactNumber.setText(contact != null ? contact : "");

                    if ("Male".equalsIgnoreCase(gender)) rbMale.setChecked(true);
                    if ("Female".equalsIgnoreCase(gender)) rbFemale.setChecked(true);

                    // ✅ Set role selection in spinner
                    if (role != null && spRole.getAdapter() != null) {
                        ArrayAdapter adapter = (ArrayAdapter) spRole.getAdapter();
                        int index = adapter.getPosition(role);
                        if (index >= 0) {
                            spRole.setSelection(index);
                        } else {
                            // If role not found in list, keep spinner at 0
                            spRole.setSelection(0);
                        }
                    } else {
                        spRole.setSelection(0);
                    }

                    // ✅ Load profile image
                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(Profile_TeacherFragment.this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_profile)
                                .circleCrop()
                                .into(imgProfile);
                    } else {
                        Glide.with(Profile_TeacherFragment.this)
                                .load(R.drawable.ic_profile)
                                .circleCrop()
                                .into(imgProfile);
                    }
                });
    }

    private void showLogoutDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();
                    if (getActivity() != null) {
                        Intent i = new Intent(getActivity(), LoginActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        getActivity().finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
