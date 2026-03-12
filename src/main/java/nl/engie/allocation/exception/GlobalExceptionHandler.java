package nl.engie.allocation.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centrale foutafhandeling voor alle REST endpoints.
 *
 * <p>Beveiligingsmaatregel: interne foutmeldingen (stack traces, class names,
 * SQL fouten) worden NOOIT doorgegeven aan de client. Alleen generieke
 * foutmeldingen worden geretourneerd. Details worden server-side gelogd.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Bean Validation fouten (@Valid mislukt) — 400 met veldfout-details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "veld", fe.getField(),
                        "melding", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Ongeldige waarde"
                ))
                .collect(Collectors.toList());

        log.warn("Validatiefout: {}", fieldErrors);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Validatiefout in de invoer");
        body.put("details", fieldErrors);
        body.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * ResponseStatusException (door controller gegooid met specifieke status en message).
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        log.warn("Response status exception: {} - {}", ex.getStatusCode(), ex.getReason());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getReason() != null ? ex.getReason() : "Fout bij verwerking");
        body.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    /**
     * Pipeline fouten — 500 met generieke melding.
     */
    @ExceptionHandler(PipelineException.class)
    public ResponseEntity<Map<String, Object>> handlePipelineException(PipelineException ex) {
        log.error("Pipeline fout: {}", ex.getMessage(), ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Er is een fout opgetreden in de verwerkingspipeline");
        body.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * IllegalArgumentException — 400, bijv. ongeldige enum waarde.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Ongeldige invoer: {}", ex.getMessage());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Ongeldige invoer");
        body.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * RuntimeException fallback — 500 met generieke melding (geen interne details!).
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Onverwachte runtime fout: {}", ex.getMessage(), ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Er is een onverwachte fout opgetreden");
        body.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * Catch-all — 500 met generieke melding.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Onverwachte fout: {}", ex.getMessage(), ex);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Er is een serverfout opgetreden. Neem contact op met de beheerder.");
        body.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
