package com.jack.catatan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    static final String[] MOODS = {"😊", "😐", "😢", "😡", "😴"};
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    static final int ACCENT = 0xFF6C5CE7;

    static class Note {
        long id, created, updated;
        String title = "", body = "";
        int mood;
        boolean pinned;
    }

    static class Watch implements TextWatcher {
        final Runnable r;
        Watch(Runnable r) { this.r = r; }
        public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
        public void onTextChanged(CharSequence s, int a, int b, int c) { r.run(); }
        public void afterTextChanged(Editable e) {}
    }

    SharedPreferences prefs;
    List<Note> notes = new ArrayList<>();
    boolean dark, isNew;
    int filter, screen;
    String query = "";
    Note editing;
    int bg, card, tx, sub;
    LinearLayout listBox;
    TextView statTv;
    EditText etTitle, etBody;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("catatan", MODE_PRIVATE);
        dark = prefs.getBoolean("dark", false);
        load();
        showList();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (screen == 1 && editing != null) commit();
    }

    @Override
    public void onBackPressed() {
        if (screen == 1) { commit(); showList(); }
        else super.onBackPressed();
    }

    // ---------- data ----------
    void load() {
        notes.clear();
        try {
            JSONArray a = new JSONArray(prefs.getString("notes", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Note n = new Note();
                n.id = o.optLong("id");
                n.title = o.optString("t");
                n.body = o.optString("b");
                n.created = o.optLong("c");
                n.updated = o.optLong("u");
                n.mood = Math.max(0, Math.min(MOODS.length - 1, o.optInt("m")));
                n.pinned = o.optBoolean("p");
                notes.add(n);
            }
        } catch (Exception e) { /* data kosong */ }
    }

    void save() {
        try {
            JSONArray a = new JSONArray();
            for (Note n : notes) {
                JSONObject o = new JSONObject();
                o.put("id", n.id);
                o.put("t", n.title);
                o.put("b", n.body);
                o.put("c", n.created);
                o.put("u", n.updated);
                o.put("m", n.mood);
                o.put("p", n.pinned);
                a.put(o);
            }
            prefs.edit().putString("notes", a.toString()).apply();
        } catch (Exception e) { /* abaikan */ }
    }

    boolean commit() {
        if (etTitle == null || etBody == null || editing == null) return false;
        String t = etTitle.getText().toString().trim();
        String b = etBody.getText().toString().trim();
        if (t.isEmpty() && b.isEmpty()) return false;
        editing.title = t;
        editing.body = b;
        editing.updated = System.currentTimeMillis();
        if (isNew) { notes.add(editing); isNew = false; }
        save();
        return true;
    }

    // ---------- helpers ----------
    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    GradientDrawable box(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    TextView tv(String s, int size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    TextView iconBtn(String s, View.OnClickListener l) {
        TextView t = tv(s, 20, Color.WHITE);
        t.setPadding(dp(12), dp(6), dp(12), dp(6));
        t.setOnClickListener(l);
        return t;
    }

    LinearLayout header() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.HORIZONTAL);
        h.setGravity(Gravity.CENTER_VERTICAL);
        h.setBackgroundColor(ACCENT);
        h.setPadding(dp(8), dp(10), dp(4), dp(10));
        return h;
    }

    void theme() {
        bg = dark ? 0xFF121212 : 0xFFF5F5F7;
        card = dark ? 0xFF1E1E1E : 0xFFFFFFFF;
        tx = dark ? 0xFFEEEEEE : 0xFF1B1B1F;
        sub = dark ? 0xFF9A9A9A : 0xFF6B6B70;
        getWindow().setStatusBarColor(ACCENT);
    }

    String fmt(long ms) {
        return new SimpleDateFormat("EEE, d MMM yyyy · HH:mm", new Locale("id", "ID")).format(new Date(ms));
    }

    long startOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    String noteText(Note n) {
        return (n.title.isEmpty() ? "" : n.title + "\n\n") + n.body;
    }

    void share(String text) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(i, "Bagikan"));
    }

    // ---------- layar daftar ----------
    void showList() {
        screen = 0;
        editing = null;
        theme();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);

        LinearLayout head = header();
        head.setPadding(dp(16), dp(10), dp(4), dp(10));
        TextView title = tv("Catatan Harian", 20, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(title, new LinearLayout.LayoutParams(0, WRAP, 1));
        head.addView(iconBtn(dark ? "☀️" : "🌙", v -> {
            dark = !dark;
            prefs.edit().putBoolean("dark", dark).apply();
            showList();
        }));
        head.addView(iconBtn("📤", v -> exportAll()));
        root.addView(head);

        final EditText search = new EditText(this);
        search.setHint("Cari catatan...");
        search.setText(query);
        search.setSingleLine(true);
        search.setTextColor(tx);
        search.setHintTextColor(sub);
        search.setBackground(box(card, 12));
        search.setPadding(dp(14), dp(10), dp(14), dp(10));
        search.addTextChangedListener(new Watch(() -> {
            query = search.getText().toString();
            fillList();
        }));
        LinearLayout.LayoutParams sp1 = new LinearLayout.LayoutParams(MATCH, WRAP);
        sp1.setMargins(dp(12), dp(12), dp(12), dp(4));
        root.addView(search, sp1);

        LinearLayout chips = new LinearLayout(this);
        chips.setPadding(dp(12), dp(4), dp(12), dp(4));
        String[] names = {"Semua", "Hari ini", "📌 Pin"};
        for (int i = 0; i < names.length; i++) {
            final int f = i;
            TextView c = tv(names[i], 14, filter == i ? Color.WHITE : tx);
            c.setBackground(box(filter == i ? ACCENT : card, 20));
            c.setPadding(dp(16), dp(8), dp(16), dp(8));
            c.setOnClickListener(v -> { filter = f; showList(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(WRAP, WRAP);
            lp.setMargins(0, 0, dp(8), 0);
            chips.addView(c, lp);
        }
        root.addView(chips);

        statTv = tv("", 12, sub);
        statTv.setPadding(dp(16), dp(4), dp(16), dp(4));
        root.addView(statTv);

        ScrollView sv = new ScrollView(this);
        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        listBox.setPadding(dp(12), 0, dp(12), dp(8));
        sv.addView(listBox);
        root.addView(sv, new LinearLayout.LayoutParams(MATCH, 0, 1));

        TextView add = tv("+  Catatan baru", 16, Color.WHITE);
        add.setGravity(Gravity.CENTER);
        add.setTypeface(Typeface.DEFAULT_BOLD);
        add.setBackground(box(ACCENT, 28));
        add.setPadding(0, dp(14), 0, dp(14));
        add.setOnClickListener(v -> openEditor(null));
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(MATCH, WRAP);
        ap.setMargins(dp(16), dp(8), dp(16), dp(16));
        root.addView(add, ap);

        setContentView(root);
        fillList();
    }

    void fillList() {
        listBox.removeAllViews();
        String q = query.toLowerCase(Locale.getDefault()).trim();
        long start = startOfToday();
        int today = 0;
        List<Note> shown = new ArrayList<>();
        for (Note n : notes) {
            if (n.created >= start) today++;
            if (filter == 1 && n.created < start) continue;
            if (filter == 2 && !n.pinned) continue;
            if (!q.isEmpty() && !(n.title + " " + n.body).toLowerCase(Locale.getDefault()).contains(q)) continue;
            shown.add(n);
        }
        Collections.sort(shown, (x, y) -> x.pinned != y.pinned
                ? (x.pinned ? -1 : 1)
                : Long.compare(y.updated, x.updated));
        statTv.setText(notes.size() + " catatan · " + today + " dibuat hari ini");
        if (shown.isEmpty()) {
            TextView e = tv("Belum ada catatan.\nKetuk \"+ Catatan baru\" untuk mulai.", 14, sub);
            e.setGravity(Gravity.CENTER);
            e.setPadding(0, dp(48), 0, 0);
            listBox.addView(e);
            return;
        }
        for (Note n : shown) listBox.addView(cardFor(n));
    }

    View cardFor(final Note n) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(box(card, 14));
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        String t = n.title.isEmpty() ? "(tanpa judul)" : n.title;
        TextView h = tv(MOODS[n.mood] + " " + (n.pinned ? "📌 " : "") + t, 16, tx);
        h.setTypeface(Typeface.DEFAULT_BOLD);
        h.setSingleLine(true);
        h.setEllipsize(TextUtils.TruncateAt.END);
        c.addView(h);
        if (!n.body.isEmpty()) {
            TextView body = tv(n.body, 14, sub);
            body.setMaxLines(2);
            body.setEllipsize(TextUtils.TruncateAt.END);
            c.addView(body);
        }
        TextView date = tv(fmt(n.updated), 12, sub);
        date.setPadding(0, dp(6), 0, 0);
        c.addView(date);
        c.setOnClickListener(v -> openEditor(n));
        c.setOnLongClickListener(v -> { menuFor(n); return true; });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
        lp.setMargins(0, dp(8), 0, 0);
        c.setLayoutParams(lp);
        return c;
    }

    void menuFor(final Note n) {
        new AlertDialog.Builder(this)
                .setItems(new String[]{n.pinned ? "Lepas pin" : "Pin catatan", "Bagikan", "Hapus"}, (d, which) -> {
                    if (which == 0) { n.pinned = !n.pinned; save(); fillList(); }
                    else if (which == 1) share(noteText(n));
                    else confirmDelete(n);
                }).show();
    }

    void confirmDelete(final Note n) {
        new AlertDialog.Builder(this)
                .setTitle("Hapus catatan?")
                .setMessage("Catatan ini akan dihapus permanen.")
                .setPositiveButton("Hapus", (d, w) -> {
                    notes.remove(n);
                    save();
                    showList();
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    void exportAll() {
        if (notes.isEmpty()) {
            Toast.makeText(this, "Belum ada catatan", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Note> l = new ArrayList<>(notes);
        Collections.sort(l, (x, y) -> Long.compare(x.created, y.created));
        StringBuilder sb = new StringBuilder("CATATAN HARIAN\n\n");
        for (Note n : l) {
            sb.append(fmt(n.created)).append(" ").append(MOODS[n.mood]).append("\n");
            if (!n.title.isEmpty()) sb.append(n.title).append("\n");
            sb.append(n.body).append("\n\n---\n\n");
        }
        share(sb.toString());
    }

    // ---------- layar editor ----------
    void paintMoods(TextView[] mv) {
        for (int i = 0; i < mv.length; i++) {
            boolean on = i == editing.mood;
            mv[i].setBackground(on ? box(0x336C5CE7, 14) : null);
            mv[i].setAlpha(on ? 1f : 0.45f);
        }
    }

    void openEditor(Note n) {
        isNew = n == null;
        editing = isNew ? new Note() : n;
        if (isNew) {
            editing.id = System.currentTimeMillis();
            editing.created = editing.id;
        }
        screen = 1;
        theme();
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);

        LinearLayout head = header();
        head.addView(iconBtn("←", v -> { commit(); showList(); }));
        TextView ttl = tv(isNew ? "Catatan baru" : "Edit catatan", 18, Color.WHITE);
        ttl.setTypeface(Typeface.DEFAULT_BOLD);
        head.addView(ttl, new LinearLayout.LayoutParams(0, WRAP, 1));

        final TextView pin = iconBtn("📌", null);
        pin.setAlpha(editing.pinned ? 1f : 0.4f);
        pin.setOnClickListener(v -> {
            editing.pinned = !editing.pinned;
            pin.setAlpha(editing.pinned ? 1f : 0.4f);
        });
        head.addView(pin);
        head.addView(iconBtn("🔗", v -> share(
                (etTitle.getText().toString().trim() + "\n\n" + etBody.getText().toString().trim()).trim())));
        if (!isNew) head.addView(iconBtn("🗑", v -> confirmDelete(editing)));
        head.addView(iconBtn("✓", v -> {
            if (commit()) Toast.makeText(this, "Tersimpan", Toast.LENGTH_SHORT).show();
            showList();
        }));
        root.addView(head);

        LinearLayout moods = new LinearLayout(this);
        moods.setPadding(dp(12), dp(12), dp(12), dp(4));
        final TextView[] mv = new TextView[MOODS.length];
        for (int i = 0; i < MOODS.length; i++) {
            final int m = i;
            mv[i] = tv(MOODS[i], 24, tx);
            mv[i].setPadding(dp(12), dp(6), dp(12), dp(6));
            mv[i].setOnClickListener(v -> { editing.mood = m; paintMoods(mv); });
            moods.addView(mv[i]);
        }
        paintMoods(mv);
        root.addView(moods);

        etTitle = new EditText(this);
        etTitle.setHint("Judul");
        etTitle.setText(editing.title);
        etTitle.setSingleLine(true);
        etTitle.setTextSize(20);
        etTitle.setTypeface(Typeface.DEFAULT_BOLD);
        etTitle.setTextColor(tx);
        etTitle.setHintTextColor(sub);
        etTitle.setBackground(null);
        etTitle.setPadding(dp(16), dp(8), dp(16), dp(8));
        root.addView(etTitle, new LinearLayout.LayoutParams(MATCH, WRAP));

        etBody = new EditText(this);
        etBody.setHint("Tulis ceritamu hari ini...");
        etBody.setText(editing.body);
        etBody.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        etBody.setGravity(Gravity.TOP | Gravity.START);
        etBody.setTextSize(16);
        etBody.setTextColor(tx);
        etBody.setHintTextColor(sub);
        etBody.setBackground(null);
        etBody.setPadding(dp(16), dp(8), dp(16), dp(8));
        root.addView(etBody, new LinearLayout.LayoutParams(MATCH, 0, 1));

        final TextView info = tv("", 12, sub);
        info.setPadding(dp(16), dp(6), dp(16), dp(10));
        root.addView(info);
        Runnable upd = () -> {
            String s = etBody.getText().toString().trim();
            int words = s.isEmpty() ? 0 : s.split("\\s+").length;
            info.setText(words + " kata · " + s.length() + " karakter · " + fmt(editing.created));
        };
        upd.run();
        etBody.addTextChangedListener(new Watch(upd));

        setContentView(root);
    }
}
