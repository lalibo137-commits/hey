import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Emits stub entries (owner, Mojang name, 1.21.1 descriptor, static, field) for every SRG member the
 * Forge jar's given classes reference, using the MCPConfig 1.21.1 table for the descriptor and static flag.
 */
public class ForgeExtras {
    public static void main(String[] a) throws Exception {
        Path map = Paths.get(a[0]);
        Map<String, String> names = new HashMap<>();
        for (String f : new String[]{"srg2moj.tsv", "manual.tsv"})
            for (String l : Files.readAllLines(map.resolve(f))) { String[] p = l.split("\t"); names.put(p[0], p[1]); }
        Map<String, String> cmap = new HashMap<>();
        for (String l : Files.readAllLines(map.resolve("classmap.tsv"))) { String[] p = l.split("\t"); cmap.put(p[0], p[1]); }
        // tsrg 1.21.1
        Map<String, String> obfToCls = new HashMap<>();
        Map<String, String[]> mem = new HashMap<>(); // srg -> {cls, desc, static}
        String cls = null; String last = null;
        for (String line : Files.readAllLines(map.resolve("tsrg121.tsrg"))) {
            if (line.startsWith("tsrg2")) continue;
            if (!line.startsWith("\t")) { String[] p = line.split(" "); cls = p[1]; obfToCls.put(p[0], p[1]); continue; }
            if (line.startsWith("\t\t")) { if (line.trim().equals("static") && last != null) mem.get(last)[2] = "1"; continue; }
            String[] p = line.substring(1).split(" ");
            if (p.length == 3) { mem.put(p[1], new String[]{cls, null, "0"}); last = p[1]; }
            else { mem.put(p[2], new String[]{cls, p[1], "0"}); last = p[2]; }
        }
        // Mojang owner -> tsrg 1.21.1 class id, via any SRG member we know the owner of; class id -> member names
        Map<String, String> srgOwner = new HashMap<>();
        for (String l : Files.readAllLines(map.resolve("srg2moj.tsv"))) { String[] p = l.split("\t"); if (p.length > 3) srgOwner.put(p[0], p[3]); }
        Map<String, Set<String>> clsMembers = new HashMap<>();
        Map<String, String> plainDesc = new HashMap<>(); // cls.name -> obf desc for non-SRG named methods
        Map<String, String> ownerToCls = new HashMap<>();
        { String c2 = null;
          for (String line : Files.readAllLines(map.resolve("tsrg121.tsrg"))) {
            if (line.startsWith("tsrg2")) continue;
            if (!line.startsWith("\t")) { c2 = line.split(" ")[1]; continue; }
            if (line.startsWith("\t\t")) continue;
            String[] p = line.substring(1).split(" ");
            String nm = p.length == 3 ? p[1] : p[2];
            clsMembers.computeIfAbsent(c2, k -> new HashSet<>()).add(nm);
            if (p.length == 4) plainDesc.put(c2 + "." + nm + "." + p[1], p[1]);
            String o = srgOwner.get(nm);
            if (o != null && !o.contains("C_")) ownerToCls.putIfAbsent(o, c2);
          } }
        for (Map.Entry<String, String> e : cmap.entrySet()) ownerToCls.putIfAbsent(e.getValue(), e.getKey());
        // 1.21.1 field descriptors (obf) from Fabric intermediary: obfOwner.obfName -> desc
        Map<String, String> fdesc = new HashMap<>();
        for (String l : Files.readAllLines(map.resolve("int121.tiny"))) { String[] p = l.split("\t"); if (p[0].equals("FIELD")) fdesc.put(p[1] + "." + p[3], p[2]); }
        Map<String, String[]> srgObf = new HashMap<>(); // srg -> obf owner, obf name
        { String oc = null;
          for (String line : Files.readAllLines(map.resolve("tsrg121.tsrg"))) {
            if (line.startsWith("tsrg2")) continue;
            if (!line.startsWith("\t")) { oc = line.split(" ")[0]; continue; }
            if (line.startsWith("\t\t")) continue;
            String[] p = line.substring(1).split(" ");
            if (p.length == 3) srgObf.put(p[1], new String[]{oc, p[0]});
          } }
        // 1.20.1 field types as class ids: srg -> id desc
        Map<String, String> ids120 = new HashMap<>(), mids120 = new HashMap<>();
        { Map<String, String> fd120 = new HashMap<>();
          for (String l : Files.readAllLines(map.resolve("int120.tiny"))) { String[] p = l.split("\t"); if (p[0].equals("FIELD")) fd120.put(p[1] + "." + p[3], p[2]); }
          Map<String, String> o2c = new HashMap<>(); String oc = null;
          List<String> lines = Files.readAllLines(map.resolve("tsrg120.tsrg"));
          for (String line : lines) if (!line.startsWith("\t") && !line.startsWith("tsrg2")) { String[] p = line.split(" "); o2c.put(p[0], p[1]); }
          for (String line : lines) {
            if (line.startsWith("tsrg2")) continue;
            if (!line.startsWith("\t")) { oc = line.split(" ")[0]; continue; }
            if (line.startsWith("\t\t")) continue;
            String[] p = line.substring(1).split(" ");
            if (p.length == 3) { String fd = fd120.get(oc + "." + p[0]); if (fd != null) ids120.put(p[1], idDesc(fd, o2c)); }
            else mids120.put(p[2], idDesc(p[1], o2c));
          } }
        Set<String> want = new HashSet<>(Arrays.asList(a[2].split(",")));
        Map<String, ClassNode> forge = new HashMap<>();
        try (ZipFile z = new ZipFile(a[1])) {
            for (ZipEntry e : Collections.list(z.entries())) {
                if (!e.getName().endsWith(".class")) continue;
                ClassNode cn = new ClassNode(); new ClassReader(z.getInputStream(e).readAllBytes()).accept(cn, 0); forge.put(cn.name, cn);
            }
        }
        Set<String> out = new TreeSet<>();
        int missing = 0;
        for (ClassNode cn : forge.values()) {
            String top = cn.name.replaceAll("\\$.*", "").replace("com/drunkard/", "");
            if (!want.contains(top) && !want.contains("*")) continue;
            for (MethodNode m : cn.methods) for (AbstractInsnNode in : m.instructions) {
                String owner, name, desc; boolean field;
                if (in instanceof MethodInsnNode mi) { owner = mi.owner; name = mi.name; desc = mi.desc; field = false; }
                else if (in instanceof FieldInsnNode fi) { owner = fi.owner; name = fi.name; desc = fi.desc; field = true; }
                else continue;
                // walk mod hierarchy to first external class (stop if a mod class declares it)
                boolean declared = false;
                while (owner.startsWith("com/drunkard") && forge.containsKey(owner)) {
                    ClassNode oc = forge.get(owner);
                    final String fn = name, fd = desc;
                    if (field ? oc.fields.stream().anyMatch(f -> f.name.equals(fn)) : oc.methods.stream().anyMatch(x -> x.name.equals(fn) && x.desc.equals(fd))) { declared = true; break; }
                    owner = oc.superName;
                }
                if (declared || owner.startsWith("com/drunkard")) continue;
                if (!name.matches("[mf]_\\d+_")) {
                    if (!(owner.startsWith("net/minecraft/") || owner.startsWith("com/mojang/blaze3d") || owner.startsWith("com/mojang/math"))) continue;
                    if (name.equals("<init>")) continue;
                    String c = ownerToCls.get(owner);
                    if (c == null || !clsMembers.getOrDefault(c, Set.of()).contains(name)) { System.err.println("plain ref not verified: " + owner + " " + name + " " + desc); continue; }
                    boolean st = in.getOpcode() == Opcodes.GETSTATIC || in.getOpcode() == Opcodes.PUTSTATIC || in.getOpcode() == Opcodes.INVOKESTATIC;
                    out.add(owner + "\t" + name + "\t" + desc + "\t" + (st ? 1 : 0) + "\t" + (field ? 1 : 0));
                    continue;
                }
                String moj = names.get(name);
                String[] t = mem.get(name);
                if (moj == null || t == null) { missing++; System.err.println("no 1.21.1 member for " + name + " " + names.get(name) + " " + owner + " " + desc); continue; }
                String d;
                if (field) {
                    String[] o = srgObf.get(name);
                    String fd = o == null ? null : fdesc.get(o[0] + "." + o[1]);
                    d = null;
                    if (fd != null) {
                        // class not in the Mojang class map: accept the 1.20.1 type if both versions use the same class id
                        String id121 = idDesc(fd, obfToCls);
                        String id120 = ids120.get(name);
                        if (id121 != null && id121.equals(id120)) d = desc;
                        else System.err.println("field type unverifiable " + owner + " " + moj + " " + desc + " " + id120 + " vs " + id121);
                    }
                    if (d != null && !d.equals(desc)) System.err.println("field type changed " + owner + " " + moj + " " + desc + " -> " + d);
                } else {
                    // Method descriptor: the Forge call site's own descriptor (real Mojang class names), accepted only if
                    // the member's parameter/return class ids are identical in 1.20.1 and 1.21.1 (no class map involved).
                    String id120 = mids120.get(name), id121 = idDesc(t[1], obfToCls);
                    if (id120 != null && id120.equals(id121)) d = desc;
                    else { System.err.println("METHOD TYPE CHANGED " + owner + " " + moj + " forge " + desc + " ids " + id120 + " -> " + id121); continue; }
                }
                if (d == null) { System.err.println("unmapped class in desc " + name + " " + moj + " " + t[1]); continue; }
                boolean isStatic = field ? in.getOpcode() == Opcodes.GETSTATIC || in.getOpcode() == Opcodes.PUTSTATIC : t[2].equals("1");
                out.add(owner + "\t" + moj + "\t" + d + "\t" + (isStatic ? 1 : 0) + "\t" + (field ? 1 : 0));
            }
        }
        Files.write(Paths.get(a[3]), out);
        System.err.println(out.size() + " entries, " + missing + " refs with no 1.21.1 member");
    }

    // Field descriptors: tsrg2 has none for fields, keep the 1.20.1 type (class names are Mojang already).
    static String fieldDesc(String d) { return d; }

    static String idDesc(String obf, Map<String, String> o2c) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < obf.length(); ) {
            char c = obf.charAt(i);
            if (c == 'L') { int e = obf.indexOf(';', i); String o = obf.substring(i + 1, e); sb.append('L').append(o2c.getOrDefault(o, o)).append(';'); i = e + 1; }
            else { sb.append(c); i++; }
        }
        return sb.toString();
    }

    static String toMoj(String obf, Map<String, String> obfToCls, Map<String, String> cmap) {
        if (obf == null) return null;
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
