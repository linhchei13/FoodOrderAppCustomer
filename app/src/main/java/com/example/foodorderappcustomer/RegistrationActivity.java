package com.example.foodorderappcustomer;

import android.content.Intent;
import android.os.Bundle;
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

public class RegistrationActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText signupFirstname;
    private EditText signupLastname;
    private EditText signupEmail;
    private EditText signupPassword;
    private EditText rePassword;
    private Button signupButton;

    private TextView signupRedirectText;
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

        auth = FirebaseAuth.getInstance();
        signupFirstname = findViewById(R.id.signup_firstname);
        signupLastname = findViewById(R.id.signup_lastname);
        signupEmail = findViewById(R.id.signup_email);
        signupPassword = findViewById(R.id.signup_password);
        rePassword = findViewById(R.id.re_password);
        signupButton = findViewById(R.id.signup_button);
        signupRedirectText = findViewById(R.id.signupRedirectText);

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String firstName = signupFirstname.getText().toString().trim();
                String lastName = signupLastname.getText().toString().trim();
                String email = signupEmail.getText().toString().trim();
                String password = signupPassword.getText().toString().trim();
                String repassword = rePassword.getText().toString().trim();
                if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || repassword.isEmpty()) {
                    signupFirstname.setError("Vui lòng nhập đầy đủ thông tin");
                    signupLastname.setError("Vui lòng nhập đầy đủ thông tin");
                    signupEmail.setError("Vui lòng nhập đầy đủ thông tin");
                    signupPassword.setError("Vui lòng nhập đầy đủ thông tin");
                    rePassword.setError("Vui lòng nhập đầy đủ thông tin");
                } else {
                    if (password.equals(repassword)) {
                        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(RegistrationActivity.this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
                                } else {
                                    Toast.makeText(RegistrationActivity.this, "Đăng ký thất bại", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                }
            }
        });

        signupRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent( RegistrationActivity.this, LoginActivity.class));
            }
        });
    }
}