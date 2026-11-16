package vn.personal.vietkey;
import java.io.*; import java.nio.charset.StandardCharsets; import java.nio.file.*; import java.util.*;
public class TypoEval {
  static char near(Random r, char c) { List<Character> l = new ArrayList<>(); for (char x = 'a'; x <= 'z'; x++) if (x != c && TypoModel.distance(c, x) <= 1.15) l.add(x); return l.isEmpty() ? c : l.get(r.nextInt(l.size())); }
  public static void main(String[] a) throws Exception {
    Bench.A = a[0]; PersonalData d = Bench.build(a[1]); if (System.getenv("STRMANH") != null) PersonalData.STRENGTH[2] = Double.parseDouble(System.getenv("STRMANH")); if (System.getenv("RELAXV") != null) Fast.relaxUnknownValid = System.getenv("RELAXV").equals("1"); if (System.getenv("RELAXI") != null) Fast.relaxInvalid = System.getenv("RELAXI").equals("1");
    ForeignWords fw = new ForeignWords(); fw.load(new FileInputStream(a[0] + "/wordfreq-small-en.cbpack"));
    List<String[]> cases = new ArrayList<>();
    for (String line : Files.readAllLines(Paths.get(a[2]), StandardCharsets.UTF_8)) {
      String prev = "";
      for (String t : line.split("\\s+")) { String w = t.toLowerCase(Locale.ROOT); if (!w.matches("\\p{L}+")) { prev = ""; continue; }
        if (w.length() >= 2 && TypoModel.validSyllable(w) && Telex.convert(TypoModel.keys(w)).equals(w)) cases.add(new String[]{prev, w}); prev = w; }
    }
    Random r = new Random(11); Collections.shuffle(cases, r);
    int n = Integer.parseInt(a[3]); String[] kn = {"go nham phim canh", "thieu chu", "thua chu", "dao 2 chu"};
    int clean = 0, cleanBad = 0; int[] tot = new int[4], fix = new int[4], wrong = new int[4], top = new int[4]; StringBuilder log = new StringBuilder();
    for (String[] c : cases) {
      if (clean >= n) break;
      String keys = TypoModel.keys(c[1]);
      CompositionBuffer cb = new CompositionBuffer(); cb.append(keys); String comp = cb.renderSmart(0, fw, d);
      String out = d.correct(keys, comp, c[0], 0); clean++; if (!out.equalsIgnoreCase(c[1])) { cleanBad++; if (log.length() < 20000) log.append("SUA-NHAM ").append(c[0]).append(' ').append(c[1]).append(" -> ").append(out).append('\n'); }
      int kind = r.nextInt(4); StringBuilder t = new StringBuilder(keys); int i = r.nextInt(keys.length()); char ch = keys.charAt(i);
      if (ch < 'a' || ch > 'z') continue;
      if (kind == 0) t.setCharAt(i, near(r, ch)); else if (kind == 1) { if (t.length() < 3) continue; t.deleteCharAt(i); } else if (kind == 2) t.insert(i, near(r, ch));
      else { if (i + 1 >= t.length() || t.charAt(i) == t.charAt(i + 1)) continue; t.setCharAt(i, keys.charAt(i + 1)); t.setCharAt(i + 1, ch); }
      String tr = t.toString(); cb = new CompositionBuffer(); cb.append(tr); comp = cb.renderSmart(0, fw, d);
      if (comp.equalsIgnoreCase(c[1]) || d.isKnown(comp)) continue; // chi tinh loi ra tu vo nghia
      tot[kind]++; out = d.correct(tr, comp, c[0], 0);
      boolean tp = false; for (Object o : d.suggest(tr, comp, c[0], 3, 0)) if (((PersonalData.Candidate) o).text.equalsIgnoreCase(c[1])) tp = true;
      if (out.equalsIgnoreCase(c[1])) fix[kind]++; else if (!out.equals(comp)) { wrong[kind]++; if (log.length() < 20000) log.append("SAI k").append(kind).append(' ').append(c[0]).append(' ').append(c[1]).append(" go=").append(tr).append(" -> ").append(out).append('\n'); }
      else if (kind == 3 && log.length() < 20000) log.append("DAO-CON ").append(c[0]).append(' ').append(c[1]).append(" go=").append(tr).append(" hien=").append(comp).append('\n');
      if (tp || out.equalsIgnoreCase(c[1])) top[kind]++;
    }
    System.out.printf("tu go dung bi sua nham: %d/%d (%.2f%%)%n", cleanBad, clean, 100.0 * cleanBad / clean);
    int T = 0, F = 0, W = 0, P = 0; for (int k = 0; k < 4; k++) { T += tot[k]; F += fix[k]; W += wrong[k]; P += top[k];
      System.out.printf("  %-18s %4d  tu sua dung %5.1f%%  sua sai %4.1f%%  co trong goi y %5.1f%%%n", kn[k], tot[k], 100.0*fix[k]/tot[k], 100.0*wrong[k]/tot[k], 100.0*top[k]/tot[k]); }
    System.out.printf("  %-18s %4d  tu sua dung %5.1f%%  sua sai %4.1f%%  co trong goi y %5.1f%%%n", "TONG", T, 100.0*F/T, 100.0*W/T, 100.0*P/T);
    if (a.length > 4) Files.write(Paths.get(a[4]), log.toString().getBytes(StandardCharsets.UTF_8));
  }
}
