package com.aikeyboard.app;

import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.Toast;

public class AIKeyboardService extends InputMethodService {

    private boolean isShiftOn = false;

    @Override
    public View onCreateInputView() {
        View keyboardView = getLayoutInflater().inflate(R.layout.keyboard_view, null);
        setupKeys(keyboardView);
        return keyboardView;
    }

    private void setupKeys(View view) {

        // ── AI Reply Button ──────────────────────────────────────────
        Button btnAiReply = view.findViewById(R.id.btnAiReply);
        btnAiReply.setOnClickListener(v -> handleAiReply());

        // ── Letter keys ──────────────────────────────────────────────
        int[] letterIds = {
            R.id.keyQ, R.id.keyW, R.id.keyE, R.id.keyR, R.id.keyT,
            R.id.keyY, R.id.keyU, R.id.keyI, R.id.keyO, R.id.keyP,
            R.id.keyA, R.id.keyS, R.id.keyD, R.id.keyF, R.id.keyG,
            R.id.keyH, R.id.keyJ, R.id.keyK, R.id.keyL,
            R.id.keyZ, R.id.keyX, R.id.keyC, R.id.keyV, R.id.keyB,
            R.id.keyN, R.id.keyM
        };

        for (int id : letterIds) {
            Button key = view.findViewById(id);
            if (key != null) {
                key.setOnClickListener(v -> {
                    String letter = ((Button) v).getText().toString();
                    if (isShiftOn) {
                        letter = letter.toUpperCase();
                        isShiftOn = false;
                        updateShiftState(view);
                    }
                    typeText(letter);
                });
            }
        }

        // ── Shift ────────────────────────────────────────────────────
        Button keyShift = view.findViewById(R.id.keyShift);
        keyShift.setOnClickListener(v -> {
            isShiftOn = !isShiftOn;
            updateShiftState(view);
        });

        // ── Backspace ─────────────────────────────────────────────────
        Button keyBackspace = view.findViewById(R.id.keyBackspace);
        keyBackspace.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.deleteSurroundingText(1, 0);
            }
        });

        // ── Space ─────────────────────────────────────────────────────
        Button keySpace = view.findViewById(R.id.keySpace);
        keySpace.setOnClickListener(v -> typeText(" "));

        // ── Enter ─────────────────────────────────────────────────────
        Button keyEnter = view.findViewById(R.id.keyEnter);
        keyEnter.setOnClickListener(v -> typeText("\n"));

        // ── Comma & Period ────────────────────────────────────────────
        Button keyComma = view.findViewById(R.id.keyComma);
        keyComma.setOnClickListener(v -> typeText(","));

        Button keyPeriod = view.findViewById(R.id.keyPeriod);
        keyPeriod.setOnClickListener(v -> typeText("."));

        // ── Numbers (placeholder) ─────────────────────────────────────
        Button keyNumbers = view.findViewById(R.id.keyNumbers);
        keyNumbers.setOnClickListener(v ->
            Toast.makeText(this, "Numbers row coming soon!", Toast.LENGTH_SHORT).show()
        );
    }

    // ── Type text into whatever app is focused ────────────────────────
    private void typeText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(text, 1);
        }
    }

    // ── Update key labels when shift toggled ──────────────────────────
    private void updateShiftState(View view) {
        int[] letterIds = {
            R.id.keyQ, R.id.keyW, R.id.keyE, R.id.keyR, R.id.keyT,
            R.id.keyY, R.id.keyU, R.id.keyI, R.id.keyO, R.id.keyP,
            R.id.keyA, R.id.keyS, R.id.keyD, R.id.keyF, R.id.keyG,
            R.id.keyH, R.id.keyJ, R.id.keyK, R.id.keyL,
            R.id.keyZ, R.id.keyX, R.id.keyC, R.id.keyV, R.id.keyB,
            R.id.keyN, R.id.keyM
        };

        for (int id : letterIds) {
            Button key = view.findViewById(id);
            if (key != null) {
                String current = key.getText().toString();
                key.setText(isShiftOn ? current.toUpperCase() : current.toLowerCase());
            }
        }
    }

    // ── AI Reply: read clipboard → open popup ─────────────────────────
    private void handleAiReply() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        String copiedText = "";

        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence text = clip.getItemAt(0).getText();
                if (text != null) {
                    copiedText = text.toString().trim();
                }
            }
        }

        if (copiedText.isEmpty()) {
            Toast.makeText(this,
                "Nothing copied! Copy a message first, then tap ✨",
                Toast.LENGTH_LONG).show();
            return;
        }

        // Launch popup activity with the copied text
        Intent intent = new Intent(this, ReplyPopupActivity.class);
        intent.putExtra("copied_text", copiedText);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
