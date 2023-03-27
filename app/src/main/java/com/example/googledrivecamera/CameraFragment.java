package com.example.googledrivecamera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.icu.text.SimpleDateFormat;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.example.googledrivecamera.databinding.FragmentCameraBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CameraFragment extends Fragment {
    private FragmentCameraBinding binding;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

    private File outputDirectory;
    private ExecutorService cameraExecutor;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        outputDirectory = getOutputDirectory();
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        checkPermission();
        binding = FragmentCameraBinding.inflate(inflater, container, false);



        return binding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("MyTag", "onViewCeated");

        Animation scaleUp, scaleDown;
        scaleUp = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_up);
        scaleDown = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_down);

        binding.cameraCaptureButton.setOnTouchListener((View.OnTouchListener) (v, event) -> {
            if(event.getAction() == MotionEvent.ACTION_UP){
                binding.cameraCaptureButton.startAnimation(scaleDown);
            } else if (event.getAction() == MotionEvent.ACTION_DOWN){
                binding.cameraCaptureButton.startAnimation(scaleUp);
            }
            return false;
        });
        binding.cameraCaptureButton.setOnClickListener(v -> {
            if(isLocationEnabled()){
                takePhoto();
            }else{
                Toast.makeText(requireContext(), "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        binding.cameraswitchImageView.setBackgroundResource(0);
        binding.cameraswitchImageView.setOnClickListener(v -> {
            cameraSelector = cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA ?
                    CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;
            startCamera(cameraSelector);
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }

    private void checkPermission() {
        int cameraPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
        );
        int locationPermission = ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
        );

        if ( cameraPermission != PackageManager.PERMISSION_GRANTED || locationPermission != PackageManager.PERMISSION_GRANTED
        ) {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_camera_to_permission);
        } else {
            hideStatusBar();

            cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
            startCamera(cameraSelector);
        }
    }

    private void startCamera(CameraSelector cameraSelector) {
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("TAG", "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        Log.d("MyTag", "takePhoto");

        ImageCapture imageCapture = this.imageCapture;
        if (imageCapture == null) {
            return;
        }

        File photoFile = new File(
                outputDirectory,
                new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        );

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        CameraFragment fragment = this;

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Uri savedUri = Uri.fromFile(photoFile);

                        if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                            rotatePicture(savedUri, photoFile);
                        }

                        NavDirections action = CameraFragmentDirections.actionCameraToPhoto(savedUri);

                        NavController navController = NavHostFragment.findNavController(fragment);
                        navController.navigate(action);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        String msg = "Photo capture failed: " + exception.getMessage();
                        Toast.makeText(requireContext().getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                        Log.e("MyTag", msg, exception);
                    }
                }
        );
    }

    private File getOutputDirectory() {
        File mediaDir = null;
        File[] externalMediaDirs = requireContext().getExternalMediaDirs();
        if (externalMediaDirs.length > 0) {
            File externalMediaDir = externalMediaDirs[0];
            mediaDir = new File(externalMediaDir, getResources().getString(R.string.app_name));
            if (!mediaDir.mkdirs() && !mediaDir.exists()) {
                mediaDir = null;
            }
        }
        if (mediaDir == null || !mediaDir.exists()) {
            mediaDir = requireContext().getFilesDir();
        }
        return mediaDir;
    }

    private void rotatePicture(Uri savedUri, File photoFile) {
        try {
            Bitmap bInput = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), savedUri);

            Bitmap bOutput;
            Matrix matrix = new Matrix();
            matrix.preScale(-1.0f, 1.0f);
            bOutput = Bitmap.createBitmap(bInput, 0, 0, bInput.getWidth(), bInput.getHeight(), matrix, true);

            FileOutputStream fos = new FileOutputStream(photoFile);

            bOutput.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            fos.flush();
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void hideStatusBar(){
        Window window = getActivity().getWindow();
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        View decorView = getActivity().getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
}