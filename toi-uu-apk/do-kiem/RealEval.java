package vn.personal.vietkey;
import java.io.*; import java.nio.charset.StandardCharsets; import java.nio.file.*; import java.util.*;
public class RealEval {
  public static void main(String[] a) throws Exception {
    Bench.A = a[0]; PersonalData d = Bench.build(a[1]);
    ForeignWords fw = new ForeignWords(); fw.load(new FileInputStream(a[0] + "/wordfreq-small-en.cbpack"));
    Fast.memoEnabled = false;
    List<String[]> cases = new ArrayList<>();
    for (String line : Files.readAllLines(Paths.get(a[2]), StandardCharsets.UTF_8)) {
      String prev = "";
      for (String t : line.split("\\s+")) {
        String w = t.toLowerCase(Locale.ROOT);
        if (!w.matches("\\p{L}+")) { prev = ""; continue; }
        if (w.length() >= 1 && TypoModel.validSyllable(w) && Telex.convert(TypoModel.keys(w)).equals(w)) cases.add(new String[]{prev, w});
        prev = w;
      }
    }
    System.out.println("tu thu: " + cases.size() + "  [top-3 dung %: 0 phim | 1 phim | 2 phim | 3 phim]  tiet kiem phim %");
    for (String ws : a[3].split(",")) {
      String[] q = ws.split("m"); Fast.reserveSlot = !q[0].startsWith("n"); Fast.ctxWeight = Double.parseDouble(q[0].replace("n", "")); Fast.ctxMin = q.length > 1 ? Integer.parseInt(q[1]) : 1;
      int[] hit = new int[4]; long keysAll = 0, keysUsed = 0;
      for (String[] c : cases) {
        String keys = TypoModel.keys(c[1]); int need = keys.length() + 1; int used = need;
        for (int k = 0; k <= Math.min(3, keys.length()); k++) {
          String raw = keys.substring(0, k); String comp = "";
          if (k > 0) { CompositionBuffer cb = new CompositionBuffer(); cb.append(raw); comp = cb.renderSmart(0, fw, d); }
          boolean ok = false; for (Object o : d.suggest(raw, comp, c[0], 3, 0)) { PersonalData.Candidate x = (PersonalData.Candidate) o; if (x.text.equalsIgnoreCase(c[1])) ok = true; }
          if (ok) { for (int j = k; j < 4; j++) hit[j]++; used = Math.min(used, k + 1); break; }
        }
        keysAll += need; keysUsed += used;
      }
      System.out.printf("%-8s %5.1f %5.1f %5.1f %5.1f   %5.1f%n", ws, 100.0*hit[0]/cases.size(), 100.0*hit[1]/cases.size(), 100.0*hit[2]/cases.size(), 100.0*hit[3]/cases.size(), 100.0*(keysAll-keysUsed)/keysAll);
    }
  }
}
