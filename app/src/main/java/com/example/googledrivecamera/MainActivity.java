package com.example.googledrivecamera;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.googledrivecamera.databinding.ActivityMainBinding;

/*
добавить отображение иконки аккаунта
добавить возможность сменить аккаунт
изменить формат сохранения текста
Добавить удаление файлов после отправки
Добавить локализацию
Добавить надписи с тем, что происходит в приложении
Исправить макет с уведами о выполнении
*/

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());


        setContentView(binding.getRoot());
    }
}