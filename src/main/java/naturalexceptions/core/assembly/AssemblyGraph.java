package naturalexceptions.core.assembly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import naturalexceptions.core.fabrication.ComponentInstance;
import naturalexceptions.core.fabrication.GeometryId;
import naturalexceptions.core.metallurgy.MaterialQuantity;

/**
 * Structural graph representing an assembly of physically connected components.
 *
 * <p>
 * Invariants:
 * <ul>
 * <li>Does NOT collapse components into a fake monolithic material (e.g. "bronze_pickaxe").</li>
 * <li>Retains individual component identities, materials, geometries, and damage states.</li>
 * <li>Disassembly returns original components preserving their accumulated physical damage.</li>
 * </ul>
 */
public final class AssemblyGraph {

    private ComponentInstance head;
    private ComponentInstance handle;
    private final AttachmentInterface attachment;

    public AssemblyGraph(ComponentInstance head, ComponentInstance handle, AttachmentInterface attachment) {
        this.head = Objects.requireNonNull(head, "head component");
        this.handle = Objects.requireNonNull(handle, "handle component");
        this.attachment = Objects.requireNonNull(attachment, "attachment interface");
        validateCompatibility(head, handle, attachment);
    }

    public static AssemblyGraph ofTool(ComponentInstance head, ComponentInstance handle) {
        return new AssemblyGraph(head, handle, AttachmentInterface.SOCKET);
    }

    private static void validateCompatibility(ComponentInstance head, ComponentInstance handle,
        AttachmentInterface attachment) {
        if (handle.geometryId() != GeometryId.HANDLE && handle.geometryId() != GeometryId.ROD) {
            throw new IllegalArgumentException(
                "Incompatible handle geometry for tool assembly: " + handle.geometryId());
        }
    }

    public ComponentInstance head() {
        return head;
    }

    public ComponentInstance handle() {
        return handle;
    }

    public AttachmentInterface attachment() {
        return attachment;
    }

    public List<ComponentInstance> components() {
        List<ComponentInstance> list = new ArrayList<>(2);
        list.add(head);
        list.add(handle);
        return Collections.unmodifiableList(list);
    }

    public MaterialQuantity totalMass() {
        return head.mass()
            .add(handle.mass());
    }

    public boolean isBroken() {
        return head.damage()
            .isFractured()
            || handle.damage()
                .isFractured();
    }

    public void replaceHead(ComponentInstance newHead) {
        this.head = Objects.requireNonNull(newHead, "newHead");
        validateCompatibility(this.head, this.handle, this.attachment);
    }

    public void replaceHandle(ComponentInstance newHandle) {
        this.handle = Objects.requireNonNull(newHandle, "newHandle");
        validateCompatibility(this.head, this.handle, this.attachment);
    }

    /**
     * Disassembles the physical assembly, returning surviving component instances with their exact
     * material references and damage states.
     */
    public List<ComponentInstance> disassemble() {
        List<ComponentInstance> parts = new ArrayList<>(components());
        return parts;
    }
}
