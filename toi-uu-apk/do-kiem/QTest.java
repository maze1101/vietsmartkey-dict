package vn.personal.vietkey;
public class QTest { public static void main(String[] a) {
  String[] s = {"tối nay mấy giờ anh về", "anh ăn cơm chưa", "em có đi không", "sao lại thế", "anh về rồi", "quan trọng nhất là bàn phím dùng phải mượt nhanh", "toàn dấu chấm không", "không thấy dấu phẩy", "ở đâu vậy", "bao giờ anh đến", "anh biết không", "thật à", "em nấu cơm rồi", "khi nào xong thì báo anh", "không sao đâu", "chưa biết làm sao"};
  for (String x : s) System.out.println((VoiceText.question(x) ? "?  " : ".  ") + x);
}}
