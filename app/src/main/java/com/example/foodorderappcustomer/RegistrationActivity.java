package com.example.foodorderappcustomer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.foodorderappcustomer.Models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class RegistrationActivity extends AppCompatActivity {

    private static final String TAG = "RegistrationActivity";

    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private EditText signupFirstname;
    private EditText signupLastname;
    private EditText signupEmail;
    private EditText signupPhone;
    private EditText signupPassword;
    private EditText rePassword;
    private Button signupButton;
    private TextView signupRedirectText;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registration);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth and Database
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Initialize UI elements
        signupFirstname = findViewById(R.id.signup_firstname);
        signupLastname = findViewById(R.id.signup_lastname);
        signupEmail = findViewById(R.id.signup_email);
        signupPhone = findViewById(R.id.signup_phone);
        signupPassword = findViewById(R.id.signup_password);
        rePassword = findViewById(R.id.re_password);
        signupButton = findViewById(R.id.signup_button);
        signupRedirectText = findViewById(R.id.signupRedirectText);

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang đăng ký...");
        progressDialog.setCancelable(false);

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String firstName = signupFirstname.getText().toString().trim();
                String lastName = signupLastname.getText().toString().trim();
                String email = signupEmail.getText().toString().trim();
                String phone = signupPhone.getText().toString().trim();
                String password = signupPassword.getText().toString().trim();
                String repassword = rePassword.getText().toString().trim();

                if (validateInputs(firstName, lastName, email, phone, password, repassword)) {
                    registerUser(firstName, lastName, email, phone, password);
                }
            }
        });

        signupRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private boolean validateInputs(String firstName, String lastName, String email, String phone, String password, String repassword) {
        boolean isValid = true;

        if (firstName.isEmpty()) {
            signupFirstname.setError("Vui lòng nhập họ");
            isValid = false;
        }

        if (lastName.isEmpty()) {
            signupLastname.setError("Vui lòng nhập tên");
            isValid = false;
        }

        if (email.isEmpty()) {
            signupEmail.setError("Vui lòng nhập email");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            signupEmail.setError("Email không hợp lệ");
            isValid = false;
        }

        if (phone.isEmpty()) {
            signupPhone.setError("Vui lòng nhập số điện thoại");
            isValid = false;
        } else if (!isValidPhoneNumber(phone)) {
            signupPhone.setError("Số điện thoại không hợp lệ");
            isValid = false;
        }

        if (password.isEmpty()) {
            signupPassword.setError("Vui lòng nhập mật khẩu");
            isValid = false;
        } else if (password.length() < 6) {
            signupPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            isValid = false;
        }

        if (repassword.isEmpty()) {
            rePassword.setError("Vui lòng nhập lại mật khẩu");
            isValid = false;
        } else if (!password.equals(repassword)) {
            rePassword.setError("Mật khẩu không khớp");
            isValid = false;
        }

        return isValid;
    }

    private boolean isValidPhoneNumber(String phone) {
        // Remove any spaces or special characters
        phone = phone.replaceAll("[^0-9+]", "");
        
        // Check if the phone number starts with +84 or 0
        if (phone.startsWith("+84")) {
            phone = "0" + phone.substring(3);
        }
        
        // Vietnamese phone number validation
        // Must start with 0 and have 10 digits
        return phone.matches("^0[0-9]{9}$");
    }

    private void registerUser(String firstName, String lastName, String email, String phone, String password) {
        // Show progress dialog
        progressDialog.show();

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Get the current user
                            FirebaseUser firebaseUser = auth.getCurrentUser();

                            if (firebaseUser != null) {
                                // Create a User object
                                User user = new User(
                                        firebaseUser.getUid(),
                                        firstName,
                                        lastName,
                                        email,
                                        phone
                                );

                                // Save user data to the database
                                saveUserToDatabase(user);
                            } else {
                                progressDialog.dismiss();
                                Toast.makeText(RegistrationActivity.this, "Đăng ký thành công nhưng không thể lưu thông tin người dùng", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
                                finish();
                            }
                        } else {
                            // If sign up fails, display a message to the user
                            progressDialog.dismiss();
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Đăng ký thất bại";
                            Toast.makeText(RegistrationActivity.this, "Đăng ký thất bại: " + errorMessage, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Registration failed: " + errorMessage);
                        }
                    }
                });
    }

    private void saveUserToDatabase(User user) {
        // Create a map of user data
        Map<String, Object> userValues = user.toMap();

        // Save user data to both "users" and "customers" nodes for compatibility
        Map<String, Object> childUpdates = new HashMap<>();
        childUpdates.put("/users/" + user.getUserId(), userValues);
        childUpdates.put("/customers/" + user.getUserId(), userValues);

        databaseReference.updateChildren(childUpdates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            Toast.makeText(RegistrationActivity.this, "Lưu thành công", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
                            finish();
                        } else {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Không thể lưu thông tin người dùng";
                            Toast.makeText(RegistrationActivity.this, "Không thể lưu thông tin người dùng: " + errorMessage, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "Failed to save user data: " + errorMessage);
                        }
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, redirect to MainActivity
            startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
            finish();
        }
    }
}