import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
/** Every bridge method a Forge class has (name translated, compared by name + parameter count) must also exist as a bridge in the ported class. */
public class BridgeCheck {
    public static void main(String[] a) throws Exception {
        Map<String, String> names = new HashMap<>();
        for (String f : new String[]{"srg2moj.tsv", "manual.tsv"})
            for (String l : Files.readAllLines(Paths.get(a[2], f))) { String[] p = l.split("\t"); names.put(p[0], p[1]); }
        int bad = 0, seen = 0;
        try (ZipFile z = new ZipFile(a[0])) {
            for (ZipEntry e : Collections.list(z.entries())) {
                if (!e.getName().endsWith(".class")) continue;
                Path np = Paths.get(a[1], e.getName());
                if (!Files.exists(np)) continue;
                ClassNode f = new ClassNode(), n = new ClassNode();
                new ClassReader(z.getInputStream(e).readAllBytes()).accept(f, 0); new ClassReader(Files.readAllBytes(np)).accept(n, 0);
                for (MethodNode m : f.methods) {
                    if ((m.access & Opcodes.ACC_BRIDGE) == 0) continue;
                    seen++;
                    String nm = names.getOrDefault(m.name, m.name);
                    int arity = Type.getArgumentTypes(m.desc).length;
                    boolean ok = n.methods.stream().anyMatch(x -> (x.access & Opcodes.ACC_BRIDGE) != 0 && x.name.equals(nm) && Type.getArgumentTypes(x.desc).length == arity);
                    if (!ok) { bad++; System.out.println("NO BRIDGE " + f.name + "." + nm + " (forge " + m.name + m.desc + ")"); }
                }
            }
        }
        System.out.println(seen + " forge bridges, " + bad + " missing");
    }
}
