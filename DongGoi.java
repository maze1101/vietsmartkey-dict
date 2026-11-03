import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * Đóng gói từ điển mở rộng VietSmartKey: nguon/*.txt -> phat-hanh/vi-extra.vskdict (+ .sha256).
 * Chạy: java DongGoi.java dict-1
 * tu-ghep*.txt -> dòng W (từ ghép<TAB>độ phổ biến), cap-tu*.txt -> dòng B (cặp từ hay đi liền, chỉ làm ngữ cảnh),
 * các tệp khác -> dòng N (tên riêng). Dòng # là chú thích.
 */
public class DongGoi {
    public static void main(String[] a)throws Exception{
        String version=a.length>0?a[0]:"dict-dev";Path root=Paths.get(System.getProperty("user.dir"));Path src=root.resolve("nguon"),out=root.resolve("phat-hanh");Files.createDirectories(out);
        StringBuilder b=new StringBuilder("VSKDICT\t1\t"+version+"\tVietSmartKey tu dien mo rong\n");LinkedHashSet<String> seen=new LinkedHashSet<>();int w=0,n=0;
        List<Path> files=new ArrayList<>();try(DirectoryStream<Path> d=Files.newDirectoryStream(src,"*.txt")){for(Path p:d)files.add(p);}Collections.sort(files);
        for(Path p:files){String fn=p.getFileName().toString();boolean words=fn.startsWith("tu-ghep")||fn.startsWith("cap-tu");String type=fn.startsWith("cap-tu")?"B":"W";
            for(String line:Files.readAllLines(p,StandardCharsets.UTF_8)){line=Normalizer.normalize(line.strip(),Normalizer.Form.NFC);if(line.isEmpty()||line.startsWith("#"))continue;
                String[] c=line.split("\t");String text=c[0].trim().replaceAll("\\s+"," ");if(text.contains("\t")||text.length()>80)continue;
                String key=(words?type:"N")+"\t"+text.toLowerCase(Locale.ROOT);if(!seen.add(key))continue;
                if(words){b.append(type).append('\t').append(text.toLowerCase(Locale.ROOT));if(c.length>1&&c[1].trim().matches("\\d{1,4}"))b.append('\t').append(c[1].trim());b.append('\n');w++;}
                else{b.append("N\t").append(text).append('\n');n++;}}}
        byte[] raw=b.toString().getBytes(StandardCharsets.UTF_8);ByteArrayOutputStream z=new ByteArrayOutputStream();try(GZIPOutputStream g=new GZIPOutputStream(z)){g.write(raw);}
        byte[] pack=z.toByteArray();Path file=out.resolve("vi-extra.vskdict");Files.write(file,pack);
        StringBuilder h=new StringBuilder();for(byte x:MessageDigest.getInstance("SHA-256").digest(pack))h.append(String.format("%02x",x));
        Files.writeString(out.resolve("vi-extra.vskdict.sha256"),h+"  vi-extra.vskdict\n",StandardCharsets.US_ASCII);
        System.out.println(version+": "+w+" từ ghép, "+n+" tên riêng, "+pack.length+" byte, sha256 "+h);
    }
}
