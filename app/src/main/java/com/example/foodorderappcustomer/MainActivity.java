package com.example.foodorderappcustomer;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.foodorderappcustomer.Fragment.AccountFragment;
import com.example.foodorderappcustomer.Fragment.HomeFragment;
import com.example.foodorderappcustomer.Fragment.HistoryFragment;
import com.example.foodorderappcustomer.util.CartManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    FrameLayout frameLayout;
    BottomNavigationView bottomNavigationView;
    private CartManager cartManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        setContentView(R.layout.activity_main);

        View bottomNav = findViewById(R.id.bottomNav);

        ViewCompat.setOnApplyWindowInsetsListener(bottomNav, (v, insets) -> {
            int bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(0, 0, 0, bottom);
            return WindowInsetsCompat.CONSUMED;
        });


        // Initialize CartManager
        cartManager = CartManager.getInstance(this);

        bottomNavigationView = findViewById(R.id.bottomNav);
        frameLayout = findViewById(R.id.fragment_container);

        getSupportFragmentManager().
                    beginTransaction().add(R.id.fragment_container, new HomeFragment()).commit();

        bottomNavigationView.setOnItemSelectedListener(item->
        {
            int itemId = item.getItemId();
            if (itemId == R.id.homeFragment) {
                replaceFragment(new HomeFragment());
                return true;
            }
            else if (itemId == R.id.orderFragment) {
                replaceFragment(new HistoryFragment());
                return true;
            }
//            else if (itemId == R.id.messageFragment) {
//                replaceFragment(new MessageFragment());
             else if (itemId == R.id.accountFragment) {
                replaceFragment(new AccountFragment());
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        updateCartBadge(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_cart) {
            // Open cart activity
            Intent intent = new Intent(this, CartActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateCartBadge(Menu menu) {
        MenuItem cartItem = menu.findItem(R.id.menu_cart);
        if (cartItem != null) {
            int itemCount = cartManager.getItemCount();
            if (itemCount > 0) {
                cartItem.setTitle("Giỏ hàng (" + itemCount + ")");
            } else {
                cartItem.setTitle("Giỏ hàng");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu(); // Refresh the menu to update cart badge
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment).commit();
    }
}