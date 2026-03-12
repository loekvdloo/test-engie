package nl.engie.allocation.controller;

import nl.engie.allocation.dto.MessageStatusResponse;
import nl.engie.allocation.dto.MessageSubmitRequest;
import nl.engie.allocation.model.enums.MessageStatus;
import nl.engie.allocation.service.MarketMessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MarketMessageController {

    private final MarketMessageService messageService;

    public MarketMessageController(MarketMessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Submit a new market message for processing through the pipeline.
     * The message goes through all steps: 1A -> 1B -> ... -> 6B
     */
    @PostMapping
    public ResponseEntity<Map<String, String>> submitMessage(@RequestBody MessageSubmitRequest request) {
        String uuid = messageService.submitMessage(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of(
                        "messageUuid", uuid,
                        "status", "ACCEPTED",
                        "message", "Bericht ontvangen en wordt verwerkt door de pipeline"
                ));
    }

    /**
     * Submit raw XML directly (convenience endpoint).
     */
    @PostMapping(value = "/xml", consumes = {"application/xml", "text/xml"})
    public ResponseEntity<Map<String, String>> submitXml(@RequestBody String xmlContent) {
        MessageSubmitRequest request = new MessageSubmitRequest(xmlContent, false, null);
        String uuid = messageService.submitMessage(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of(
                        "messageUuid", uuid,
                        "status", "ACCEPTED",
                        "message", "XML bericht ontvangen en wordt verwerkt"
                ));
    }

    /**
     * Get the full status of a message including all pipeline steps.
     */
    @GetMapping("/{uuid}")
    public ResponseEntity<MessageStatusResponse> getMessageStatus(@PathVariable String uuid) {
        MessageStatusResponse status = messageService.getMessageStatus(uuid);
        return ResponseEntity.ok(status);
    }

    /**
     * Get all messages (overview).
     */
    @GetMapping
    public ResponseEntity<List<MessageStatusResponse>> getAllMessages() {
        return ResponseEntity.ok(messageService.getAllMessages());
    }

    /**
     * Get messages by status.
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<MessageStatusResponse>> getByStatus(@PathVariable String status) {
        MessageStatus messageStatus = MessageStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(messageService.getMessagesByStatus(messageStatus));
    }

    /**
     * Reprocess a failed or parked message.
     */
    @PostMapping("/{uuid}/reprocess")
    public ResponseEntity<Map<String, String>> reprocessMessage(@PathVariable String uuid) {
        String result = messageService.reprocessMessage(uuid);
        return ResponseEntity.ok(Map.of(
                "messageUuid", result,
                "status", "REPROCESSING",
                "message", "Bericht wordt opnieuw verwerkt"
        ));
    }
}
