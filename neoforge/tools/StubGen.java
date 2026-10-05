import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Generates compile-only Java source stubs for every external (Minecraft / Forge / library)
 * class and member referenced by a mod jar. A small overrides file supplies the type
 * hierarchy and generic signatures that bytecode references alone do not reveal.
 *
 * Usage: StubGen <mod.jar> <outDir> <overrides.txt>
 */
public class StubGen {
    static final class Member {
        String name, desc;
        boolean isStatic, isField, isAbstract;
        int access = Opcodes.ACC_PUBLIC;
    }

    static final class Cls {
        String name;
        boolean iface, annotation, isEnum, runtime;
        final Map<String, Member> members = new LinkedHashMap<>();
        String typeParams = "";
        String superDecl;
        final List<String> ifaces = new ArrayList<>();
        final List<String> extra = new ArrayList<>();
    }

    static final Map<String, Cls> classes = new TreeMap<>();
    static final Map<String, Integer> modDeclAccess = new HashMap<>();
    static final Set<String> modClasses = new HashSet<>();
    static final Map<String, String> memberOverride = new HashMap<>();
    static final Set<String> skipMember = new HashSet<>();

    static boolean external(String internal) {
        return internal != null && !internal.startsWith("[") && !modClasses.contains(internal)
                && !internal.startsWith("java/") && !internal.startsWith("javax/") && !internal.startsWith("jdk/");
    }

    static Cls cls(String internal) {
        Cls c = classes.computeIfAbsent(internal, k -> {
            Cls n = new Cls();
            n.name = k;
            return n;
        });
        int d = internal.lastIndexOf('$');
        if (d > 0) cls(internal.substring(0, d));
        return c;
    }

    static void type(Type t) {
        while (t.getSort() == Type.ARRAY) t = t.getElementType();
        if (t.getSort() == Type.OBJECT && external(t.getInternalName())) cls(t.getInternalName());
        if (t.getSort() == Type.METHOD) {
            for (Type a : t.getArgumentTypes()) type(a);
            type(t.getReturnType());
        }
    }

    static void desc(String d) {
        if (d != null) type(Type.getType(d));
    }

    static void sig(String s) {
        if (s == null) return;
        new org.objectweb.asm.signature.SignatureReader(s).accept(new org.objectweb.asm.signature.SignatureVisitor(Opcodes.ASM9) {
            @Override
            public void visitClassType(String name) {
                if (external(name)) cls(name);
            }
        });
    }

    static final Map<String, ClassNode> modNodes = new HashMap<>();

    static void member(String owner, String name, String desc, boolean isStatic, boolean field, boolean itf) {
        while (owner != null && modNodes.containsKey(owner)) {
            ClassNode cn = modNodes.get(owner);
            boolean declared = false;
            if (field) { for (FieldNode f : cn.fields) if (f.name.equals(name) && f.desc.equals(desc)) declared = true; }
            else { for (MethodNode m : cn.methods) if (m.name.equals(name) && m.desc.equals(desc)) declared = true; }
            if (declared || name.equals("<init>")) return;
            owner = cn.superName;
        }
        if (!external(owner)) return;
        Cls c = cls(owner);
        if (itf) c.iface = true;
        String key = name + desc;
        Member m = c.members.get(key);
        if (m == null) {
            m = new Member();
            m.name = name;
            m.desc = desc;
            m.isStatic = isStatic;
            m.isField = field;
            c.members.put(key, m);
        }
        desc(desc);
    }

