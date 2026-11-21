package vn.personal.vietkey;
import java.util.*;
public class BoundStat {
  public static void main(String[] a) throws Exception {
    Bench.A = a[0]; PersonalData d = Bench.build(a[1]);
    List<List<String[]>> sents = VoiceEval.load(a[2]);
    int[][] h = new int[4][5]; String[] bins = {"0", "1-4", "5-19", "20-99", "100+"};
    List<String[]> all = new ArrayList<>(); for (List<String[]> s : sents) all.addAll(s);
    for (int i = 0; i + 1 < all.size(); i++) {
      String L = all.get(i)[0].toLowerCase(), F = all.get(i + 1)[0].toLowerCase();
      int c = d.contextCount(L, F); HashMap<String,Integer> ph = d.phraseNext.get(L); if (ph != null && ph.containsKey(F)) c += 50;
      int b = c == 0 ? 0 : c < 5 ? 1 : c < 20 ? 2 : c < 100 ? 3 : 4;
      h[VoiceEval.cls(all.get(i)[1])][b]++;
    }
    String[] nm = {"khong", "phay", "cham", "hoi"};
    System.out.println("so lan cap (tu cuoi, tu dau) trong bang:  " + String.join(" | ", bins));
    for (int k = 0; k < 4; k++) { int t = 0; for (int x : h[k]) t += x; StringBuilder sb = new StringBuilder(); for (int x : h[k]) sb.append(String.format("%5.1f%% ", 100.0 * x / Math.max(1, t))); System.out.printf("  %-6s (%5d)  %s%n", nm[k], t, sb); }
  }
}
