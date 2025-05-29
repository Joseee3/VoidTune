package com.example.voidtune.Activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.voidtune.entities.LikeSong;
import com.example.voidtune.entities.Playlist;
import com.example.voidtune.entities.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.voidtune.R;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAPTURE_IMAGE_REQUEST = 2;
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    private Uri imageUri;
    private Uri photoUri;

    private ImageView profileImageView;
    private EditText usernameEditText, emailEditText, passwordEditText;
    private Button selectImageButton, registerButton, takePhotoButton;


    private static final int CAMERA_REQUEST_CODE = 1000;
    private FirebaseAuth auth;
    private DatabaseReference databaseReference;


    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_user);

        // Inicializar vistas
        profileImageView = findViewById(R.id.profileImageView);
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        selectImageButton = findViewById(R.id.selectImageButton);
        takePhotoButton = findViewById(R.id.takePhotoButton);
        registerButton = findViewById(R.id.Registeruser);

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("users");

        // Acción para seleccionar imagen
        selectImageButton.setOnClickListener(v -> openImageSelector());

        // Acción para tomar foto
        takePhotoButton.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                openCamera();
            } else {
                requestCameraPermission();
            }
        });

        // Acción para registrar usuario
        registerButton.setOnClickListener(v -> registerUser());
    }

    private void openImageSelector() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Selecciona una imagen"), PICK_IMAGE_REQUEST);
    }



    private void openCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            try {
                File file = createImageFile();
                if (file != null) {
                    photoUri = FileProvider.getUriForFile(this, "com.example.voidtune.fileprovider", file);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    startActivityForResult(intent, CAMERA_REQUEST_CODE);
                }
            } catch (IOException e) {
                Toast.makeText(this, "Error creating file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Permiso de cámara denegado.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
                imageUri = data.getData(); // URI de la imagen seleccionada
                profileImageView.setImageURI(imageUri); // Mostrar la imagen seleccionada
            } else if (requestCode == CAMERA_REQUEST_CODE) {
                imageUri = photoUri; // Actualizar imageUri con la URI de la foto tomada
                profileImageView.setImageURI(imageUri); // Mostrar la imagen capturada
            }
        }
    }

   private void registerUser() {
       String username = usernameEditText.getText().toString().trim();
       String email = emailEditText.getText().toString().trim();
       String password = passwordEditText.getText().toString().trim();

       if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || imageUri == null) {
           Toast.makeText(this, "Please complete all fields and select an image.", Toast.LENGTH_SHORT).show();
           return;
       }

       // Mostrar spinner y deshabilitar botones
       showLoading(true);

       auth.createUserWithEmailAndPassword(email, password)
           .addOnCompleteListener(task -> {
               if (task.isSuccessful()) {
                   FirebaseUser firebaseUser = auth.getCurrentUser();
                   if (firebaseUser != null) {
                       String userId = firebaseUser.getUid();

                       // Create initial user structure
                       HashMap<String, Object> userData = new HashMap<>();
                       userData.put("email", email);
                       userData.put("profileImage", imageUri.toString());
                       userData.put("username", username);


                       // Save data to Firebase
                       databaseReference.child(userId).setValue(userData)
                           .addOnCompleteListener(dbTask -> {
                               if (dbTask.isSuccessful()) {
                                   Toast.makeText(this, "User registered successfully.", Toast.LENGTH_SHORT).show();
                                   finish(); // Close activity
                               } else {
                                   Log.e("Firebase", "Error saving data", dbTask.getException());
                                   Toast.makeText(this, "Error saving data: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                               }
                           });
                   }
               } else {
                   // Ocultar spinner
                   showLoading(false);
                   Log.e("Firebase", "Error registering user", task.getException());
                   Toast.makeText(this, "Error registrando usuario: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
               }
           });
   }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            registerButton.setEnabled(false);
            selectImageButton.setEnabled(false);
            takePhotoButton.setEnabled(false);
            registerButton.setText("Registrando...");
        } else {
            progressBar.setVisibility(View.GONE);
            registerButton.setEnabled(true);
            selectImageButton.setEnabled(true);
            takePhotoButton.setEnabled(true);
            registerButton.setText("Registrarse");
        }
    }
}