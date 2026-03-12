package nl.engie.allocation.pipeline;

import nl.engie.allocation.model.enums.StepCode;

/**
 * Represents a single step in the message processing pipeline.
 * Each step must complete before the next can start.
 */
public interface PipelineStep {

    /**
     * The step code identifying this step.
     */
    StepCode getStepCode();

    /**
     * Execute this pipeline step.
     * @param context the pipeline context containing message data and accumulated state
     * @return the result of the step execution
     */
    StepResult execute(PipelineContext context);
}
