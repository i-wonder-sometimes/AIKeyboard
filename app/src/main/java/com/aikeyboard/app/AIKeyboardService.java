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

    private boolean isShiftOn = false;
    private boolean isSymbolMode = false;
    private boolean isClipboardPanelExpanded = false;
    private View keyboardView;

    private final String[] languages = {
        "English", "Français", "Español", "Deutsch", "Italiano",
        "Português", "Nederlands", "Русский", "العربية", "中文",
        "日本語", "한국어", "हिन्दी", "Türkçe", "Polski"
    };
    private int currentLanguageIndex = 0;

    private final int[] letterIds = {
        R.id.keyA, R.id.keyZ, R.id.keyE, R.id.keyR, R.id.keyT,
        R.id.keyY, R.id.keyU, R.id.keyI, R.id.keyO, R.id.keyP,
        R.id.keyQ, R.id.keyS, R.id.keyD, R.id.keyF, R.id.keyG,
        R.id.keyH, R.id.keyJ, R.id.keyK, R.id.keyL, R.id.keyM,
        R.id.keyW, R.id.keyX, R.id.keyC, R.id.keyV, R.id.keyB,
        R.id.keyN
    };

    private final int[] numberIds = {
        R.id.keyNum1, R.id.keyNum2, R.id.keyNum3, R.id.keyNum4, R.id.keyNum5,
        R.id.keyNum6, R.id.keyNum7, R.id.keyNum8, R.id.keyNum9, R.id.keyNum0
    };

    private final int[] symbolIds = {
        R.id.keyAt, R.id.keyHash, R.id.keyDollar, R.id.keyPercent,
        R.id.keyAmpersand, R.id.keyAsterisk, R.id.keyExclaim, R.id.keyQuestion,
        R.id.keySlash, R.id.keyBackslashSym,
        R.id.keyParenOpen, R.id.keyParenClose, R.id.keyBracketOpen, R.id.keyBracketClose,
        R.id.keyBraceOpen, R.id.keyBraceClose, R.id.keyMinus, R.id.keyPlus,
        R.id.keyEqual, R.id.keyColon
    };

    @Override
    public View onCreateInputView() {
        keyboardView = getLayoutInflater().inflate(R.layout.keyboard_view, null);
        setupKeys(keyboardView);
        return keyboardView;
    }

    private void setupKeys(View view) {

        // Grid button → clipboard panel
        ImageButton btnGrid = view.findViewById(R.id.btnLayoutGrid);
        if (btnGrid != null) {
            btnGrid.setOnClickListener(v -> handleShowCopiedText(view));
        }

        // Globe button → cycle language / long-press → dialog
        ImageButton btnGlobe = view.findViewById(R.id.btnGlobeTop);
        if (btnGlobe != null) {
            btnGlobe.setOnClickListener(v -> cycleLanguage(view));
            btnGlobe.setOnLongClickListener(v -> { showLanguageDialog(); return true; });
        }

        // Clipboard panel: expand/collapse
        ImageButton btnToggle = view.findViewById(R.id.btnToggleTextScroll);
        if (btnToggle != null) {
            btnToggle.setOnClickListener(v -> toggleTextScrollHeight(view));
        }

        // Clipboard panel: Generate AI Reply
        Button btnAiReply = view.findViewById(R.id.btnGenerateAiReply);
        if (btnAiReply != null) {
            btnAiReply.setOnClickListener(v ->
                Toast.makeText(this, "AI Reply generation coming soon!", Toast.LENGTH_SHORT).show());
        }

        // Clipboard panel: Close
        Button btnClose = view.findViewById(R.id.btnClosePanelInline);
        if (btnClose != null) {
            btnClose.setOnClickListener(v ->
                view.findViewById(R.id.panelCopiedText).setVisibility(View.GONE));
        }

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
                    ImageButton shift = view.findViewById(R.id.keyShift);
                    if (shift != null) shift.setImageResource(R.drawable.ic_shift);
                }
                typeText(letter);
            });
        }

        // Apostrophe
        Button keyApo = view.findViewById(R.id.keyApostrophe);
        if (keyApo != null) keyApo.setOnClickListener(v -> typeText("'"));

        // Shift
        ImageButton keyShift = view.findViewById(R.id.keyShift);
        if (keyShift != null) {
            keyShift.setOnClickListener(v -> {
                isShiftOn = !isShiftOn;
                updateShiftState(view);
                keyShift.setImageResource(isShiftOn
                    ? R.drawable.ic_shift_active
                    : R.drawable.ic_shift);
            });
        }

        // Backspace (alpha)
        ImageButton keyBackspace = view.findViewById(R.id.keyBackspace);
        if (keyBackspace != null) {
            keyBackspace.setOnClickListener(v -> deleteChar());
            keyBackspace.setOnLongClickListener(v -> { deleteWord(); return true; });
        }

        // ?123 toggle
        Button keyNumbers = view.findViewById(R.id.keyNumbers);
        if (keyNumbers != null) {
            keyNumbers.setOnClickListener(v -> toggleSymbolMode(view));
        }

        // Emoji
        ImageButton keyEmoji = view.findViewById(R.id.keyEmoji);
        if (keyEmoji != null) {
            keyEmoji.setOnClickListener(v ->
                Toast.makeText(this, "Emoji picker coming soon!", Toast.LENGTH_SHORT).show());
        }

        // Space (alpha)
        Button keySpace = view.findViewById(R.id.keySpace);
        if (keySpace != null) {
            keySpace.setOnClickListener(v -> typeText(" "));
            keySpace.setOnLongClickListener(v -> { showKeyboardSwitcher(); return true; });
        }

        // Period
        Button keyPeriod = view.findViewById(R.id.keyPeriod);
        if (keyPeriod != null) keyPeriod.setOnClickListener(v -> typeText("."));

        // Enter (alpha)
        ImageButton keyEnter = view.findViewById(R.id.keyEnter);
        if (keyEnter != null) {
            keyEnter.setOnClickListener(v -> handleEnterKey());
        }

        // Number keys
        for (int id : numberIds) {
            Button key = view.findViewById(id);
            if (key == null) continue;
            key.setOnClickListener(v -> typeText(((Button) v).getText().toString()));
        }

        // Symbol keys
        for (int id : symbolIds) {
            Button key = view.findViewById(id);
            if (key == null) continue;
            key.setOnClickListener(v -> typeText(((Button) v).getText().toString()));
        }

        // ABC → back to alpha
        Button keyABC = view.findViewById(R.id.keyABC);
        if (keyABC != null) {
            keyABC.setOnClickListener(v -> toggleSymbolMode(view));
        }

        // Quote
        Button keyQuote = view.findViewById(R.id.keyQuote);
        if (keyQuote != null) keyQuote.setOnClickListener(v -> typeText("\""));

        // Space (symbol)
        Button keySpaceSymbol = view.findViewById(R.id.keySpaceSymbol);
        if (keySpaceSymbol != null) {
            keySpaceSymbol.setOnClickListener(v -> typeText(" "));
            keySpaceSymbol.setOnLongClickListener(v -> { showKeyboardSwitcher(); return true; });
        }

        // Comma
        Button keyComma = view.findViewById(R.id.keyComma);
        if (keyComma != null) keyComma.setOnClickListener(v -> typeText(","));

        // Backspace (symbol)
        ImageButton keyBackspaceSym = view.findViewById(R.id.keyBackspaceSymbol);
        if (keyBackspaceSym != null) {
            keyBackspaceSym.setOnClickListener(v -> deleteChar());
            keyBackspaceSym.setOnLongClickListener(v -> { deleteWord(); return true; });
        }

        // Enter (symbol)
        ImageButton keyEnterSymbol = view.findViewById(R.id.keyEnterSymbol);
        if (keyEnterSymbol != null) {
            keyEnterSymbol.setOnClickListener(v -> handleEnterKey());
        }
    }

    // ── Symbol mode toggle ────────────────────────────────────────
    private void toggleSymbolMode(View view) {
        isSymbolMode = !isSymbolMode;
        LinearLayout alpha  = view.findViewById(R.id.layoutAlphabetMode);
        LinearLayout symbol = view.findViewById(R.id.layoutSymbolMode);
        if (alpha  != null) alpha.setVisibility(isSymbolMode ? View.GONE  : View.VISIBLE);
        if (symbol != null) symbol.setVisibility(isSymbolMode ? View.VISIBLE : View.GONE);
    }

    // ── Language ──────────────────────────────────────────────────
    private void cycleLanguage(View view) {
        currentLanguageIndex = (currentLanguageIndex + 1) % languages.length;
        applyLanguage(view, currentLanguageIndex);
    }

    private void applyLanguage(View view, int index) {
        currentLanguageIndex = index;
        String lang = languages[index];
        Button keySpace = view.findViewById(R.id.keySpace);
        if (keySpace != null) keySpace.setText(lang);
        Toast.makeText(this, lang, Toast.LENGTH_SHORT).show();
    }

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

    // ── Clipboard panel ───────────────────────────────────────────
    private void handleShowCopiedText(View root) {
        LinearLayout panel = root.findViewById(R.id.panelCopiedText);
        if (panel != null && panel.getVisibility() == View.VISIBLE) {
            panel.setVisibility(View.GONE);
            return;
        }

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

        TextView tvText = root.findViewById(R.id.tvCopiedTextInline);
        if (tvText != null) tvText.setText(copiedText);

        isClipboardPanelExpanded = false;
        setScrollViewHeight(root, dpToPx(64));
        updateToggleArrow(root, false);

        if (panel != null) panel.setVisibility(View.VISIBLE);
    }

    private void toggleTextScrollHeight(View root) {
        isClipboardPanelExpanded = !isClipboardPanelExpanded;
        setScrollViewHeight(root, dpToPx(isClipboardPanelExpanded ? 160 : 64));
        updateToggleArrow(root, isClipboardPanelExpanded);
    }

    private void setScrollViewHeight(View root, int heightPx) {
        ScrollView sv = root.findViewById(R.id.svCopiedText);
        if (sv == null) return;
        ViewGroup.LayoutParams p = sv.getLayoutParams();
        p.height = heightPx;
        sv.setLayoutParams(p);
    }

    private void updateToggleArrow(View root, boolean expanded) {
        ImageButton btn = root.findViewById(R.id.btnToggleTextScroll);
        if (btn != null)
            btn.setImageResource(expanded ? R.drawable.ic_collapse : R.drawable.ic_expand);
    }

    // ── Enter key ─────────────────────────────────────────────────
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

    // ── Shift ─────────────────────────────────────────────────────
    private void updateShiftState(View view) {
        for (int id : letterIds) {
            Button key = view.findViewById(id);
            if (key == null) continue;
            String t = key.getText().toString();
            key.setText(isShiftOn ? t.toUpperCase() : t.toLowerCase());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────
    private void typeText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(text, 1);
    }

    private void deleteChar() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.deleteSurroundingText(1, 0);
    }

    private void deleteWord() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence sel = ic.getTextBeforeCursor(50, 0);
        if (sel == null || sel.length() == 0) return;
        String s = sel.toString();
        int end = s.length(), start = end;
        while (start > 0 && s.charAt(start - 1) == ' ') start--;
        while (start > 0 && s.charAt(start - 1) != ' ') start--;
        int count = end - start;
        if (count > 0) ic.deleteSurroundingText(count, 0);
    }

    private void showKeyboardSwitcher() {
        InputMethodManager imm =
            (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showInputMethodPicker();
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
