package com.example.licentaapp;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.licentaapp.ml.Model;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class AfterClassificationPage extends AppCompatActivity {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.after_classification_page);

        textView = findViewById(R.id.textView);
        Intent intent = getIntent();
        String pathToAudioFile = intent.getStringExtra("path");
        textView.setText(pathToAudioFile);
        Log.d("Path", pathToAudioFile);

        try {
            Model model = Model.newInstance(getApplicationContext());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