    public static void main(String[] args) throws Exception {
        List<ClassNode> nodes = new ArrayList<>();
        try (ZipFile zip = new ZipFile(args[0])) {
            for (ZipEntry e : Collections.list(zip.entries())) {
                if (!e.getName().endsWith(".class")) continue;
                ClassNode cn = new ClassNode();
                new ClassReader(zip.getInputStream(e).readAllBytes()).accept(cn, 0);
                nodes.add(cn);
                modClasses.add(cn.name);
                modNodes.put(cn.name, cn);
            }
        }
        for (ClassNode cn : nodes) {
            for (MethodNode mn : cn.methods) modDeclAccess.merge(mn.name + mn.desc, mn.access, (a, b) -> (a & Opcodes.ACC_PROTECTED) != 0 ? a : b);
        }
        for (ClassNode cn : nodes) {
            if (external(cn.superName)) cls(cn.superName);
            for (String i : cn.interfaces) if (external(i)) cls(i).iface = true;
            sig(cn.signature);
            annotations(cn.visibleAnnotations, true);
            annotations(cn.invisibleAnnotations, false);
            for (FieldNode f : cn.fields) {
                desc(f.desc);
                sig(f.signature);
                annotations(f.visibleAnnotations, true);
                annotations(f.invisibleAnnotations, false);
            }
            for (MethodNode mn : cn.methods) {
                desc(mn.desc);
                sig(mn.signature);
                annotations(mn.visibleAnnotations, true);
                annotations(mn.invisibleAnnotations, false);
                if (mn.invisibleParameterAnnotations != null) for (List<AnnotationNode> l : mn.invisibleParameterAnnotations) annotations(l, false);
                if (mn.visibleParameterAnnotations != null) for (List<AnnotationNode> l : mn.visibleParameterAnnotations) annotations(l, true);
                if (mn.localVariables != null) for (LocalVariableNode lv : mn.localVariables) { desc(lv.desc); sig(lv.signature); }
                if (mn.tryCatchBlocks != null) for (TryCatchBlockNode t : mn.tryCatchBlocks) if (external(t.type)) cls(t.type);
                for (AbstractInsnNode in : mn.instructions) {
                    if (in instanceof MethodInsnNode m) {
                        member(m.owner, m.name, m.desc, m.getOpcode() == Opcodes.INVOKESTATIC, false, m.itf);
                    } else if (in instanceof FieldInsnNode f) {
                        member(f.owner, f.name, f.desc, f.getOpcode() == Opcodes.GETSTATIC || f.getOpcode() == Opcodes.PUTSTATIC, true, false);
                    } else if (in instanceof TypeInsnNode t) {
                        type(Type.getObjectType(t.desc));
                    } else if (in instanceof LdcInsnNode l && l.cst instanceof Type t) {
                        type(t);
                    } else if (in instanceof MultiANewArrayInsnNode a) {
                        desc(a.desc);
                    } else if (in instanceof InvokeDynamicInsnNode indy) {
                        desc(indy.desc);
                        if (indy.bsm.getName().equals("metafactory") && indy.bsmArgs.length > 0 && indy.bsmArgs[0] instanceof Type sam) {
                            String itf = Type.getMethodType(indy.desc).getReturnType().getInternalName();
                            if (external(itf)) {
                                member(itf, indy.name, sam.getDescriptor(), false, false, true);
                                Member am = cls(itf).members.get(indy.name + sam.getDescriptor());
                                if (am != null) am.isAbstract = true;
                            }
                        }
                        for (Object o : indy.bsmArgs) {
                            if (o instanceof Handle h) member(h.getOwner(), h.getName(), h.getDesc(), h.getTag() == Opcodes.H_INVOKESTATIC, h.getTag() <= 4, h.isInterface());
                            else if (o instanceof Type t) type(t);
                        }
                    } else if (in instanceof FrameNode fr) {
                        if (fr.local != null) for (Object o : fr.local) if (o instanceof String s && external(s)) cls(s);
                        if (fr.stack != null) for (Object o : fr.stack) if (o instanceof String s && external(s)) cls(s);
                    }
                }
            }
        }
        if (args.length > 3) {
            for (String l : Files.readAllLines(Paths.get(args[3]))) {
                if (l.isBlank() || l.startsWith("#")) continue;
                String[] p = l.split("\t");
                boolean isField = p[4].equals("1");
                Cls ex = classes.get(p[0]);
                if (ex != null) {
                    boolean clash = false;
                    for (Member m : ex.members.values()) {
                        if (!m.name.equals(p[1]) || m.isField != isField) continue;
                        if (isField || m.desc.substring(0, m.desc.indexOf(')')).equals(p[2].substring(0, p[2].indexOf(')')))) clash = true;
                    }
                    if (clash) continue; // the NeoForge jar's own reference wins
                }
                member(p[0], p[1], p[2], p[3].equals("1"), isField, false);
                if (!isField) {
                    for (Type t : Type.getArgumentTypes(p[2])) type(t);
                    type(Type.getReturnType(p[2]));
                } else type(Type.getType(p[2]));
            }
        }
        overrides(Paths.get(args[2]));
        Path out = Paths.get(args[1]);
        for (Cls c : classes.values()) {
            if (c.name.contains("$")) continue;
            Path p = out.resolve(c.name + ".java");
            Files.createDirectories(p.getParent());
            StringBuilder sb = new StringBuilder();
            int slash = c.name.lastIndexOf('/');
            if (slash > 0) sb.append("package ").append(c.name.substring(0, slash).replace('/', '.')).append(";\n\n");
            sb.append("@SuppressWarnings({\"unchecked\", \"rawtypes\"})\n");
            write(sb, c, "", false);
            Files.writeString(p, sb.toString());
        }
    }

