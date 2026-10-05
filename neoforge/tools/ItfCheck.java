import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
/** Interface-vs-class and static-vs-instance use of external owners/members must agree with the original jars. */
public class ItfCheck {
    static void scan(ClassNode cn, Map<String, Set<String>> owners, Map<String, Set<String>> members) {
        for (MethodNode m : cn.methods) for (AbstractInsnNode in : m.instructions) {
            if (in instanceof MethodInsnNode mi && !mi.owner.startsWith("com/drunkard") && !mi.owner.startsWith("java/") && !mi.owner.startsWith("[")) {
                owners.computeIfAbsent(mi.owner, k -> new TreeSet<>()).add(mi.itf ? "itf" : "class");
                members.computeIfAbsent(mi.owner + "." + mi.name + mi.desc, k -> new TreeSet<>()).add(mi.getOpcode() == Opcodes.INVOKESTATIC ? "static" : "inst");
            } else if (in instanceof FieldInsnNode fi && !fi.owner.startsWith("com/drunkard") && !fi.owner.startsWith("java/")) {
                members.computeIfAbsent(fi.owner + "." + fi.name, k -> new TreeSet<>()).add(fi.getOpcode() == Opcodes.GETSTATIC || fi.getOpcode() == Opcodes.PUTSTATIC ? "static" : "inst");
            }
        }
    }
    static void jar(String j, Map<String, Set<String>> o, Map<String, Set<String>> m) throws Exception {
        try (ZipFile z = new ZipFile(j)) {
            for (ZipEntry e : Collections.list(z.entries())) if (e.getName().endsWith(".class")) {
                ClassNode cn = new ClassNode(); new ClassReader(z.getInputStream(e).readAllBytes()).accept(cn, 0); scan(cn, o, m);
            }
        }
    }
    public static void main(String[] a) throws Exception {
        Map<String, Set<String>> no = new HashMap<>(), nm = new HashMap<>(), fo = new HashMap<>(), fm = new HashMap<>(), wo = new TreeMap<>(), wm = new TreeMap<>();
        jar(a[0], no, nm); jar(a[1], fo, fm);
        try (var w = Files.walk(Paths.get(a[2]))) {
            for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".class"))::iterator) {
                ClassNode cn = new ClassNode(); new ClassReader(Files.readAllBytes(p)).accept(cn, 0); scan(cn, wo, wm);
            }
        }
        for (var e : wo.entrySet()) {
            Set<String> ref = no.containsKey(e.getKey()) ? no.get(e.getKey()) : fo.get(e.getKey());
            String src = no.containsKey(e.getKey()) ? "neo" : "forge";
            if (ref == null) System.out.println("NEW-OWNER\t" + e.getKey() + "\t" + e.getValue());
            else if (!ref.containsAll(e.getValue())) System.out.println("ITF-MISMATCH\t" + e.getKey() + "\t" + e.getValue() + " vs " + src + " " + ref);
        }
        for (var e : wm.entrySet()) {
            Set<String> ref = nm.containsKey(e.getKey()) ? nm.get(e.getKey()) : fm.get(e.getKey());
            if (ref != null && !ref.containsAll(e.getValue())) System.out.println("STATIC-MISMATCH\t" + e.getKey() + "\t" + e.getValue() + " vs " + ref);
        }
    }
}
