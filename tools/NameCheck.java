import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;

/** Every name+descriptor of an obfuscated (m_/f_) member used by new classes must already be used by the original jar. */
public class NameCheck {
    static Set<String> refs(Path dir) throws Exception {
        Set<String> s = new TreeSet<>();
        try (var w = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) w.filter(f -> f.toString().endsWith(".class"))::iterator) {
                ClassNode cn = new ClassNode();
                new ClassReader(Files.readAllBytes(p)).accept(cn, 0);
                for (MethodNode m : cn.methods) for (AbstractInsnNode in : m.instructions) {
                    if (in instanceof MethodInsnNode mi && (mi.name.startsWith("m_") || mi.name.startsWith("f_"))) s.add(mi.name + mi.desc);
                    if (in instanceof FieldInsnNode fi && (fi.name.startsWith("m_") || fi.name.startsWith("f_"))) s.add(fi.name + fi.desc);
                }
            }
        }
        return s;
    }
    public static void main(String[] a) throws Exception {
        Set<String> known = refs(Paths.get(a[0]));
        for (String r : refs(Paths.get(a[1]))) if (!known.contains(r)) System.out.println("UNKNOWN " + r);
        System.out.println("checked");
    }
}
