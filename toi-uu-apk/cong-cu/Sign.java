import com.android.apksig.*;
import java.io.*; import java.security.*; import java.security.cert.*; import java.util.*;
public class Sign {
  public static void main(String[] a) throws Exception {
    char[] pw = new String(java.nio.file.Files.readAllBytes(new File(a[1]).toPath())).trim().toCharArray();
    KeyStore ks = KeyStore.getInstance("PKCS12"); ks.load(new FileInputStream(a[0]), pw);
    PrivateKey k = (PrivateKey) ks.getKey("vsk", pw); X509Certificate c = (X509Certificate) ks.getCertificate("vsk");
    ApkSigner.SignerConfig sc = new ApkSigner.SignerConfig.Builder("VSK", k, Collections.singletonList(c)).build();
    new ApkSigner.Builder(Collections.singletonList(sc)).setInputApk(new File(a[2])).setOutputApk(new File(a[3]))
      .setMinSdkVersion(34).setV1SigningEnabled(false).setV2SigningEnabled(true).setCreatedBy("VietSmartKey-fast").build().sign();
    ApkVerifier.Result r = new ApkVerifier.Builder(new File(a[3])).setMinCheckedPlatformVersion(24).build().verify();
    System.out.println("verified=" + r.isVerified() + " v2=" + r.isVerifiedUsingV2Scheme() + " errors=" + r.getErrors() + " warnings=" + r.getWarnings());
    for (ApkVerifier.Result.V2SchemeSignerInfo s : r.getV2SchemeSigners()) System.out.println("v2 signer errors " + s.getErrors());
  }
}
