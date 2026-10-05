import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import java.nio.file.*;
/** Runs ASM's type-inferring analyzer over every method of every class in a directory. */
public class BcVerify {
    public static void main(String[] a) throws Exception {
        int n = 0, bad = 0;
        try (var w = Files.walk(Paths.get(a[0]))) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".class"))::iterator) {
                ClassNode cn = new ClassNode(); new ClassReader(Files.readAllBytes(p)).accept(cn, 0); n++;
                for (MethodNode m : cn.methods) {
                    try { new Analyzer<>(new BasicVerifier()).analyze(cn.name, m); }
                    catch (AnalyzerException e) { bad++; System.out.println(cn.name + "." + m.name + m.desc + ": " + e.getMessage()); }
                }
            }
        }
        System.out.println(n + " classes, " + bad + " bad methods");
    }
}
