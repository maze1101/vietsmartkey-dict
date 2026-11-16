package vn.personal.vietkey;
import java.io.*; import java.util.*;
public class LearnSim {
  public static void main(String[] a) throws Exception {
    Bench.A = a[0];
    ForeignWords fw = new ForeignWords(); fw.load(new FileInputStream(a[0] + "/wordfreq-small-en.cbpack"));
    String[][] typos = {{"thnh","đã","thành"},{"chihs","rất","chính"},{"nhuwnx","những","những"},{"tihj","được","thị"}};
    for (boolean filter : new boolean[]{false, true}) {
      Fast.learnFilter = filter; PersonalData d = Bench.build(a[1]);
      System.out.println(filter ? "== CO bo loc (ban moi)" : "== KHONG bo loc (ban cu)");
      for (String[] t : typos) {
        StringBuilder b = new StringBuilder(t[0] + ": ");
        for (int rep = 1; rep <= 4; rep++) {
          CompositionBuffer cb = new CompositionBuffer(); cb.append(t[0]); String comp = cb.renderSmart(0, fw, d);
          String out = d.correct(t[0], comp, t[1], 0);
          b.append("lan").append(rep).append("->").append(out).append("  ");
          // nguoi dung khong sua, nhan cach -> ban phim hoc tu vua go (neu bo loc cho phep)
          if (out.equals(comp) && Fast.worthLearning(d, fw, out)) d.learned.observe(t[1], out);
        }
        System.out.println(b);
      }
      for (String w : new String[]{"được","hello","okela","hok","thnh"}) System.out.print(w + "=" + (Fast.worthLearning(d, fw, w) ? "hoc" : "bo") + "  ");
      System.out.println();
    }
  }
}
