package com.edoardonapoli.dosdeck;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int REQUEST_HOME_ROLE = 2001;
    private static final int DOS_BLACK = Color.rgb(0, 0, 0);
    private static final int DOS_BLUE = Color.rgb(0, 0, 170);
    private static final int DOS_WHITE = Color.rgb(255, 255, 255);
    private static final int DEFAULT_GREEN = Color.rgb(51, 255, 102);

    private final SpannableStringBuilder screen = new SpannableStringBuilder();
    private final List<TextView> terminalColorViews = new ArrayList<>();
    private final CommandParser parser = new CommandParser();

    private SharedPreferences preferences;
    private AppRepository appRepository;
    private List<AppEntry> apps = new ArrayList<>();
    private ScrollView scrollView;
    private TextView outputView;
    private TextView roleView;
    private EditText commandView;
    private int terminalColor;
    private float terminalTextSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences("dosdeck", MODE_PRIVATE);
        terminalColor = preferences.getInt("terminalColor", DEFAULT_GREEN);
        terminalTextSize = preferences.getFloat("terminalTextSize", 15f);
        appRepository = new AppRepository(this);

        configureSystemBars();
        setContentView(createTerminal());
        reloadApps(false);
        showBootScreen();
        updateRoleState();
        maybeRequestHomeRole();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (appRepository != null) {
            reloadApps(false);
            updateRoleState();
        }
    }

    @Override
    public void onBackPressed() {
        appendLine("HOME SHELL ACTIVE. TYPE SYSTEM TO CHOOSE ANOTHER LAUNCHER.");
    }

    private void configureSystemBars() {
        getWindow().setStatusBarColor(DOS_BLACK);
        getWindow().setNavigationBarColor(DOS_BLACK);
        getWindow().setNavigationBarContrastEnforced(false);
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        }
    }

    private View createTerminal() {
        FrameLayout frame = new FrameLayout(this);
        frame.setBackgroundColor(DOS_BLACK);
        frame.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                Insets insets = windowInsets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            } else {
                view.setPadding(
                    windowInsets.getSystemWindowInsetLeft(),
                    windowInsets.getSystemWindowInsetTop(),
                    windowInsets.getSystemWindowInsetRight(),
                    windowInsets.getSystemWindowInsetBottom());
            }
            return windowInsets;
        });

        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(DOS_BLACK);

        LinearLayout titleBar = new LinearLayout(this);
        titleBar.setOrientation(LinearLayout.HORIZONTAL);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setPadding(dp(10), dp(7), dp(8), dp(7));
        titleBar.setBackgroundColor(DOS_BLUE);

        TextView title = text(" DOSDECK COMMAND SHELL ", DOS_WHITE, 14f, true);
        title.setSingleLine(true);
        titleBar.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        roleView = text("[SET DEFAULT]", DOS_WHITE, 12f, true);
        roleView.setPadding(dp(8), dp(5), dp(8), dp(5));
        roleView.setBackground(bordered(DOS_WHITE));
        roleView.setOnClickListener(v -> requestHomeRole());
        roleView.setContentDescription(getString(R.string.default_role_explanation));
        titleBar.addView(roleView);
        shell.addView(titleBar);

        outputView = text("", terminalColor, terminalTextSize, false);
        outputView.setGravity(Gravity.START | Gravity.TOP);
        outputView.setPadding(dp(12), dp(12), dp(12), dp(18));
        outputView.setTextIsSelectable(false);
        outputView.setLinksClickable(true);
        outputView.setMovementMethod(LinkMovementMethod.getInstance());
        outputView.setHighlightColor(Color.rgb(25, 80, 40));
        terminalColorViews.add(outputView);

        scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(DOS_BLACK);
        scrollView.addView(outputView, new ScrollView.LayoutParams(
            ScrollView.LayoutParams.MATCH_PARENT,
            ScrollView.LayoutParams.WRAP_CONTENT));
        shell.addView(scrollView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout shortcuts = new LinearLayout(this);
        shortcuts.setOrientation(LinearLayout.HORIZONTAL);
        shortcuts.setPadding(dp(8), dp(4), dp(8), dp(4));
        shortcuts.setBackgroundColor(DOS_BLACK);
        shortcuts.addView(shortcut("[DIR]", "DIR"), weighted());
        shortcuts.addView(shortcut("[HELP]", "HELP"), weighted());
        shortcuts.addView(shortcut("[RELOAD]", "RELOAD"), weighted());
        shortcuts.addView(shortcut("[HOME]", "HOME"), weighted());
        shell.addView(shortcuts);

        LinearLayout commandLine = new LinearLayout(this);
        commandLine.setOrientation(LinearLayout.HORIZONTAL);
        commandLine.setGravity(Gravity.CENTER_VERTICAL);
        commandLine.setPadding(dp(10), dp(3), dp(10), dp(8));
        commandLine.setBackgroundColor(DOS_BLACK);

        TextView prompt = text(getString(R.string.terminal_prompt), terminalColor, terminalTextSize, true);
        terminalColorViews.add(prompt);
        commandLine.addView(prompt);

        commandView = new EditText(this);
        commandView.setSingleLine(true);
        commandView.setTextColor(terminalColor);
        commandView.setHintTextColor(Color.rgb(20, 105, 45));
        commandView.setHint(getString(R.string.command_hint));
        commandView.setTextSize(terminalTextSize);
        commandView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        commandView.setBackgroundColor(Color.TRANSPARENT);
        commandView.setPadding(dp(6), 0, 0, 0);
        commandView.setImeOptions(EditorInfo.IME_ACTION_GO | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        commandView.setInputType(android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        commandView.setOnEditorActionListener((v, actionId, event) -> {
            boolean enter = actionId == EditorInfo.IME_ACTION_GO
                || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN);
            if (enter) {
                executeFromInput();
                return true;
            }
            return false;
        });
        terminalColorViews.add(commandView);
        commandLine.addView(commandView,
            new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        shell.addView(commandLine);

        frame.addView(shell, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));
        frame.addView(new ScanlineOverlayView(this), new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT));
        return frame;
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1f);
        params.setMargins(dp(3), 0, dp(3), 0);
        return params;
    }

    private TextView shortcut(String label, String command) {
        TextView button = text(label, terminalColor, 12f, true);
        button.setGravity(Gravity.CENTER);
        button.setBackground(bordered(terminalColor));
        button.setTag("terminalButton");
        button.setOnClickListener(v -> executeCommand(command));
        terminalColorViews.add(button);
        return button;
    }

    private TextView text(String value, int color, float size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setTypeface(Typeface.MONOSPACE, bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setIncludeFontPadding(true);
        return view;
    }

    private GradientDrawable bordered(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(DOS_BLACK);
        drawable.setStroke(dp(1), color);
        drawable.setCornerRadius(0f);
        return drawable;
    }

    private void executeFromInput() {
        String raw = commandView.getText().toString();
        commandView.setText("");
        executeCommand(raw);
    }

    private void executeCommand(String raw) {
        appendLine("");
        appendLine("C:\\>" + raw);
        CommandParser.Command command = parser.parse(raw);
        switch (command.action) {
            case EMPTY:
                break;
            case HELP:
                showHelp();
                break;
            case DIR:
                showDirectory(apps, "C:\\APPS");
                break;
            case START:
                startMatching(command.argument);
                break;
            case FIND:
                findApps(command.argument);
                break;
            case CLS:
                screen.clear();
                render();
                break;
            case HOME:
                requestHomeRole();
                break;
            case SETTINGS:
                safeStart(new Intent(Settings.ACTION_SETTINGS), "SYSTEM SETTINGS NOT AVAILABLE.");
                break;
            case WALLPAPER:
                safeStart(new Intent(Intent.ACTION_SET_WALLPAPER), "WALLPAPER PICKER NOT AVAILABLE.");
                break;
            case ABOUT:
                showAbout();
                break;
            case DATE:
                appendLine(LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)));
                break;
            case TIME:
                appendLine(LocalTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)));
                break;
            case RELOAD:
                reloadApps(true);
                break;
            case COLOR:
                changeColor(command.argument);
                break;
            case FONT:
                changeFont(command.argument);
                break;
            case VER:
                appendLine("DOSDECK LAUNCHER " + BuildConfig.VERSION_NAME + " / ANDROID "
                    + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + ")");
                break;
            case SYSTEM:
                safeStart(new Intent(Settings.ACTION_HOME_SETTINGS), "HOME SETTINGS NOT AVAILABLE.");
                break;
            case UNKNOWN:
                appendLine("BAD COMMAND OR FILE NAME: " + command.argument);
                appendLine("TYPE HELP FOR AVAILABLE COMMANDS.");
                break;
        }
    }

    private void showBootScreen() {
        screen.clear();
        appendLine("DOSDECK MOBILE OPERATING SHELL");
        appendLine("VERSION " + BuildConfig.VERSION_NAME);
        appendLine("COPYRIGHT (C) 2026 EDOARDO NAPOLI");
        appendLine("");
        appendLine("MEMORY TEST: OK");
        appendLine("DISPLAY: PIXEL / EDGE-TO-EDGE READY");
        appendLine("ANDROID API: " + android.os.Build.VERSION.SDK_INT);
        appendLine("PROGRAMS FOUND: " + apps.size());
        appendLine("");
        appendLine("TYPE DIR TO LIST PROGRAMS OR HELP FOR COMMANDS.");
        appendLine("TOUCH ANY PROGRAM NAME TO START IT.");
        appendLine("");
        appendLine("C:\\>");
    }

    private void showHelp() {
        appendLine("AVAILABLE COMMANDS");
        appendLine("------------------");
        appendLine("DIR / APPS       LIST INSTALLED PROGRAMS");
        appendLine("START <N|NAME>   START A PROGRAM");
        appendLine("FIND <TEXT>      FILTER PROGRAMS");
        appendLine("RELOAD           RESCAN INSTALLED PROGRAMS");
        appendLine("HOME             SET DOSDECK AS HOME APP");
        appendLine("SETTINGS         OPEN ANDROID SETTINGS");
        appendLine("WALLPAPER        CHANGE WALLPAPER");
        appendLine("SYSTEM           CHOOSE ANOTHER HOME APP");
        appendLine("COLOR <NAME>     GREEN, AMBER, CYAN, WHITE");
        appendLine("FONT <SIZE>      SMALL, NORMAL, LARGE");
        appendLine("DATE / TIME      SHOW CURRENT DATE OR TIME");
        appendLine("VER / ABOUT      VERSION INFORMATION");
        appendLine("CLS              CLEAR THE SCREEN");
    }

    private void showAbout() {
        appendLine("DOSDECK LAUNCHER " + BuildConfig.VERSION_NAME);
        appendLine("A TEXT-FIRST ANDROID HOME SCREEN INSPIRED BY CLASSIC DOS TERMINALS.");
        appendLine("NO NETWORK ACCESS. NO ADS. NO ANALYTICS.");
        appendLine("NOT AFFILIATED WITH MICROSOFT OR GOOGLE.");
    }

    private void reloadApps(boolean report) {
        apps = appRepository.loadLaunchableApps();
        if (report) {
            appendLine("PROGRAM DIRECTORY RELOADED: " + apps.size() + " ENTRIES.");
        }
    }

    private void showDirectory(List<AppEntry> entries, String heading) {
        appendLine(" DIRECTORY OF " + heading);
        appendLine("");
        if (entries.isEmpty()) {
            appendLine("NO PROGRAMS FOUND.");
            return;
        }
        for (AppEntry entry : entries) {
            int originalIndex = apps.indexOf(entry);
            String prefix = String.format(Locale.ROOT, "%03d  ", originalIndex + 1);
            append(prefix);
            int start = screen.length();
            append(entry.label.toUpperCase(Locale.getDefault()));
            int end = screen.length();
            screen.setSpan(new AppClickSpan(entry), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            append("\n     " + entry.packageName + "\n");
        }
        appendLine("");
        appendLine(entries.size() + " PROGRAM(S)");
        render();
    }

    private void startMatching(String argument) {
        if (argument.isEmpty()) {
            appendLine("REQUIRED: START <NUMBER OR PROGRAM NAME>");
            return;
        }
        try {
            int index = Integer.parseInt(argument) - 1;
            if (index >= 0 && index < apps.size()) {
                launchApp(apps.get(index));
            } else {
                appendLine("PROGRAM NUMBER OUT OF RANGE.");
            }
            return;
        } catch (NumberFormatException ignored) {
            // Match by name or package below.
        }

        String needle = argument.toLowerCase(Locale.ROOT);
        List<AppEntry> matches = new ArrayList<>();
        for (AppEntry app : apps) {
            if (app.label.toLowerCase(Locale.ROOT).contains(needle)
                || app.packageName.toLowerCase(Locale.ROOT).contains(needle)) {
                matches.add(app);
            }
        }
        if (matches.size() == 1) {
            launchApp(matches.get(0));
        } else if (matches.isEmpty()) {
            appendLine("PROGRAM NOT FOUND: " + argument);
        } else {
            showDirectory(matches, "C:\\APPS\\SEARCH");
        }
    }

    private void findApps(String argument) {
        if (argument.isEmpty()) {
            appendLine("REQUIRED: FIND <TEXT>");
            return;
        }
        String needle = argument.toLowerCase(Locale.ROOT);
        List<AppEntry> matches = new ArrayList<>();
        for (AppEntry app : apps) {
            if (app.label.toLowerCase(Locale.ROOT).contains(needle)
                || app.packageName.toLowerCase(Locale.ROOT).contains(needle)) {
                matches.add(app);
            }
        }
        showDirectory(matches, "C:\\APPS\\SEARCH\\" + argument.toUpperCase(Locale.getDefault()));
    }

    private void launchApp(AppEntry app) {
        Intent launch = new Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(launch);
        } catch (ActivityNotFoundException | SecurityException exception) {
            appendLine("UNABLE TO START " + app.label.toUpperCase(Locale.getDefault()) + ".");
            reloadApps(false);
        }
    }

    private void changeColor(String value) {
        String name = value.toUpperCase(Locale.ROOT);
        switch (name) {
            case "GREEN": terminalColor = Color.rgb(51, 255, 102); break;
            case "AMBER": terminalColor = Color.rgb(255, 191, 0); break;
            case "CYAN": terminalColor = Color.rgb(0, 255, 255); break;
            case "WHITE": terminalColor = DOS_WHITE; break;
            default:
                appendLine("VALID COLORS: GREEN, AMBER, CYAN, WHITE.");
                return;
        }
        preferences.edit().putInt("terminalColor", terminalColor).apply();
        for (TextView view : terminalColorViews) {
            view.setTextColor(terminalColor);
            if ("terminalButton".equals(view.getTag())) {
                view.setBackground(bordered(terminalColor));
            }
        }
        commandView.setHintTextColor(withAlpha(terminalColor, 110));
        render();
    }

    private void changeFont(String value) {
        String size = value.toUpperCase(Locale.ROOT);
        switch (size) {
            case "SMALL": terminalTextSize = 12f; break;
            case "NORMAL": terminalTextSize = 15f; break;
            case "LARGE": terminalTextSize = 19f; break;
            default:
                appendLine("VALID FONT SIZES: SMALL, NORMAL, LARGE.");
                return;
        }
        preferences.edit().putFloat("terminalTextSize", terminalTextSize).apply();
        outputView.setTextSize(terminalTextSize);
        commandView.setTextSize(terminalTextSize);
        appendLine("FONT SIZE SET TO " + size + ".");
    }

    private int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private void maybeRequestHomeRole() {
        RoleManager manager = getSystemService(RoleManager.class);
        boolean shouldAsk = manager != null
            && manager.isRoleAvailable(RoleManager.ROLE_HOME)
            && !manager.isRoleHeld(RoleManager.ROLE_HOME)
            && !preferences.getBoolean("rolePrompted", false);
        if (shouldAsk) {
            preferences.edit().putBoolean("rolePrompted", true).apply();
            new Handler(Looper.getMainLooper()).postDelayed(this::requestHomeRole, 700);
        }
    }

    private void requestHomeRole() {
        RoleManager manager = getSystemService(RoleManager.class);
        if (manager == null || !manager.isRoleAvailable(RoleManager.ROLE_HOME)) {
            safeStart(new Intent(Settings.ACTION_HOME_SETTINGS), "HOME ROLE IS NOT AVAILABLE.");
            return;
        }
        if (manager.isRoleHeld(RoleManager.ROLE_HOME)) {
            appendLine("DOSDECK IS ALREADY THE DEFAULT HOME APP.");
            updateRoleState();
            return;
        }
        startActivityForResult(manager.createRequestRoleIntent(RoleManager.ROLE_HOME), REQUEST_HOME_ROLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_HOME_ROLE) {
            updateRoleState();
            appendLine(resultCode == RESULT_OK
                ? "DOSDECK IS NOW THE DEFAULT HOME APP."
                : "HOME ROLE WAS NOT GRANTED.");
        }
    }

    private void updateRoleState() {
        if (roleView == null) {
            return;
        }
        RoleManager manager = getSystemService(RoleManager.class);
        boolean held = manager != null
            && manager.isRoleAvailable(RoleManager.ROLE_HOME)
            && manager.isRoleHeld(RoleManager.ROLE_HOME);
        roleView.setText(held ? "[HOME: ACTIVE]" : "[SET DEFAULT]");
        roleView.setTextColor(held ? terminalColor : DOS_WHITE);
    }

    private void safeStart(Intent intent, String failureMessage) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException exception) {
            appendLine(failureMessage);
        }
    }

    private void appendLine(String text) {
        append(text + "\n");
        render();
    }

    private void append(String text) {
        screen.append(text);
    }

    private void render() {
        if (outputView == null) {
            return;
        }
        outputView.setText(screen, TextView.BufferType.SPANNABLE);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private final class AppClickSpan extends ClickableSpan {
        private final AppEntry entry;

        AppClickSpan(AppEntry entry) {
            this.entry = entry;
        }

        @Override
        public void onClick(View widget) {
            launchApp(entry);
        }

        @Override
        public void updateDrawState(TextPaint drawState) {
            drawState.setColor(terminalColor);
            drawState.setUnderlineText(true);
            drawState.setTypeface(Typeface.MONOSPACE);
        }
    }
}
