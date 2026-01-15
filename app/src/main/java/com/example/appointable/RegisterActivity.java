package com.example.appointable;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private Button btnRegister;
    private EditText etFirstname, etLastname, etMiddlename, etSuffix;
    private EditText etBirthdate, etAge, etContact;
    private EditText etEmail, etUsername, etPassword, etConfirmPassword;
    private Spinner spinnerRole;

    private ImageView ivTogglePassword, ivToggleConfirmPassword;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    private RadioGroup rgGender;
    private RadioButton rbMale, rbFemale;

    private static final String DEFAULT_PROFILE_IMAGE_URL =
            "https://res.cloudinary.com/djqcwj12e/image/upload/v1763541795/default-profile_ro3fld.jpg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeUI();
        setupPasswordToggles();
        setupLoginClickable();
        setupBirthdatePicker();

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void initializeUI() {

        btnRegister = findViewById(R.id.btnRegister);

        etFirstname = findViewById(R.id.etFirstname);
        etLastname = findViewById(R.id.etLastname);
        etMiddlename = findViewById(R.id.etMiddlename);
        etSuffix = findViewById(R.id.etSuffix);

        etBirthdate = findViewById(R.id.etBirthdate);
        etAge = findViewById(R.id.etAge);
        etContact = findViewById(R.id.etContact);

        etEmail = findViewById(R.id.etEmail);
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etconfirmPassword);

        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);

        spinnerRole = findViewById(R.id.spinnerRole);

        rgGender = findViewById(R.id.rgGender);
        rbMale = findViewById(R.id.rbMale);
        rbFemale = findViewById(R.id.rbFemale);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.role_list,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);
    }

    private void setupPasswordToggles() {
        ivTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivTogglePassword.setImageResource(R.drawable.ic_eye);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivTogglePassword.setImageResource(R.drawable.ic_eye_closed);
            }
            etPassword.setSelection(etPassword.getText().length());
            isPasswordVisible = !isPasswordVisible;
        });

        ivToggleConfirmPassword.setOnClickListener(v -> {
            if (isConfirmPasswordVisible) {
                etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye);
            } else {
                etConfirmPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivToggleConfirmPassword.setImageResource(R.drawable.ic_eye_closed);
            }
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
        });
    }

    private void setupLoginClickable() {
        TextView tvLogin = findViewById(R.id.tvLogin);
        String fullText = "Already have an account? Login";

        SpannableString span = new SpannableString(fullText);

        int start = fullText.indexOf("Login");
        int end = start + "Login".length();

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(android.view.View widget) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        };

        span.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new ForegroundColorSpan(Color.BLUE), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvLogin.setText(span);
        tvLogin.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setupBirthdatePicker() {
        etBirthdate.setOnClickListener(v -> {
            final Calendar c = Calendar.getInstance();

            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String date = (selectedMonth + 1) + "/" + selectedDay + "/" + selectedYear;
                        etBirthdate.setText(date);

                        int age = calculateAge(selectedYear, selectedMonth, selectedDay);
                        etAge.setText(String.valueOf(age));
                    },
                    year, month, day
            );

            dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            dialog.show();
        });
    }

    private int calculateAge(int y, int m, int d) {
        Calendar dob = Calendar.getInstance();
        dob.set(y, m, d);

        Calendar today = Calendar.getInstance();
        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        return age;
    }

    private String generateUserId() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        String yearPart = String.valueOf(year);

        Random random = new Random();
        int randomNumber = random.nextInt(1_000_000);
        String randomPart = String.format("%06d", randomNumber);

        return yearPart + randomPart;
    }

    private void clearFieldErrors() {
        etFirstname.setError(null);
        etLastname.setError(null);
        etBirthdate.setError(null);
        etContact.setError(null);
        etEmail.setError(null);
        etUsername.setError(null);
        etPassword.setError(null);
        etConfirmPassword.setError(null);
    }

    private void setLoading(boolean loading) {
        if (loading) {
            btnRegister.setEnabled(false);
            btnRegister.setText("Registering...");
        } else {
            btnRegister.setEnabled(true);
            btnRegister.setText("Register");
        }
    }

    private boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) return false;

        boolean hasUpper = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            }
            if ("!@#$%^&*()_+-={}[]|:;\"'<>,.?/".indexOf(c) >= 0) {
                hasSpecial = true;
            }
        }
        return hasUpper && hasSpecial;
    }

    private boolean isValidContact(String contact) {
        if (contact == null) return false;
        return contact.matches("\\d{11,12}");
    }

    private void registerUser() {

        clearFieldErrors();

        String first = etFirstname.getText().toString().trim();
        String last = etLastname.getText().toString().trim();
        String middle = etMiddlename.getText().toString().trim();
        String suffix = etSuffix.getText().toString().trim();
        String birthdate = etBirthdate.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String contact = etContact.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();

        int selectedGenderId = rgGender.getCheckedRadioButtonId();
        String genderTemp = null;
        if (selectedGenderId == R.id.rbMale) {
            genderTemp = "Male";
        } else if (selectedGenderId == R.id.rbFemale) {
            genderTemp = "Female";
        }
        final String gender = genderTemp;

        String role = spinnerRole.getSelectedItem() != null
                ? spinnerRole.getSelectedItem().toString()
                : "";

        boolean isValid = true;

        if (first.isEmpty()) {
            etFirstname.setError("First name is required");
            isValid = false;
        }

        if (last.isEmpty()) {
            etLastname.setError("Last name is required");
            isValid = false;
        }

        if (birthdate.isEmpty()) {
            etBirthdate.setError("Birthdate is required");
            isValid = false;
        }

        if (contact.isEmpty()) {
            etContact.setError("Contact number is required");
            isValid = false;
        }

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            isValid = false;
        }

        if (username.isEmpty()) {
            etUsername.setError("Username is required");
            isValid = false;
        }

        if (pass.isEmpty()) {
            etPassword.setError("Password is required");
            isValid = false;
        }

        if (confirm.isEmpty()) {
            etConfirmPassword.setError("Please confirm your password");
            isValid = false;
        }

        if (gender == null) {
            Toast.makeText(this, "Please select gender.", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (role.isEmpty()
                || spinnerRole.getSelectedItemPosition() == 0
                || "--Select Role--".equals(role)) {
            Toast.makeText(this, "Please select a valid role.", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (!contact.isEmpty() && !isValidContact(contact)) {
            etContact.setError("Contact number must be 11 or 12 digits.");
            isValid = false;
        }

        if (!pass.isEmpty() && !isValidPassword(pass)) {
            etPassword.setError("Min 8 chars, 1 capital letter, and 1 special character.");
            isValid = false;
        }

        if (!pass.isEmpty() && !confirm.isEmpty() && !pass.equals(confirm)) {
            etConfirmPassword.setError("Passwords do not match.");
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (!isValid) return;

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener(auth -> {
                    if (auth.getUser() == null) {
                        setLoading(false);
                        Toast.makeText(this, "Registration failed: user is null.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    String uid = auth.getUser().getUid();
                    String userId = generateUserId();

                    Map<String, Object> user = new HashMap<>();
                    user.put("uid", uid);
                    user.put("userId", userId);
                    user.put("firstName", first);
                    user.put("lastName", last);
                    user.put("middleName", middle);
                    user.put("suffix", suffix);
                    user.put("birthdate", birthdate);
                    user.put("age", age);
                    user.put("contact", contact);
                    user.put("email", email);
                    user.put("username", username);
                    user.put("role", role);
                    user.put("gender", gender);
                    user.put("status", "Active");
                    user.put("profileImageUrl", DEFAULT_PROFILE_IMAGE_URL);

                    db.collection("users").document(uid)
                            .set(user)
                            .addOnSuccessListener(a -> {
                                Toast.makeText(this, "Registered successfully!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, LoginActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this, "Error saving user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });

                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
