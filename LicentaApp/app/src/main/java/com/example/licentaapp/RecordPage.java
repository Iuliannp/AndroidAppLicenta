package com.example.licentaapp;

import static android.app.PendingIntent.getActivity;

import android.Manifest;
import android.app.Activity;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;

public class RecordPage extends AppCompatActivity {

    ArrayList<Button> buttonList = new ArrayList<Button>();
    private Button startRecording, stopRecording, uploadFile, selectFile, sendForClassification;
    private EditText displayDuration;
    private MediaRecorder mediaRecorder;
    private String RecordedPath = null;
    private String UploadedPath = null;
    private static final int REQUEST_FILES = 200;
    private static final int RECORDING = 0;
    private static final int UPLOAD = 1;

    private int choice = -1;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_page);

        startRecording = findViewById(R.id.StartRecordingButton);
        buttonList.add(startRecording);

        stopRecording = findViewById(R.id.StopRecordingButton);
        buttonList.add(stopRecording);

        uploadFile = findViewById(R.id.uploadFileButton);
        buttonList.add(uploadFile);

        selectFile = findViewById(R.id.selectFileButton);
        buttonList.add(selectFile);

        sendForClassification = findViewById(R.id.SendForClassificationButton);
        buttonList.add(sendForClassification);

        displayDuration = findViewById(R.id.displayDuration);

        startRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermissionsForAudio()) {

                    mediaRecorder = new MediaRecorder();
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    mediaRecorder.setOutputFile(RecordedPath = getFilePath());

                    try {
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                        isRecording = true;
                        choice = RECORDING;
                        ArrayList<Button> exceptThese = new ArrayList<Button>();
                        exceptThese.add(stopRecording);
                        disableButtons(exceptThese);
                        Toast.makeText(RecordPage.this, "Recording Started", Toast.LENGTH_LONG).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else {
                    ActivityCompat.requestPermissions(RecordPage.this, new String[] {
                            android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },1);
                }
            }
        });

        stopRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    enableAllButtons();

                    Toast.makeText(RecordPage.this, "Recording Stopped", Toast.LENGTH_LONG).show();
                    MediaPlayer mp = MediaPlayer.create(RecordPage.this, Uri.parse(RecordedPath));
                    String duration = formatDuration(mp.getDuration());
                    displayDuration.setText(duration);
                }
                catch (IllegalStateException e) {
                    e.printStackTrace();
                }


            }
        });

        selectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT>=23){
                    if(checkPermissionFileRead()){
                        filePicker();
                    }
                    else{
                        requestPermissionFileRead();
                    }
                }
                else{
                    filePicker();
                }
            }
        });

        sendForClassification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(choice == RECORDING) {
                    Intent intent = new Intent(RecordPage.this, AfterClassificationPage.class);
                    intent.putExtra("path", RecordedPath);
                    Log.d("Path", RecordedPath);
                    startActivity(intent);
                }
                else if (choice == UPLOAD) {
                    Intent intent = new Intent(RecordPage.this, AfterClassificationPage.class);
                    intent.putExtra("path", UploadedPath);
                    Log.d("Path", UploadedPath);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_FILES && resultCode == Activity.RESULT_OK) {
            choice = UPLOAD;
            String filePath = getPathFromUri(data.getData(),RecordPage.this);
            UploadedPath = filePath;
            MediaPlayer mp = MediaPlayer.create(RecordPage.this, Uri.parse(filePath));
            String duration = formatDuration(mp.getDuration());
            displayDuration.setText(duration);
        }
    }

    public String getPathFromUri(Uri uri, Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                ParcelFileDescriptor pfd = activity.getContentResolver().openFileDescriptor(uri, "r");
                if (pfd != null) {
                    FileDescriptor fd = pfd.getFileDescriptor();
                    FileInputStream inputStream = new FileInputStream(fd);
                    File file = new File(activity.getCacheDir(), "uploadedFile.mp3");
                    FileOutputStream outputStream = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, len);
                    }
                    inputStream.close();
                    outputStream.close();
                    return file.getAbsolutePath();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        } else {
            Cursor cursor = activity.getContentResolver().query(uri, null, null, null, null);
            if (cursor == null) {
                return uri.getPath();
            } else {
                cursor.moveToFirst();
                int id = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DATA);
                return cursor.getString(id);
            }
        }
    }

    private void filePicker() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(intent, REQUEST_FILES);
    }

    private String getFilePath() {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File musicDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        File file = new File(musicDirectory, "recordedAudio" + ".mp3");

        return file.getPath();
    }

    private boolean checkPermissionsForAudio() {
        int first = ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.RECORD_AUDIO);
        int second = ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        return first == PackageManager.PERMISSION_GRANTED && second == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionFileRead() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(RecordPage.this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(RecordPage.this, "Give permission to Upload File", Toast.LENGTH_LONG).show();
        }
        else {
            ActivityCompat.requestPermissions(RecordPage.this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},1);
        }
    }

    private boolean checkPermissionFileRead() {
        int result = ContextCompat.checkSelfPermission(RecordPage.this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(RecordPage.this, "Permission Granted", Toast.LENGTH_LONG).show();
            return true;
        } else {
            Toast.makeText(RecordPage.this, "Permission Denied", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    public static String formatDuration(long durationInMillis) {
        long seconds = durationInMillis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;

        return String.format("%d:%02d", minutes, seconds);
    }

    private void disableButtons(ArrayList<Button> exceptThese) {
        for (Button button : buttonList) {
            if(!exceptThese.contains(button)) {
                button.setEnabled(false);
            }
        }
    }

    private void enableAllButtons() {
        for (Button button : buttonList) {
            if (!button.isEnabled()) {
                button.setEnabled(true);
            }
        }
    }
}
