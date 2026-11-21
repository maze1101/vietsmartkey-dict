package vn.personal.vietkey;
import java.io.*; import java.nio.charset.StandardCharsets; import java.nio.file.*; import java.util.*;
/** Do dieu kien "tu da tron + chu cai kho noi tiep" cua Fast.hit tren van ban that. */
public class SpaceSim {
  public static void main(String[] a) throws Exception {
    for (float th : new float[]{0.03f, 0.02f, 0.01f}) run(a, th);
  }
  static void run(String[] a, float TH) throws Exception {
    Bench.A = a[0]; PersonalData d = Bench.build(a[1]);
    ForeignWords fw = new ForeignWords(); fw.load(new FileInputStream(a[0] + "/wordfreq-small-en.cbpack"));
    int ends = 0, endsCovered = 0, letters = 0, lettersRisk = 0; Map<String,Integer> riskEx = new TreeMap<>();
    for (String line : Files.readAllLines(Paths.get(a[2]), StandardCharsets.UTF_8)) {
      for (String t : line.split("\\s+")) {
        String w = t.toLowerCase(Locale.ROOT); if (!w.matches("\\p{L}+") || !TypoModel.validSyllable(w)) continue;
        String keys = TypoModel.keys(w); if (!Telex.convert(keys).equals(w)) continue;
        for (int i = 1; i <= keys.length(); i++) {
          String raw = keys.substring(0, i);
          CompositionBuffer cb = new CompositionBuffer(); cb.append(raw); String comp = cb.renderSmart(0, fw, d);
          boolean known = d.isKnown(comp); float[] odds = d.nextKeyOdds(raw);
          if (i == keys.length()) { // het tu: nguoi dung muon phim cach; nhan nham vao b hoac n
            for (char c : new char[]{'b', 'n'}) { ends++; float p = odds == null ? 0 : odds[c - 'a']; if (known && p < TH) endsCovered++; }
          } else { char nx = keys.charAt(i); if ("cvbn".indexOf(nx) >= 0) { letters++; float p = odds == null ? 1 : odds[nx - 'a'];
              if (known && p < TH) { lettersRisk++; riskEx.merge(comp + "+" + nx + "(" + w + ")", 1, Integer::sum); } } }
        }
      }
    }
    System.out.printf("== nguong %.2f%n", TH); System.out.printf("Het tu, cham nham b/n: quy tac nhan ra la phim cach %.1f%% (%d/%d)%n", 100.0 * endsCovered / ends, endsCovered, ends);
    System.out.printf("Muon go z..m giua tu: quy tac co the hieu nham %.2f%% (%d/%d) - chi khi cham dung 30%% mep duoi%n", 100.0 * lettersRisk / letters, lettersRisk, letters);
    riskEx.entrySet().stream().sorted((x, y) -> y.getValue() - x.getValue()).limit(6).forEach(e -> System.out.println("   " + e.getKey() + " x" + e.getValue()));
  }
}
