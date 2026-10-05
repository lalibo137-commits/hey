import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
/** Known-good 1.21.1 members: NeoForge jar refs + Parchment p121 (name+desc). Checks a TSV of owner,name,desc,static,field. */
public class Verify {
    public static Set<String> known = new HashSet<>(), knownNames = new HashSet<>();
    public static void load(String neoJar, Path parchment) throws Exception {
        try (ZipFile z = new ZipFile(neoJar)) {
            for (ZipEntry e : Collections.list(z.entries())) {
                if (!e.getName().endsWith(".class")) continue;
                ClassNode cn = new ClassNode(); new ClassReader(z.getInputStream(e).readAllBytes()).accept(cn, 0);
                for (MethodNode m : cn.methods) {
                    if (cn.superName != null) { known.add(m.name + m.desc); } // overrides of MC methods declared in the mod
                    for (AbstractInsnNode in : m.instructions) {
                        if (in instanceof MethodInsnNode mi) add(mi.owner, mi.name, mi.desc);
                        else if (in instanceof FieldInsnNode fi) add(fi.owner, fi.name, fi.desc);
                    }
                }
            }
        }
        try (var w = Files.walk(parchment)) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".mapping"))::iterator) {
                String owner = null;
                for (String l : Files.readAllLines(p)) {
                    String t = l.strip(); String[] s = t.split(" ");
                    int depth = l.length() - l.replaceAll("^\t+", "").length();
                    if (s[0].equals("CLASS") && depth == 0) owner = s[1];
                    else if (s[0].equals("CLASS")) owner = owner.replaceAll("\\$.*", "") + "$" + s[1].replaceAll(".*\\$", "");
                    else if ((s[0].equals("METHOD") || s[0].equals("FIELD")) && s.length >= 3) add(owner, s[1], s[2]);
                }
            }
        }
    }
    static void add(String o, String n, String d) { known.add(o + "." + n + d); known.add(n + d); knownNames.add(o + "." + n); }
    public static void main(String[] a) throws Exception {
        load(a[0], Paths.get(a[1]));
        for (String l : Files.readAllLines(Paths.get(a[2]))) {
            String[] p = l.split("\t");
            String k = p[0] + "." + p[1] + p[2];
            if (known.contains(k)) continue;
            String how = known.contains(p[1] + p[2]) ? "name+desc on other owner" : knownNames.contains(p[0] + "." + p[1]) ? "name ok, desc differs" : "UNVERIFIED";
            System.out.println(how + "\t" + l);
        }
    }
}
