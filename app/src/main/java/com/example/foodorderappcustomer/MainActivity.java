package com.example.foodorderappcustomer;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;

import com.example.foodorderappcustomer.Fragment.AccountFragment;
import com.example.foodorderappcustomer.Fragment.HomeFragment;
import com.example.foodorderappcustomer.Fragment.MessageFragment;
import com.example.foodorderappcustomer.Fragment.OrderFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    FrameLayout frameLayout;
    BottomNavigationView bottomNavigationView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        bottomNavigationView = findViewById(R.id.bottomNav);
        frameLayout = findViewById(R.id.fragment_container);
        getSupportFragmentManager().
                beginTransaction().add(R.id.fragment_container, new HomeFragment()).commit();
        bottomNavigationView.setOnItemSelectedListener(item->
        {
            int itemId = item.getItemId();
            if (itemId == R.id.homeFragment) {
                replaceFragment(new HomeFragment());
            }
            else if (itemId == R.id.orderFragment) {
                replaceFragment(new OrderFragment());
            }
            else if (itemId == R.id.messageFragment) {
                replaceFragment(new MessageFragment());
            } else if (itemId == R.id.accountFragment) {
                replaceFragment(new AccountFragment());
            }
            return false;
        });
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment).commit();
    }
}