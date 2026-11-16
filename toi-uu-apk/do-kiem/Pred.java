package vn.personal.vietkey;
import java.io.*; import java.util.*;
public class Pred {
  public static void main(String[] a) throws Exception {
    Bench.A = a[0]; PersonalData d = Bench.build(a[1]);
    for (String p : new String[]{"tôi","anh","em","không","đã","của","người","cảm","chúng","bạn","hôm","đi"}) {
      List<PersonalData.Candidate> c = d.suggest("", "", p, 3, 0);
      StringBuilder b = new StringBuilder(p + " -> ");
      for (PersonalData.Candidate x : c) b.append(x.text).append('(').append(x.score).append(") ");
      HashMap<String,Integer> ctx = d.contextNext.get(p); HashMap<String,Integer> ph = d.phraseNext.get(p);
      b.append(" | ctx=").append(ctx==null?0:ctx.size()).append(" phr=").append(ph==null?0:ph.size());
      System.out.println(b);
    }
    System.out.println("freq là=" + d.frequencies.score("là") + " không=" + d.frequencies.score("không") + " tớ=" + d.frequencies.score("tớ"));
  }
}
