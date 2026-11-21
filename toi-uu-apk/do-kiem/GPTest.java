package vn.personal.vietkey;
public class GPTest {
  // mo phong: van ban truoc con tro + cac luot Google tra ve
  static String run(String... g) {
    StringBuilder doc = new StringBuilder();
    for (String t : g) {
      t = Fast.fixQuestion(t);
      if (Fast.joinWithComma(doc.toString(), t)) { int cut = doc.toString().endsWith(". ") ? 2 : 1; doc.setLength(doc.length() - cut); doc.append(", ");
        t = Character.toLowerCase(t.charAt(0)) + t.substring(1); }
      doc.append(t).append(' ');
    }
    return doc.toString().trim();
  }
  public static void main(String[] a) {
    System.out.println(run("Quan trọng nhất là bàn phím dùng phải mượt nhanh.", "Chế độ giọng nói hiện tại bây giờ ngắt nhanh quá.", "Máy bật lần đầu thì ngắt câu đầu tiên.", "Sau đó nếu bật lần thứ hai thì mở liên tục.", "Toàn dấu chấm không?", "Không thấy dấu phẩy.", "Ngắt nhịp và tự bật lại quá nhiều lần."));
    System.out.println(run("Anh ơi.", "Tối nay mấy giờ anh về.", "Em nấu cơm rồi.", "Nhớ mua giúp em chai nước mắm nhé.", "Cảm ơn anh."));
    System.out.println(run("Dạ.", "Em gửi rồi ạ.", "Anh kiểm tra giúp em.", "Nhưng file hơi nặng."));
    System.out.println(run("Hôm nay trời mưa.", "Nên anh về muộn.", "Em ăn cơm chưa."));
  }
}