    static void annotations(List<AnnotationNode> list, boolean visible) {
        if (list == null) return;
        for (AnnotationNode a : list) {
            Type t = Type.getType(a.desc);
            if (!external(t.getInternalName())) continue;
            Cls c = cls(t.getInternalName());
            c.annotation = true;
            // Keep the original retention: Forge reads @SubscribeEvent and friends by reflection at runtime.
            c.runtime |= visible;
            if (a.values != null) {
                for (int i = 0; i < a.values.size(); i += 2) {
                    String name = (String) a.values.get(i);
                    Object v = a.values.get(i + 1);
                    String rt;
                    if (v instanceof String) rt = "Ljava/lang/String;";
                    else if (v instanceof Integer) rt = "I";
                    else if (v instanceof Boolean) rt = "Z";
                    else if (v instanceof Long) rt = "J";
                    else if (v instanceof Type) rt = "Ljava/lang/Class;";
                    else if (v instanceof String[] en) { rt = en[0]; cls(Type.getType(en[0]).getInternalName()).isEnum = true; member(Type.getType(en[0]).getInternalName(), en[1], en[0], true, true, false); }
                    else if (v instanceof List<?> l) {
                        Object f = l.isEmpty() ? "" : l.get(0);
                        if (f instanceof String[] en) { rt = "[" + en[0]; cls(Type.getType(en[0]).getInternalName()).isEnum = true; for (Object o : l) member(Type.getType(en[0]).getInternalName(), ((String[]) o)[1], en[0], true, true, false); }
                        else if (f instanceof Type) rt = "[Ljava/lang/Class;";
                        else rt = "[Ljava/lang/String;";
                    } else rt = "Ljava/lang/String;";
                    Member m = new Member();
                    m.name = name;
                    m.desc = "()" + rt;
                    c.members.put(name + m.desc, m);
                }
            }
        }
    }

