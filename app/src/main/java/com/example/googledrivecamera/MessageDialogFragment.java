package com.example.googledrivecamera;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.googledrivecamera.databinding.FragmentMessageBinding;

public class MessageDialogFragment extends DialogFragment {
    private FragmentMessageBinding binding;

    public MessageDialogFragment() {
        super(R.layout.fragment_message);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMessageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        boolean isSuccessful = MessageDialogFragmentArgs.fromBundle(getArguments()).getIsSuccessfully();

        if(!isSuccessful){
            binding.imageView.setImageResource(R.drawable.ic_error);
            binding.messageTextView.setText("All is not ok");
        }

        NavController navController = NavHostFragment.findNavController(this);
        getDialog().setOnCancelListener(dialog -> {
            if(isSuccessful){
                navController.navigate(MessageDialogFragmentDirections.actionMessageToCamera());
            }else{
                navController.popBackStack();
            }
        });
        /*boolean isSuccessful = MessageDialogFragmentArgs.fromBundle(getArguments()).getIsSuccessfully();
        if(!isSuccessful){
            binding.imageView.setImageResource(R.drawable.ic_error);
            binding.messageTextView.setText("All is not ok");
        }else{
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(MessageDialogFragmentDirections.actionMessageToCamera());
        }*/
    }
}
