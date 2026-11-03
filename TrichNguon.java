import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.*;

/**
 * Trích dữ liệu cộng đồng vào nguon/ (chạy lại khi nguồn cập nhật):
 *   java TrichNguon.java <thư mục nguồn tải về> <phrases.tsv của app>
 * - dvhcvn.json (daohoangson/dvhcvn, GPLv3): tỉnh, quận huyện, phường xã -> nguon/hanh-chinh.txt (N)
 * - vie_sentences.tsv (Tatoeba, CC BY 2.0 FR): cặp từ liền nhau trong câu giao tiếp -> nguon/cap-tu-tatoeba.txt (B, chỉ làm ngữ cảnh)
 * - vi_50k.txt (hermitdave/FrequencyWords, OpenSubtitles 2018, CC BY-SA 4.0): âm tiết hay dùng -> nguon/tu-ghep-phu-de.txt (W)
 * Phường xã trùng một từ ghép thông thường (Hòa Bình, Đoàn Kết) bị bỏ để giọng nói không viết hoa nhầm.
 */
public class TrichNguon {
    static String nfc(String s){return Normalizer.normalize(s,Normalizer.Form.NFC);}
    public static void main(String[] a)throws Exception{
        Path in=Paths.get(a[0]),out=Paths.get("nguon");Set<String> common=new HashSet<>();
        for(String line:Files.readAllLines(Paths.get(a[1]),StandardCharsets.UTF_8)){String[] c=line.split("\t");if(c.length>=2)common.add(nfc(c[0]+" "+c[1]).toLowerCase(Locale.ROOT));}
        // Administrative units: "name" fields with their type prefix removed.
        String json=Files.readString(in.resolve("dvhcvn.json"),StandardCharsets.UTF_8);Matcher m=Pattern.compile("\"level(\\d)_id\"\\s*:\\s*\"[^\"]*\",\\s*\"name\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        TreeSet<String> admin=new TreeSet<>();int dropped=0;
        while(m.find()){int level=Integer.parseInt(m.group(1));String name=nfc(m.group(2)).replaceFirst("^(Thành phố|Tỉnh|Quận|Huyện|Thị xã|Phường|Xã|Thị trấn)\\s+","").trim();
            if(name.isEmpty()||name.matches(".*\\d.*"))continue;if(level==3&&common.contains(name.toLowerCase(Locale.ROOT))){dropped++;continue;}admin.add(name);}
        Files.write(out.resolve("hanh-chinh.txt"),header("Đơn vị hành chính Việt Nam (tỉnh, quận huyện, phường xã) từ daohoangson/dvhcvn, GPLv3.",admin),StandardCharsets.UTF_8);
        // Conversation pairs.
        Map<String,Integer> pairs=new HashMap<>();
        for(String line:Files.readAllLines(in.resolve("vie_sentences.tsv"),StandardCharsets.UTF_8)){String[] c=line.split("\t");if(c.length<3)continue;
            String[] w=nfc(c[2]).toLowerCase(Locale.ROOT).split("[^\\p{L}]+");for(int i=0;i+1<w.length;i++)if(!w[i].isEmpty()&&!w[i+1].isEmpty())pairs.merge(w[i]+" "+w[i+1],1,Integer::sum);}
        List<String> pairLines=new ArrayList<>();pairs.entrySet().stream().filter(e->e.getValue()>=2).sorted((x,y)->y.getValue()-x.getValue()).forEach(e->pairLines.add(e.getKey()+"\t"+Math.min(1000,e.getValue())));
        Files.write(out.resolve("cap-tu-tatoeba.txt"),header("Cặp từ liền nhau (xuất hiện >= 2 lần) trong câu tiếng Việt của Tatoeba (tatoeba.org), CC BY 2.0 FR.",pairLines),StandardCharsets.UTF_8);
        // Subtitle syllables: frequent ones only (they become known words, so rare noise must stay out).
        List<String> syl=new ArrayList<>();for(String line:Files.readAllLines(in.resolve("vi_50k.txt"),StandardCharsets.UTF_8)){String[] c=line.trim().split(" ");if(c.length==2&&c[0].matches("\\p{L}+")&&Long.parseLong(c[1])>=200)syl.add(nfc(c[0]).toLowerCase(Locale.ROOT));}
        Files.write(out.resolve("tu-ghep-phu-de.txt"),header("Âm tiết dùng >= 200 lần trong phụ đề OpenSubtitles 2018, từ hermitdave/FrequencyWords, CC BY-SA 4.0.",syl),StandardCharsets.UTF_8);
        System.out.println("hành chính "+admin.size()+" (bỏ "+dropped+" trùng từ thường), cặp từ "+pairLines.size()+", âm tiết "+syl.size());
    }
    static List<String> header(String note,Collection<String> lines){List<String> l=new ArrayList<>();l.add("# "+note);l.add("# Tự động trích bằng TrichNguon.java; sửa ở nguồn rồi chạy lại.");l.addAll(lines);return l;}
}
