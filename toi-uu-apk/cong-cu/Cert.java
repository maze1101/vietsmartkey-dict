import com.android.apksig.*; import java.io.*; import java.security.MessageDigest;
public class Cert { public static void main(String[] a) throws Exception { for (String f : a) { ApkVerifier.Result r = new ApkVerifier.Builder(new File(f)).setMinCheckedPlatformVersion(24).build().verify();
  byte[] d = MessageDigest.getInstance("SHA-256").digest(r.getSignerCertificates().get(0).getEncoded()); StringBuilder b = new StringBuilder(); for (byte x : d) b.append(String.format("%02x", x)); System.out.println(f.replaceAll(".*/","") + " verified=" + r.isVerified() + " cert=" + b); } } }
