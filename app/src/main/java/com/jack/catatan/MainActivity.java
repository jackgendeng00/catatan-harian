package com.jack.catatan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
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
    // Urutan harus tetap sama dengan versi lama (disimpan sebagai indeks).
    static final String[] MOODS = {"Senang", "Biasa", "Sedih", "Marah", "Lelah"};
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    static final Locale ID = new Locale("id", "ID");
    static final int GUTTER = 20; // margin kiri-kanan halaman (dp)

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
    int bg, tx, sub, hair, accent;
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

    LinearLayout.LayoutParams lp(int w, int h) { return new LinearLayout.LayoutParams(w, h); }

    LinearLayout.LayoutParams lp(int w, int h, float weight) { return new LinearLayout.LayoutParams(w, h, weight); }

    LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    LinearLayout col() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    TextView tv(String s, float size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    /** Teks isi: serif. */
    TextView serif(String s, float size, int color, int style) {
        TextView t = tv(s, size, color);
        t.setTypeface(Typeface.create(Typeface.SERIF, style));
        return t;
    }

    /** Label kecil huruf kapital, monospace, sedikit renggang. */
    TextView label(String s, int color) {
        TextView t = tv(s.toUpperCase(ID), 10.5f, color);
        t.setTypeface(Typeface.MONOSPACE);
        t.setLetterSpacing(0.08f);
        return t;
    }

    TextView action(String s, int color, View.OnClickListener l) {
        TextView t = label(s, color);
        t.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        t.setPadding(dp(10), dp(10), dp(10), dp(10));
        t.setOnClickListener(l);
        return t;
    }

    View rule(int heightDp, int color) {
        View v = new View(this);
        v.setBackgroundColor(color);
        v.setLayoutParams(lp(MATCH, dp(heightDp)));
        return v;
    }

    void theme() {
        bg = dark ? 0xFF14110E : 0xFFF3EEE4;
        tx = dark ? 0xFFEFE8DA : 0xFF1C1814;
        sub = dark ? 0xFF9C9387 : 0xFF72695D;
        hair = dark ? 0xFF3A342C : 0xFFD3CABB;
        accent = dark ? 0xFFEE6A4D : 0xFFBF3A1F;
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        int flags = 0;
        if (!dark) {
            if (Build.VERSION.SDK_INT >= 23) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    String fmt(long ms) {
        return new SimpleDateFormat("EEE, d MMM yyyy · HH:mm", ID).format(new Date(ms));
    }

    String fmtLong(long ms) {
        return new SimpleDateFormat("EEEE, d MMMM yyyy · HH.mm", ID).format(new Date(ms));
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
        LinearLayout root = col();
        root.setBackgroundColor(bg);

        // Baris atas: tanggal edisi + aksi
        LinearLayout strip = row();
        strip.setGravity(Gravity.CENTER_VERTICAL);
        strip.setPadding(dp(GUTTER), dp(6), dp(GUTTER - 10), 0);
        TextView edition = label(fmtLong(System.currentTimeMillis()).split(" · ")[0], sub);
        strip.addView(edition, lp(0, WRAP, 1));
        strip.addView(action(dark ? "Terang" : "Gelap", sub, v -> {
            dark = !dark;
            prefs.edit().putBoolean("dark", dark).apply();
            showList();
        }));
        strip.addView(action("Ekspor", sub, v -> exportAll()));
        root.addView(strip);

        // Masthead
        SpannableString name = new SpannableString("Catatan\nHarian");
        name.setSpan(new StyleSpan(Typeface.ITALIC), 8, name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        name.setSpan(new ForegroundColorSpan(accent), 8, name.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView mast = new TextView(this);
        mast.setText(name);
        mast.setTextSize(48);
        mast.setTextColor(tx);
        mast.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        mast.setLineSpacing(0, 0.92f);
        mast.setLetterSpacing(-0.02f);
        mast.setPadding(dp(GUTTER), dp(4), dp(GUTTER), dp(12));
        root.addView(mast);

        // Garis ganda ala koran
        LinearLayout rules = col();
        rules.setPadding(dp(GUTTER), 0, dp(GUTTER), 0);
        rules.addView(rule(3, tx));
        rules.addView(rule(2, bg));
        rules.addView(rule(1, tx));
        root.addView(rules);

        // Pencarian
        final EditText search = new EditText(this);
        search.setHint("Cari di arsip");
        search.setText(query);
        search.setSingleLine(true);
        search.setTextSize(16);
        search.setTypeface(Typeface.create(Typeface.SERIF, Typeface.ITALIC));
        search.setTextColor(tx);
        search.setHintTextColor(sub);
        search.setBackground(null);
        search.setPadding(dp(GUTTER), dp(14), dp(GUTTER), dp(12));
        search.addTextChangedListener(new Watch(() -> {
            query = search.getText().toString();
            fillList();
        }));
        root.addView(search, lp(MATCH, WRAP));
        root.addView(rule(1, hair), lp(MATCH, dp(1), GUTTER, 0, GUTTER, 0));

        // Tab filter
        LinearLayout tabs = row();
        tabs.setPadding(dp(GUTTER), dp(12), dp(GUTTER), 0);
        String[] names = {"Semua", "Hari ini", "Disematkan"};
        for (int i = 0; i < names.length; i++) {
            final int f = i;
            LinearLayout tab = col();
            TextView t = label(names[i], filter == i ? tx : sub);
            if (filter == i) t.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
            t.setPadding(0, 0, 0, dp(6));
            tab.addView(t);
            tab.addView(rule(2, filter == i ? accent : bg));
            tab.setOnClickListener(v -> { filter = f; showList(); });
            tabs.addView(tab, lp(WRAP, WRAP, 0, 0, 22, 0));
        }
        root.addView(tabs);

        statTv = label("", sub);
        statTv.setPadding(dp(GUTTER), dp(14), dp(GUTTER), dp(2));
        root.addView(statTv);

        ScrollView sv = new ScrollView(this);
        sv.setVerticalScrollBarEnabled(false);
        listBox = col();
        listBox.setPadding(dp(GUTTER), 0, dp(GUTTER), dp(8));
        sv.addView(listBox);
        root.addView(sv, lp(MATCH, 0, 1));

        // Tombol tulis: blok polos, tanpa sudut bulat
        TextView add = tv("TULIS CATATAN BARU  →", 12.5f, bg);
        add.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        add.setLetterSpacing(0.1f);
        add.setGravity(Gravity.CENTER);
        add.setBackgroundColor(tx);
        add.setPadding(0, dp(16), 0, dp(16));
        add.setOnClickListener(v -> openEditor(null));
        root.addView(add, lp(MATCH, WRAP, GUTTER, 10, GUTTER, 16));

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
        statTv.setText((notes.size() + " catatan — " + today + " hari ini").toUpperCase(ID));
        if (shown.isEmpty()) {
            boolean filtered = !q.isEmpty() || filter != 0;
            LinearLayout empty = col();
            empty.setPadding(0, dp(48), 0, 0);
            empty.addView(serif(filtered ? "Tidak ada yang cocok." : "Halaman masih kosong.",
                    24, tx, Typeface.ITALIC));
            TextView hint = label(filtered ? "Coba kata kunci atau filter lain"
                    : "Mulai menulis lewat tombol di bawah", sub);
            hint.setPadding(0, dp(10), 0, 0);
            empty.addView(hint);
            listBox.addView(empty);
            return;
        }
        for (int i = 0; i < shown.size(); i++) listBox.addView(entryFor(shown.get(i), i == 0));
    }

    View entryFor(final Note n, boolean lead) {
        LinearLayout wrap = col();
        LinearLayout r = row();
        int pad = lead ? 20 : 16;
        r.setPadding(0, dp(pad), 0, dp(pad));

        // Kolom tanggal
        LinearLayout date = col();
        TextView day = serif(new SimpleDateFormat("d", ID).format(new Date(n.updated)),
                lead ? 36 : 30, tx, Typeface.NORMAL);
        TextView mon = label(new SimpleDateFormat("MMM", ID).format(new Date(n.updated)), sub);
        date.addView(day);
        date.addView(mon);
        r.addView(date, lp(dp(56), WRAP));

        // Kolom isi
        LinearLayout body = col();
        String kick = (n.pinned ? "Disematkan · " : "") + MOODS[n.mood];
        body.addView(label(kick, n.pinned ? accent : sub));

        String t = n.title.isEmpty() ? "Tanpa judul" : n.title;
        TextView h = serif(t, lead ? 26 : 19, tx, Typeface.BOLD);
        h.setLineSpacing(0, 1.05f);
        h.setMaxLines(lead ? 3 : 2);
        h.setEllipsize(TextUtils.TruncateAt.END);
        h.setPadding(0, dp(4), 0, 0);
        body.addView(h);

        if (!n.body.isEmpty()) {
            TextView ex = serif(n.body, 14.5f, sub, Typeface.NORMAL);
            ex.setLineSpacing(0, 1.25f);
            ex.setMaxLines(lead ? 4 : 2);
            ex.setEllipsize(TextUtils.TruncateAt.END);
            ex.setPadding(0, dp(6), 0, 0);
            body.addView(ex);
        }
        r.addView(body, lp(0, WRAP, 1));

        wrap.addView(r);
        wrap.addView(rule(1, hair));
        wrap.setOnClickListener(v -> openEditor(n));
        wrap.setOnLongClickListener(v -> { menuFor(n); return true; });
        return wrap;
    }

    void menuFor(final Note n) {
        new AlertDialog.Builder(this)
                .setItems(new String[]{n.pinned ? "Lepas sematan" : "Sematkan catatan", "Bagikan", "Hapus"}, (d, which) -> {
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
            sb.append(fmt(n.created)).append(" · ").append(MOODS[n.mood]).append("\n");
            if (!n.title.isEmpty()) sb.append(n.title).append("\n");
            sb.append(n.body).append("\n\n---\n\n");
        }
        share(sb.toString());
    }

    // ---------- layar editor ----------
    void paintMoods(TextView[] mv) {
        for (int i = 0; i < mv.length; i++) {
            boolean on = i == editing.mood;
            mv[i].setTextColor(on ? accent : sub);
            int f = mv[i].getPaintFlags();
            mv[i].setPaintFlags(on ? (f | Paint.UNDERLINE_TEXT_FLAG) : (f & ~Paint.UNDERLINE_TEXT_FLAG));
        }
    }

    void paintPin(TextView pin) {
        pin.setText((editing.pinned ? "Disematkan" : "Sematkan").toUpperCase(ID));
        pin.setTextColor(editing.pinned ? accent : sub);
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
        LinearLayout root = col();
        root.setBackgroundColor(bg);

        // Bar atas
        LinearLayout bar = row();
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(GUTTER - 10), dp(6), dp(GUTTER - 10), dp(6));
        bar.addView(action("← Arsip", tx, v -> { commit(); showList(); }), lp(0, WRAP, 1));
        bar.addView(action("Simpan", accent, v -> {
            if (commit()) Toast.makeText(this, "Tersimpan", Toast.LENGTH_SHORT).show();
            showList();
        }));
        root.addView(bar);
        root.addView(rule(2, tx), lp(MATCH, dp(2), GUTTER, 0, GUTTER, 0));

        // Toolbar sekunder
        LinearLayout tools = row();
        tools.setGravity(Gravity.CENTER_VERTICAL);
        tools.setPadding(dp(GUTTER - 10), dp(2), dp(GUTTER - 10), dp(2));
        final TextView pin = action("", sub, null);
        paintPin(pin);
        pin.setOnClickListener(v -> { editing.pinned = !editing.pinned; paintPin(pin); });
        tools.addView(pin);
        tools.addView(action("Bagikan", sub, v -> share(
                (etTitle.getText().toString().trim() + "\n\n" + etBody.getText().toString().trim()).trim())));
        if (!isNew) tools.addView(action("Hapus", sub, v -> confirmDelete(editing)));
        root.addView(tools);
        root.addView(rule(1, hair), lp(MATCH, dp(1), GUTTER, 0, GUTTER, 0));

        // Tanggal + suasana
        TextView when = label(fmtLong(editing.created), sub);
        when.setPadding(dp(GUTTER), dp(16), dp(GUTTER), 0);
        root.addView(when);

        LinearLayout moods = row();
        moods.setPadding(dp(GUTTER), dp(10), dp(GUTTER), 0);
        final TextView[] mv = new TextView[MOODS.length];
        for (int i = 0; i < MOODS.length; i++) {
            final int m = i;
            mv[i] = serif(MOODS[i], 14.5f, sub, Typeface.ITALIC);
            mv[i].setPadding(0, dp(4), 0, dp(4));
            mv[i].setOnClickListener(v -> { editing.mood = m; paintMoods(mv); });
            moods.addView(mv[i], lp(WRAP, WRAP, 0, 0, 16, 0));
        }
        paintMoods(mv);
        root.addView(moods);

        // Judul
        etTitle = new EditText(this);
        etTitle.setHint("Judul");
        etTitle.setText(editing.title);
        etTitle.setSingleLine(true);
        etTitle.setTextSize(26);
        etTitle.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
        etTitle.setTextColor(tx);
        etTitle.setHintTextColor(hair);
        etTitle.setBackground(null);
        etTitle.setPadding(dp(GUTTER), dp(14), dp(GUTTER), dp(8));
        root.addView(etTitle, lp(MATCH, WRAP));
        root.addView(rule(1, hair), lp(MATCH, dp(1), GUTTER, 0, GUTTER, 0));

        // Isi
        etBody = new EditText(this);
        etBody.setHint("Tulis ceritamu hari ini…");
        etBody.setText(editing.body);
        etBody.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        etBody.setGravity(Gravity.TOP | Gravity.START);
        etBody.setTextSize(17);
        etBody.setTypeface(Typeface.create(Typeface.SERIF, Typeface.NORMAL));
        etBody.setLineSpacing(0, 1.4f);
        etBody.setTextColor(tx);
        etBody.setHintTextColor(sub);
        etBody.setBackground(null);
        etBody.setPadding(dp(GUTTER), dp(14), dp(GUTTER), dp(8));
        root.addView(etBody, lp(MATCH, 0, 1));

        // Footer hitung kata
        root.addView(rule(1, hair), lp(MATCH, dp(1), GUTTER, 0, GUTTER, 0));
        final TextView info = label("", sub);
        info.setPadding(dp(GUTTER), dp(10), dp(GUTTER), dp(12));
        root.addView(info);
        Runnable upd = () -> {
            String s = etBody.getText().toString().trim();
            int words = s.isEmpty() ? 0 : s.split("\\s+").length;
            info.setText((words + " kata — " + s.length() + " karakter").toUpperCase(ID));
        };
        upd.run();
        etBody.addTextChangedListener(new Watch(upd));

        setContentView(root);
    }
}
