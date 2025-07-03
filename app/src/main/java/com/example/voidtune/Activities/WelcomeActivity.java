package com.example.voidtune.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.voidtune.R;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;

public class WelcomeActivity extends AppCompatActivity {
    private FirebaseAnalytics mFirebaseAnalytics;
    private static final String TAG = "FirebaseDebug";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

    // Verificar si el usuario ya está autenticado
    FirebaseAuth auth = FirebaseAuth.getInstance();
    if (auth.getCurrentUser() != null) {
        // Redirigir directamente al Activity_home
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        return;
    }

    // Si no está autenticado, mostrar el WelcomeActivity
    setContentView(R.layout.activity_welcome);

        // Inicializar Firebase Analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        //Prubea registro de evento

        Bundle bundle = new Bundle();
        bundle.putString("key", "value");
        mFirebaseAnalytics.logEvent("welcome_event", bundle);
        //Log de evento de inicio de sesión
        Log.d(TAG,"Eventro de prueba enviado a Firebase Analytics");

        // Referencia al botón de iniciar sesión
        Button loginButton = findViewById(R.id.loginButton);
        Button registerButton = findViewById(R.id.RegisterButton);

        // Configurar el evento onClick para abrir Login
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this, Login.class);
                startActivity(intent);
            }
        });

        // Configurar el evento onClick para abrir Register
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}
