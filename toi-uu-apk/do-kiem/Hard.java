package vn.personal.vietkey;
import java.io.*; import java.util.*;
public class Hard {
  public static void main(String[] a) throws Exception {
    Bench.A = a[0]; PersonalData d = Bench.build(a[1]);
    ForeignWords fw = new ForeignWords(); fw.load(new FileInputStream(a[0] + "/wordfreq-small-en.cbpack"));
    String[][] cases = {{"ddwowcj","khi"},{"vieej","làm"},{"tuwown","một"},{"traar","không"},{"gaaj","được"},{"thaa","tôi"},{"nguowif","người"}};
    for (int rep = 0; rep < 400; rep++) for (String[] c : cases) {
      CompositionBuffer cb = new CompositionBuffer(); cb.append(c[0]); String comp = cb.renderSmart(0, fw, d);
      d.learned.version++; // bust memo
      long t = System.nanoTime(); String r = d.correct(c[0], comp, c[1], 0); long t1 = System.nanoTime() - t;
      if (rep == 399) System.out.printf("%-10s %-8s -> %-8s %.2f ms valid=%s%n", c[0], comp, r, t1/1e6, TypoModel.validSyllable(comp));
    }
  }
}
