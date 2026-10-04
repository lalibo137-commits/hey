import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;

/** Compares the external member references and per-method shapes of two class directories. */
public class RefDiff {
    static Map<String, Set<String>> refs(Path dir) throws Exception {
        Map<String, Set<String>> out = new TreeMap<>();
        try (var w = Files.walk(dir)) {
            for (Path p : (Iterable<Path>) w.filter(f -> f.toString().endsWith(".class"))::iterator) {
                ClassNode cn = new ClassNode();
                new ClassReader(Files.readAllBytes(p)).accept(cn, 0);
                Set<String> s = new TreeSet<>();
                s.add("super " + cn.superName + " " + cn.interfaces);
                for (FieldNode f : cn.fields) s.add("field " + f.access + " " + f.name + f.desc);
                for (MethodNode m : cn.methods) {
                    if (m.name.startsWith("lambda$")) continue;
                    s.add("method " + (m.access & 0x7) + " " + m.name + m.desc);
                    for (AbstractInsnNode in : m.instructions) {
                        if (in instanceof MethodInsnNode mi && !mi.owner.startsWith("java/")) s.add("  call " + mi.getOpcode() + " " + mi.owner + "." + mi.name + mi.desc);
                        if (in instanceof InvokeDynamicInsnNode di) {
                            StringBuilder b = new StringBuilder("  indy " + di.name + di.desc);
                            for (Object o : di.bsmArgs) if (o instanceof Handle h) b.append(" ").append(h.getOwner().startsWith("com/drunkard") && h.getName().startsWith("lambda$") ? "lambda" : h.getOwner() + "." + h.getName() + h.getDesc()); else if (o instanceof Type t) b.append(" ").append(t.getDescriptor());
                            s.add(b.toString());
                        }
                        if (in instanceof FieldInsnNode fi && !fi.owner.startsWith("java/")) s.add("  fld " + fi.getOpcode() + " " + fi.owner + "." + fi.name + fi.desc);
                    }
                }
                out.put(cn.name, s);
            }
        }
        return out;
    }

    public static void main(String[] a) throws Exception {
        Map<String, Set<String>> x = refs(Paths.get(a[0])), y = refs(Paths.get(a[1]));
        for (String k : y.keySet()) {
            Set<String> o = x.getOrDefault(k, Set.of()), n = y.get(k);
            for (String s : o) if (!n.contains(s)) System.out.println(k + " - " + s);
            for (String s : n) if (!o.contains(s)) System.out.println(k + " + " + s);
        }
    }
}
