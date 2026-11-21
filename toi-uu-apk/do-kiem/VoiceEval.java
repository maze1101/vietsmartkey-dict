package vn.personal.vietkey;
import java.io.*; import java.nio.charset.StandardCharsets; import java.nio.file.*; import java.util.*;
/** Do cham phay giong noi: mo phong Google tra chu khong dau cau, cat luot theo cho ngat hoi, so voi dau cau that. */
public class VoiceEval {
  // lop dau cau sau moi tu: 0 khong, 1 phay, 2 cham, 3 hoi
  static int cls(String p) { if (p.isEmpty()) return 0; if (p.contains("?")) return 3; if (p.matches(".*[.!…].*")) return 2; return 1; }
  static List<List<String[]>> load(String f) throws IOException {
    List<List<String[]>> out = new ArrayList<>();
    for (String line : Files.readAllLines(Paths.get(f), StandardCharsets.UTF_8)) {
      List<String[]> toks = new ArrayList<>();
      for (String t : line.trim().split("\\s+")) {
        if (t.isEmpty()) continue;
        if (t.matches("[,;:\\-–]+")) { if (!toks.isEmpty() && toks.get(toks.size()-1)[1].isEmpty()) toks.get(toks.size()-1)[1] = ","; continue; }
        if (t.matches("[.!?…]+")) { if (!toks.isEmpty()) toks.get(toks.size()-1)[1] = t.contains("?") ? "?" : "."; continue; }
        String w = t.replaceAll("^[\"'(“”]+|[\"')“”]+$", ""); if (w.isEmpty()) continue;
        String p = ""; java.util.regex.Matcher m = java.util.regex.Pattern.compile("([.,?!;:…]+)$").matcher(w);
        if (m.find()) { p = m.group(1); w = w.substring(0, m.start()); }
        if (!w.matches("[\\p{L}\\p{N}]+")) continue;
        toks.add(new String[]{w, p.isEmpty() ? "" : (cls(p) == 3 ? "?" : cls(p) == 2 ? "." : ",")});
      }
      if (toks.size() >= 3) { if (toks.get(toks.size()-1)[1].isEmpty() || toks.get(toks.size()-1)[1].equals(",")) toks.get(toks.size()-1)[1] = "."; out.add(toks); }
    }
    return out;
  }
  public static void main(String[] a) throws Exception {
    Bench.A = a[0]; final PersonalData d = Bench.build(a[1].equals("-") ? null : a[1]);
    List<List<String[]>> sents = load(a[2]);
    Random r = new Random(3);
    int[][] conf = new int[4][4]; int capOk = 0, capAll = 0;
    StringBuilder ex = new StringBuilder();
    for (int s = 0; s + 2 < sents.size(); s += 3) {
      List<String[]> truth = new ArrayList<>(); for (int k = 0; k < 3; k++) truth.addAll(sents.get(s + k));
      // chu Google tra ve: thuong, giu hoa cho ten rieng giua cau
      List<String> turns = new ArrayList<>(); StringBuilder cur = new StringBuilder(); boolean start = true;
      for (String[] t : truth) {
        String w = t[0]; if (start || !Character.isUpperCase(w.charAt(0))) w = w.toLowerCase(Locale.ROOT);
        cur.append(w).append(' ');
        int c = cls(t[1]); start = c >= 2;
        double p = c >= 2 ? 0.95 : c == 1 ? 0.6 : 0.04;
        if (r.nextDouble() < p) { turns.add(cur.toString().trim()); cur.setLength(0); }
      }
      if (cur.length() > 0) turns.add(cur.toString().trim());
      String out = VoiceText.dictate("", d.extraNames, (java.util.function.Predicate<String>) d::isCommonWord, (java.util.function.BiPredicate<String, String>) d::isPhrase, turns.toArray(new String[0]));
      String[] ot = out.split("\\s+");
      if (ot.length != truth.size()) continue;
      boolean sentStart = true;
      for (int i = 0; i < ot.length; i++) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([.,?!;:…]+)$").matcher(ot[i]);
        String pp = m.find() ? m.group(1) : "";
        conf[cls(truth.get(i)[1])][cls(pp)]++;
        if (sentStart) { capAll++; if (Character.isUpperCase(ot[i].charAt(0))) capOk++; }
        sentStart = cls(truth.get(i)[1]) >= 2;
      }
      if (ex.length() < 3000) { StringBuilder tt = new StringBuilder(); for (String[] t : truth) tt.append(t[0]).append(t[1]).append(' '); ex.append("THAT: ").append(tt.toString().trim()).append("\nRA  : ").append(out).append("\n\n"); }
    }
    String[] nm = {"khong", "phay", "cham", "hoi"};
    System.out.println("ma tran (hang = that, cot = app dat): khong / phay / cham / hoi");
    for (int i = 0; i < 4; i++) System.out.printf("  %-6s %6d %6d %6d %6d%n", nm[i], conf[i][0], conf[i][1], conf[i][2], conf[i][3]);
    for (int k = 1; k < 4; k++) { int tp = conf[k][k], pr = 0, re = 0; for (int i = 0; i < 4; i++) { pr += conf[i][k]; re += conf[k][i]; }
      double P = pr == 0 ? 0 : 100.0 * tp / pr, R = re == 0 ? 0 : 100.0 * tp / re; System.out.printf("  %-5s dung %5.1f%%  bat duoc %5.1f%%  F1 %5.1f%n", nm[k], P, R, P + R == 0 ? 0 : 2 * P * R / (P + R)); }
    int bt = 0, bp = 0, bb = 0; for (int i = 0; i < 4; i++) for (int j = 0; j < 4; j++) { if (i > 0) bt += conf[i][j]; if (j > 0) bp += conf[i][j]; if (i > 0 && j > 0) bb += conf[i][j]; }
    double P = 100.0 * bb / bp, R = 100.0 * bb / bt; System.out.printf("  co dau (bat ky) dung %5.1f%%  bat duoc %5.1f%%  F1 %5.1f | viet hoa dau cau %5.1f%%%n", P, R, 2*P*R/(P+R), 100.0 * capOk / capAll);
    if (a.length > 3) Files.write(Paths.get(a[3]), ex.toString().getBytes(StandardCharsets.UTF_8));
  }
}
