import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
/** For every recompiled class that exists in the original jar: methods (name+desc, incl. bridges) and fields the original had but the new one lacks. Anonymous/lambda names are skipped. */
public class MethodDiff {
    public static void main(String[] a) throws Exception {
        int bad = 0;
        try (var w = Files.walk(Paths.get(a[1]))) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".class"))::iterator) {
                Path rel = Paths.get(a[1]).relativize(p), orig = Paths.get(a[0]).resolve(rel);
                if (!Files.exists(orig) || rel.toString().matches(".*\\$\\d+\\.class")) continue;
                ClassNode o = new ClassNode(), n = new ClassNode();
                new ClassReader(Files.readAllBytes(orig)).accept(o, 0); new ClassReader(Files.readAllBytes(p)).accept(n, 0);
                Set<String> have = new HashSet<>();
                for (MethodNode m : n.methods) have.add(m.name + m.desc);
                for (FieldNode f : n.fields) have.add(f.name + ":" + f.desc);
                for (MethodNode m : o.methods) if (!m.name.startsWith("lambda$") && !have.contains(m.name + m.desc)) { System.out.println("MISSING METHOD " + o.name + "." + m.name + m.desc + ((m.access & Opcodes.ACC_BRIDGE) != 0 ? " (bridge)" : "")); bad++; }
                for (FieldNode f : o.fields) if (!have.contains(f.name + ":" + f.desc)) { System.out.println("MISSING FIELD " + o.name + "." + f.name + ":" + f.desc); bad++; }
                if (!Objects.equals(o.superName, n.superName) || !new HashSet<>(o.interfaces).equals(new HashSet<>(n.interfaces))) { System.out.println("HIERARCHY " + o.name + " " + o.superName + o.interfaces + " -> " + n.superName + n.interfaces); bad++; }
            }
        }
        System.out.println(bad + " differences");
        if (bad > 0) System.exit(1);
    }
}
