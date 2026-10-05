import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
/**
 * Bytecode comparison of the Forge 1.2.0 classes against the NeoForge-compiled ports: per method (matched by translated
 * name + parameter count), the multiset of calls/field accesses/constants/arithmetic. Prints every method that differs.
 */
public class PortAudit {
    static Map<String, String> names = new HashMap<>();
    static String nm(String n) { return n.startsWith("lambda$") ? "lambda" : names.getOrDefault(n, n); }
    static List<String> fp(MethodNode m) {
        List<String> f = new ArrayList<>();
        int br = 0;
        for (AbstractInsnNode in : m.instructions) {
            int op = in.getOpcode();
            if (in instanceof MethodInsnNode mi) f.add("call " + (mi.owner.startsWith("com/drunkard") ? mi.owner + "." + nm(mi.name) + mi.desc : nm(mi.name)));
            else if (in instanceof FieldInsnNode fi) f.add("fld " + op + " " + nm(fi.name));
            else if (in instanceof TypeInsnNode ti) f.add("type " + op + " " + (ti.desc.startsWith("com/drunkard") ? ti.desc : "ext"));
            else if (in instanceof LdcInsnNode l) f.add("ldc " + (l.cst instanceof Type ? "T" : l.cst));
            else if (in instanceof IntInsnNode ii) f.add("int " + ii.operand);
            else if (in instanceof IincInsnNode ic) f.add("iinc " + ic.incr);
            else if (in instanceof InvokeDynamicInsnNode di) f.add("indy " + di.name);
            else if (in instanceof TableSwitchInsnNode ts) f.add("switch " + ts.min + ".." + ts.max);
            else if (in instanceof LookupSwitchInsnNode ls) f.add("lswitch " + ls.keys);
            else if (op >= Opcodes.ICONST_M1 && op <= Opcodes.DCONST_1) f.add("const " + op);
            else if (in instanceof JumpInsnNode && op != Opcodes.GOTO) f.add("jump " + op);
            else if (op >= Opcodes.IADD && op <= Opcodes.DCMPG) f.add("arith " + op);
        }
        f.add("branches " + br);
        Collections.sort(f);
        return f;
    }
    public static void main(String[] a) throws Exception {
        for (String fn : new String[]{"srg2moj.tsv", "manual.tsv"})
            for (String l : Files.readAllLines(Paths.get(a[2], fn))) { String[] p = l.split("\t"); names.put(p[0], p[1]); }
        Set<String> only = a.length > 3 ? new HashSet<>(Arrays.asList(a[3].split(","))) : null;
        int diff = 0, total = 0;
        try (ZipFile z = new ZipFile(a[0])) {
            for (ZipEntry e : Collections.list(z.entries())) {
                if (!e.getName().endsWith(".class")) continue;
                Path np = Paths.get(a[1], e.getName());
                if (!Files.exists(np)) continue;
                String top = e.getName().replaceAll("\\$.*", "").replaceAll(".*/", "").replace(".class", "");
                if (only != null && !only.contains(top)) continue;
                ClassNode f = new ClassNode(), n = new ClassNode();
                new ClassReader(z.getInputStream(e).readAllBytes()).accept(f, 0); new ClassReader(Files.readAllBytes(np)).accept(n, 0);
                Map<String, List<MethodNode>> nm = new HashMap<>();
                for (MethodNode m : n.methods) if (!m.name.startsWith("lambda$") && (m.access & Opcodes.ACC_BRIDGE) == 0) nm.computeIfAbsent(m.name + "/" + Type.getArgumentTypes(m.desc).length, k -> new ArrayList<>()).add(m);
                // lambdas: compare the sorted multiset of all lambda fingerprints per class
                List<String> fl = new ArrayList<>(), nl = new ArrayList<>();
                for (MethodNode m : f.methods) if (m.name.startsWith("lambda$")) fl.add(fp(m).toString());
                for (MethodNode m : n.methods) if (m.name.startsWith("lambda$")) nl.add(fp(m).toString());
                Collections.sort(fl); Collections.sort(nl);
                if (!fl.equals(nl)) { diff++; System.out.println("LAMBDAS " + f.name + " (" + fl.size() + " vs " + nl.size() + ")"); }
                for (MethodNode m : f.methods) {
                    if (m.name.startsWith("lambda$") || (m.access & Opcodes.ACC_BRIDGE) != 0) continue;
                    total++;
                    String key = nm(m.name) + "/" + Type.getArgumentTypes(m.desc).length;
                    List<MethodNode> c = nm.get(key);
                    if (c == null) { diff++; System.out.println("NO MATCH " + f.name + "." + key); continue; }
                    List<String> ff = fp(m);
                    if (c.stream().noneMatch(x -> fp(x).equals(ff))) {
                        diff++;
                        List<String> nf = fp(c.get(0));
                        List<String> minus = new ArrayList<>(ff), plus = new ArrayList<>(nf);
                        for (String s : nf) minus.remove(s);
                        for (String s : ff) plus.remove(s);
                        System.out.println("DIFF " + f.name + "." + key + "  -" + minus + "  +" + plus);
                    }
                }
            }
        }
        System.out.println(total + " methods, " + diff + " differ");
    }
}
