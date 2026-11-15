package vn.personal.vietkey;
import java.util.*;
public class Equiv {
    public static void main(String[] a) throws Exception {
        Random r = new Random(42);
        String alpha = "qwertyuiopasdfghjklzxcvbnmwsfrxjd";
        long bad = 0, n = 0;
        // cost
        for (int k = 0; k < 300000; k++) {
            String x = rnd(r, alpha, r.nextInt(9)), y = rnd(r, alpha, r.nextInt(9));
            if (r.nextInt(3)==0) y = mutate(r, x, alpha);
            double lim = new double[]{1.0,1.6,2.1,2.2,3.0,99}[r.nextInt(6)];
            double o = Fast.cost(x, y, lim), f = Fast.cost(y.isEmpty()? x : x, y, lim);
            n++; if (Double.doubleToLongBits(o) != Double.doubleToLongBits(f)) { if (bad++ < 5) System.out.println("cost " + x + " " + y + " " + lim + " " + o + " " + f); }
        }
        System.out.println("cost checked " + n + " mismatches " + bad);
        // fold / clean
        String vi = "aăâeêioôơuưyáàảãạắằẳẵặấầẩẫậéèẻẽẹếềểễệíìỉĩịóòỏõọốồổỗộớờởỡợúùủũụứừửữựýỳỷỹỵđĐÁÀABCxyz İ";
        bad = 0; n = 0;
        for (int k = 0; k < 100000; k++) {
            String x = rnd(r, vi, r.nextInt(8));
            if (r.nextBoolean()) x = java.text.Normalizer.normalize(x, java.text.Normalizer.Form.NFD);
            n++;
            if (!PersonalData.foldOrig(x).equals(Fast.fold(x))) bad++;
            if (!LearnedModel.cleanOrig(x).equals(Fast.clean(x))) bad++;
            if (!PersonalData.foldOrig(x).equals(Fast.fold(x))) bad++; // cached path
        }
        System.out.println("fold/clean checked " + n + " mismatches " + bad);
        // regex helpers
        String[] rx = {"[\\p{L}]+", "\\s+", "\t", " ", "/", "[A-Za-z]+", ".*[0-9].*", "^[^\\p{L}]+|[^\\p{L}]+$", "\\|", " +", ",", "\\.", "=", "\\p{M}"};
        bad = 0; n = 0;
        for (int k = 0; k < 50000; k++) {
            String x = rnd(r, vi + " \t/|,.=123", r.nextInt(12));
            String re = rx[r.nextInt(rx.length)];
            n++;
            if (x.matches(re) != Fast.matches(x, re)) bad++;
            if (!x.replaceAll(re, "#").equals(Fast.replaceAll(x, re, "#"))) bad++;
            if (!Arrays.equals(x.split(re), Fast.split(x, re))) bad++;
            int lim = r.nextInt(4) - 1;
            if (!Arrays.equals(x.split(re, lim), Fast.split(x, re, lim))) bad++;
        }
        System.out.println("regex checked " + n + " mismatches " + bad);
        bad = 0;
        for (int c = 0; c < 65536; c++) { if (Telex.bareOrig((char) c) != Fast.bare((char) c)) bad++; if (Telex.toneOrig(String.valueOf((char) c)) != Fast.tone(String.valueOf((char) c))) bad++; }
        for (int k = 0; k < 100000; k++) { String x = rnd(r, vi + "QWERTYabc", r.nextInt(8)); if (Telex.toneOrig(x) != Fast.tone(x)) bad++; }
        System.out.println("tone/bare checked mismatches " + bad);
    }
    static String rnd(Random r, String al, int len) { StringBuilder b = new StringBuilder(); for (int i = 0; i < len; i++) b.append(al.charAt(r.nextInt(al.length()))); return b.toString(); }
    static String mutate(Random r, String x, String al) { if (x.isEmpty()) return x; StringBuilder b = new StringBuilder(x); int i = r.nextInt(b.length()); switch (r.nextInt(3)) { case 0: b.setCharAt(i, al.charAt(r.nextInt(al.length()))); break; case 1: b.deleteCharAt(i); break; default: b.insert(i, al.charAt(r.nextInt(al.length()))); } return b.toString(); }
}
