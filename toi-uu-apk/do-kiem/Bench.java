package vn.personal.vietkey;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class Bench {
    static String A;
    static Reader r2(String f) throws IOException { return new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8); }
    static Reader r(String f) throws IOException { return new InputStreamReader(new FileInputStream(A+"/"+f), StandardCharsets.UTF_8); }
    public static PersonalData build(String extra) throws Exception {
        PersonalData d = new PersonalData();
        d.loadMacros(new StringReader(""));
        d.loadVocabulary(r("vi.dic"));
        d.loadPhrases(r("phrases.tsv"));
        d.loadFrequencies(new FileInputStream(A+"/wordfreq-small-vi.cbpack"));
        d.learned.load(System.getenv("LEARNED")==null?new StringReader(""):r2(System.getenv("LEARNED")));
        d.setStrength(1);
        if (extra != null) d.loadExtra(new InputStreamReader(new GZIPInputStream(new FileInputStream(extra)), StandardCharsets.UTF_8));
        return d;
    }
    // simulated session: telex raw words
    static final String TEXT = "xin chaof cacs banj hoom nay tooi ddi lafm vieejc ow coong ty vaf gawpj nhieeuf nguowif mowis tooi thaays raats vui vaf hanhj phucs khi ddwowcj gaaj moij nguowif trong nhom chungs ta seex cungf nhau xaay dwngj mootj tuwowng lai toots ddepj hown cho ddaats nuwowcs vieetj nam thaan yeeu cuar chungs ta tooi muoons ddi hocj tieengs anh vaf lafm vieecj owr thanhf phoos hoof chis minh ddeer kieems theem tieenf nuoois gia ddinhf nhor beee cuar minhf tooi nghix raangf ddaay laf mootj dduwowngf ddi ddungs ddawns maf tooi ddax chonj trong cuoocj ddowif nayf "
      + "baanj cos khoer khoong hoom qua tooi goji ddieenj thoaij maf baanj khoong nghe maays tooi lo quas ddi maats nguw caar ddeem anh ow ddaau vaayj sao khoong traar lowif tin nhawns cuar em ";
    public static void main(String[] a) throws Exception {
        A = a[0]; String extra = a.length > 1 && !a[1].equals("-") ? a[1] : null;
        int rounds = a.length > 2 ? Integer.parseInt(a[2]) : 3;
        long t0 = System.nanoTime();
        PersonalData d = build(extra);
        ForeignWords fw = new ForeignWords(); fw.load(new FileInputStream(A+"/wordfreq-small-en.cbpack"));
        System.out.printf("load %.0f ms  words=%d general=%d vocab=%d%n", (System.nanoTime()-t0)/1e6, d.words.size(), d.generalWords.size(), d.vocabulary.size());
        StringBuilder out = new StringBuilder(); StringBuilder dump = new StringBuilder();
        String[] ws = TEXT.trim().split(" ");
        for (int round = 0; round < rounds; round++) {
            String worstRaw = ""; long tOdds=0,tCompose=0,tSuggest=0,tCorrect=0,tCommit=0; int keys=0; long worst=0;
            String prev = "";
            out.setLength(0); dump.setLength(0);
            for (String w : ws) {
                CompositionBuffer raw = new CompositionBuffer();
                for (int i = 0; i < w.length(); i++) {
                    long k0 = System.nanoTime();
                    long s = System.nanoTime(); float[] od = d.nextKeyOdds(raw.toString()); tOdds += System.nanoTime()-s; dump.append("odds ").append(raw.toString()).append(" ").append(Arrays.toString(od)).append("\n");
                    raw.append(String.valueOf(w.charAt(i)));
                    s = System.nanoTime();
                    String comp = raw.renderSmart(0, fw, d);
                    fw.keepLiteral(raw.toString(), raw.render(0), 0, d);
                    tCompose += System.nanoTime()-s;
                    // refreshSuggestions x2 (key() + onUpdateSelection)
                    for (int rep = 0; rep < 2; rep++) {
                        s = System.nanoTime();
                        List<PersonalData.Candidate> c = d.suggest(raw.toString(), raw.renderSmart(0,fw,d), prev, 3, 0);
                        tSuggest += System.nanoTime()-s;
                        s = System.nanoTime();
                        d.correct(raw.toString(), raw.renderSmart(0,fw,d), prev, 0);
                        tCorrect += System.nanoTime()-s;
                        if (rep==0) { dump.append(raw.toString()).append(" | ").append(prev).append(" =>"); for (PersonalData.Candidate x : c) dump.append(" ").append(x.text).append("/").append(x.score).append("/").append(x.macro).append("/").append(x.kind); dump.append(" || ").append(d.correct(raw.toString(), raw.renderSmart(0,fw,d), prev, 0)).append("\n"); }
                        if (rep==0 && i == w.length()-1) { out.append('[').append(w).append("->"); for (PersonalData.Candidate x : c) out.append(x.text).append('|'); out.append(']'); }
                    }
                    keys++;
                    long kt = System.nanoTime()-k0; if (kt > worst) { worst = kt; worstRaw = raw.toString() + " prev=" + prev; }
                }
                long s = System.nanoTime();
                String comp = raw.renderSmart(0, fw, d);
                String fixed = d.correct(raw.toString(), comp, prev, 0);
                d.learned.observe(prev, fixed);
                tCommit += System.nanoTime()-s;
                out.append(fixed).append(' ');
                prev = fixed;
            }
            System.out.printf("round %d: keys=%d  per-key avg: odds %.2f compose %.2f suggest %.2f correct %.2f ms | commit avg %.2f ms | worst key %.1f ms%n", round, keys,
                tOdds/1e6/keys, tCompose/1e6/keys, tSuggest/1e6/keys, tCorrect/1e6/keys, tCommit/1e6/ws.length, worst/1e6); System.out.println("   worst key: " + worstRaw);
        }
        try (Writer wr = new OutputStreamWriter(new FileOutputStream(a.length>3?a[3]:"out.txt"), StandardCharsets.UTF_8)) { wr.write(out.toString()); wr.write("\n"); wr.write(dump.toString()); }
        System.out.println(out.substring(0, Math.min(1500, out.length())));
    }
}
