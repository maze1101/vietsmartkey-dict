package vn.personal.vietkey;
import java.util.*;
public class UserVoice {
  public static void main(String[] a) throws Exception {
    Bench.A = a[0]; PersonalData d = Bench.build(a[1]);
    String[] g = {"Quan trọng nhất là bàn phím dùng phải mượt nhanh.", "Chế độ giọng nói hiện tại bây giờ ngắt nhanh quá.", "Mở dịch lần đầu thì nhất.",
      "Máy bật lần đầu thì ngắt câu đầu tiên.", "Sau đó nếu bật lần thứ hai thì mở liên tục.", "Toàn dấu chấm không?", "Không thấy dấu phẩy.", "Ngắt nhịp và tự bật lại quá nhiều lần."};
    java.util.function.Predicate<String> p = d::isCommonWord; java.util.function.BiPredicate<String,String> bp = d::isPhrase;
    System.out.println("Hien tai (giu dau cua Google):\n  " + VoiceText.dictate("", d.extraNames, p, bp, g));
    String[] s = new String[g.length]; for (int i = 0; i < g.length; i++) s[i] = g[i].endsWith(".") ? g[i].substring(0, g[i].length() - 1) : g[i];
    System.out.println("Bo dau cham cuoi cua Google, de app noi cau:\n  " + VoiceText.dictate("", d.extraNames, p, bp, s));
    String[] chat = {"Anh ơi.", "Tối nay mấy giờ anh về.", "Em nấu cơm rồi.", "Nhớ mua giúp em chai nước mắm nhé.", "Cảm ơn anh."};
    String[] c2 = new String[chat.length]; for (int i = 0; i < chat.length; i++) c2[i] = chat[i].endsWith(".") ? chat[i].substring(0, chat[i].length() - 1) : chat[i];
    System.out.println("Vi du nhan tin, giu dau Google:\n  " + VoiceText.dictate("", d.extraNames, p, bp, chat));
    System.out.println("Vi du nhan tin, app noi cau:\n  " + VoiceText.dictate("", d.extraNames, p, bp, c2));
  }
}
