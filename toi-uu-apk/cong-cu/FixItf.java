import org.objectweb.asm.*;
import java.io.*; import java.util.*; import java.util.zip.*;
public class FixItf {
  public static void main(String[] a) throws Exception {
    ZipInputStream in = new ZipInputStream(new FileInputStream(a[0])); ZipOutputStream out = new ZipOutputStream(new FileOutputStream(a[1]));
    ZipEntry e; int fixed = 0;
    while ((e = in.getNextEntry()) != null) {
      byte[] b = in.readAllBytes();
      if (e.getName().endsWith(".class")) {
        ClassReader cr = new ClassReader(b); ClassWriter cw = new ClassWriter(0);
        final int[] f = {0};
        cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
          public MethodVisitor visitMethod(int acc, String n, String d, String s, String[] ex) {
            return new MethodVisitor(Opcodes.ASM9, super.visitMethod(acc, n, d, s, ex)) {
              public void visitMethodInsn(int op, String owner, String name, String desc, boolean itf) {
                if (op == Opcodes.INVOKESTATIC && !itf && owner.startsWith("java/")) {
                  try { if (Class.forName(owner.replace('/', '.')).isInterface()) { itf = true; f[0]++; } } catch (Throwable t) {}
                }
                super.visitMethodInsn(op, owner, name, desc, itf);
              }
            };
          }
        }, 0);
        fixed += f[0]; b = cw.toByteArray();
      }
      out.putNextEntry(new ZipEntry(e.getName())); out.write(b); out.closeEntry();
    }
    out.close(); System.out.println("fixed " + fixed);
  }
}
