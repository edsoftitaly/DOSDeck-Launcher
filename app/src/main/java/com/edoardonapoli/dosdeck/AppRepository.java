package com.edoardonapoli.dosdeck;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AppRepository {
    private final Context context;

    AppRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    List<AppEntry> loadLaunchableApps() {
        PackageManager packageManager = context.getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            resolved = packageManager.queryIntentActivities(query, PackageManager.ResolveInfoFlags.of(0));
        } else {
            resolved = packageManager.queryIntentActivities(query, 0);
        }

        Map<String, AppEntry> unique = new LinkedHashMap<>();
        for (ResolveInfo info : resolved) {
            if (info.activityInfo == null || info.activityInfo.packageName.equals(context.getPackageName())) {
                continue;
            }
            CharSequence rawLabel = info.loadLabel(packageManager);
            String label = rawLabel == null ? info.activityInfo.packageName : rawLabel.toString().trim();
            ComponentName component = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
            String key = component.flattenToString();
            unique.put(key, new AppEntry(label, info.activityInfo.packageName, component));
        }

        List<AppEntry> apps = new ArrayList<>(unique.values());
        Collections.sort(apps);
        return apps;
    }
}
