import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
/**
 * Every external member referenced by the recompiled classes must be confirmed by an independent 1.21.1 source:
 * the original NeoForge jar's own references, Parchment 1.21.1, or a tsrg-1.21.1-verified extras entry.
 */
public class ApiCheck {
    public static void main(String[] a) throws Exception {
        Verify.load(a[0], Paths.get(a[1]));
        Set<String> exact = new HashSet<>(Verify.known);
        for (int i = 3; i < a.length; i++)
            for (String l : Files.readAllLines(Paths.get(a[i]))) { String[] p = l.split("\t"); if (p.length >= 3) exact.add(p[0] + "." + p[1] + p[2]); }
        Set<String> out = new TreeSet<>();
        try (var w = Files.walk(Paths.get(a[2]))) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".class"))::iterator) {
                ClassNode cn = new ClassNode(); new ClassReader(Files.readAllBytes(p)).accept(cn, 0);
                for (MethodNode m : cn.methods) for (AbstractInsnNode in : m.instructions) {
                    List<String[]> refs = new ArrayList<>();
                    if (in instanceof MethodInsnNode mi) refs.add(new String[]{mi.owner, mi.name, mi.desc});
                    else if (in instanceof FieldInsnNode fi) refs.add(new String[]{fi.owner, fi.name, fi.desc});
                    else if (in instanceof InvokeDynamicInsnNode indy) for (Object o : indy.bsmArgs) if (o instanceof Handle h) refs.add(new String[]{h.getOwner(), h.getName(), h.getDesc()});
                    for (String[] r : refs) {
                        if (r[0].startsWith("com/drunkard") || r[0].startsWith("java/") || r[0].startsWith("[")) continue;
                        String k = r[0] + "." + r[1] + r[2];
                        if (exact.contains(k)) continue;
                        String how = Verify.known.contains(r[1] + r[2]) ? "other-owner" : "UNVERIFIED";
                        out.add(how + "\t" + r[0] + "\t" + r[1] + "\t" + r[2]);
                    }
                }
            }
        }
        out.forEach(System.out::println);
    }
}
