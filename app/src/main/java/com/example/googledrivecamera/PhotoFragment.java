package com.example.googledrivecamera;

import android.annotation.SuppressLint;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.example.googledrivecamera.databinding.FragmentPhotoBinding;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.android.gms.tasks.Task;

public class PhotoFragment extends Fragment {
    private FragmentPhotoBinding binding;

    FusedLocationProviderClient mFusedLocationClient;

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPhotoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Uri photoUri = PhotoFragmentArgs.fromBundle(getArguments()).getPhotoUri();
        binding.photoView.setImageURI(photoUri);

        binding.cancelButton.setBackgroundResource(0);
        binding.cloudButton.setBackgroundResource(0);

        binding.cancelButton.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.popBackStack();
        });

        binding.cloudButton.setOnClickListener(v -> {
            loading(true);

            CancellationToken token = new CancellationToken() {
                @NonNull
                @Override
                public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
                    return new CancellationTokenSource().getToken();
                }
                @Override
                public boolean isCancellationRequested() {
                    return false;
                }
            };

            NavController navController = NavHostFragment.findNavController(this);

            Task<Location> task = mFusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token);
            task.addOnSuccessListener(location -> {
                if (location == null) {
                    Toast.makeText(requireContext(), "Cannot get location.", Toast.LENGTH_SHORT).show();

                    loading(false);

                    NavDirections action = PhotoFragmentDirections.actionPhotoToMessage(false);
                    navController.navigate(action);
                } else {
                    double lat = location.getLatitude();
                    double lon = location.getLongitude();

                    loading(false);

                    Toast.makeText(requireContext(), "Current location: " + lat + " " + lon, Toast.LENGTH_SHORT).show();

                    NavDirections action = PhotoFragmentDirections.actionPhotoToMessage(true);
                    navController.navigate(action);
                }
            });
        });
    }

    private void loading(boolean isLoading){
        if(isLoading){
            binding.photoView.setAlpha(0.5f);
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.cancelButton.setEnabled(false);
            binding.cloudButton.setEnabled(false);
        }else{
            binding.photoView.setAlpha(1f);
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.cancelButton.setEnabled(true);
            binding.cloudButton.setEnabled(true);
        }
    }
}