import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;

/** Bytecode patches to the original NeoForge game tests, which are not recompiled: expectations changed by the wort fix. */
public class NeoPatcher {
    public static void main(String[] a) throws Exception {
        Path in = Paths.get(a[0], "com/drunkard/test/DrunkardGameTests.class"), out = Paths.get(a[1], "com/drunkard/test/DrunkardGameTests.class");
        ClassNode cn = new ClassNode();
        new ClassReader(Files.readAllBytes(in)).accept(cn, 0);
        int kettle = 0, barrel = 0;
        for (MethodNode m : cn.methods) {
            if (m.name.equals("kettle_gui_inputs_make_wort_without_hand_interaction")) {
                for (AbstractInsnNode n : m.instructions) {
                    if (n instanceof MethodInsnNode mi && mi.name.equals("getCount") && mi.getNext() != null && mi.getNext().getOpcode() == Opcodes.ICONST_3) {
                        // The kettle no longer uses up bottles: the bottle slot keeps all 4.
                        m.instructions.set(mi.getNext(), new InsnNode(Opcodes.ICONST_4));
                        kettle++;
                    }
                }
            } else if (m.name.equals("all_beers_and_mead_ferment_with_metadata")) {
                int kind = -1;
                for (LocalVariableNode lv : m.localVariables) if (lv.name.equals("kind")) kind = lv.index;
                for (AbstractInsnNode n : m.instructions) {
                    if (n instanceof MethodInsnNode mi && mi.name.equals("getCount") && mi.getNext() != null && mi.getNext().getOpcode() == Opcodes.ICONST_5) {
                        // Beer from wort hands back no bottle (4 left); mead still returns its must bottle (5).
                        InsnList repl = new InsnList();
                        repl.add(new VarInsnNode(Opcodes.ALOAD, kind));
                        repl.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/drunkard/test/PubGameTests", "bottlesAfterStart", "(Ljava/lang/String;)I", false));
                        AbstractInsnNode five = mi.getNext();
                        m.instructions.insert(five, repl);
                        m.instructions.remove(five);
                        barrel++;
                    }
                }
                if (kind < 0) throw new IllegalStateException("no kind local");
            }
        }
        if (kettle != 1 || barrel != 1) throw new IllegalStateException("test patch mismatch: kettle=" + kettle + " barrel=" + barrel);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        Files.createDirectories(out.getParent());
        Files.write(out, cw.toByteArray());
        System.out.println("patched DrunkardGameTests (kettle " + kettle + ", barrel " + barrel + ")");
    }
}
