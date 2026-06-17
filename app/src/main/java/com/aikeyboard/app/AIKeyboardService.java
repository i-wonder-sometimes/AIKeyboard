package com.aikeyboard.app;

import android.app.AlertDialog;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class AIKeyboardService extends InputMethodService {

    private boolean isShiftOn = false;
    private View keyboardView;
    private boolean isSymbolMode = false;
    private String currentLanguage = "English";
    private boolean isClipboardPanelExpanded = false;

    // Letter IDs for AZERTY layout
    private final int[] letterIds = {
        R.id.keyA, R.id.keyZ, R.id.keyE, R.id.keyR, R.id.keyT,
        R.id.keyY, R.id.keyU, R.id.keyI, R.id.keyO, R.id.keyP,
        R.id.keyQ, R.id.keyS, R.id.keyD, R.id.keyF, R.id.keyG,
        R.id.keyH, R.id.keyJ, R.id.keyK, R.id.keyL, R.id.keyM,
        R.id.keyW, R.id.keyX, R.id.keyC, R.id.keyV, R.id.keyB,
        R.id.keyN
    };

    // Number/Symbol IDs
    private final int[] numberIds = {
        R.id.key1, R.id.key2, R.id.key3, R.id.key4, R.id.key5,
        R.id.key6, R.id.key7, R.id.key8, R.id.key9, R.id.key0
    };

    private final int[] symbolIds = {
        R.id.keyAt, R.id.keyHash, R.id.keyDollar, R.id.keyPercent,
        R.id.keyAmpersand, R.id.keyAsterisk, R.id.keyExclaim, R.id.keyQuestion,
        R.id.keySlash, R.id.keyBackslash, R.id.keyParen1, R.id.keyParen2,
        R.id.keyBracket1, R.id.keyBracket2, R.id.keyBrace1, R.id.keyBrace2,
        R.id.keyMinus, R.id.keyPlus, R.id.keyEqual, R.id.keyColon,
        R.id.keyQuote, R.id.keyComma
    };

    private final String[] languages = {"English", "French", "Spanish"};
    private int currentLanguageIndex = 0;

    @Override
    public View onCreateInputView() {
        keyboardView = getLayoutInflater().inflate(R.layout.keyboard_view, null);
        setupKeys(keyboardView);
        return keyboardView;
    }

    // ───────────────────────────────────────────────────────────────
    // Key setup
    // ───────────────────────────────────────────────────────────────
    private void setupKeys(View view) {

        // === TOP BAR BUTTONS ===

        // Grid button: Show clipboard panel
        ImageButton btnLayoutGrid = view.findViewById(R.id.btnLayoutGrid);
        btnLayoutGrid.setOnClickListener(v -> handleShowCopiedText(view));

        // Globe button: Cycle language on click, open dialog on long-press
        ImageButton btnGlobeTop = view.findViewById(R.id.btnGlobeTop);
        btnGlobeTop.setOnClickListener(v -> cycleLanguage(view));
        btnGlobeTop.setOnLongClickListener(v -> {
            showLanguageDialog();
            return true;
        });

        // === CLIPBOARD PANEL BUTTONS ===

        // Expand/collapse text scroll button
        ImageButton btnToggleTextScroll = view.findViewById(R.id.btnToggleTextScroll);
        btnToggleTextScroll.setOnClickListener(v -> toggleTextScrollHeight(view));

        // Generate AI Reply button
        Button btnGenerateAiReply = view.findViewById(R.id.btnGenerateAiReply);
        btnGenerateAiReply.setOnClickListener(v ->
            Toast.makeText(this, "AI Reply generation coming soon!", Toast.LENGTH_SHORT).show());

        // Close clipboard panel
        Button btnClosePanelInline = view.findViewById(R.id.btnClosePanelInline);
        btnClosePanelInline.setOnClickListener(v -> {
            view.findViewById(R.id.panelCopiedText).setVisibility(View.GONE);
        });

        // === ALPHABET MODE LETTER KEYS ===

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

        // === ALPHABET MODE BOTTOM ROW ===

        // ?123 button: Toggle to symbol mode
        Button keyNumbers = view.findViewById(R.id.keyNumbers);
        keyNumbers.setOnClickListener(v -> toggleSymbolMode(view));

        // Emoji button
        Button keyEmoji = view.findViewById(R.id.keyEmoji);
        keyEmoji.setOnClickListener(v ->
            Toast.makeText(this, "Emoji picker coming soon!", Toast.LENGTH_SHORT).show());

        // Space
        Button keySpace = view.findViewById(R.id.keySpace);
        keySpace.setOnClickListener(v -> typeText(" "));

        // Period
        Button keyPeriod = view.findViewById(R.id.keyPeriod);
        keyPeriod.setOnClickListener(v -> typeText("."));

        // Enter
        Button keyEnter = view.findViewById(R.id.keyEnter);
        keyEnter.setOnClickListener(v -> handleEnterKey());

        // === SYMBOL MODE NUMBER KEYS ===

        for (int id : numberIds) {
            Button key = view.findViewById(id);
            if (key == null) continue;
            key.setOnClickListener(v -> typeText(((Button) v).getText().toString()));
        }

        // === SYMBOL MODE SYMBOL KEYS ===

        for (int id : symbolIds) {
            Button key = view.findViewById(id);
            if (key == null) continue;
            key.setOnClickListener(v -> typeText(((Button) v).getText().toString()));
        }

        // === SYMBOL MODE BOTTOM ROW ===

        // ABC button: Return to alphabet mode
        Button keyABC = view.findViewById(R.id.keyABC);
        keyABC.setOnClickListener(v -> toggleSymbolMode(view));

        // Space (symbol mode)
        Button keySpaceSymbol = view.findViewById(R.id.keySpaceSymbol);
        keySpaceSymbol.setOnClickListener(v -> typeText(" "));

        // Comma
        Button keyComma = view.findViewById(R.id.keyComma);
        keyComma.setOnClickListener(v -> typeText(","));

        // Enter (symbol mode)
        Button keyEnterSymbol = view.findViewById(R.id.keyEnterSymbol);
        keyEnterSymbol.setOnClickListener(v -> handleEnterKey());
    }

    // ───────────────────────────────────────────────────────────────
    // Toggle between Alphabet and Symbol modes
    // ───────────────────────────────────────────────────────────────
    private void toggleSymbolMode(View view) {
        LinearLayout layoutAlpha = view.findViewById(R.id.layoutAlphabetMode);
        LinearLayout layoutSymbol = view.findViewById(R.id.layoutSymbolMode);
        Button keyNumbers = view.findViewById(R.id.keyNumbers);

        isSymbolMode = !isSymbolMode;

        if (isSymbolMode) {
            layoutAlpha.setVisibility(View.GONE);
            layoutSymbol.setVisibility(View.VISIBLE);
            keyNumbers.setText("ABC");
        } else {
            layoutAlpha.setVisibility(View.VISIBLE);
            layoutSymbol.setVisibility(View.GONE);
            keyNumbers.setText("?123");
        }
    }

    // ───────────────────────────────────────────────────────────────
    // Cycle through languages
    // ───────────────────────────────────────────────────────────────
    private void cycleLanguage(View view) {
        currentLanguageIndex = (currentLanguageIndex + 1) % languages.length;
        currentLanguage = languages[currentLanguageIndex];

        Button keySpace = view.findViewById(R.id.keySpace);
        if (keySpace != null) {
            keySpace.setText(currentLanguage);
        }

        Toast.makeText(this, "Language: " + currentLanguage, Toast.LENGTH_SHORT).show();
    }

    // ───────────────────────────────────────────────────────────────
    // Show language selection dialog
    // ───────────────────────────────────────────────────────────────
    private void showLanguageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog);
        builder.setTitle("Select Language");
        builder.setItems(languages, (dialog, which) -> {
            currentLanguageIndex = which;
            currentLanguage = languages[which];
            Button keySpace = keyboardView.findViewById(R.id.keySpace);
            if (keySpace != null) {
                keySpace.setText(currentLanguage);
            }
            Toast.makeText(this, "Language: " + currentLanguage, Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    // ───────────────────────────────────────────────────────────────
    // Show copied text panel INSIDE the keyboard
    // ───────────────────────────────────────────────────────────────
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
        TextView tvText = root.findViewById(R.id.tvCopiedTextInline);

        if (copiedText.isEmpty()) {
            Toast.makeText(this,
                "Nothing copied! Copy a message first.",
                Toast.LENGTH_LONG).show();
            return;
        }

        tvText.setText(copiedText);
        panel.setVisibility(View.VISIBLE);

        // Reset scroll view height to default collapsed state
        isClipboardPanelExpanded = false;
        resetTextScrollHeight(root);
    }

    // ───────────────────────────────────────────────────────────────
    // Toggle ScrollView height for clipboard panel
    // ───────────────────────────────────────────────────────────────
    private void toggleTextScrollHeight(View root) {
        ScrollView sv = root.findViewById(R.id.svCopiedText);
        if (sv == null) return;

        if (isClipboardPanelExpanded) {
            resetTextScrollHeight(root);
        } else {
            // Expand to 150dp
            ViewGroup.LayoutParams params = sv.getLayoutParams();
            params.height = dpToPx(150);
            sv.setLayoutParams(params);
            isClipboardPanelExpanded = true;
        }
    }

    private void resetTextScrollHeight(View root) {
        ScrollView sv = root.findViewById(R.id.svCopiedText);
        if (sv == null) return;

        ViewGroup.LayoutParams params = sv.getLayoutParams();
        params.height = dpToPx(60);
        sv.setLayoutParams(params);
        isClipboardPanelExpanded = false;
    }

    // ───────────────────────────────────────────────────────────────
    // Handle Enter key
    // ───────────────────────────────────────────────────────────────
    private void handleEnterKey() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        EditorInfo ei = getCurrentInputEditorInfo();
        if (ei != null) {
            int action = ei.imeOptions & EditorInfo.IME_MASK_ACTION;
            if (action == EditorInfo.IME_ACTION_SEND
                || action == EditorInfo.IME_ACTION_GO
                || action == EditorInfo.IME_ACTION_SEARCH
                || action == EditorInfo.IME_ACTION_DONE
                || action == EditorInfo.IME_ACTION_NEXT) {
                ic.performEditorAction(action);
                return;
            }
        }
        ic.commitText("\n", 1);
    }

    // ───────────────────────────────────────────────────────────────
    // Helpers
    // ───────────────────────────────────────────────────────────────
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

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
