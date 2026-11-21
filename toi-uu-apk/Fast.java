package vn.personal.vietkey;

import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.TextPaint;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Lop tang toc cho VietSmartKey (ban va 0.56-fast).
 * Moi ham o day tra ve dung ket qua nhu ham goc; chi bo phan tinh lap lai:
 *  - regex: String.matches/replaceAll/split bien dich lai Pattern o moi lan goi -> dung Pattern da bien dich san
 *  - fold()/clean(): chuan hoa Unicode (Normalizer) cho cung mot chuoi hang nghin lan moi phim -> nho ket qua
 *  - TypoModel.cost(): cap phat mang 2 chieu bang phan xa (Array.newInstance) cho moi tu -> dung lai mang 1 chieu
 *  - suggest(): chay 2 lan moi phim voi cung tham so -> ghi nho ket qua; vong tu da hoc khong tinh thua
 *  - refreshSuggestions(): goi previousWord() (IPC dong bo sang app dang go) 2 lan -> 1 lan
 *  - KeyboardView.themeColor(): ~1000 lan doc SharedPreferences moi khung hinh -> nho theo chu de
 */
public final class Fast {
    private Fast() {}

    // ---------------------------------------------------------------- regex
    private static final ConcurrentHashMap<String, Pattern> PATTERNS = new ConcurrentHashMap<>();

    static Pattern pattern(String regex) {
        Pattern p = PATTERNS.get(regex);
        if (p == null) {
            p = Pattern.compile(regex);
            if (PATTERNS.size() > 512) PATTERNS.clear();
            PATTERNS.put(regex, p);
        }
        return p;
    }

    public static boolean matches(String s, String regex) {
        return pattern(regex).matcher(s).matches();
    }

    public static String replaceAll(String s, String regex, String replacement) {
        return pattern(regex).matcher(s).replaceAll(replacement);
    }

