package com.aikeyboard.app;

import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class AIKeyboardService extends InputMethodService {

    // ─── State ────────────────────────────────────────────────────
    private boolean isShiftOn               = false;
    private boolean isSymbolMode            = false;
    private boolean isClipboardPanelExpanded = false;
    private View    keyboardView;

    // ─── Language list ────────────────────────────────────────────
    private final String[] languages = {
        "English", "French", "Spanish", "German", "Italian",
        "Portuguese", "Dutch", "Russian", "Arabic", "Chinese",
        "Japanese", "Korean", "Hindi", "Turkish", "Polish"
    };
    private int currentLanguageIndex = 0;

    // ─── AZERTY letter key IDs ────────────────────────────────────
    private final int[] letterIds = {
        R.id.keyA, R.id.keyZ, R.id.keyE, R.id.keyR, R.id.keyT,
        R.id.keyY, R.id.keyU, R.id.keyI, R.id.keyO, R.id.keyP,
        R.id.keyQ, R.id.keyS, R.id.keyD, R.id.keyF, R.id.keyG,
        R.id.keyH, R.id.keyJ, R.id.keyK, R.id.keyL, R.id.keyM,
        R.id.keyW, R.id.keyX, R.id.keyC, R.id.keyV, R.id.keyB,
        R.id.keyN
    };

    // ─── Number key IDs ───────────────────────────────────────────
    private final int[] numberIds = {
        R.id.key1, R.id.key2, R.id.key3, R.id.key4, R.id.key5,
        R.id.key6, R.id.key7, R.id.key8, R.id.key9, R.id.key0
    };

    // ─── Symbol key IDs ───────────────────────────────────────────
    private final int[] symbolIds = {
        R.id.keyAt, R.id.keyHash, R.id.keyDollar, R.id.keyPercent,
        R.id.keyAmpersand, R.id.keyAsterisk, R.id.keyExclaim, R.id.keyQuestion,
        R.id.keySlash, R.id.keyBackslashSym,
        R.id.keyParen1, R.id.keyParen2, R.id.keyBracket1, R.id.keyBracket2,
        R.id.keyBrace1, R.id.keyBrace2, R.id.keyMinus, R.id.keyPlus,
        R.id.keyEqual, R.id.keyColon
    };

    // ─────────────────────────────────────────────────────────────
    @Override
    public View onCreateInputView() {
        keyboardView = getLayoutInflater().inflate(R.layout.keyboard_view, null);
        setupKeys(keyboardView);
        return keyboardView;
    }

    // ═════════════════════════════════════════════════════════════
    //  KEY SETUP
    // ═════════════════════════════════════════════════════════════
    private void setupKeys(View view) {

        // ── Top-bar: Grid button (clipboard / AI panel) ──────────
        ImageButton btnGrid = view.findViewById(R.id.btnLayoutGrid);
        if (btnGrid != null) {
            btnGrid.setOnClickListener(v -> handleShowCopiedText(view));
        }

        // ── Top-bar: Globe button (language) ────────────────────
        ImageButton btnGlobe = view.findViewById(R.id.btnGlobeTop);
        if (btnGlobe != null) {
            btnGlobe.setOnClickListener(v -> cycleLanguage(view));
            btnGlobe.setOnLongClickListener(v -> {
                showLanguageDialog();
                return true;
            });
        }

        // ── Clipboard panel: expand/collapse arrow ───────────────
        ImageButton btnToggle = view.findViewById(R.id.btnToggleTextScroll);
        if (btnToggle != null) {
            btnToggle.setOnClickListener(v -> toggleTextScrollHeight(view));
        }

        // ── Clipboard panel: Generate AI Reply ───────────────────
        Button btnAiReply = view.findViewById(R.id.btnGenerateAiReply);
        if (btnAiReply != null) {
            btnAiReply.setOnClickListener(v ->
                Toast.makeText(this, "AI Reply generation coming soon!", Toast.LENGTH_SHORT).show());
        }

        // ── Clipboard panel: Close ────────────────────────────────
        Button btnClose = view.findViewById(R.id.btnClosePanelInline);
        if (btnClose != null) {
            btnClose.setOnClickListener(v ->
                view.findViewById(R.id.panelCopiedText).setVisibility(View.GONE));
        }

        // ── Letter keys (AZERTY) ─────────────────────────────────
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

        // ── Apostrophe ────────────────────────────────────────────
        Button keyApo = view.findViewById(R.id.keyApostrophe);
        if (keyApo != null) keyApo.setOnClickListener(v -> typeText("'"));

        // ── Shift key (ImageButton with SVG) ─────────────────────
        ImageButton keyShift = view.findViewById(R.id.keyShift);
        if (keyShift != null) {
            keyShift.setOnClickListener(v -> {
                isShiftOn = !isShiftOn;
                updateShiftState(view);
                // Visually indicate shift is active
                keyShift.setImageResource(isShiftOn
                    ? R.drawable.ic_shift_active
                    : R.drawable.ic_shift);
            });
        }

        // ── Backspace (ImageButton with SVG) ─────────────────────
        ImageButton keyBackspace = view.findViewById(R.id.keyBackspace);
        if (keyBackspace != null) {
            keyBackspace.setOnClickListener(v -> {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.deleteSurroundingText(1, 0);
            });
            // Long-press: delete whole word
            keyBackspace.setOnLongClickListener(v -> {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    CharSequence sel = ic.getTextBeforeCursor(50, 0);
                    if (sel != null && sel.length() > 0) {
                        String s = sel.toString();
                        int end = s.length();
                        int start = end;
                        // Skip trailing spaces
                        while (start > 0 && s.charAt(start - 1) == ' ') start--;
                        // Delete back to previous space
                        while (start > 0 && s.charAt(start - 1) != ' ') start--;
                        int deleteCount = end - start;
                        if (deleteCount > 0) ic.deleteSurroundingText(deleteCount, 0);
                    }
                }
                return true;
            });
        }

        // ── ?123 toggle ───────────────────────────────────────────
        Button keyNumbers = view.findViewById(R.id.keyNumbers);
        if (keyNumbers != null) {
            keyNumbers.setOnClickListener(v -> toggleSymbolMode(view));
        }

        // ── Emoji button (SVG face) ───────────────────────────────
        ImageButton keyEmoji = view.findViewById(R.id.keyEmoji);
        if (keyEmoji != null) {
            keyEmoji.setOnClickListener(v ->
                Toast.makeText(this, "Emoji picker coming soon!", Toast.LENGTH_SHORT).show());
        }

        // ── Space bar ─────────────────────────────────────────────
        Button keySpace = view.findViewById(R.id.keySpace);
        if (keySpace != null) {
            keySpace.setOnClickListener(v -> typeText(" "));
            // Long-press: open keyboard switcher (switch IME)
            keySpace.setOnLongClickListener(v -> {
                InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showInputMethodPicker();
                }
                return true;
            });
        }

        // ── Period ────────────────────────────────────────────────
        Button keyPeriod = view.findViewById(R.id.keyPeriod);
        if (keyPeriod != null) keyPeriod.setOnClickListener(v -> typeText("."));

        // ── Enter (alphabet mode — ImageButton with SVG) ─────────
        ImageButton keyEnter = view.findViewById(R.id.keyEnter);
        if (keyEnter != null) {
            keyEnter.setOnClickListener(v -> handleEnterKey());
        }

        // ── Number keys ───────────────────────────────────────────
        for (int id : numberIds) {
            Button key = view.findViewById(id);
            if (key == null) continue;
            key.setOnClickListener(v -> typeText(((Button) v).getText().toString()));
        }

        // ── Symbol keys ───────────────────────────────────────────
        for (int id : symbolIds) {
            Button key = view.findViewById(id);
            if (key == null) continue;
            key.setOnClickListener(v -> typeText(((Button) v).getText().toString()));
        }

        // ── ABC button (return from symbol mode) ─────────────────
        Button keyABC = view.findViewById(R.id.keyABC);
        if (keyABC != null) {
            keyABC.setOnClickListener(v -> toggleSymbolMode(view));
        }

        // ── Quote (symbol mode) ───────────────────────────────────
        Button keyQuote = view.findViewById(R.id.keyQuote);
        if (keyQuote != null) keyQuote.setOnClickListener(v -> typeText("\""));

        // ── Space (symbol mode) ───────────────────────────────────
        Button keySpaceSymbol = view.findViewById(R.id.keySpaceSymbol);
        if (keySpaceSymbol != null) {
            keySpaceSymbol.setOnClickListener(v -> typeText(" "));
            keySpaceSymbol.setOnLongClickListener(v -> {
                InputMethodManager imm = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showInputMethodPicker();
                return true;
            });
        }

        // ── Comma (symbol mode) ───────────────────────────────────
        Button keyComma = view.findViewById(R.id.keyComma);
        if (keyComma != null) keyComma.setOnClickListener(v -> typeText(","));

        // ── Enter (symbol mode — ImageButton with SVG) ────────────
        ImageButton keyEnterSymbol = view.findViewById(R.id.keyEnterSymbol);
        if (keyEnterSymbol != null) {
            keyEnterSymbol.setOnClickListener(v -> handleEnterKey());
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  SYMBOL MODE TOGGLE
    // ═════════════════════════════════════════════════════════════
    private void toggleSymbolMode(View view) {
        LinearLayout layoutAlpha  = view.findViewById(R.id.layoutAlphabetMode);
        LinearLayout layoutSymbol = view.findViewById(R.id.layoutSymbolMode);

        isSymbolMode = !isSymbolMode;

        if (isSymbolMode) {
            if (layoutAlpha  != null) layoutAlpha.setVisibility(View.GONE);
            if (layoutSymbol != null) layoutSymbol.setVisibility(View.VISIBLE);
        } else {
            if (layoutAlpha  != null) layoutAlpha.setVisibility(View.VISIBLE);
            if (layoutSymbol != null) layoutSymbol.setVisibility(View.GONE);
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  LANGUAGE CYCLING
    // ═════════════════════════════════════════════════════════════
    private void cycleLanguage(View view) {
        currentLanguageIndex = (currentLanguageIndex + 1) % languages.length;
        applyLanguage(view, currentLanguageIndex);
    }

    private void applyLanguage(View view, int index) {
        currentLanguageIndex = index;
        String lang = languages[index];

        // Update spacebar label in alpha mode
        Button keySpace = view.findViewById(R.id.keySpace);
        if (keySpace != null) keySpace.setText(lang);

        Toast.makeText(this, lang, Toast.LENGTH_SHORT).show();
    }

    // ═════════════════════════════════════════════════════════════
    //  LANGUAGE DIALOG (long-press globe)
    // ═════════════════════════════════════════════════════════════
    private void showLanguageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(
            this, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        builder.setTitle("Select Language");
        builder.setSingleChoiceItems(languages, currentLanguageIndex, (dialog, which) -> {
            applyLanguage(keyboardView, which);
            dialog.dismiss();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    // ═════════════════════════════════════════════════════════════
    //  CLIPBOARD PANEL
    // ═════════════════════════════════════════════════════════════
    private void handleShowCopiedText(View root) {
        // Toggle panel off if already visible
        LinearLayout panel = root.findViewById(R.id.panelCopiedText);
        if (panel != null && panel.getVisibility() == View.VISIBLE) {
            panel.setVisibility(View.GONE);
            return;
        }

        // Read clipboard
        ClipboardManager clipboard =
            (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        String copiedText = "";

        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            if (clip != null && clip.getItemCount() > 0) {
                CharSequence text = clip.getItemAt(0).getText();
                if (text != null) copiedText = text.toString().trim();
            }
        }

        if (copiedText.isEmpty()) {
            Toast.makeText(this,
                "Nothing copied — copy a message first.", Toast.LENGTH_LONG).show();
            return;
        }

        // Populate text view
        TextView tvText = root.findViewById(R.id.tvCopiedTextInline);
        if (tvText != null) tvText.setText(copiedText);

        // Reset scroll view to collapsed height
        isClipboardPanelExpanded = false;
        setScrollViewHeight(root, dpToPx(64));
        updateToggleArrow(root, false);

        if (panel != null) panel.setVisibility(View.VISIBLE);
    }

    // ─── Expand / collapse clipboard scroll view ─────────────────
    private void toggleTextScrollHeight(View root) {
        if (isClipboardPanelExpanded) {
            setScrollViewHeight(root, dpToPx(64));
            updateToggleArrow(root, false);
            isClipboardPanelExpanded = false;
        } else {
            setScrollViewHeight(root, dpToPx(160));
            updateToggleArrow(root, true);
            isClipboardPanelExpanded = true;
        }
    }

    private void setScrollViewHeight(View root, int heightPx) {
        ScrollView sv = root.findViewById(R.id.svCopiedText);
        if (sv == null) return;
        ViewGroup.LayoutParams params = sv.getLayoutParams();
        params.height = heightPx;
        sv.setLayoutParams(params);
    }

    private void updateToggleArrow(View root, boolean expanded) {
        ImageButton btn = root.findViewById(R.id.btnToggleTextScroll);
        if (btn == null) return;
        btn.setImageResource(expanded ? R.drawable.ic_collapse : R.drawable.ic_expand);
    }

    // ═════════════════════════════════════════════════════════════
    //  ENTER KEY
    // ═════════════════════════════════════════════════════════════
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

    // ═════════════════════════════════════════════════════════════
    //  SHIFT STATE
    // ═════════════════════════════════════════════════════════════
    private void updateShiftState(View view) {
        for (int id : letterIds) {
            Button key = view.findViewById(id);
            if (key == null) continue;
            String current = key.getText().toString();
            key.setText(isShiftOn ? current.toUpperCase() : current.toLowerCase());
        }
    }

    // ═════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════
    private void typeText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(text, 1);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
