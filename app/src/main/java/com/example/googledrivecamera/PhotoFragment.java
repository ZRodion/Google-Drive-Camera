package com.example.googledrivecamera;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import com.example.googledrivecamera.databinding.FragmentPhotoBinding;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;

/*
1. get google account
2. get location
3. get network state
 */

public class PhotoFragment extends Fragment {
    private FragmentPhotoBinding binding;

    private static final String TAG = "MyTag";

    FusedLocationProviderClient mFusedLocationClient;
    Location userLocation;
    Uri photoUri;

    private DriveServiceHelper mDriveServiceHelper;
    private String mOpenFileId;

    ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                handleSignInResult(result.getData());
            });


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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        photoUri = PhotoFragmentArgs.fromBundle(getArguments()).getPhotoUri();
        binding.photoView.setImageURI(photoUri);

        binding.cancelButton.setBackgroundResource(0);
        binding.cloudButton.setBackgroundResource(0);

        binding.cancelButton.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.popBackStack();
            /*Bitmap bitmap = ((BitmapDrawable) binding.photoView.getDrawable()).getBitmap();

            createImageFile(bitmap);*/
        });

        binding.cloudButton.setOnClickListener(v -> {
            loadingState(true);

            requestSignIn();
        });
    }

    private void requestSignIn() {

        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                        .build();
        GoogleSignInClient client = GoogleSignIn.getClient(requireContext(), signInOptions);

        signInLauncher.launch(client.getSignInIntent());
    }

    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
                .addOnSuccessListener(googleAccount -> {
                    Log.d(TAG, "Signed in as " + googleAccount.getEmail());

                    GoogleAccountCredential credential =
                            GoogleAccountCredential.usingOAuth2(
                                    requireContext(), Collections.singleton(DriveScopes.DRIVE_FILE));
                    credential.setSelectedAccount(googleAccount.getAccount());
                    Drive googleDriveService =
                            new Drive.Builder(
                                    AndroidHttp.newCompatibleTransport(),
                                    new GsonFactory(),
                                    credential)
                                    .setApplicationName("Google Drive Camera")
                                    .build();

                    mDriveServiceHelper = new DriveServiceHelper(googleDriveService);

                    if (isLocationEnabled()) {
                        getLocation();
                    } else {
                        loadingState(false);
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(exception -> {
                    Toast.makeText(requireContext(), "SignIn is error", Toast.LENGTH_SHORT);
                    toMessageFragment(false);
                    loadingState(false);
                });
    }

    @SuppressLint("MissingPermission")
    private void getLocation() {
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

        Task<Location> locationTask = mFusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token);
        locationTask.addOnSuccessListener(location -> {
            if (location == null) {
                Toast.makeText(requireContext(), "Cannot get location.", Toast.LENGTH_SHORT).show();
                toMessageFragment(false);
                loadingState(false);
            } else {
                userLocation = location;

                checkNetworkState();
            }
        });
    }

    private void checkNetworkState() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnected()) {
            Toast.makeText(requireContext(), "Network is off", Toast.LENGTH_SHORT).show();
            toMessageFragment(false);
            loadingState(false);
        } else {
            Bitmap bitmap = ((BitmapDrawable) binding.photoView.getDrawable()).getBitmap();

            createFile(bitmap);
        }
    }


    private void createFile(Bitmap bitmap) {
        String fileName = getFileNameFromUri(photoUri).split("\\.")[0];

        String content = "Latitude: " + userLocation.getLatitude() + ", Longitude: " + userLocation.getLongitude() +
                "\nFile name: " + fileName;

        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Creating a file.");

            mDriveServiceHelper.createFile(bitmap, content, fileName)
                    .addOnSuccessListener(fileId -> {
                        toMessageFragment(true);
                    })
                    .addOnFailureListener(exception -> {
                        toMessageFragment(false);
                        Toast.makeText(requireContext(), "Files is not saved", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.d(TAG, "dont Creating a file.");
        }
    }

    public String getFileNameFromUri(Uri uri) {
        String fileName = null;
        if (uri.getScheme().equals("content")) {
            ContentResolver contentResolver = requireContext().getContentResolver();
            Cursor cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }
        }
        if (fileName == null) {
            fileName = uri.getLastPathSegment();
        }
        return fileName;
    }

    private void loadingState(boolean isLoading) {
        if (isLoading) {
            binding.photoView.setAlpha(0.5f);
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.cancelButton.setEnabled(false);
            binding.cloudButton.setEnabled(false);
        } else {
            binding.photoView.setAlpha(1f);
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.cancelButton.setEnabled(true);
            binding.cloudButton.setEnabled(true);
        }
    }

    private void toMessageFragment(boolean isSuccessful) {
        NavController navController = NavHostFragment.findNavController(this);
        NavDirections action = PhotoFragmentDirections.actionPhotoToMessage(isSuccessful);
        navController.navigate(action);
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
}