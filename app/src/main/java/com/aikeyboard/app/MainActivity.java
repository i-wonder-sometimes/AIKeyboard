package com.aikeyboard.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Button opens Android keyboard settings so user can enable AI Keyboard
        Button btnEnable = findViewById(R.id.btnEnableKeyboard);
        btnEnable.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);
            startActivity(intent);
        });
    }
}
