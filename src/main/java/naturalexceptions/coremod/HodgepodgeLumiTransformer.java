package naturalexceptions.coremod;

import net.minecraft.launchwrapper.IClassTransformer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Registered only when Lumi is present (see {@link NECoreMod}). In Hodgepodge's list of mixins, where it reads
 * {@code FixesConfig.optimizeWorldUpdateLight} to decide on its world-lighting fix, it reads false instead. That fix,
 * which collides with Lumi's lighting, is then left out, as Hodgepodge already does for Angelica.
 */
public final class HodgepodgeLumiTransformer implements IClassTransformer {

    private static final Logger LOG = LogManager.getLogger("NaturalExceptionsCore");
    static final String MIXINS = "com.mitchej123.hodgepodge.mixins.Mixins";
    static final String CONFIG = "com/mitchej123/hodgepodge/config/FixesConfig";
    static final String FIX = "optimizeWorldUpdateLight";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !MIXINS.equals(transformedName)) return bytes;
        byte[] out = withoutFix(bytes);
        if (out == bytes) LOG.warn(
            "Hodgepodge's {} was not found to turn off; if startup fails in its mixins, set it "
                + "false in config/hodgepodge.cfg.",
            FIX);
        else LOG.info("Left out Hodgepodge's {}: it collides with Lumi's lighting.", FIX);
        return out;
    }

    /** The class with every read of the fix's setting made a constant false; the same bytes if it has none. */
    static byte[] withoutFix(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        boolean changed = false;
        for (MethodNode method : node.methods)
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null;) {
                AbstractInsnNode next = insn.getNext();
                if (insn.getOpcode() == Opcodes.GETSTATIC) {
                    FieldInsnNode field = (FieldInsnNode) insn;
                    if (CONFIG.equals(field.owner) && FIX.equals(field.name) && "Z".equals(field.desc)) {
                        method.instructions.set(insn, new InsnNode(Opcodes.ICONST_0));
                        changed = true;
                    }
                }
                insn = next;
            }
        if (!changed) return bytes;
        // The stack and frames are unchanged (a boolean in place of a boolean), so nothing needs computing.
        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }
}
