package naturalexceptions;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * NE's code that runs on a dedicated server must not use what Minecraft has only on the client. In 1.7.10 such
 * members are marked {@code @SideOnly(Side.CLIENT)} and are missing from the server jar, so a call to one works in
 * single player and throws NoSuchMethodError (or NoClassDefFoundError) on a server: as
 * {@code FoodStats.setFoodLevel} did, kicking every player. This walks NE's compiled classes and reports every such use
 * outside client-only code (classes and methods marked client-only, and the {@code client} packages).
 */
class ServerSideLinkageTest {

    private static final String SIDE_ONLY = "Lcpw/mods/fml/relauncher/SideOnly;";
    private final Map<String, ClassNode> minecraft = new HashMap<>();

    @Test
    void commonCodeUsesNothingClientOnly() throws IOException, URISyntaxException {
        Path root = Paths.get(
            NaturalExceptions.class.getResource("NaturalExceptions.class")
                .toURI())
            .getParent()
            .getParent();
        Map<String, ClassNode> ne = new HashMap<>();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : (Iterable<Path>) files.filter(
                f -> f.toString()
                    .endsWith(".class"))::iterator) {
                ClassNode node = read(Files.readAllBytes(file));
                ne.put(node.name, node);
            }
        }
        java.util.Set<String> clientMixins = clientMixins();
        TreeSet<String> found = new TreeSet<>();
        for (ClassNode node : ne.values()) {
            if (clientOnly(node, ne) || clientMixins.contains(node.name)) continue;
            for (MethodNode method : node.methods) {
                if (clientOnly(method.visibleAnnotations) || CLIENT_CALLBACKS.contains(method.name)) continue;
                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    String use = clientOnlyUse(insn);
                    if (use == null) use = neClientUse(insn, ne);
                    if (use != null) found.add(node.name.replace('/', '.') + "." + method.name + " uses " + use);
                }
            }
        }
        assertTrue(
            found.isEmpty(),
            "client-only Minecraft used where a server runs it:\n  " + String.join("\n  ", found));
    }

    /**
     * Methods Minecraft and Forge only ever call on the client: a tile entity's update from the server, the screen for
     * a GUI being opened.
     */
    private static final java.util.Set<String> CLIENT_CALLBACKS = new java.util.HashSet<>(
        java.util.Arrays.asList("onDataPacket", "getClientGuiElement"));

    /**
     * NE's class is client-only: marked so, in a {@code client} package, nested in a class that is, or made inside a
     * method that is (an anonymous renderer registered on the client, say).
     */
    private static boolean clientOnly(ClassNode node, Map<String, ClassNode> ne) {
        if (node.name.contains("/client/") || clientOnly(node.visibleAnnotations)) return true;
        if (node.outerClass != null && node.outerMethod != null) {
            ClassNode outer = ne.get(node.outerClass);
            if (outer != null) for (MethodNode m : outer.methods)
                if (m.name.equals(node.outerMethod) && m.desc.equals(node.outerMethodDesc)
                    && clientOnly(m.visibleAnnotations)) return true;
        }
        int nest = node.name.lastIndexOf('$');
        if (nest < 0) return false;
        ClassNode outer = ne.get(node.name.substring(0, nest));
        return outer != null && clientOnly(outer, ne);
    }

    /** The mixins NE applies on the client only (its mixin config's "client" list), as internal class names. */
    private static java.util.Set<String> clientMixins() throws IOException {
        java.util.Set<String> out = new java.util.HashSet<>();
        try (InputStream in = NaturalExceptions.class.getResourceAsStream("/mixins.naturalexceptions.json")) {
            if (in == null) return out;
            String json = new String(readAll(in), java.nio.charset.StandardCharsets.UTF_8);
            java.util.regex.Matcher pkg = java.util.regex.Pattern.compile("\"package\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(json);
            java.util.regex.Matcher client = java.util.regex.Pattern.compile("\"client\"\\s*:\\s*\\[([^\\]]*)\\]")
                .matcher(json);
            if (!pkg.find() || !client.find()) return out;
            java.util.regex.Matcher name = java.util.regex.Pattern.compile("\"([^\"]+)\"")
                .matcher(client.group(1));
            while (name.find()) out.add((pkg.group(1) + "." + name.group(1)).replace('.', '/'));
        }
        return out;
    }

    private static boolean clientOnly(List<AnnotationNode> annotations) {
        if (annotations == null) return false;
        for (AnnotationNode a : annotations) if (SIDE_ONLY.equals(a.desc) && a.values != null)
            for (int i = 0; i + 1 < a.values.size(); i += 2) if ("value".equals(a.values.get(i))) {
                Object v = a.values.get(i + 1);
                if (v instanceof String[] && "CLIENT".equals(((String[]) v)[1])) return true;
            }
        return false;
    }

    /** Which of NE's own client-only classes this instruction uses, or null. */
    private static String neClientUse(AbstractInsnNode insn, Map<String, ClassNode> ne) {
        String owner = insn instanceof MethodInsnNode ? ((MethodInsnNode) insn).owner
            : insn instanceof FieldInsnNode ? ((FieldInsnNode) insn).owner
                : insn instanceof TypeInsnNode ? type(((TypeInsnNode) insn).desc) : null;
        ClassNode c = owner == null ? null : ne.get(owner);
        return c != null && clientOnly(c, ne) ? owner + " (NE's, client-only)" : null;
    }

    /** What client-only Minecraft class or member this instruction uses, or null. */
    private String clientOnlyUse(AbstractInsnNode insn) {
        if (insn instanceof MethodInsnNode) {
            MethodInsnNode m = (MethodInsnNode) insn;
            return member(m.owner, m.name, m.desc, true);
        }
        if (insn instanceof FieldInsnNode) {
            FieldInsnNode f = (FieldInsnNode) insn;
            return member(f.owner, f.name, f.desc, false);
        }
        if (insn instanceof TypeInsnNode) return clientClass(type(((TypeInsnNode) insn).desc));
        if (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof Type)
            return clientClass(type(((Type) ((LdcInsnNode) insn).cst).getInternalName()));
        return null;
    }

    private static String type(String internalName) {
        if (!internalName.startsWith("[")) return internalName;
        Type element = Type.getType(internalName)
            .getElementType();
        return element.getSort() == Type.OBJECT ? element.getInternalName() : "";
    }

    private String clientClass(String owner) {
        ClassNode c = minecraft(owner);
        return c != null && clientOnly(c.visibleAnnotations) ? owner : null;
    }

    /** The member, found up the owner's class tree, if it (or the class declaring it) is client-only. */
    private String member(String owner, String name, String desc, boolean method) {
        if (clientClass(owner) != null) return owner;
        List<String> queue = new ArrayList<>();
        queue.add(owner);
        while (!queue.isEmpty()) {
            ClassNode c = minecraft(queue.remove(0));
            if (c == null) continue;
            if (method) {
                for (MethodNode m : c.methods) if (m.name.equals(name) && m.desc.equals(desc))
                    return clientOnly(m.visibleAnnotations) || clientOnly(c.visibleAnnotations)
                        ? c.name + "." + name + desc
                        : null;
            } else {
                for (FieldNode f : c.fields) if (f.name.equals(name) && f.desc.equals(desc))
                    return clientOnly(f.visibleAnnotations) || clientOnly(c.visibleAnnotations) ? c.name + "." + name
                        : null;
            }
            if (c.superName != null) queue.add(c.superName);
            queue.addAll(c.interfaces);
        }
        return null;
    }

    /** A Minecraft or Forge class as compiled (with its side annotations), or null for anything else. */
    private ClassNode minecraft(String name) {
        if (!(name.startsWith("net/minecraft/") || name.startsWith("cpw/mods/fml/"))) return null;
        return minecraft.computeIfAbsent(name, n -> {
            try (InputStream in = ClassLoader.getSystemResourceAsStream(n + ".class")) {
                return in == null ? null : read(readAll(in));
            } catch (IOException e) {
                return null;
            }
        });
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        for (int n; (n = in.read(buffer)) > 0;) out.write(buffer, 0, n);
        return out.toByteArray();
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_FRAMES);
        return node;
    }
}
