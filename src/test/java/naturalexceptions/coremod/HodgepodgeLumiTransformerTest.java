package naturalexceptions.coremod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

class HodgepodgeLumiTransformerTest {

    /** A class with Hodgepodge's condition for the fix: a method returning the config field (or another one). */
    private static byte[] condition(String field) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(
            Opcodes.V1_8,
            Opcodes.ACC_PUBLIC,
            "com/mitchej123/hodgepodge/mixins/Mixins",
            null,
            "java/lang/Object",
            null);
        MethodVisitor mv = cw
            .visitMethod(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC, "lambda$static$24", "()Z", null, null);
        mv.visitCode();
        mv.visitFieldInsn(Opcodes.GETSTATIC, HodgepodgeLumiTransformer.CONFIG, field, "Z");
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    private static int[] opcodes(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        MethodNode method = node.methods.get(0);
        return java.util.stream.StreamSupport
            .stream(java.util.Spliterators.spliteratorUnknownSize(method.instructions.iterator(), 0), false)
            .mapToInt(AbstractInsnNode::getOpcode)
            .filter(op -> op >= 0)
            .toArray();
    }

    @Test
    void theFixsConditionReadsFalse() {
        byte[] out = HodgepodgeLumiTransformer.withoutFix(condition(HodgepodgeLumiTransformer.FIX));
        int[] ops = opcodes(out);
        assertEquals(Opcodes.ICONST_0, ops[0]);
        assertEquals(Opcodes.IRETURN, ops[1]);
        assertFalse(
            java.util.Arrays.stream(ops)
                .anyMatch(op -> op == Opcodes.GETSTATIC));
    }

    @Test
    void otherSettingsAreLeftAlone() {
        byte[] in = condition("fixFriendlyCreatureSounds");
        assertSame(in, HodgepodgeLumiTransformer.withoutFix(in));
    }

    @Test
    void otherClassesAreLeftAlone() {
        byte[] in = condition(HodgepodgeLumiTransformer.FIX);
        assertSame(in, new HodgepodgeLumiTransformer().transform("x", "net.minecraft.world.World", in));
        assertTrue(new HodgepodgeLumiTransformer().transform("x", HodgepodgeLumiTransformer.MIXINS, in) != in);
    }
}
