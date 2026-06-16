package com.aikeyboard.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

public class ReplyPopupActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_reply);

        TextView tvCopiedText = findViewById(R.id.tvCopiedText);
        TextView tvAiReply    = findViewById(R.id.tvAiReply);
        Button btnClose       = findViewById(R.id.btnClose);

        // Get copied text from keyboard service
        String copiedText = getIntent().getStringExtra("copied_text");
        if (copiedText == null || copiedText.isEmpty()) {
            copiedText = "No text found.";
        }

        // Show the copied message
        tvCopiedText.setText(copiedText);

        // Simple reply — no AI yet
        tvAiReply.setText("Text seen! ✅");

        // Close button
        btnClose.setOnClickListener(v -> finish());
    }
}
