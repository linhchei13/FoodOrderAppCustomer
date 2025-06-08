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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private FirebaseAuth auth;
    private EditText loginEmail;
    private EditText loginPassword;
    private Button loginButton;
    private TextView loginRedirectText;
    private TextView forgotPasswordText;
    private TextView loginErrorText;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // Initialize UI elements
        loginEmail = findViewById(R.id.login_email);
        loginPassword = findViewById(R.id.login_password);
        loginButton = findViewById(R.id.login_button);
        loginRedirectText = findViewById(R.id.loginRedirectText);
        loginErrorText = findViewById(R.id.login_error_text);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);

        // Initialize progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Đang đăng nhập...");
        progressDialog.setCancelable(false);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear any previous error messages
                loginErrorText.setVisibility(View.GONE);
                String email = loginEmail.getText().toString().trim();
                String password = loginPassword.getText().toString().trim();

                if (validateInputs(email, password)) {
                    loginUser(email, password);
                }
            }
        });

        loginRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
                finish();
            }
        });

        // Add forgot password functionality
        forgotPasswordText.setOnClickListener(v -> {
            String email = loginEmail.getText().toString().trim();
            if (email.isEmpty()) {
                loginEmail.setError("Vui lòng nhập email để đặt lại mật khẩu");
                loginEmail.requestFocus();
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                loginEmail.setError("Email không hợp lệ");
                loginEmail.requestFocus();
            } else {
                showResetPasswordDialog(email);
            }
        });
    }

    private boolean validateInputs(String email, String password) {
        boolean isValid = true;

        if (email.isEmpty()) {
            loginEmail.setError("Vui lòng nhập email");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            loginEmail.setError("Email không hợp lệ");
            isValid = false;
        }

        if (password.isEmpty()) {
            loginPassword.setError("Vui lòng nhập mật khẩu");
            isValid = false;
        }

        return isValid;
    }

    private void loginUser(String email, String password) {
        // Show progress dialog
        progressDialog.show();

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            // Sign in success
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = auth.getCurrentUser();
                            Toast.makeText(LoginActivity.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        } else {
                            // If sign in fails, display a message to the user
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Đăng nhập thất bại";
                            
                            // Show error in TextView
                            loginErrorText.setText("Sai email hoặc mật khẩu. Vui lòng kiểm tra lại");
                            loginErrorText.setVisibility(View.VISIBLE);
                            
                        }
                    }
                });
    }

    private String getErrorMessage(String firebaseError) {
        if (firebaseError.contains("password is invalid") || firebaseError.contains("no user record")) {
            return "Email hoặc mật khẩu không đúng";
        } else if (firebaseError.contains("badly formatted")) {
            return "Email không đúng định dạng";
        } else if (firebaseError.contains("network")) {
            return "Lỗi kết nối mạng. Vui lòng kiểm tra lại kết nối";
        } else {
            return "Đăng nhập thất bại. Vui lòng thử lại";
        }
    }

    private void showResetPasswordDialog(String email) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Đặt lại mật khẩu");
        builder.setMessage("Bạn có muốn gửi email đặt lại mật khẩu đến " + email + " không?");
        
        builder.setPositiveButton("Gửi", (dialog, which) -> {
            resetPassword(email);
            dialog.dismiss();
        });
        
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void resetPassword(String email) {
        progressDialog.setMessage("Đang gửi email đặt lại mật khẩu...");
        progressDialog.show();

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        // Show success dialog
                        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(LoginActivity.this);
                        builder.setTitle("Thành công");
                        builder.setMessage("Email đặt lại mật khẩu đã được gửi đến " + email + ". Vui lòng kiểm tra hộp thư của bạn.");
                        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                        builder.show();
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Không thể gửi email đặt lại mật khẩu";
                        
                        // Show error dialog
                        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(LoginActivity.this);
                        builder.setTitle("Lỗi");
                        builder.setMessage(getResetPasswordErrorMessage(errorMessage));
                        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
                        builder.show();
                    }
                });
    }

    private String getResetPasswordErrorMessage(String firebaseError) {
        if (firebaseError.contains("no user record")) {
            return "Không tìm thấy tài khoản với email này";
        } else if (firebaseError.contains("badly formatted")) {
            return "Email không đúng định dạng";
        } else if (firebaseError.contains("network")) {
            return "Lỗi kết nối mạng. Vui lòng kiểm tra lại kết nối";
        } else {
            return "Không thể gửi email đặt lại mật khẩu. Vui lòng thử lại sau";
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // User is already signed in, redirect to MainActivity
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }
}