    private static boolean fastSplit(String regex) {
        int n = regex.length();
        if (n == 1) return ".$|()[{^?*+\\".indexOf(regex.charAt(0)) < 0;
        if (n == 2 && regex.charAt(0) == '\\') {
            char c = regex.charAt(1);
            return !((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) && c < 128;
        }
        return false;
    }

    public static String[] split(String s, String regex) {
        return split(s, regex, 0);
    }

    public static String[] split(String s, String regex, int limit) {
        if (fastSplit(regex)) return s.split(regex, limit);
        if (s == null) throw new NullPointerException();
        return pattern(regex).split(s, limit);
    }

    // ---------------------------------------------------------------- fold / clean caches
    private static final ConcurrentHashMap<String, String> FOLD = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, String> CLEAN = new ConcurrentHashMap<>();
    private static final int CACHE_MAX = 120000;

    public static String fold(String s) {
        String r = FOLD.get(s);
        if (r == null) {
            r = PersonalData.foldOrig(s);
            if (FOLD.size() > CACHE_MAX) FOLD.clear();
            FOLD.put(s, r);
        }
        return r;
    }

    public static String clean(String s) {
        String r = CLEAN.get(s);
        if (r == null) {
            r = LearnedModel.cleanOrig(s);
            if (CLEAN.size() > CACHE_MAX) CLEAN.clear();
            CLEAN.put(s, r);
        }
        return r;
    }

    // ---------------------------------------------------------------- edit distance without reflection

    // Bang khoang cach phim tinh san: cung gia tri double voi TypoModel.distance() (von goi Math.hypot moi lan)
    private static final double[] DIST = new double[128 * 128];
    private static final double[] SUB = new double[128 * 128];
    static {
        for (char x = 0; x < 128; x++) {
            for (char y = 0; y < 128; y++) {
                DIST[x * 128 + y] = TypoModel.distance(x, y);
                SUB[x * 128 + y] = TypoModel.substitute(x, y);
            }
        }
    }

    static double dist(char x, char y) {
        return (x < 128 && y < 128) ? DIST[x * 128 + y] : 9.0d;
    }

    static double sub(char x, char y) {
        return (x < 128 && y < 128) ? SUB[x * 128 + y] : TypoModel.substitute(x, y);
    }

    /** = TypoModel.insert(str, i) */
    static double ins(String str, int i) {
        char c = str.charAt(i);
        if (i > 0 && str.charAt(i - 1) == c) return 0.5d;
        if (i > 0 && dist(str.charAt(i - 1), c) <= 1.15d) return 0.6d;
        int k = i + 1;
        if (k >= str.length() || dist(str.charAt(k), c) > 1.15d) return TypoModel.toneKey(c) ? 0.7d : 0.9d;
        return 0.6d;
    }

    /** = TypoModel.delete(str, i) */
    static double del(String str, int i) {
        char c = str.charAt(i);
        return ((TypoModel.toneKey(c) && i == str.length() - 1) || TypoModel.shapeKey(c) || (i > 0 && str.charAt(i - 1) == c)) ? 0.45d : 0.85d;
    }

    private static final ThreadLocal<double[][]> BUF = new ThreadLocal<double[][]>() {
        @Override protected double[][] initialValue() { return new double[][]{new double[32 * 32], new double[32], new double[32]}; }
    };

    public static double cost(String a, String b, double limit) {
        int n = a.length();
        int m = b.length();
        if (Math.abs(n - m) > 3) return 99.0d;
        int w = m + 1;
        int size = (n + 1) * w;
        double[][] bufs = BUF.get();
        double[] dp = bufs[0];
        if (dp.length < size) { dp = new double[Math.max(size, dp.length * 2)]; bufs[0] = dp; }
        double[] insA = bufs[1];
        if (insA.length < n + 1) { insA = new double[Math.max(n + 1, insA.length * 2)]; bufs[1] = insA; }
        double[] delB = bufs[2];
        if (delB.length < m + 1) { delB = new double[Math.max(m + 1, delB.length * 2)]; bufs[2] = delB; }
        for (int i = 0; i < n; i++) insA[i] = ins(a, i);
        for (int j = 0; j < m; j++) delB[j] = del(b, j);
        dp[0] = 0.0d;
        for (int i = 1; i <= n; i++) dp[i * w] = dp[(i - 1) * w] + insA[i - 1];
        for (int j = 1; j <= m; j++) dp[j] = dp[j - 1] + delB[j - 1];
        for (int i = 1; i <= n; i++) {
            double rowMin = 99.0d;
            int row = i * w, up = (i - 1) * w;
            char ai = a.charAt(i - 1);
            double insI = insA[i - 1];
            for (int j = 1; j <= m; j++) {
                double v = Math.min(sub(ai, b.charAt(j - 1)) + dp[up + j - 1],
                        Math.min(dp[up + j] + insI, dp[row + j - 1] + delB[j - 1]));
                if (i > 1 && j > 1 && ai == b.charAt(j - 2) && a.charAt(i - 2) == b.charAt(j - 1)) {
                    v = Math.min(v, dp[(i - 2) * w + j - 2] + 0.6d);
                }
                dp[row + j] = v;
                rowMin = Math.min(rowMin, v);
            }
            if (rowMin > limit) return 99.0d;
        }
        return dp[n * w + m];
    }

    // ---------------------------------------------------------------- chi muc tu dien (giu nguyen thu tu goc)
    static final class Index {
        PersonalData d;
        int nWords, nGeneral;
        PersonalData.Word[] all;          // words + generalWords, dung thu tu cua nextKeyOdds
        String[] lower;
        HashMap<Character, int[]> byKey;   // vi tri trong all, theo ky tu dau cua Word.keys
        HashMap<Character, int[]> byFold;  // vi tri trong generalWords, theo ky tu dau cua Word.folded
    }

    private static Index index;

    static int[] bucket(HashMap<Character, ArrayList<Integer>> tmp, Character c, int i) {
        ArrayList<Integer> l = tmp.get(c);
        if (l == null) { l = new ArrayList<>(); tmp.put(c, l); }
        l.add(i);
        return null;
    }

    static HashMap<Character, int[]> freeze(HashMap<Character, ArrayList<Integer>> tmp) {
        HashMap<Character, int[]> out = new HashMap<>();
        for (Map.Entry<Character, ArrayList<Integer>> e : tmp.entrySet()) {
            ArrayList<Integer> l = e.getValue();
            int[] a = new int[l.size()];
            for (int i = 0; i < a.length; i++) a[i] = l.get(i);
            out.put(e.getKey(), a);
        }
        return out;
    }

    static Index index(PersonalData d) {
        Index x = index;
        if (x != null && x.d == d && x.nWords == d.words.size() && x.nGeneral == d.generalWords.size()) return x;
        x = new Index();
        x.d = d;
        x.nWords = d.words.size();
        x.nGeneral = d.generalWords.size();
        int n = x.nWords + x.nGeneral;
        x.all = new PersonalData.Word[n];
        x.lower = new String[n];
        HashMap<Character, ArrayList<Integer>> k = new HashMap<>(), f = new HashMap<>();
        for (int i = 0; i < n; i++) {
            PersonalData.Word w = i < x.nWords ? d.words.get(i) : d.generalWords.get(i - x.nWords);
            x.all[i] = w;
            x.lower[i] = w.text.toLowerCase(Locale.ROOT);
            if (!w.keys.isEmpty()) bucket(k, Character.valueOf(w.keys.charAt(0)), i);
            if (i >= x.nWords && !w.folded.isEmpty()) bucket(f, Character.valueOf(w.folded.charAt(0)), i - x.nWords);
        }
        x.byKey = freeze(k);
        x.byFold = freeze(f);
        index = x;
        return x;
    }

    private static PersonalData oddsData;
    private static String oddsPrefix;
    private static float[] odds;
    private static final int[] EMPTY = new int[0];

    /** = PersonalData.nextKeyOdds(); chi duyet nhom tu co cung chu dau, cung thu tu nen tong float giong het. */
    public static float[] nextKeyOdds(PersonalData d, String str) {
        String conv = Telex.convert(str.toLowerCase(Locale.ROOT));
        String keys = TypoModel.keys(conv);
        if (Telex.tone(conv) > 0 && !keys.isEmpty()) keys = keys.substring(0, keys.length() - 1);
        if (oddsData == d && keys.equals(oddsPrefix)) return odds;
        Index x = index(d);
        float[] acc = new float[26];
        float total = 0.0f;
        int pl = keys.length();
        int[] idx = null;
        if (pl > 0) {
            idx = x.byKey.get(Character.valueOf(keys.charAt(0)));
            if (idx == null) idx = EMPTY;
        }
        int cnt = idx == null ? x.all.length : idx.length;
        for (int t = 0; t < cnt; t++) {
            int i = idx == null ? t : idx[t];
            PersonalData.Word w = x.all[i];
            String wk = w.keys;
            if (wk.length() <= pl || !wk.startsWith(keys) || d.weakPersonal(w)) continue;
            char c = wk.charAt(pl);
            if (c < 'a' || c > 'z') continue;
            String lower = x.lower[i];
            int v = d.frequencies.score(lower) + 1 + d.personalScore.getOrDefault(lower, 0).intValue() * 2
                    + Math.min(d.learned.count(lower), 10) * 5;
            float fv = (float) v;
            acc[c - 'a'] = acc[c - 'a'] + fv;
            total += fv;
        }
        float[] r = null;
        if (total > 0.0f) {
            r = new float[26];
            for (int i = 0; i < 26; i++) r[i] = ((0.01f * total) + acc[i]) / (1.26f * total);
        }
        oddsData = d;
        oddsPrefix = keys;
        odds = r;
        return r;
    }

    // ---------------------------------------------------------------- suggest (memo + no wasted work)
    private static PersonalData memoData;
    private static String memoA, memoB, memoC;
    private static int memoN, memoMethod, memoVersion, memoStrength, memoMacros, memoMacroGen;
    private static List<PersonalData.Candidate> memoResult;
    static volatile int macroGen;
    public static boolean memoEnabled = true;
    /** Trong so cap tu ngu canh khi doan tu (0 = nhu ban goc). */
    public static double ctxWeight = 12.0;
    public static boolean reserveSlot = true;
    public static int ctxMin = 1;

    public static void macrosChanged() {
        macroGen++;
    }

    static final Comparator<PersonalData.Candidate> BY_SCORE = new Comparator<PersonalData.Candidate>() {
        @Override public int compare(PersonalData.Candidate x, PersonalData.Candidate y) {
            return Integer.compare(y.score, x.score);
        }
    };

    public static List<PersonalData.Candidate> suggest(PersonalData d, String raw, String word, String prev, int n, int method) {
        if (memoEnabled && memoData == d && memoResult != null && memoN == n && memoMethod == method
                && memoVersion == d.learned.version && memoStrength == d.strength
                && memoMacros == d.macros.size() && memoMacroGen == macroGen
                && raw.equals(memoA) && word.equals(memoB) && prev.equals(memoC)) {
            return memoResult;
        }
        int gen = macroGen;
        List<PersonalData.Candidate> r = suggestFast(d, raw, word, prev, n, method);
        memoData = d; memoA = raw; memoB = word; memoC = prev; memoN = n; memoMethod = method;
        memoVersion = d.learned.version; memoStrength = d.strength; memoMacros = d.macros.size(); memoMacroGen = gen;
        memoResult = r;
        return r;
    }

    @SuppressWarnings("unchecked")
    static List<PersonalData.Candidate> suggestFast(PersonalData d, String str, String str2, String str3, int i, int i2) {
        ArrayList<PersonalData.Candidate> list = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        String exp = d.expansion(str, str2);
        if (exp != null) {
            list.add(new PersonalData.Candidate(exp, true, 1000000));
            seen.add(exp);
        }
        String corr = str2.length() >= 2 ? d.correct(str, str2, str3, i2) : str2;
        if (!corr.equals(str2) && seen.add(corr)) list.add(new PersonalData.Candidate(corr, false, 500000));
        if (str2.length() >= 2 && !d.known(str2)) {
            int k = 0;
            for (String alt : d.alternatives) {
                if (seen.add(alt)) {
                    list.add(new PersonalData.Candidate(alt, false, 400000 - k));
                    k++;
                }
            }
        }
        String strFold = fold(str2);
        boolean foldEmpty = strFold.isEmpty();
        String lower = str2.toLowerCase(Locale.ROOT);
        Integer num = d.ids.get(str3.toLowerCase(Locale.ROOT));
        Map<Integer, Integer> nextMap = num == null ? Collections.EMPTY_MAP : d.next.getOrDefault(num, new HashMap<Integer, Integer>());
        ArrayList<PersonalData.Word> words = d.words;
        for (int a = 0, sz = words.size(); a < sz; a++) {
            PersonalData.Word w = words.get(a);
            String t = w.text;
            if (t.isEmpty() || !Character.isLetter(t.codePointAt(0)) || seen.contains(t) || t.equalsIgnoreCase(str2)) continue;
            if (foldEmpty ? !nextMap.containsKey(Integer.valueOf(w.id)) : !w.folded.startsWith(strFold)) continue;
            int sc = nextMap.getOrDefault(Integer.valueOf(w.id), 0).intValue() * 100 + w.score
                    + (t.toLowerCase(Locale.ROOT).startsWith(lower) ? 1000 : 0);
            list.add(new PersonalData.Candidate(t, false, sc));
            seen.add(t);
        }
        if (!foldEmpty) {
            ArrayList<PersonalData.Word> general = d.generalWords;
            int[] ids = index(d).byFold.get(Character.valueOf(strFold.charAt(0)));
            if (ids == null) ids = EMPTY;
            for (int a = 0, sz = ids.length; a < sz; a++) {
                PersonalData.Word w = general.get(ids[a]);
                String t = w.text;
                // startsWith dat truoc: loai nhanh tu khong khop, cac dieu kien con lai khong co tac dung phu
                if (!w.folded.startsWith(strFold) || t.equalsIgnoreCase(str2) || seen.contains(t)) continue;
                list.add(new PersonalData.Candidate(t, false, t.startsWith(lower) ? 5 : 1));
                seen.add(t);
            }
        }
        String lower2 = str3.toLowerCase(Locale.ROOT);
        HashMap<String, Integer> phr = d.phraseNext.getOrDefault(lower2, new HashMap<String, Integer>());
        for (Map.Entry<String, Integer> e : phr.entrySet()) {
            String key = e.getKey();
            if (!key.equalsIgnoreCase(str2) && (foldEmpty || fold(key).startsWith(strFold)) && seen.add(key)) {
                list.add(new PersonalData.Candidate(key, false, e.getValue().intValue() * 5 + 20));
            }
        }
        // Cap tu hay di lien (Wikipedia/Tatoeba trong tu dien mo rong): ban goc chi dung de sua loi,
        // o day dung them de doan tu tiep theo va xep hang tu dang go do.
        HashMap<String, Integer> ctx = ctxWeight > 0 ? d.contextNext.get(lower2) : null;
        if (ctx != null) {
            for (Map.Entry<String, Integer> e : ctx.entrySet()) {
                String key = e.getKey();
                if (e.getValue().intValue() >= ctxMin && !key.equalsIgnoreCase(str2)
                        && (foldEmpty || fold(key).startsWith(strFold)) && seen.add(key)) {
                    list.add(new PersonalData.Candidate(key, false, 0));
                }
            }
        }
        LearnedModel learned = d.learned;
        Iterator<Map.Entry<String, Integer>> it = learned.words.entrySet().iterator();
        while (it.hasNext()) {
            String key2 = it.next().getKey();
            if (key2.equalsIgnoreCase(str2)) continue;
            if (foldEmpty) {
                if (learned.pairCount(lower2, key2) != 0) list.add(new PersonalData.Candidate(key2, false, 100));
            } else if (fold(key2).startsWith(strFold)) {
                list.add(new PersonalData.Candidate(key2, false, 100));
            }
        }
        java.util.IdentityHashMap<PersonalData.Candidate, Integer> boosted = new java.util.IdentityHashMap<>();
        for (int a = 0; a < list.size(); a++) {
            PersonalData.Candidate c = list.get(a);
            if (!c.macro) {
                String t = c.text;
                int bonus = 0;
                if (ctx != null) {
                    Integer n = ctx.get(t.toLowerCase(Locale.ROOT));
                    if (n != null && n.intValue() >= ctxMin) bonus = (int) Math.round(ctxWeight * Math.log1p(n.intValue()));
                }
                int base = learned.pairCount(lower2, c.text) * 250 + learned.count(c.text) * 20 + d.frequencies.score(t) + c.score;
                PersonalData.Candidate nc = new PersonalData.Candidate(t, false, base + bonus);
                list.set(a, nc);
                if (bonus > 0) boosted.put(nc, base);
            }
        }
        HashMap<String, PersonalData.Candidate> best = new HashMap<>();
        for (int a = 0, sz = list.size(); a < sz; a++) {
            PersonalData.Candidate c = list.get(a);
            String lk = c.text.toLowerCase(Locale.ROOT);
            PersonalData.Candidate old = best.get(lk);
            if (old == null || c.score > old.score) best.put(lk, c);
        }
        ArrayList<PersonalData.Candidate> sorted = new ArrayList<>(best.values());
        sorted.sort(BY_SCORE);
        ArrayList<PersonalData.Candidate> top = new ArrayList<>(sorted.subList(0, Math.min(i, sorted.size())));
        if (ctx != null && reserveSlot && i >= 3 && top.size() == i) {
            // Giu 1 o cho tu manh nhat theo cach xep cu (khong tinh ngu canh): cau la van doan duoc
            PersonalData.Candidate plain = null;
            int ps = Integer.MIN_VALUE;
            for (PersonalData.Candidate c : sorted) {
                Integer b = boosted.get(c);
                int sc = b != null ? b.intValue() : c.score;
                if (sc > ps) { ps = sc; plain = c; }
            }
            if (plain != null && !top.contains(plain)) top.set(i - 1, plain);
        }
        ArrayList<PersonalData.Candidate> out = new ArrayList<>();
        for (PersonalData.Candidate c : top) {
            if (!c.macro && !str2.isEmpty()) c = new PersonalData.Candidate(PersonalData.preserveCase(str2, c.text), false, c.score);
            out.add(c);
        }
        return out;
    }



    // ---------------------------------------------------------------- Telex.tone / Telex.bare bang bang tra
    private static final int TABLE = 0x2000; // gom toan bo chu Latin + tieng Viet (U+1EA0..U+1EF9)
    private static final byte[] TONE = new byte[TABLE];
    private static final char[] BARE = new char[TABLE];
    static {
        String[] rows = {"a\u00e1\u00e0\u1ea3\u00e3\u1ea1", "\u0103\u1eaf\u1eb1\u1eb3\u1eb5\u1eb7", "\u00e2\u1ea5\u1ea7\u1ea9\u1eab\u1ead",
                "e\u00e9\u00e8\u1ebb\u1ebd\u1eb9", "\u00ea\u1ebf\u1ec1\u1ec3\u1ec5\u1ec7", "i\u00ed\u00ec\u1ec9\u0129\u1ecb",
                "o\u00f3\u00f2\u1ecf\u00f5\u1ecd", "\u00f4\u1ed1\u1ed3\u1ed5\u1ed7\u1ed9", "\u01a1\u1edb\u1edd\u1edf\u1ee1\u1ee3",
                "u\u00fa\u00f9\u1ee7\u0169\u1ee5", "\u01b0\u1ee9\u1eeb\u1eed\u1eef\u1ef1", "y\u00fd\u1ef3\u1ef7\u1ef9\u1ef5"};
        for (char c = 0; c < TABLE; c++) {
            for (String r : rows) {
                int i = r.indexOf(c);
                if (i > 0) { TONE[c] = (byte) i; break; }
            }
            BARE[c] = Telex.bareOrig(c);
        }
    }

    /** = Telex.tone() */
    public static int tone(String str) {
        String l = str.toLowerCase(Locale.ROOT);
        for (int i = 0, n = l.length(); i < n; i++) {
            char c = l.charAt(i);
            if (c < TABLE) {
                int t = TONE[c];
                if (t > 0) return t;
            } else {
                int t = Telex.toneOrig(String.valueOf(c));
                if (t > 0) return t;
            }
        }
        return 0;
    }

    /** = Telex.bare() */
    public static char bare(char c) {
        return c < TABLE ? BARE[c] : Telex.bareOrig(c);
    }

    // ---------------------------------------------------------------- tu sua: chinh xac hon voi chuoi khong phai am tiet
    private static PersonalData corrData;
    private static String corrA, corrB, corrC, corrResult;
    private static int corrMethod, corrVersion, corrStrength = -1;
    public static boolean relaxInvalid = true;
    /** Ap dung ca khi chu go ra dung cau truc am tiet nhung khong co trong tieng Viet (vd "nòa", "tịa"). */
    public static boolean relaxUnknownValid = false;

    /**
     * = PersonalData.correct(), them mot buoc: o muc "Vua", neu chu go ra KHONG phai am tiet tieng Viet hop le
     * (vd "thnh", "chihs", "puwowng") va khong phai tu da biet ma ban goc giu nguyen, thi xet lai voi nguong
     * cua muc "Manh". Tu go dung luon la am tiet hop le nen khong bao gio bi sua nham vi buoc nay.
     */
    public static String correct(PersonalData d, String raw, String word, String prev, int method) {
        int strength = d.strength;
        int version = d.learned.version;
        if (corrData == d && corrStrength == strength && corrVersion == version && corrMethod == method
                && raw.equals(corrA) && word.equals(corrB) && prev.equals(corrC)) {
            return corrResult;
        }
        String r = d.correctOrig(raw, word, prev, method);
        if (relaxInvalid && strength == 1 && r.equals(word) && word.length() >= 2
                && (relaxUnknownValid || !TypoModel.validSyllable(word)) && !d.known(word)) {
            d.strength = 2;
            try {
                r = d.correctOrig(raw, word, prev, method);
            } finally {
                d.strength = strength;
            }
        }
        corrData = d; corrA = raw; corrB = word; corrC = prev; corrMethod = method;
        corrVersion = d.learned.version; corrStrength = strength; corrResult = r;
        return r;
    }

    // ---------------------------------------------------------------- service: compose memo, one IPC per refresh
    private static VietKeyboardService composeService;
    private static PersonalData composeData;
    private static ForeignWords composeForeign;
    private static String composeRaw, composeResult;
    private static int composeMethod, composeVersion;
    private static boolean composeAuto;

    public static String compose(VietKeyboardService s) {
        SharedPreferences p = s.getSharedPreferences("settings", 0);
        int method = p.getInt("inputMethod", 0);
        boolean auto = p.getBoolean("autoForeign", true);
        // seed (chu khoi phuc tu van ban) va keys (phim vua go) render khac nhau du toString() giong nhau
        CompositionBuffer cb = s.raw;
        String raw = cb.seed + '\u0001' + cb.keys.toString();
        PersonalData d = s.data;
        ForeignWords f = s.foreign;
        int ver = d.learned.version;
        if (composeService == s && composeData == d && composeForeign == f && composeMethod == method
                && composeAuto == auto && composeVersion == ver && raw.equals(composeRaw)) {
            return composeResult;
        }
        String r = s.composeOrig();
        composeService = s; composeData = d; composeForeign = f; composeMethod = method; composeAuto = auto;
        composeVersion = ver; composeRaw = raw; composeResult = r;
        return r;
    }

    private static int inRefresh;
    private static VietKeyboardService prevService;
    private static String prevWord;
    private static Runnable pending;
    private static VietKeyboardService pendingService;

    public static String previousWord(VietKeyboardService s) {
        if (inRefresh > 0) {
            if (prevService == s && prevWord != null) return prevWord;
            String r = s.previousWordOrig();
            prevService = s;
            prevWord = r;
            return r;
        }
        return s.previousWordOrig();
    }

    public static void refresh(VietKeyboardService s) {
        if (pending != null && pendingService == s) s.learningHandler.removeCallbacks(pending);
        inRefresh++;
        prevWord = null;
        try {
            s.refreshSuggestionsOrig();
        } finally {
            inRefresh--;
            prevWord = null;
            prevService = null;
        }
    }

    /** onUpdateSelection: gop nhieu lan cap nhat con tro lien tiep thanh mot lan lam moi goi y. */
    public static void postRefresh(final VietKeyboardService s) {
        Handler h = s.learningHandler;
        if (pending == null || pendingService != s) {
            pendingService = s;
            pending = new Runnable() {
                @Override public void run() {
                    s.refreshSuggestions();
                    KeyboardView k = s.keyboard;
                    if (k != null) {
                        k.rebuild();
                        k.invalidate();
                    }
                }
            };
        }
        h.removeCallbacks(pending);
        h.post(pending);
    }

    // ---------------------------------------------------------------- macros: parse lai chi khi chuoi doi
    private static PersonalData macroData;
    private static String macroText;
    private static int macroLoadedGen = -1, macroSize = -1;

    public static void reloadMacros(VietKeyboardService s) {
        String text = s.getSharedPreferences("settings", 0).getString("macros", "");
        PersonalData d = s.data;
        if (d == macroData && macroLoadedGen == macroGen && macroSize == d.macros.size() && text != null && text.equals(macroText)) {
            return;
        }
        s.reloadActiveMacrosOrig();
        macroData = d;
        macroText = text;
        macroLoadedGen = macroGen;
        macroSize = d.macros.size();
    }

    // ---------------------------------------------------------------- KeyboardView theme cache
    private static final class Tc {
        boolean present;
        int value;
        boolean dynKnown;
        int dyn;
    }

    private static KeyboardView tcView;
    private static final HashMap<String, Tc> TC = new HashMap<>();
    private static int tcDynamic = -1;

    public static void themeChanged() {
        TC.clear();
        ELL.clear();
        tcDynamic = -1;
        tcView = null;
    }

    public static int themeColor(KeyboardView v, String name, int def) {
        if (v.themePrefs != null) return v.themeColorOrig(name, def);
        if (tcView != v) {
            TC.clear();
            tcDynamic = -1;
            tcView = v;
        }
        SharedPreferences p = v.prefs();
        if (tcDynamic < 0) tcDynamic = p.getBoolean("themeDynamic", false) ? 1 : 0;
        Tc e = TC.get(name);
        if (e == null) {
            e = new Tc();
            e.present = p.contains(name);
            if (e.present) e.value = p.getInt(name, def);
            TC.put(name, e);
        }
        if (tcDynamic == 1) {
            if (!e.dynKnown) {
                e.dyn = v.dynamicColor(name);
                e.dynKnown = true;
            }
            if (e.dyn != 0) return e.dyn;
        }
        return e.present ? e.value : def;
    }

    // ---------------------------------------------------------------- ellipsize: nhan phim duoc tao lai sau moi lan go
    private static final class Ell {
        Typeface face;
        CharSequence out;
    }

    private static final HashMap<String, Ell> ELL = new HashMap<>();

    public static CharSequence ellipsize(CharSequence text, TextPaint p, float avail, TextUtils.TruncateAt where) {
        if (!(text instanceof String) || where == null) return TextUtils.ellipsize(text, p, avail, where);
        String key = text + "\u0001" + Float.floatToIntBits(avail) + "\u0001" + Float.floatToIntBits(p.getTextSize())
                + "\u0001" + Float.floatToIntBits(p.getTextScaleX()) + "\u0001" + Float.floatToIntBits(p.getLetterSpacing())
                + "\u0001" + Float.floatToIntBits(p.getTextSkewX()) + "\u0001" + p.getFlags() + "\u0001" + where.ordinal()
                + "\u0001" + p.getFontFeatureSettings() + "\u0001" + p.getFontVariationSettings() + "\u0001" + p.getTextLocales();
        Typeface face = p.getTypeface();
        Ell e = ELL.get(key);
        if (e != null && e.face == face) return e.out;
        CharSequence out = TextUtils.ellipsize(text, p, avail, where);
        if (!(out instanceof String)) return out;
        if (ELL.size() > 400) ELL.clear();
        e = new Ell();
        e.face = face;
        e.out = out;
        ELL.put(key, e);
        return out;
    }

    // ---------------------------------------------------------------- hoc thoi quen: khong hoc chu go nham
    public static boolean learnFilter = true;

    /** true neu nen hoc tu nay: am tiet tieng Viet hop le, tu da biet, hoac tu tieng Anh. */
    public static boolean worthLearning(VietKeyboardService s, String word) {
        return worthLearning(s.data, s.foreign, word);
    }

    public static boolean worthLearning(PersonalData d, ForeignWords f, String word) {
        if (!learnFilter || word == null) return true;
        String w = word.trim().toLowerCase(Locale.ROOT);
        if (w.isEmpty()) return true;
        int end = w.length();
        while (end > 0 && ".,?!:;".indexOf(w.charAt(end - 1)) >= 0) end--;
        w = w.substring(0, end);
        if (w.isEmpty() || TypoModel.validSyllable(w)) return true;
        if (d != null && d.known(w)) return true;
        return f != null && f.english.score(w) >= 5;
    }

    /** = VietKeyboardService.remember(), nhung bo qua chu go nham (khong phai am tiet, khong co trong tu dien). */
    public static void remember(VietKeyboardService s, String prev, String word) {
        if (worthLearning(s, word)) {
            s.rememberOrig(prev, word);
        } else {
            s.learnedPrevious = "";
            s.learnedWord = "";
        }
    }

    // ---------------------------------------------------------------- giong noi: ghi chu Google tra ve vao nhat ky (tu chon)
    static String cut(CharSequence t) {
        if (t == null) return "(null)";
        String s = t.toString().replace('\n', ' ');
        return s.length() > 220 ? "…" + s.substring(s.length() - 220) : s;
    }

    /** = VietKeyboardService.voiceCommit(); neu bat "voiceLogText" thi ghi chu Google tra ve va chu da chen. */
    public static void voiceCommit(VietKeyboardService s, String text) {
        boolean log = s.getSharedPreferences("settings", 0).getBoolean("voiceLogText", false);
        if (log) VoiceInput.log(s, "Google: " + cut(text));
        s.voiceCommitOrig(text);
        if (log) {
            android.view.inputmethod.InputConnection ic = s.getCurrentInputConnection();
            VoiceInput.log(s, "Đã chèn: " + cut(ic == null ? null : ic.getTextBeforeCursor(220, 0)));
        }
    }
}
