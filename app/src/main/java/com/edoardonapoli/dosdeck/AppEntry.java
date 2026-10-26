package com.edoardonapoli.dosdeck;

import android.content.ComponentName;

import java.text.Collator;
import java.util.Locale;

final class AppEntry implements Comparable<AppEntry> {
    private static final Collator COLLATOR = Collator.getInstance(Locale.getDefault());

    final String label;
    final String packageName;
    final ComponentName component;

    AppEntry(String label, String packageName, ComponentName component) {
        this.label = label;
        this.packageName = packageName;
        this.component = component;
    }

    @Override
    public int compareTo(AppEntry other) {
        int byLabel = COLLATOR.compare(label, other.label);
        return byLabel != 0 ? byLabel : packageName.compareToIgnoreCase(other.packageName);
    }
}
