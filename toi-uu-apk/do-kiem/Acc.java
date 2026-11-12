package vn.personal.vietkey;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
public class Acc {
    static final String[] ROWS = {"qwertyuiop", "asdfghjkl", "zxcvbnm"};
    static char near(Random r, char c) {
        List<Character> l = new ArrayList<>();
        for (char x = 'a'; x <= 'z'; x++) if (x != c && TypoModel.distance(c, x) <= 1.15) l.add(x);
        return l.isEmpty() ? c : l.get(r.nextInt(l.size()));
    }
    public static void main(String[] a) throws Exception {
        Bench.A = a[0];
        PersonalData d = Bench.build(a[1]); if (System.getenv("STRENGTH") != null) d.setStrength(Integer.parseInt(System.getenv("STRENGTH")));
        ForeignWords fw = new ForeignWords(); fw.load(new FileInputStream(a[0] + "/wordfreq-small-en.cbpack"));
        List<String[]> pairs = new ArrayList<>();
        for (String f : new String[]{a[2], a[3]}) for (String l : Files.readAllLines(Paths.get(f), StandardCharsets.UTF_8)) {
            if (l.startsWith("#")) continue; String[] c = l.split("\t")[0].split(" "); if (c.length == 2) pairs.add(c);
        }
        Random r = new Random(7); Collections.shuffle(pairs, r);
        int cases = Integer.parseInt(a[4]);
        int clean = 0, cleanOk = 0, typo = 0, fixed = 0, inTop = 0, unchanged = 0, wrong = 0;
        int[] byKind = new int[4], okKind = new int[4];
        StringBuilder log = new StringBuilder();
        for (String[] p : pairs) {
            if (clean >= cases) break;
            String prev = p[0], w = p[1];
            if (w.length() < 2 || !w.matches("\\p{L}+")) continue;
            String keys = TypoModel.keys(w);
            if (!Telex.convert(keys).equals(w) || keys.length() < 3) continue;
            // clean
            CompositionBuffer cb = new CompositionBuffer(); cb.append(keys);
            String comp = cb.renderSmart(0, fw, d);
            String out = d.correct(keys, comp, prev, 0);
            if (System.getenv("RELAX") != null && out.equals(comp) && comp.length() >= 2 && !TypoModel.validSyllable(comp) && !d.isKnown(comp)) { int old = d.strength; d.setStrength(Integer.parseInt(System.getenv("RELAX"))); out = d.correct(keys, comp, prev, 0); d.setStrength(old); }
            clean++; if (out.equalsIgnoreCase(w)) cleanOk++; else if (log.length() < 4000) log.append("FALSE-FIX ").append(prev).append(' ').append(w).append(" -> ").append(out).append('\n');
            // typo
            int kind = r.nextInt(4); StringBuilder t = new StringBuilder(keys); int i = r.nextInt(keys.length());
            char ch = keys.charAt(i);
            if (ch < 'a' || ch > 'z') continue;
            switch (kind) {
                case 0: t.setCharAt(i, near(r, ch)); break;
                case 1: if (t.length() < 4) continue; t.deleteCharAt(i); break;
                case 2: t.insert(i, near(r, ch)); break;
                default: if (i + 1 >= t.length() || t.charAt(i) == t.charAt(i+1)) continue; t.setCharAt(i, keys.charAt(i+1)); t.setCharAt(i+1, ch);
            }
            String tr = t.toString();
            cb = new CompositionBuffer(); cb.append(tr);
            comp = cb.renderSmart(0, fw, d);
            if (comp.equalsIgnoreCase(w)) continue; // typo produced the same word (e.g. tone order)
            if (System.getenv("NONWORD") != null && d.isKnown(comp)) continue;
            typo++; byKind[kind]++;
            out = d.correct(tr, comp, prev, 0);
            if (System.getenv("RELAX") != null && out.equals(comp) && comp.length() >= 2 && !TypoModel.validSyllable(comp) && !d.isKnown(comp)) { int old = d.strength; d.setStrength(Integer.parseInt(System.getenv("RELAX"))); out = d.correct(tr, comp, prev, 0); d.setStrength(old); }
            List<PersonalData.Candidate> s = d.suggest(tr, comp, prev, 3, 0);
            boolean top = false; for (PersonalData.Candidate c : s) if (c.text.equalsIgnoreCase(w)) top = true;
            if (out.equalsIgnoreCase(w)) { fixed++; okKind[kind]++; }
            else if (out.equals(comp)) { unchanged++; if (kind==1 && log.length() < 60000) log.append("MISS-UNFIXED ").append(prev).append(" ").append(w).append(" typed=").append(tr).append(" shown=").append(comp).append(" top=").append(s.isEmpty()?"":s.get(0).text).append("\n"); } else { wrong++; if (log.length() < 60000) log.append("WRONG k").append(kind).append(" ").append(prev).append(" ").append(w).append(" typed=").append(tr).append(" -> ").append(out).append("\n"); }
            if (top || out.equalsIgnoreCase(w)) inTop++;
        }
        System.out.printf("clean words: %d  kept correct: %.1f%%%n", clean, 100.0*cleanOk/clean);
        System.out.printf("typos: %d  auto-fixed: %.1f%%  left as typed: %.1f%%  changed to WRONG word: %.1f%%  right word in fix-or-top3: %.1f%%%n", typo, 100.0*fixed/typo, 100.0*unchanged/typo, 100.0*wrong/typo, 100.0*inTop/typo);
        String[] kn = {"near-key","missing","extra","swap"};
        for (int k = 0; k < 4; k++) System.out.printf("  %-8s %5d fixed %.1f%%%n", kn[k], byKind[k], 100.0*okKind[k]/Math.max(1,byKind[k]));
        if (a.length > 5) Files.write(Paths.get(a[5]), log.toString().getBytes(StandardCharsets.UTF_8));
    }
}
