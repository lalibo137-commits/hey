import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
public class Refs {
    public static void main(String[] a) throws Exception {
        Set<String> out = new TreeSet<>();
        try (var w = Files.walk(Paths.get(a[0]))) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".class") && x.toString().contains("/com/drunkard/"))::iterator) {
                ClassNode cn = new ClassNode(); new ClassReader(Files.readAllBytes(p)).accept(cn, 0);
                for (MethodNode m : cn.methods) for (AbstractInsnNode in : m.instructions) {
                    if (in instanceof MethodInsnNode mi && !mi.owner.startsWith("com/drunkard") && !mi.owner.startsWith("java/")) out.add(mi.owner + "." + mi.name + mi.desc);
                    if (in instanceof FieldInsnNode fi && !fi.owner.startsWith("com/drunkard") && !fi.owner.startsWith("java/")) out.add(fi.owner + "." + fi.name + ":" + fi.desc);
                }
            }
        }
        out.forEach(System.out::println);
    }
}
