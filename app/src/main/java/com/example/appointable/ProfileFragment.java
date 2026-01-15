package com.example.appointable;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private SharedPreferences sharedPreferences;

    private ImageView imgProfile;
    private EditText etFirstname, etLastname, etMiddlename, etSuffix;
    private EditText etBirthdate, etAge, etEmail, etUsername, etContactNumber;
    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale;
    private Spinner spRole;

    private Button btnLogout;

    private String currentProfileImageUrl = null;

    public ProfileFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_profile_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", requireContext().MODE_PRIVATE);

        initViews(view);
        setupRoleSpinner();

        setReadOnly();     // always non-editable
        loadUserData();    // load from Firestore

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
                getContext(),
                R.array.role_list,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRole.setAdapter(adapter);
    }

    private void setReadOnly() {
        // Disable typing for all EditTexts
        disableEditText(etFirstname);
        disableEditText(etLastname);
        disableEditText(etMiddlename);
        disableEditText(etSuffix);
        disableEditText(etBirthdate);
        disableEditText(etAge);
        disableEditText(etEmail);
        disableEditText(etUsername);
        disableEditText(etContactNumber);

        // Disable gender + role selection
        rgGender.setEnabled(false);
        rbMale.setEnabled(false);
        rbFemale.setEnabled(false);

        spRole.setEnabled(false);
        spRole.setClickable(false);

        // Disable changing profile image
        imgProfile.setEnabled(false);
        imgProfile.setClickable(false);
        imgProfile.setFocusable(false);
        imgProfile.setFocusableInTouchMode(false);
    }

    private void disableEditText(EditText et) {
        if (et == null) return;
        et.setEnabled(false);
        et.setFocusable(false);
        et.setFocusableInTouchMode(false);
        et.setClickable(false);
        et.setLongClickable(false);
        et.setCursorVisible(false);
        et.setTextIsSelectable(false);
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();

        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot doc) {
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
                        String role = doc.getString("role");
                        String gender = doc.getString("gender");
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

                        if (!TextUtils.isEmpty(gender)) {
                            if ("Male".equalsIgnoreCase(gender)) rbMale.setChecked(true);
                            else if ("Female".equalsIgnoreCase(gender)) rbFemale.setChecked(true);
                        }

                        if (!TextUtils.isEmpty(role)) {
                            ArrayAdapter adapter = (ArrayAdapter) spRole.getAdapter();
                            if (adapter != null) {
                                int index = adapter.getPosition(role);
                                if (index >= 0) spRole.setSelection(index);
                            }
                        }

                        currentProfileImageUrl = profileImageUrl;

                        if (getContext() == null) return;

                        if (!TextUtils.isEmpty(profileImageUrl)) {
                            Glide.with(ProfileFragment.this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.ic_profile)
                                    .circleCrop()
                                    .into(imgProfile);
                        } else {
                            Glide.with(ProfileFragment.this)
                                    .load(R.drawable.ic_profile)
                                    .circleCrop()
                                    .into(imgProfile);
                        }
                    }
                });
    }

    private void showLogoutDialog() {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    mAuth.signOut();

                    // CLEAR REMEMBER ME
                    sharedPreferences.edit().clear().apply();

                    if (getActivity() != null) {
                        Intent i = new Intent(getActivity(), LoginActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(i);
                        getActivity().finish();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
