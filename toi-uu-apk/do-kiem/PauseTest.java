package vn.personal.vietkey;
public class PauseTest {
  // moi phan tu: {thoi diem ms, chu tam thoi}
  static String run(PersonalData d, Object[][] seq, String fin) {
    Fast.resetPauses();
    for (Object[] e : seq) Fast.notePartial((String) e[1], (Long) e[0]);
    return Fast.pauseCommas(d, fin, Fast.pauses) + "   [ngat sau tu: " + Fast.pauses + "]";
  }
  public static void main(String[] a) throws Exception {
    Bench.A = a[0]; PersonalData d = Bench.build(a[1]);
    System.out.println(run(d, new Object[][]{{0L,"hôm nay"},{300L,"hôm nay anh"},{600L,"hôm nay anh bận"},{1700L,"hôm nay anh bận nên"},{2000L,"hôm nay anh bận nên không đi được"}}, "hôm nay anh bận nên không đi được"));
    System.out.println(run(d, new Object[][]{{0L,"em ơi"},{1200L,"em ơi tối nay"},{1500L,"em ơi tối nay ăn gì"}}, "em ơi tối nay ăn gì"));
    System.out.println(run(d, new Object[][]{{0L,"anh đi"},{300L,"anh đi mua"},{1400L,"anh đi mua sữa"},{1700L,"anh đi mua sữa rồi về"}}, "anh đi mua sữa rồi về"));
    System.out.println(run(d, new Object[][]{{0L,"cảm ơn"},{400L,"cảm ơn anh"},{1500L,"cảm ơn anh nhé"}}, "cảm ơn anh nhé"));
    System.out.println(run(d, new Object[][]{{0L,"mai họp"},{350L,"mai họp lúc chín giờ"},{1600L,"mai họp lúc chín giờ anh nhớ"},{1900L,"mai họp lúc chín giờ anh nhớ đến sớm"}}, "mai họp lúc chín giờ anh nhớ đến sớm"));
    System.out.println(run(d, new Object[][]{{0L,"người"},{1300L,"người dân"},{1600L,"người dân rất vui"}}, "người dân rất vui"));
    System.out.println(run(d, new Object[][]{{0L,"xin chào."},{1500L,"xin chào. Hôm nay"}}, "Xin chào. Hôm nay đẹp trời."));
  }
}
