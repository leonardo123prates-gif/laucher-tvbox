package com.boxlauncher.tv;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Launcher minimo para TV box: uma unica tela, sem servicos, sem rede, sem
 * imagens proprias. Tema escuro estilo Google TV.
 */
public class MainActivity extends Activity {

    private static final int BG = 0xFF0B0B10;
    private static final int TILE = 0xFF1B1C24;
    private static final int TILE_FOCUS = 0xFF2B2F45;
    private static final int ACCENT = 0xFF8AB4F8;

    private PackageManager pm;
    private FrameLayout root;
    private View homeView;
    private LinearLayout tileRow;
    private GridView grid;

    private static class AppEntry {
        String pkg;
        String label;
        Drawable icon;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pm = getPackageManager();
        root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        homeView = buildHome();
        root.addView(homeView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
        focusFirstTile();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        showHome();
    }

    @Override
    public void onBackPressed() {
        if (grid != null && grid.getVisibility() == View.VISIBLE) {
            showHome();
        }
        // Na tela inicial o botao Voltar nao faz nada (como todo launcher).
    }

    // ---------------------------------------------------------------- home

    private View buildHome() {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(56), dp(32), dp(56), dp(32));

        LinearLayout clockBox = new LinearLayout(this);
        clockBox.setOrientation(LinearLayout.VERTICAL);
        clockBox.setGravity(Gravity.END);
        clockBox.addView(clock("HH:mm", 44, 0xFFFFFFFF));
        clockBox.addView(clock("EEE, d MMM", 16, 0xFF9AA0A6));
        col.addView(clockBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        col.addView(new View(this), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        tileRow = new LinearLayout(this);
        tileRow.setOrientation(LinearLayout.HORIZONTAL);
        tileRow.setGravity(Gravity.CENTER);

        String[][] specs = {
                {"YouTube", "com.google.android.youtube.tv", "com.google.android.youtube"},
                {"Netflix", "com.netflix.mediaclient", "com.netflix.ninja"},
                {"Play Store", "com.android.vending"},
                {"Navegador", "com.brave.browser", "org.mozilla.firefox",
                        "org.mozilla.fenix", "com.android.chrome"},
        };
        for (String[] spec : specs) {
            final String pkg = firstInstalled(spec);
            if (pkg == null) continue;
            addTile(spec[0], iconOf(pkg), new Runnable() {
                @Override
                public void run() {
                    launchPkg(pkg);
                }
            });
        }

        addTile("Configurações", settingsIcon(), new Runnable() {
            @Override
            public void run() {
                try {
                    Intent i = new Intent(Settings.ACTION_SETTINGS);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } catch (Exception e) {
                    toast("Não foi possível abrir as configurações");
                }
            }
        });

        addTile("Todos os apps", whiteMenuIcon(android.R.drawable.ic_menu_agenda), new Runnable() {
            @Override
            public void run() {
                showGrid();
            }
        });

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setFillViewport(true);
        hs.addView(tileRow, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        col.addView(hs, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        col.addView(new View(this), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.4f));
        return col;
    }

    private void addTile(String label, Drawable icon, final Runnable action) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);
        t.setFocusable(true);
        t.setClickable(true);
        t.setBackground(tileBackground());

        ImageView iv = new ImageView(this);
        iv.setImageDrawable(icon);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        t.addView(iv, new LinearLayout.LayoutParams(dp(72), dp(72)));

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(0xFFE8EAED);
        tv.setTextSize(16);
        tv.setGravity(Gravity.CENTER);
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tvLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tvLp.topMargin = dp(12);
        t.addView(tv, tvLp);

        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
            }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(168), dp(150));
        lp.setMargins(dp(10), dp(10), dp(10), dp(10));
        tileRow.addView(t, lp);
    }

    private void showHome() {
        if (grid != null) grid.setVisibility(View.GONE);
        homeView.setVisibility(View.VISIBLE);
        focusFirstTile();
    }

    private void focusFirstTile() {
        if (tileRow != null && tileRow.getChildCount() > 0) {
            tileRow.getChildAt(0).requestFocus();
        }
    }

    // ---------------------------------------------------------------- grade de apps

    private void showGrid() {
        if (grid == null) buildGrid();
        homeView.setVisibility(View.GONE);
        grid.setVisibility(View.VISIBLE);
        grid.requestFocus();
    }

    private void buildGrid() {
        final List<AppEntry> apps = loadApps();
        grid = new GridView(this);
        grid.setNumColumns(GridView.AUTO_FIT);
        grid.setColumnWidth(dp(150));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setVerticalSpacing(dp(12));
        grid.setHorizontalSpacing(dp(12));
        grid.setPadding(dp(56), dp(40), dp(56), dp(40));
        grid.setClipToPadding(false);
        grid.setSelector(rounded(0x00000000, ACCENT, 3));
        grid.setDrawSelectorOnTop(true);
        grid.setAdapter(new AppAdapter(apps));
        grid.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(android.widget.AdapterView<?> parent, View view,
                                    int position, long id) {
                launchPkg(apps.get(position).pkg);
            }
        });
        root.addView(grid, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private List<AppEntry> loadApps() {
        Map<String, AppEntry> map = new HashMap<String, AppEntry>();
        String[] cats = {Intent.CATEGORY_LAUNCHER, Intent.CATEGORY_LEANBACK_LAUNCHER};
        for (String cat : cats) {
            Intent q = new Intent(Intent.ACTION_MAIN);
            q.addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName()) || map.containsKey(pkg)) continue;
                AppEntry e = new AppEntry();
                e.pkg = pkg;
                e.label = String.valueOf(ri.loadLabel(pm));
                e.icon = ri.loadIcon(pm);
                map.put(pkg, e);
            }
        }
        List<AppEntry> list = new ArrayList<AppEntry>(map.values());
        Collections.sort(list, new java.util.Comparator<AppEntry>() {
            @Override
            public int compare(AppEntry a, AppEntry b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return list;
    }

    private class AppAdapter extends BaseAdapter {
        private final List<AppEntry> items;

        AppAdapter(List<AppEntry> items) {
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout item;
            ImageView iv;
            TextView tv;
            if (convertView == null) {
                item = new LinearLayout(MainActivity.this);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setGravity(Gravity.CENTER);
                item.setPadding(dp(8), dp(8), dp(8), dp(8));
                item.setLayoutParams(new GridView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(112)));
                iv = new ImageView(MainActivity.this);
                iv.setId(1);
                item.addView(iv, new LinearLayout.LayoutParams(dp(56), dp(56)));
                tv = new TextView(MainActivity.this);
                tv.setId(2);
                tv.setTextColor(0xFFE8EAED);
                tv.setTextSize(14);
                tv.setGravity(Gravity.CENTER);
                tv.setSingleLine(true);
                tv.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = dp(8);
                item.addView(tv, lp);
            } else {
                item = (LinearLayout) convertView;
            }
            iv = (ImageView) item.findViewById(1);
            tv = (TextView) item.findViewById(2);
            AppEntry e = items.get(position);
            iv.setImageDrawable(e.icon);
            tv.setText(e.label);
            return item;
        }
    }

    // ---------------------------------------------------------------- lancamento

    private Intent launchIntent(String pkg) {
        Intent i = pm.getLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLeanbackLaunchIntentForPackage(pkg);
        return i;
    }

    private String firstInstalled(String[] spec) {
        for (int i = 1; i < spec.length; i++) {
            if (launchIntent(spec[i]) != null) return spec[i];
        }
        return null;
    }

    private void launchPkg(String pkg) {
        Intent i = launchIntent(pkg);
        if (i == null) {
            toast("App não encontrado");
            return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (Exception e) {
            toast("Não foi possível abrir o app");
        }
    }

    // ---------------------------------------------------------------- visual

    private Drawable iconOf(String pkg) {
        try {
            return pm.getApplicationIcon(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            return new ColorDrawable(0xFF3C4043);
        }
    }

    private Drawable settingsIcon() {
        String[] pkgs = {"com.android.tv.settings", "com.android.settings"};
        for (String p : pkgs) {
            try {
                return pm.getApplicationIcon(p);
            } catch (PackageManager.NameNotFoundException ignored) {
                // tenta o proximo
            }
        }
        return whiteMenuIcon(android.R.drawable.ic_menu_preferences);
    }

    @SuppressWarnings("deprecation")
    private Drawable whiteMenuIcon(int resId) {
        Drawable d = getResources().getDrawable(resId).mutate();
        d.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN);
        return d;
    }

    private Drawable tileBackground() {
        StateListDrawable s = new StateListDrawable();
        s.addState(new int[]{android.R.attr.state_focused}, rounded(TILE_FOCUS, ACCENT, 3));
        s.addState(new int[]{android.R.attr.state_pressed}, rounded(TILE_FOCUS, ACCENT, 3));
        s.addState(new int[]{}, rounded(TILE, 0, 0));
        return s;
    }

    private GradientDrawable rounded(int fill, int stroke, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(16));
        if (strokeDp > 0) g.setStroke(dp(strokeDp), stroke);
        return g;
    }

    private TextClock clock(String format, int sp, int color) {
        TextClock c = new TextClock(this);
        c.setFormat24Hour(format);
        c.setFormat12Hour(format);
        c.setTextColor(color);
        c.setTextSize(sp);
        c.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        return c;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
