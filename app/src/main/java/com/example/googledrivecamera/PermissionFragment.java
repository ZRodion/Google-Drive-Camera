package com.example.googledrivecamera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import com.example.googledrivecamera.databinding.FragmentPermissionBinding;

public class PermissionFragment extends Fragment {
    private FragmentPermissionBinding binding;

    ActivityResultLauncher<String> requestLocationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    NavController navController = NavHostFragment.findNavController(this);
                    navController.popBackStack();
                }
            });

    ActivityResultLauncher<String> requestCameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    setLocation();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPermissionBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!checkCameraPermission()) {
            binding.imageView.setImageResource(R.drawable.camera_disabled);
            binding.requestPermissionButton.setOnClickListener(v -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA));
        } else if (!checkLocationPermission()) {
            setLocation();
        }
    }

    private void setLocation(){
        binding.imageView.setImageResource(R.drawable.ic_location_off);
        binding.requestPermissionButton.setOnClickListener(v -> requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION));
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }
}