    static void overrides(Path file) throws IOException {
        if (!Files.exists(file)) return;
        Cls cur = null;
        for (String raw : Files.readAllLines(file)) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.startsWith("class ")) {
                // class <internal> [<typeParams>] [enum|interface] [extends <decl>] [implements <decl>;<decl>]
                String rest = line.substring(6).strip();
                String name = rest.split("[\\s<]+")[0];
                cur = cls(name);
                rest = rest.substring(name.length()).strip();
                if (rest.startsWith("<")) {
                    int depth = 0, i = 0;
                    for (; i < rest.length(); i++) {
                        if (rest.charAt(i) == '<') depth++;
                        if (rest.charAt(i) == '>' && --depth == 0) break;
                    }
                    cur.typeParams = rest.substring(0, i + 1);
                    rest = rest.substring(i + 1).strip();
                }
                if (rest.startsWith("enum")) { cur.isEnum = true; rest = rest.substring(4).strip(); }
                if (rest.startsWith("interface")) { cur.iface = true; rest = rest.substring(9).strip(); }
                if (rest.startsWith("extends ")) {
                    int imp = rest.indexOf(" implements ");
                    cur.superDecl = (imp < 0 ? rest.substring(8) : rest.substring(8, imp)).strip();
                    rest = imp < 0 ? "" : rest.substring(imp + 1);
                }
                if (rest.startsWith("implements ")) {
                    cur.ifaces.clear();
                    for (String s : rest.substring(11).split(";")) cur.ifaces.add(s.strip());
                }
            } else if (line.startsWith("member ")) {
                // member <internal>.<name><desc> = <java declaration>
                String rest = line.substring(7);
                int eq = rest.indexOf(" = ");
                memberOverride.put(rest.substring(0, eq).strip(), rest.substring(eq + 3).strip());
            } else if (line.startsWith("skip ")) {
                skipMember.add(line.substring(5).strip());
            } else if (line.startsWith("extra ") && cur != null) {
                cur.extra.add(line.substring(6));
            }
        }
    }

    static String simple(String internal) {
        return internal.substring(Math.max(internal.lastIndexOf('/'), internal.lastIndexOf('$')) + 1);
    }

    static String java(Type t) {
        return switch (t.getSort()) {
            case Type.VOID -> "void";
            case Type.BOOLEAN -> "boolean";
            case Type.CHAR -> "char";
            case Type.BYTE -> "byte";
            case Type.SHORT -> "short";
            case Type.INT -> "int";
            case Type.FLOAT -> "float";
            case Type.LONG -> "long";
            case Type.DOUBLE -> "double";
            case Type.ARRAY -> java(t.getElementType()) + "[]".repeat(t.getDimensions());
            default -> t.getClassName().replace('$', '.');
        };
    }

    static String dflt(Type t) {
        return switch (t.getSort()) {
            case Type.VOID -> "";
            case Type.BOOLEAN -> "false";
            case Type.OBJECT, Type.ARRAY -> "null";
            default -> "0";
        };
    }

    static void write(StringBuilder sb, Cls c, String ind, boolean nested) {
        String kind = c.annotation ? "@interface" : c.isEnum ? "enum" : c.iface ? "interface" : "class";
        if (c.annotation && c.runtime) sb.append(ind).append("@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)\n");
        sb.append(ind).append("public ").append(nested && !c.iface && !c.annotation && !c.isEnum ? "static " : "").append(kind).append(' ').append(simple(c.name)).append(c.typeParams);
        if (c.superDecl != null && !c.isEnum && !c.iface && !c.annotation) sb.append(" extends ").append(c.superDecl);
        List<String> ifs = new ArrayList<>(c.ifaces);
        if (c.iface && c.superDecl != null) ifs.add(0, c.superDecl);
        if (!ifs.isEmpty()) sb.append(c.iface ? " extends " : " implements ").append(String.join(", ", ifs));
        sb.append(" {\n");
        String in = ind + "    ";
        if (c.isEnum) {
            List<String> consts = new ArrayList<>();
            for (Member m : c.members.values())
                if (m.isField && m.isStatic && Type.getType(m.desc).getSort() == Type.OBJECT && Type.getType(m.desc).getInternalName().equals(c.name)) consts.add(m.name);
            sb.append(in).append(String.join(", ", consts)).append(";\n");
        }
        Set<String> ctorDescs = new HashSet<>();
        for (Member m : c.members.values()) {
            String key = c.name + "." + m.name + m.desc;
            if (skipMember.contains(key)) continue;
            if (!m.isField && jdkHas(c, m)) continue;
            if (c.isEnum && m.isField && m.isStatic && m.desc.equals("L" + c.name + ";")) continue;
            if (c.isEnum && (m.name.equals("values") || m.name.equals("valueOf") || m.name.equals("ordinal") || m.name.equals("name"))) continue;
            if (memberOverride.containsKey(key)) {
                sb.append(in).append(memberOverride.get(key)).append('\n');
                if (m.name.equals("<init>")) ctorDescs.add(m.desc);
                continue;
            }
            if (m.isField) {
                Type t = Type.getType(m.desc);
                sb.append(in).append("public ").append(m.isStatic ? "static " : "").append(java(t)).append(' ').append(m.name);
                if (c.iface || c.annotation) sb.append(" = ").append(dflt(t).isEmpty() ? "null" : dflt(t));
                sb.append(";\n");
                continue;
            }
            Type mt = Type.getMethodType(m.desc);
            StringBuilder params = new StringBuilder();
            Type[] a = mt.getArgumentTypes();
            for (int i = 0; i < a.length; i++) params.append(i > 0 ? ", " : "").append(java(a[i])).append(" a").append(i);
            if (m.name.equals("<clinit>")) continue;
            if (m.name.equals("<init>")) {
                if (c.iface || c.annotation) continue;
                ctorDescs.add(m.desc);
                sb.append(in).append("public ").append(simple(c.name)).append('(').append(params).append(") {").append(superCall(c)).append("}\n");
                continue;
            }
            if (c.annotation) {
                sb.append(in).append(java(mt.getReturnType())).append(' ').append(m.name).append("()");
                Type r = mt.getReturnType();
                if (r.getSort() == Type.ARRAY) sb.append(" default {}");
                else if (r.getSort() == Type.OBJECT && r.getInternalName().equals("java/lang/String")) sb.append(" default \"\"");
                else if (r.getSort() == Type.OBJECT && r.getInternalName().equals("java/lang/Class")) sb.append(" default Object.class");
                else if (r.getSort() == Type.OBJECT) sb.append(" default ").append(java(r)).append('.').append(firstConst(r.getInternalName()));
                else if (r.getSort() == Type.BOOLEAN) sb.append(" default false");
                else sb.append(" default 0");
                sb.append(";\n");
                continue;
            }
            Integer modAccess = m.isStatic ? null : modDeclAccess.get(m.name + m.desc);
            String vis = c.iface ? "" : (modAccess != null && (modAccess & Opcodes.ACC_PROTECTED) != 0 ? "protected " : "public ");
            String body = dflt(mt.getReturnType());
            if (c.iface && m.isAbstract) {
                sb.append(in).append(java(mt.getReturnType())).append(' ').append(m.name).append('(').append(params).append(");\n");
                continue;
            }
            sb.append(in).append(vis).append(c.iface && !m.isStatic ? "default " : "").append(m.isStatic ? "static " : "")
                    .append(java(mt.getReturnType())).append(' ').append(m.name).append('(').append(params).append(") { ")
                    .append(body.isEmpty() ? "" : "return " + body + "; ").append("}\n");
        }
        if (!c.iface && !c.annotation && !c.isEnum && !ctorDescs.contains("()V")) {
            sb.append(in).append("protected ").append(simple(c.name)).append("() {").append(superCall(c)).append("}\n");
        }
        for (String e : c.extra) sb.append(in).append(e).append('\n');
        if (!c.iface && !c.annotation) {
            for (Member am : abstractsOf(c, new HashSet<>())) {
                if (c.members.containsKey(am.name + am.desc) && !skipMember.contains(c.name + "." + am.name + am.desc)) continue;
                Type mt = Type.getMethodType(am.desc);
                StringBuilder params = new StringBuilder();
                Type[] a = mt.getArgumentTypes();
                for (int i = 0; i < a.length; i++) params.append(i > 0 ? ", " : "").append(java(a[i])).append(" a").append(i);
                String body = dflt(mt.getReturnType());
                sb.append(in).append("public ").append(java(mt.getReturnType())).append(' ').append(am.name).append('(').append(params).append(") { ")
                        .append(body.isEmpty() ? "" : "return " + body + "; ").append("}\n");
            }
        }
        for (Cls inner : classes.values()) {
            if (inner.name.startsWith(c.name + "$") && !inner.name.substring(c.name.length() + 1).contains("$")) write(sb, inner, in, true);
        }
        sb.append(ind).append("}\n");
    }

    static List<Member> abstractsOf(Cls c, Set<String> seen) {
        List<Member> out = new ArrayList<>();
        List<String> sup = new ArrayList<>(c.ifaces);
        for (String d : sup) {
            String n = (d.contains("<") ? d.substring(0, d.indexOf('<')) : d).strip();
            Cls ic = null;
            for (Cls k : classes.values()) if (k.name.replace('/', '.').replace('$', '.').equals(n)) ic = k;
            if (ic == null || !seen.add(ic.name)) continue;
            for (Member m : ic.members.values()) if (m.isAbstract) out.add(m);
            if (ic.superDecl != null) {
                Cls tmp = new Cls();
                tmp.ifaces.add(ic.superDecl);
                out.addAll(abstractsOf(tmp, seen));
            }
            Cls tmp2 = new Cls();
            tmp2.ifaces.addAll(ic.ifaces);
            out.addAll(abstractsOf(tmp2, seen));
        }
        return out;
    }

    static boolean jdkHas(Cls c, Member m) {
        List<String> sup = new ArrayList<>(c.ifaces);
        if (c.superDecl != null) sup.add(c.superDecl);
        int argc = Type.getArgumentTypes(m.desc).length;
        for (String s : sup) {
            if (!s.startsWith("java.")) continue;
            String n = s.contains("<") ? s.substring(0, s.indexOf('<')) : s;
            try {
                for (java.lang.reflect.Method r : Class.forName(n).getMethods())
                    if (r.getName().equals(m.name) && r.getParameterCount() == argc) return true;
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    }

    static String firstConst(String enumName) {
        Cls c = classes.get(enumName);
        for (Member m : c.members.values()) if (m.isField && m.isStatic && m.desc.equals("L" + enumName + ";")) return m.name;
        return "values()[0]";
    }

    static String superCall(Cls c) {
        return " ";
    }
}
