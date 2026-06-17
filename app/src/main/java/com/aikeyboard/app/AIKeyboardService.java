package com.aikeyboard.app;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class AIKeyboardService extends InputMethodService {

    private boolean isShiftOn = false;
    private View keyboardView;

    // Letter IDs for AZERTY layout
    private final int[] letterIds = {
        R.id.keyA, R.id.keyZ, R.id.keyE, R.id.keyR, R.id.keyT,
        R.id.keyY, R.id.keyU, R.id.keyI, R.id.keyO, R.id.keyP,
        R.id.keyQ, R.id.keyS, R.id.keyD, R.id.keyF, R.id.keyG,
        R.id.keyH, R.id.keyJ, R.id.keyK, R.id.keyL, R.id.keyM,
        R.id.keyW, R.id.keyX, R.id.keyC, R.id.keyV, R.id.keyB,
        R.id.keyN
    };

    @Override
    public View onCreateInputView() {
        keyboardView = getLayoutInflater().inflate(R.layout.keyboard_view, null);
        setupKeys(keyboardView);
        startGoldBorderAnimation(keyboardView);
        return keyboardView;
    }

    // ──────────────────────────────────────────────────────────────────
    // Gold border pulse animation on the 4 block buttons + enter
    // ──────────────────────────────────────────────────────────────────
    private void startGoldBorderAnimation(View root) {
        int[] blockIds = {
            R.id.btnBlock1, R.id.btnBlock2, R.id.btnBlock3, R.id.btnBlock4,
            R.id.keyEnter
        };

        int colorWhite  = 0xFFFFFFFF;
        int colorGold   = 0xFFFFD700;

        for (int id : blockIds) {
            View btn = root.findViewById(id);
            if (btn == null) continue;

            // We need a mutable copy of the drawable to animate stroke color
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.RECTANGLE);
            drawable.setColor(0xFF000000);
            drawable.setCornerRadius(dpToPx(8));
            drawable.setStroke(dpToPx(2), colorWhite);
            btn.setBackground(drawable);

            // Stagger start offset per button for wave effect
            long offset = id % 5 * 180L;

            ValueAnimator animator = ValueAnimator.ofObject(
                new ArgbEvaluator(), colorWhite, colorGold
            );
            animator.setDuration(900);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setStartDelay(offset);
            animator.addUpdateListener(anim -> {
                int color = (int) anim.getAnimatedValue();
                drawable.setStroke(dpToPx(2), color);
            });
            animator.start();
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ──────────────────────────────────────────────────────────────────
    // Key setup
    // ──────────────────────────────────────────────────────────────────
    private void setupKeys(View view) {

        // Block 1: Reply to copied text → shows inline panel
        Button btnBlock1 = view.findViewById(R.id.btnBlock1);
        btnBlock1.setOnClickListener(v -> handleShowCopiedText(view));

        // Block 2, 3, 4: placeholders
        view.findViewById(R.id.btnBlock2).setOnClickListener(v ->
            Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btnBlock3).setOnClickListener(v ->
            Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btnBlock4).setOnClickListener(v ->
            Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show());

        // Close inline panel
        Button btnClose = view.findViewById(R.id.btnClosePanelInline);
        btnClose.setOnClickListener(v -> {
            view.findViewById(R.id.panelCopiedText).setVisibility(View.GONE);
        });

        // Letter keys
        for (int id : letterIds) {
            Button key = view.findViewById(id);
            if (key == null) continue;
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

        // Apostrophe
        Button keyApo = view.findViewById(R.id.keyApostrophe);
        if (keyApo != null) keyApo.setOnClickListener(v -> typeText("'"));

        // Shift
        Button keyShift = view.findViewById(R.id.keyShift);
        keyShift.setOnClickListener(v -> {
            isShiftOn = !isShiftOn;
            updateShiftState(view);
        });

        // Backspace
        Button keyBackspace = view.findViewById(R.id.keyBackspace);
        keyBackspace.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.deleteSurroundingText(1, 0);
        });

        // Space
        Button keySpace = view.findViewById(R.id.keySpace);
        keySpace.setOnClickListener(v -> typeText(" "));

        // Enter — sends in Snapchat/all apps
        Button keyEnter = view.findViewById(R.id.keyEnter);
        keyEnter.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic == null) return;

            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null) {
                int action = ei.imeOptions & EditorInfo.IME_MASK_ACTION;
                // If the field has a send/go/search/done action, fire it
                if (action == EditorInfo.IME_ACTION_SEND
                    || action == EditorInfo.IME_ACTION_GO
                    || action == EditorInfo.IME_ACTION_SEARCH
                    || action == EditorInfo.IME_ACTION_DONE
                    || action == EditorInfo.IME_ACTION_NEXT) {
                    ic.performEditorAction(action);
                    return;
                }
            }
            // Otherwise insert a newline (regular text fields)
            ic.commitText("\n", 1);
        });

        // Period
        Button keyPeriod = view.findViewById(R.id.keyPeriod);
        keyPeriod.setOnClickListener(v -> typeText("."));

        // Numbers / emoji / globe — placeholder
        view.findViewById(R.id.keyNumbers).setOnClickListener(v ->
            Toast.makeText(this, "Numbers coming soon!", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.keyEmoji).setOnClickListener(v ->
            Toast.makeText(this, "Emoji picker coming soon!", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.keyGlobe).setOnClickListener(v ->
            Toast.makeText(this, "Switch language coming soon!", Toast.LENGTH_SHORT).show());
    }

    // ──────────────────────────────────────────────────────────────────
    // Show copied text panel INSIDE the keyboard
    // ──────────────────────────────────────────────────────────────────
    private void handleShowCopiedText(View root) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        String copiedText = "";

        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence text = clip.getItemAt(0).getText();
                if (text != null) copiedText = text.toString().trim();
            }
        }

        LinearLayout panel = root.findViewById(R.id.panelCopiedText);
        TextView tvText    = root.findViewById(R.id.tvCopiedTextInline);

        if (copiedText.isEmpty()) {
            Toast.makeText(this,
                "Nothing copied! Copy a message first.",
                Toast.LENGTH_LONG).show();
            return;
        }

        tvText.setText(copiedText);
        panel.setVisibility(View.VISIBLE);
    }

    // ──────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────
    private void typeText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(text, 1);
    }

    private void updateShiftState(View view) {
        for (int id : letterIds) {
            Button key = view.findViewById(id);
            if (key == null) continue;
            String current = key.getText().toString();
            key.setText(isShiftOn ? current.toUpperCase() : current.toLowerCase());
        }
    }
}
