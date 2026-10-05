import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
/**
 * For every SRG-named method a Forge mod class declares (an override of a Minecraft method), looks up the same
 * member id in the 1.21.1 table and checks the ported class declares that 1.21.1 name + descriptor.
 */
public class OverrideCheck {
    public static void main(String[] a) throws Exception {
        Path map = Paths.get(a[0]);
        Map<String, String> names = new HashMap<>();
        for (String f : new String[]{"srg2moj.tsv", "manual.tsv"})
            for (String l : Files.readAllLines(map.resolve(f))) { String[] p = l.split("\t"); names.put(p[0], p[1]); }
        Map<String, String> fix = new HashMap<>(); // 1.21.1 renames of the same id
        if (a.length > 3) for (String l : Files.readAllLines(Paths.get(a[3]))) { String[] p = l.split("\t"); fix.put(p[0], p[1]); }
        Map<String, String> cmap = new HashMap<>();
        for (String l : Files.readAllLines(map.resolve("classmap.tsv"))) { String[] p = l.split("\t"); cmap.put(p[0], p[1]); }
        Map<String, String> obfToCls = new HashMap<>();
        Map<String, String> desc121 = new HashMap<>();
        String cls = null;
        for (String line : Files.readAllLines(map.resolve("tsrg121.tsrg"))) {
            if (line.startsWith("tsrg2")) continue;
            if (!line.startsWith("\t")) { String[] p = line.split(" "); obfToCls.put(p[0], p[1]); continue; }
            if (line.startsWith("\t\t")) continue;
            String[] p = line.substring(1).split(" ");
            if (p.length == 4) desc121.put(p[2], p[1]);
        }
        // 1.20.1 descriptors as class ids, to compare with 1.21.1 without any class-name map
        Map<String, String> o2c120 = new HashMap<>(), desc120 = new HashMap<>();
        List<String> l120 = Files.readAllLines(map.resolve("tsrg120.tsrg"));
        for (String line : l120) if (!line.startsWith("\t") && !line.startsWith("tsrg2")) { String[] p = line.split(" "); o2c120.put(p[0], p[1]); }
        for (String line : l120) {
            if (line.startsWith("tsrg2") || !line.startsWith("\t") || line.startsWith("\t\t")) continue;
            String[] p = line.substring(1).split(" ");
            if (p.length == 4) desc120.put(p[2], p[1]);
        }
        Map<String, ClassNode> out = new HashMap<>();
        try (var w = Files.walk(Paths.get(a[2]))) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".class"))::iterator) {
                ClassNode cn = new ClassNode(); new ClassReader(Files.readAllBytes(p)).accept(cn, 0); out.put(cn.name, cn);
            }
        }
        try (ZipFile z = new ZipFile(a[1])) {
            for (ZipEntry e : Collections.list(z.entries())) {
                if (!e.getName().endsWith(".class")) continue;
                ClassNode fc = new ClassNode(); new ClassReader(z.getInputStream(e).readAllBytes()).accept(fc, 0);
                ClassNode nc = out.get(fc.name);
                if (nc == null) continue;
                for (MethodNode m : fc.methods) {
                    if (!m.name.matches("m_\\d+_")) continue;
                    String n = fix.getOrDefault(m.name, names.get(m.name));
                    String od = desc121.get(m.name);
                    String o20 = desc120.get(m.name);
                    String d = od == null || o20 == null ? null : (toMoj(od, obfToCls, Map.of()) == null && ids(od, obfToCls).equals(ids(o20, o2c120)) ? m.desc : (ids(od, obfToCls).equals(ids(o20, o2c120)) ? m.desc : null));
                    String status;
                    if (od == null) status = "GONE-IN-1.21";
                    else if (d == null) status = nc.methods.stream().anyMatch(x -> x.name.equals(n)) ? "ok?(unmapped desc)" : "MISSING(unmapped desc)";
                    else status = nc.methods.stream().anyMatch(x -> x.name.equals(n) && x.desc.equals(d)) ? "ok" : "MISSING";
                    if (!status.equals("ok")) System.out.println(status + "\t" + fc.name.replace("com/drunkard/", "") + "\t" + m.name + "\t" + n + "\t" + (d == null ? od : d) + "\t(forge " + m.desc + ")");
                }
            }
        }
    }
    static String ids(String obf, Map<String, String> o2c) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < obf.length(); ) {
            char c = obf.charAt(i);
            if (c == 'L') { int e = obf.indexOf(';', i); String o = obf.substring(i + 1, e); sb.append('L').append(o2c.getOrDefault(o, o)).append(';'); i = e + 1; }
            else { sb.append(c); i++; }
        }
        return sb.toString();
    }

    static String toMoj(String obf, Map<String, String> obfToCls, Map<String, String> cmap) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < obf.length(); ) {
            char c = obf.charAt(i);
            if (c == 'L') {
                int e = obf.indexOf(';', i);
                String o = obf.substring(i + 1, e);
                String cid = obfToCls.get(o);
                String m = cid == null ? o : cmap.get(cid);
                if (m == null) return null;
                sb.append('L').append(m).append(';');
                i = e + 1;
            } else { sb.append(c); i++; }
        }
        return sb.toString();
    }
}
