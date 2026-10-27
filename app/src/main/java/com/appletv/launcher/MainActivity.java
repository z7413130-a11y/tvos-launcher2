package com.appletv.launcher;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends Activity {

    private final List<AppInfo> appList = new ArrayList<>();

    public static class AppInfo {
        CharSequence label;
        String packageName;

        public AppInfo(CharSequence label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }

        @Override
        public String toString() {
            return label != null ? label.toString() : packageName;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = findViewById(R.id.apps_list);
        loadInstalledApps();

        ArrayAdapter<AppInfo> adapter = new ArrayAdapter<AppInfo>(this, android.R.layout.simple_list_item_1, appList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = view.findViewById(android.R.id.text1);
                if (tv != null) {
                    tv.setTextColor(Color.WHITE);
                    tv.setTextSize(22f);
                    tv.setPadding(30, 30, 30, 30);
                }
                return view;
            }
        };

        listView.setAdapter(adapter);
        listView.requestFocus();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            AppInfo app = appList.get(position);
            Intent intent = getPackageManager().getLaunchIntentForPackage(app.packageName);
            if (intent != null) {
                startActivity(intent);
            }
        });
    }

    private void loadInstalledApps() {
        PackageManager pm = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> availableActivities = pm.queryIntentActivities(intent, 0);

        appList.clear();
        for (ResolveInfo ri : availableActivities) {
            if (!ri.activityInfo.packageName.equals(getPackageName())) {
                appList.add(new AppInfo(ri.loadLabel(pm), ri.activityInfo.packageName));
            }
        }
        
        Collections.sort(appList, (a, b) -> a.toString().compareToIgnoreCase(b.toString()));
    }
}
