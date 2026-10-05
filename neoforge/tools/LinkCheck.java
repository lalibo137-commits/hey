import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.*;
import java.util.zip.*;

/** Every reference to a com/drunkard class or member in the jar must resolve within the jar. */
public class LinkCheck {
    static boolean resolves(Map<String, ClassNode> all, String c, String name, String desc, boolean field) {
        while (c != null && all.containsKey(c)) {
            ClassNode k = all.get(c);
            if (field) { for (FieldNode f : k.fields) if (f.name.equals(name) && f.desc.equals(desc)) return true; }
            else { for (MethodNode mm : k.methods) if (mm.name.equals(name) && mm.desc.equals(desc)) return true; }
            for (String i : k.interfaces) if (resolves(all, i, name, desc, field)) return true;
            c = k.superName;
        }
        return false;
    }

    public static void main(String[] a) throws Exception {
        Map<String, ClassNode> all = new HashMap<>();
        try (ZipFile z = new ZipFile(a[0])) {
            for (ZipEntry e : Collections.list(z.entries())) if (e.getName().endsWith(".class")) {
                ClassNode cn = new ClassNode(); new ClassReader(z.getInputStream(e).readAllBytes()).accept(cn, 0); all.put(cn.name, cn);
            }
        }
        int bad = 0;
        for (ClassNode cn : all.values()) for (MethodNode m : cn.methods) for (AbstractInsnNode in : m.instructions) {
            String owner = null, name = null, desc = null; boolean field = false;
            if (in instanceof MethodInsnNode mi) { owner = mi.owner; name = mi.name; desc = mi.desc; }
            else if (in instanceof FieldInsnNode fi) { owner = fi.owner; name = fi.name; desc = fi.desc; field = true; }
            else if (in instanceof TypeInsnNode ti) { owner = ti.desc; }
            else if (in instanceof InvokeDynamicInsnNode indy) {
                for (Object arg : indy.bsmArgs) if (arg instanceof Handle h && h.getOwner().startsWith("com/drunkard")) {
                    boolean hf = h.getTag() <= Opcodes.H_PUTSTATIC;
                    if (!resolves(all, h.getOwner(), h.getName(), h.getDesc(), hf)) { System.out.println("UNRESOLVED handle " + h.getOwner() + "." + h.getName() + h.getDesc() + " from " + cn.name); bad++; }
                }
                continue;
            }
            if (owner == null || !owner.startsWith("com/drunkard")) continue;
            if (!all.containsKey(owner)) { System.out.println("MISSING CLASS " + owner + " from " + cn.name); bad++; continue; }
            if (name == null) continue;
            boolean found = false; String c = owner;
            while (c != null && all.containsKey(c) && !found) {
                ClassNode k = all.get(c);
                if (field) { for (FieldNode f : k.fields) if (f.name.equals(name) && f.desc.equals(desc)) found = true; }
                else { for (MethodNode mm : k.methods) if (mm.name.equals(name) && mm.desc.equals(desc)) found = true; }
                c = k.superName;
            }
            if (!found && c != null && !c.startsWith("com/drunkard")) found = true; // inherited from Minecraft
            if (!found) { System.out.println("MISSING " + owner + "." + name + desc + " from " + cn.name); bad++; }
        }
        System.out.println(all.size() + " classes, " + bad + " problems");
    }
}
