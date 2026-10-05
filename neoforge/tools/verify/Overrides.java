import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
/** Lists SRG-named method declarations (overrides of MC methods) in the Forge jar with their translated name, flagged if unverified. */
public class Overrides {
    public static void main(String[] a) throws Exception {
        Verify.load(a[0], Paths.get(a[1]));
        Map<String, String> names = new HashMap<>();
        for (String f : new String[]{"srg2moj.tsv", "manual.tsv"})
            for (String l : Files.readAllLines(Paths.get(a[2], f))) { String[] p = l.split("\t"); names.put(p[0], p[1]); }
        Set<String> out = new TreeSet<>();
        try (ZipFile z = new ZipFile(a[3])) {
            for (ZipEntry e : Collections.list(z.entries())) {
                if (!e.getName().endsWith(".class")) continue;
                ClassNode cn = new ClassNode(); new ClassReader(z.getInputStream(e).readAllBytes()).accept(cn, 0);
                for (MethodNode m : cn.methods) {
                    if (!m.name.matches("m_\\d+_")) continue;
                    String n = names.get(m.name);
                    boolean ok = n != null && (Verify.known.contains(n + m.desc));
                    out.add((ok ? "ok" : "CHECK") + "\t" + m.name + "\t" + n + "\t" + m.desc + "\t" + cn.name);
                }
            }
        }
        out.forEach(System.out::println);
    }
}
