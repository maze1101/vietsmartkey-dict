package vn.personal.vietkey;
import java.io.*; import java.nio.charset.StandardCharsets; import java.nio.file.*; import java.util.*; import java.util.zip.*;
public class PredEval {
  static ForeignWords fw;
  static List<String[]> pairs(String f) throws IOException {
    List<String[]> out = new ArrayList<>();
    for (String l : Files.readAllLines(Paths.get(f), StandardCharsets.UTF_8)) {
      if (l.startsWith("#")) continue; String[] c = l.split("\t"); String[] w = c[0].split(" ");
      if (w.length == 2 && w[0].matches("\\p{L}+") && w[1].matches("\\p{L}{2,}")) out.add(new String[]{w[0], w[1], c.length > 1 ? c[1] : "1"});
    }
    return out;
  }
  static double[] eval(PersonalData d, List<String[]> test) {
    double tot = 0, h0 = 0, h1 = 0, h2 = 0;
    for (String[] p : test) {
      double wt = Math.log1p(Double.parseDouble(p[2]));
      tot += wt;
      if (top(d.suggest("", "", p[0], 3, 0), p[1])) h0 += wt;
      String keys = TypoModel.keys(p[1]);
      for (int k = 1; k <= 2 && k < keys.length(); k++) {
        CompositionBuffer cb = new CompositionBuffer(); cb.append(keys.substring(0, k));
        String comp = cb.renderSmart(0, fw, d);
        if (top(d.suggest(keys.substring(0, k), comp, p[0], 3, 0), p[1])) { if (k == 1) h1 += wt; else h2 += wt; }
      }
    }
    return new double[]{100 * h0 / tot, 100 * h1 / tot, 100 * h2 / tot};
  }
  static boolean top(List<PersonalData.Candidate> c, String w) { for (PersonalData.Candidate x : c) if (x.text.equalsIgnoreCase(w)) return true; return false; }
  public static void main(String[] a) throws Exception {
    Bench.A = a[0]; String dict = a[1], tat = a[2], wiki = a[3];
    fw = new ForeignWords(); fw.load(new FileInputStream(a[0] + "/wordfreq-small-en.cbpack"));
    Fast.memoEnabled = false;
    Random r = new Random(5);
    List<String[]> tp = pairs(tat), wp = pairs(wiki);
    Collections.shuffle(tp, r); Collections.shuffle(wp, r);
    // giau 15% cap Tatoeba khoi tu dien de kiem tra ngoai mau
    Set<String> held = new HashSet<>(); List<String[]> heldList = new ArrayList<>();
    for (String[] p : tp.subList(0, tp.size() * 15 / 100)) { held.add(p[0] + " " + p[1]); heldList.add(p); }
    File filtered = new File("dict_heldout.vskdict");
    try (BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(dict)), StandardCharsets.UTF_8));
         Writer w = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(filtered)), StandardCharsets.UTF_8)) {
      String l; while ((l = br.readLine()) != null) { String[] c = l.split("\t"); if (c.length >= 2 && c[0].equals("B") && held.contains(c[1])) continue; w.write(l); w.write('\n'); }
    }
    PersonalData full = Bench.build(dict), ho = Bench.build(filtered.getPath());
    List<String[]> inT = tp.subList(tp.size() * 15 / 100, Math.min(tp.size(), tp.size() * 15 / 100 + 2000));
    List<String[]> inW = wp.subList(0, 2000);
    List<String[]> outT = heldList.subList(0, Math.min(2000, heldList.size()));
    System.out.println("top-3 dung (%): [doan tu tiep | sau 1 phim | sau 2 phim]");
    for (String ws : a[4].split(",")) {
      String[] q = ws.split("m"); Fast.reserveSlot = !q[0].startsWith("n"); Fast.ctxWeight = Double.parseDouble(q[0].replace("n","")); Fast.ctxMin = q.length > 1 ? Integer.parseInt(q[1]) : 1;
      double[] x = eval(full, inT), y = eval(full, inW), z = eval(ho, outT);
      System.out.printf("w=%-5s hoi thoai %5.1f %5.1f %5.1f | wikipedia %5.1f %5.1f %5.1f | ngoai mau %5.1f %5.1f %5.1f%n", ws, x[0], x[1], x[2], y[0], y[1], y[2], z[0], z[1], z[2]);
    }
  }
}
