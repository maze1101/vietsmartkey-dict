import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.*;

/**
 * Đếm cặp âm tiết liền nhau trong Wikipedia tiếng Việt (CC BY-SA 4.0) -> nguon/cap-tu-wikipedia.txt (dòng B).
 *   bzip2 -dc viwiki-...-pages-articles1.xml...bz2 | java -Xmx4g TrichWiki.java <vi.dic> <số cặp giữ lại>
 * Bỏ trang đổi hướng, bản mẫu {{...}}, bảng {|...|}, <ref>, thẻ HTML, tệp/hình; [[a|b]] -> b. Chỉ đếm âm tiết có trong
 * vi.dic, trong cùng một câu (không nối qua dấu câu), nên số liệu gần với cách viết thường ngày.
 */
public class TrichWiki {
    public static void main(String[] a)throws Exception{
        Set<String> vocab=new HashSet<>();for(String s:Files.readAllLines(Paths.get(a[0]),StandardCharsets.UTF_8)){s=Normalizer.normalize(s.split("/",2)[0].trim().toLowerCase(Locale.ROOT),Normalizer.Form.NFC);if(s.matches("\\p{L}+"))vocab.add(s);}
        int keep=Integer.parseInt(a[1]);HashMap<String,int[]> pairs=new HashMap<>(1<<22);
        BufferedReader in=new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8),1<<20);
        StringBuilder page=new StringBuilder();boolean inText=false;String line;long pages=0,sentences=0;
        while((line=in.readLine())!=null){
            if(!inText){int at=line.indexOf("<text");if(at<0)continue;int gt=line.indexOf('>',at);if(gt<0)continue;line=line.substring(gt+1);inText=true;page.setLength(0);}
            int end=line.indexOf("</text>");if(end>=0){page.append(line,0,end);inText=false;pages++;sentences+=count(page.toString(),vocab,pairs);
                if(pairs.size()>12_000_000)prune(pairs);if(pages%20000==0)System.err.println(pages+" trang, "+sentences+" câu, "+pairs.size()+" cặp");}
            else page.append(line).append('\n');
        }
        List<Map.Entry<String,int[]>> list=new ArrayList<>(pairs.entrySet());list.sort((x,y)->y.getValue()[0]-x.getValue()[0]);
        List<String> out=new ArrayList<>();out.add("# Cặp âm tiết liền nhau trong Wikipedia tiếng Việt (vi.wikipedia.org, phần bài 1–832082), CC BY-SA 4.0.");out.add("# Tự động trích bằng TrichWiki.java: "+pages+" trang, "+sentences+" câu; giữ "+Math.min(keep,list.size())+" cặp phổ biến nhất.");
        for(int i=0;i<Math.min(keep,list.size());i++){int n=list.get(i).getValue()[0];out.add(list.get(i).getKey()+"\t"+Math.max(1,Math.min(1000,n/20)));}
        Files.write(Paths.get("nguon","cap-tu-wikipedia.txt"),out,StandardCharsets.UTF_8);
        System.err.println("Xong: "+pages+" trang, "+sentences+" câu, giữ "+(out.size()-2)+" cặp");
    }
    static void prune(HashMap<String,int[]> pairs){pairs.values().removeIf(v->v[0]<=2);}
    static long count(String raw,Set<String> vocab,HashMap<String,int[]> pairs){
        if(raw.regionMatches(true,0,"#redirect",0,9)||raw.regionMatches(true,0,"#đổi",0,4))return 0;
        String t=xml(raw);t=strip(t);long n=0;
        for(String sentence:t.split("[.!?;:\\n()\\[\\]\"“”]+")){String[] w=Normalizer.normalize(sentence,Normalizer.Form.NFC).toLowerCase(Locale.ROOT).split("[^\\p{L}]+");
            boolean any=false;for(int i=0;i+1<w.length;i++)if(vocab.contains(w[i])&&vocab.contains(w[i+1])){pairs.computeIfAbsent(w[i]+" "+w[i+1],k->new int[1])[0]++;any=true;}if(any)n++;}
        return n;
    }
    static String xml(String s){return s.replace("&lt;","<").replace("&gt;",">").replace("&quot;","\"").replace("&#039;","'").replace("&nbsp;"," ").replace("&amp;","&");}
    /** Removes wiki markup that is not running text. */
    static String strip(String s){
        StringBuilder b=new StringBuilder(s.length());int depth=0,table=0;
        for(int i=0;i<s.length();i++){
            if(s.startsWith("{{",i)){depth++;i++;continue;}if(depth>0&&s.startsWith("}}",i)){depth--;i++;continue;}
            if(s.startsWith("{|",i)){table++;i++;continue;}if(table>0&&s.startsWith("|}",i)){table--;i++;continue;}
            if(depth==0&&table==0)b.append(s.charAt(i));}
        String t=b.toString().replaceAll("(?s)<ref[^>]*/>|<ref[^>]*>.*?</ref>|<!--.*?-->","").replaceAll("<[^>]+>","");
        t=t.replaceAll("\\[\\[(?:Tập tin|Hình|File|Image|Thể loại|Category):[^\\]]*\\]\\]","").replaceAll("\\[\\[[^\\]|]*\\|([^\\]]*)\\]\\]","$1").replaceAll("\\[\\[([^\\]]*)\\]\\]","$1");
        return t.replaceAll("\\[https?://\\S+\\s?([^\\]]*)\\]","$1").replaceAll("'{2,}","").replaceAll("(?m)^=+.*?=+\\s*$","\n").replaceAll("(?m)^[*#:;|!].*$","\n");
    }
}
