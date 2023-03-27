package com.example.googledrivecamera;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        boolean isSuccessful = MessageDialogFragmentArgs.fromBundle(getArguments()).getIsSuccessfully();
        if(!isSuccessful){
            binding.imageView.setImageResource(R.drawable.ic_error);
            binding.messageTextView.setText("All is not ok");
        }
    }
}
