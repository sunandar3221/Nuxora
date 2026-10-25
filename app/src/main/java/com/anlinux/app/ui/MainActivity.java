package com.anlinux.app.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.anlinux.app.R;
import com.anlinux.app.ui.catalog.CatalogFragment;
import com.anlinux.app.ui.distros.InstalledDistrosFragment;
import com.anlinux.app.ui.settings.SettingsFragment;
import com.anlinux.app.ui.tools.ToolsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private final InstalledDistrosFragment installedFragment = new InstalledDistrosFragment();
    private final CatalogFragment catalogFragment = new CatalogFragment();
    private final ToolsFragment toolsFragment = new ToolsFragment();
    private final SettingsFragment settingsFragment = new SettingsFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNavigation);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_distros) {
                switchFragment(installedFragment);
                return true;
            } else if (itemId == R.id.navigation_catalog) {
                switchFragment(catalogFragment);
                return true;
            } else if (itemId == R.id.navigation_tools) {
                switchFragment(toolsFragment);
                return true;
            } else if (itemId == R.id.navigation_settings) {
                switchFragment(settingsFragment);
                return true;
            }
            return false;
        });

        // Default tab
        if (savedInstanceState == null) {
            switchFragment(installedFragment);
        }

        checkAndRequestPermissions();
    }

    private void switchFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    public void navigateToCatalog() {
        bottomNav.setSelectedItemId(R.id.navigation_catalog);
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }
}
