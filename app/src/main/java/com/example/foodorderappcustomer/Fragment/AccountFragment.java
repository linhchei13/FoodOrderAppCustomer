package com.example.foodorderappcustomer.Fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.foodorderappcustomer.EditProfileActivity;
import com.example.foodorderappcustomer.LoginActivity;
import com.example.foodorderappcustomer.R;
import com.example.foodorderappcustomer.util.ImageUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;


public class AccountFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;
    private TextView nameTextView;
    private TextView emailTextView, phoneTextView;
    private Button logoutButton, editProfileButton;
    private SwipeRefreshLayout swipeRefreshLayout;
    private StorageReference storageReference;
    private ImageView profileImageView;

    public AccountFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference();

        // Initialize UI elements
        nameTextView = view.findViewById(R.id.nameTextView);
        emailTextView = view.findViewById(R.id.emailTextView);
        phoneTextView = view.findViewById(R.id.phoneTextView);
        logoutButton = view.findViewById(R.id.logoutButton);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        editProfileButton = view.findViewById(R.id.editProfileButton);
        profileImageView = view.findViewById(R.id.profileImageView);
        // Set up refresh listener
        swipeRefreshLayout.setOnRefreshListener(this::loadUserData);

        // Set up logout button
        logoutButton.setOnClickListener(v -> signOut());
        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            startActivity(intent);
        });
        // Load user data
        loadUserData();

    }

    private void loadUserData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // Show loading indicator
            swipeRefreshLayout.setRefreshing(true);

            databaseReference.child("users").child(currentUser.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            swipeRefreshLayout.setRefreshing(false);
                            
                            if (dataSnapshot.exists()) {
                                // Get user's name
                                String firstName = dataSnapshot.child("firstName").getValue(String.class);
                                String lastName = dataSnapshot.child("lastName").getValue(String.class);
                                String email = dataSnapshot.child("email").getValue(String.class);
                                String phone = dataSnapshot.child("phone").getValue(String.class);
                                String profile_url = dataSnapshot.child("profileImageUrl").getValue(String.class);
                                ImageUtils.loadImage(getContext(), profile_url, profileImageView, R.drawable.baseline_person_24, R.drawable.baseline_person_24);
                                if (firstName != null && lastName != null) {
                                    nameTextView.setText(firstName + " " + lastName);
                                } else {
                                    nameTextView.setText(currentUser.getEmail());
                                }
                                
                                if (email != null) {
                                    emailTextView.setText(email);
                                } else {
                                    emailTextView.setText(currentUser.getEmail());
                                }
                                if (phone != null) {
                                    phoneTextView.setText(phone);
                                } else {
                                    phoneTextView.setText(currentUser.getEmail());
                                }
                            } else {
                                // No user data in database, use info from auth
                                nameTextView.setText(currentUser.getEmail());
                                emailTextView.setText(currentUser.getEmail());
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            swipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(getContext(), "Lỗi: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // User not logged in
            nameTextView.setText("Chưa đăng nhập");
            emailTextView.setText("Vui lòng đăng nhập để xem thông tin");
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void signOut() {
        // Sign out from Firebase
        auth.signOut();
        
        // Show success message
        Toast.makeText(getContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show();
        
        // Redirect to login screen
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh user data when returning to this fragment
        loadUserData();
    }
}