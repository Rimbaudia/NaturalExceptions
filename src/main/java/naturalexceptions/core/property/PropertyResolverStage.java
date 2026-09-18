package naturalexceptions.core.property;

/**
 * A single stage in the layered material property resolution pipeline.
 */
public interface PropertyResolverStage {

    /**
     * Diagnostic name of this pipeline stage.
     */
    String stageName();

    /**
     * Executes this resolution stage on the given context.
     */
    void apply(ResolutionContext context);
}
