import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;

/** Small in-place bytecode patches for classes that are not recompiled from source. */
public class Patcher {
    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0]), out = Paths.get(a[1]);
        jei(in.resolve("com/drunkard/compat/DrunkardJei.class"), out.resolve("com/drunkard/compat/DrunkardJei.class"));
        test(in.resolve("com/drunkard/test/DrunkardGameTests.class"), out.resolve("com/drunkard/test/DrunkardGameTests.class"));
    }

    static ClassNode read(Path p) throws Exception {
        ClassNode cn = new ClassNode();
        new ClassReader(Files.readAllBytes(p)).accept(cn, 0);
        return cn;
    }

    static void write(ClassNode cn, Path p) throws Exception {
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        Files.createDirectories(p.getParent());
        Files.write(p, cw.toByteArray());
    }

    static void jei(Path src, Path dst) throws Exception {
        ClassNode cn = read(src);
        String o = "Ljava/lang/Object;";
        int five = 0, two = 0;
        for (MethodNode m : cn.methods) {
            if (!m.name.equals("registerRecipes")) continue;
            for (AbstractInsnNode n : m.instructions) {
                if (n instanceof MethodInsnNode mi && mi.owner.equals("java/util/List") && mi.name.equals("of")) {
                    if (mi.desc.equals("(" + o.repeat(5) + ")Ljava/util/List;")) {
                        m.instructions.set(mi, new MethodInsnNode(Opcodes.INVOKESTATIC, "com/drunkard/compat/JeiWortRecipes", "kettleInputs", mi.desc, false));
                        five++;
                    } else if (mi.desc.equals("(" + o.repeat(2) + ")Ljava/util/List;")) {
                        m.instructions.set(mi, new MethodInsnNode(Opcodes.INVOKESTATIC, "com/drunkard/compat/JeiWortRecipes", "pair", mi.desc, false));
                        two++;
                    }
                }
            }
        }
        if (five != 1 || two == 0) throw new IllegalStateException("JEI patch mismatch: five=" + five + " two=" + two);
        System.out.println("JEI: kettle inputs " + five + ", pairs " + two);
        write(cn, dst);
    }

    static void test(Path src, Path dst) throws Exception {
        ClassNode cn = read(src);
        int hits = 0;
        for (MethodNode m : cn.methods) {
            if (!m.name.equals("kettle_gui_inputs_make_wort_without_hand_interaction")) continue;
            for (AbstractInsnNode n : m.instructions) {
                if (n instanceof MethodInsnNode mi && mi.name.equals("m_41613_")) {
                    AbstractInsnNode next = mi.getNext();
                    if (next != null && next.getOpcode() == Opcodes.ICONST_3) {
                        // Bottles are no longer used up: the bottle slot keeps all 4.
                        m.instructions.set(next, new InsnNode(Opcodes.ICONST_4));
                        hits++;
                    }
                }
            }
        }
        if (hits != 1) throw new IllegalStateException("test patch mismatch: " + hits);
        System.out.println("test: patched " + hits);
        write(cn, dst);
    }
}
