package nl.engie.allocation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.engie.allocation.dto.MessageStatusResponse;
import nl.engie.allocation.dto.MessageSubmitRequest;
import nl.engie.allocation.model.enums.MessageStatus;
import nl.engie.allocation.service.MarketMessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MarketMessageController.class)
class MarketMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MarketMessageService messageService;

    @Nested
    @DisplayName("POST /api/messages")
    class SubmitMessageTests {

        @Test
        void submitMessage_shouldReturn202WithUuid() throws Exception {
            when(messageService.submitMessage(any())).thenReturn("gen-uuid-123");

            MessageSubmitRequest request = new MessageSubmitRequest(
                    "<AllocationSeries/>", false, "EAN123"
            );

            mockMvc.perform(post("/api/messages")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.messageUuid").value("gen-uuid-123"))
                    .andExpect(jsonPath("$.status").value("ACCEPTED"));
        }
    }

    @Nested
    @DisplayName("POST /api/messages/xml")
    class SubmitXmlTests {

        @Test
        void submitXml_shouldReturn202() throws Exception {
            when(messageService.submitMessage(any())).thenReturn("xml-uuid");

            mockMvc.perform(post("/api/messages/xml")
                            .contentType(MediaType.APPLICATION_XML)
                            .content("<AllocationSeries><mRID>test</mRID></AllocationSeries>"))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.messageUuid").value("xml-uuid"));
        }
    }

    @Nested
    @DisplayName("GET /api/messages/{uuid}")
    class GetMessageStatusTests {

        @Test
        void getMessageStatus_shouldReturn200() throws Exception {
            MessageStatusResponse response = new MessageStatusResponse(
                    "test-uuid", "ALLOCATION_SERIES", "COMPLETED", "STEP_6B",
                    LocalDateTime.now(), LocalDateTime.now(), 1, "ACK", "<ack/>", List.of()
            );
            when(messageService.getMessageStatus("test-uuid")).thenReturn(response);

            mockMvc.perform(get("/api/messages/test-uuid"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messageUuid").value("test-uuid"))
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.responseType").value("ACK"));
        }

        @Test
        void getMessageStatus_notFound_shouldReturn400() throws Exception {
            when(messageService.getMessageStatus("unknown"))
                    .thenThrow(new RuntimeException("Bericht niet gevonden"));

            mockMvc.perform(get("/api/messages/unknown"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/messages")
    class GetAllMessagesTests {

        @Test
        void getAllMessages_shouldReturnList() throws Exception {
            MessageStatusResponse r1 = new MessageStatusResponse(
                    "uuid-1", "ALLOCATION_SERIES", "COMPLETED", null,
                    LocalDateTime.now(), LocalDateTime.now(), 1, null, null, null
            );
            MessageStatusResponse r2 = new MessageStatusResponse(
                    "uuid-2", null, "FAILED", null,
                    LocalDateTime.now(), null, null, null, null, null
            );
            when(messageService.getAllMessages()).thenReturn(List.of(r1, r2));

            mockMvc.perform(get("/api/messages"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].messageUuid").value("uuid-1"))
                    .andExpect(jsonPath("$[1].messageUuid").value("uuid-2"));
        }

        @Test
        void getAllMessages_empty_shouldReturnEmptyArray() throws Exception {
            when(messageService.getAllMessages()).thenReturn(List.of());

            mockMvc.perform(get("/api/messages"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/messages/status/{status}")
    class GetByStatusTests {

        @Test
        void getByStatus_shouldReturnFilteredMessages() throws Exception {
            MessageStatusResponse r = new MessageStatusResponse(
                    "failed-uuid", null, "FAILED", null,
                    LocalDateTime.now(), null, null, null, null, null
            );
            when(messageService.getMessagesByStatus(MessageStatus.FAILED))
                    .thenReturn(List.of(r));

            mockMvc.perform(get("/api/messages/status/FAILED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].status").value("FAILED"));
        }

        @Test
        void getByStatus_invalidStatus_shouldReturn400() throws Exception {
            mockMvc.perform(get("/api/messages/status/INVALID_STATUS"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/messages/{uuid}/reprocess")
    class ReprocessTests {

        @Test
        void reprocess_shouldReturn200() throws Exception {
            when(messageService.reprocessMessage("failed-uuid")).thenReturn("failed-uuid");

            mockMvc.perform(post("/api/messages/failed-uuid/reprocess"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.messageUuid").value("failed-uuid"))
                    .andExpect(jsonPath("$.status").value("REPROCESSING"));
        }

        @Test
        void reprocess_completedMessage_shouldReturn400() throws Exception {
            when(messageService.reprocessMessage("completed-uuid"))
                    .thenThrow(new RuntimeException("Kan niet herverwerken"));

            mockMvc.perform(post("/api/messages/completed-uuid/reprocess"))
                    .andExpect(status().isBadRequest());
        }
    }
}
