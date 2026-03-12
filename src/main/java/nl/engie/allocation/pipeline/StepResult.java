package nl.engie.allocation.pipeline;

/**
 * Result of a pipeline step execution.
 */
public class StepResult {

    private final boolean success;
    private final boolean skipped;
    private final String message;

    private StepResult(boolean success, boolean skipped, String message) {
        this.success = success;
        this.skipped = skipped;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public String getMessage() {
        return message;
    }

    public static StepResult success(String message) {
        return new StepResult(true, false, message);
    }

    public static StepResult failure(String message) {
        return new StepResult(false, false, message);
    }

    public static StepResult skipped(String message) {
        return new StepResult(true, true, message);
    }
}
