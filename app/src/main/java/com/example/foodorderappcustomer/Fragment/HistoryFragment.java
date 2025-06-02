package com.example.foodorderappcustomer.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.foodorderappcustomer.R;
import com.google.android.material.tabs.TabLayout;

public class HistoryFragment extends Fragment {
    private TabLayout tabLayout;
    private FrameLayout fragmentContainer;
    private static final String[] TAB_TITLES = {
            "Chờ xác nhận",
            "Đang giao",
            "Deal đã mua",
            "Lịch sử",
            "Đánh giá"
    };
    private static final String[] ORDER_STATUSES = {
            "pending",
            "contacted",
            "completed",
            "all",
            "rated"
    };

    private void replaceFragment(Fragment fragment) {
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment).commit();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize TabLayout
        tabLayout = view.findViewById(R.id.tabLayout);
        setupTabLayout();

        // Load initial fragment
        loadFragment(0);
    }

    private void setupTabLayout() {
        // Remove existing tabs
        tabLayout.removeAllTabs();

        // Add tabs
        for (String title : TAB_TITLES) {
            tabLayout.addTab(tabLayout.newTab().setText(title));
        }

        // Set up tab selection listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadFragment(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Not needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Not needed
            }
        });
    }

    private void loadFragment(int position) {
        if (position == 4) {
            FeedbackFragment fragment = new FeedbackFragment();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
        } else {
            String status = ORDER_STATUSES[position];
            OrderListFragment fragment = OrderListFragment.newInstance(status);

            getChildFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .commit();
        }
    }
}
