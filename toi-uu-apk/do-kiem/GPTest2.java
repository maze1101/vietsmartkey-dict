package vn.personal.vietkey;
import java.util.regex.*;
public class GPTest2 {
  static String run(String... g) {
    StringBuilder doc = new StringBuilder();
    for (String t : g) {
      Matcher m = Pattern.compile("([.?!])(\\s?)$").matcher(doc);
      if (Fast.isTail(t) && m.find()) {
        String prev = Fast.stripTail(Fast.bareSentence(Fast.lastSentence(doc.substring(0, m.start()))));
        String p = m.group(1).equals("?") || VoiceText.question(prev) ? "?" : m.group(1);
        doc.setLength(m.start()); doc.append(' ').append(Fast.bareSentence(t)).append(p).append(' '); continue;
      }
      t = Fast.fixQuestion(t);
      if (Fast.joinWithComma(doc.toString(), t)) { int cut = doc.toString().endsWith(". ") ? 2 : 1; doc.setLength(doc.length() - cut); doc.append(", "); t = Character.toLowerCase(t.charAt(0)) + t.substring(1); }
      doc.append(t).append(' ');
    }
    return doc.toString().trim();
  }
  public static void main(String[] a) {
    System.out.println(run("Chị ơi, khi nào chị đến vậy?", "Chị."));
    System.out.println(run("Chị ơi, khi nào chị đến vậy.", "Chị."));
    System.out.println(run("Em gửi rồi.", "Anh ạ."));
    System.out.println(run("Thần ơi, sao không thấy mình đến thuê đồ anh ơi."));
    System.out.println(run("Bé Lan hôm nay có khỏe không?", "Cô."));
    System.out.println(run("Mai em đi học.", "Nhé."));
    System.out.println(run("Chị ơi, khi nào mình đến trả đồ vậy?", "Bé Lan hôm nay có khỏe không?"));
  }
}
