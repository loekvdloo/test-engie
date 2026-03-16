package nl.engie.allocation.pipeline.step;

import nl.engie.allocation.model.entity.MarketMessage;
import nl.engie.allocation.model.entity.ValidationResult;
import nl.engie.allocation.model.entity.ValidationRule;
import nl.engie.allocation.model.enums.MessageStatus;
import nl.engie.allocation.model.enums.MessageType;
import nl.engie.allocation.model.enums.StepCode;
import nl.engie.allocation.pipeline.PipelineContext;
import nl.engie.allocation.pipeline.StepResult;
import nl.engie.allocation.repository.BrpRegisterRepository;
import nl.engie.allocation.repository.MarketMessageRepository;
import nl.engie.allocation.repository.ValidationResultRepository;
import nl.engie.allocation.repository.ValidationRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Phase 3 steps: 3A through 3G.
 */
@ExtendWith(MockitoExtension.class)
class Phase3StepTests {

    private static final String VALID_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <AllocationSeries>
                <mRID>a1b2c3d4-e5f6-7890-abcd-ef1234567890</mRID>
                <product><identification>8716867000030</identification></product>
                <startDateTime>2025-01-01T00:00:00Z</startDateTime>
                <endDateTime>2025-01-02T00:00:00Z</endDateTime>
                <resolution>PT15M</resolution>
                <position>1</position><quantity>100.000</quantity>
                <position>2</position><quantity>200.000</quantity>
            </AllocationSeries>
            """;

    private MarketMessage createMessage(String xml) {
        MarketMessage msg = MarketMessage.builder()
                .messageUuid("test-uuid")
                .xmlContent(xml)
                .status(MessageStatus.RECEIVED)
                .receivedAt(LocalDateTime.now())
                .messageType(MessageType.ALLOCATION_SERIES)
                .build();
        msg.setId(1L);
        return msg;
    }

    @Nested
    @DisplayName("Step 3A: BRP Register")
    class Step3aTests {

        @Mock
        private BrpRegisterRepository brpRegisterRepository;
        private Step3aBrpRegister step;

        @BeforeEach
        void setUp() {
            step = new Step3aBrpRegister(brpRegisterRepository);
        }

        @Test
        void stepCode_shouldBeStep3A() {
            assertEquals(StepCode.STEP_3A, step.getStepCode());
        }

        @Test
        void execute_withValidEan_shouldSucceed() {
            MarketMessage msg = createMessage(VALID_XML);
            msg.setEanCode("871686700012345678");
            when(brpRegisterRepository.existsByEanCodeAndIsActiveTrue("871686700012345678")).thenReturn(true);

            PipelineContext ctx = new PipelineContext(msg);
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            assertTrue(ctx.getValidationErrors().isEmpty());
        }

        @Test
        void execute_withUnknownEan_shouldAddValidationError() {
            MarketMessage msg = createMessage(VALID_XML);
            msg.setEanCode("999999999999999999");
            when(brpRegisterRepository.existsByEanCodeAndIsActiveTrue("999999999999999999")).thenReturn(false);

            PipelineContext ctx = new PipelineContext(msg);
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess()); // Step succeeds but adds validation error
            assertFalse(ctx.getValidationErrors().isEmpty());
            assertEquals("765", ctx.getValidationErrors().get(0).code());
        }

        @Test
        void execute_withNoEan_shouldSucceedWithoutCheck() {
            MarketMessage msg = createMessage(VALID_XML);
            msg.setEanCode(null);

            PipelineContext ctx = new PipelineContext(msg);
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            verify(brpRegisterRepository, never()).existsByEanCodeAndIsActiveTrue(any());
        }
    }

    @Nested
    @DisplayName("Step 3B: Markt Business Validaties")
    class Step3bTests {

        private Step3bMarktBusinessValidaties step;

        @BeforeEach
        void setUp() {
            step = new Step3bMarktBusinessValidaties();
        }

        @Test
        void stepCode_shouldBeStep3B() {
            assertEquals(StepCode.STEP_3B, step.getStepCode());
        }

        @Test
        void execute_withValidProduct_shouldNotAddErrors() {
            PipelineContext ctx = new PipelineContext(createMessage(VALID_XML));
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            // Valid XML contains 8716867000030 (actieve energie, ebIX code list) which is valid
        }

        @Test
        void execute_withInvalidProduct_shouldAddError() {
            String xml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <AllocationSeries>
                        <product><identification>INVALID</identification></product>
                    </AllocationSeries>
                    """;
            MarketMessage msg = createMessage(xml);
            PipelineContext ctx = new PipelineContext(msg);
            step.execute(ctx);

            // E_667: product past niet bij de productsoort (spec §3.4)
            boolean hasBiz001 = ctx.getValidationErrors().stream()
                    .anyMatch(e -> e.code().equals("667"));
            assertTrue(hasBiz001);
        }

        @Test
        void execute_withAggregatedWithoutGroup_shouldAddError() {
            String xml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <AggregatedAllocation>
                        <product><identification>023</identification></product>
                    </AggregatedAllocation>
                    """;
            MarketMessage msg = createMessage(xml);
            msg.setMessageType(MessageType.AGGREGATED_ALLOCATION_SERIES);
            PipelineContext ctx = new PipelineContext(msg);
            step.execute(ctx);

            boolean hasBiz002 = ctx.getValidationErrors().stream()
                    .anyMatch(e -> e.code().equals("764"));
            assertTrue(hasBiz002);
        }
    }

    @Nested
    @DisplayName("Step 3C: Controle verplichte velden")
    class Step3cTests {

        private Step3cControleVerplicht step;

        @BeforeEach
        void setUp() {
            step = new Step3cControleVerplicht();
        }

        @Test
        void stepCode_shouldBeStep3C() {
            assertEquals(StepCode.STEP_3C, step.getStepCode());
        }

        @Test
        void execute_withAllFields_shouldNotAddErrors() {
            PipelineContext ctx = new PipelineContext(createMessage(VALID_XML));
            int errorsBefore = ctx.getValidationErrors().size();
            step.execute(ctx);

            assertEquals(errorsBefore, ctx.getValidationErrors().size());
        }

        @Test
        void execute_withMissingFields_shouldAddErrors() {
            String xml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <AllocationSeries><mRID>test</mRID><data>only mRID present</data></AllocationSeries>
                    """;
            PipelineContext ctx = new PipelineContext(createMessage(xml));
            step.execute(ctx);

            // Missing: product, startDateTime, endDateTime, resolution (4 errors)
            long vld001Count = ctx.getValidationErrors().stream()
                    .filter(e -> e.code().equals("999"))
                    .count();
            assertEquals(4, vld001Count);
        }
    }

    @Nested
    @DisplayName("Step 3D: Configureerbare regels")
    class Step3dTests {

        @Mock
        private ValidationRuleRepository validationRuleRepository;
        @Mock
        private ValidationResultRepository validationResultRepository;
        private Step3dConfigureerbareRegels step;

        @BeforeEach
        void setUp() {
            step = new Step3dConfigureerbareRegels(validationRuleRepository, validationResultRepository);
        }

        @Test
        void stepCode_shouldBeStep3D() {
            assertEquals(StepCode.STEP_3D, step.getStepCode());
        }

        @Test
        void execute_withPassingRules_shouldSucceed() {
            ValidationRule rule = new ValidationRule();
            rule.setRuleCode("TEST001");
            rule.setRuleExpression("CONTAINS:AllocationSeries");
            rule.setErrorCode("ERR001");
            rule.setErrorMessage("Test error");

            when(validationRuleRepository.findByMessageTypeAndIsActiveTrue("ALLOCATION_SERIES"))
                    .thenReturn(List.of(rule));
            when(validationRuleRepository.findByIsActiveTrue()).thenReturn(List.of());

            PipelineContext ctx = new PipelineContext(createMessage(VALID_XML));
            StepResult result = step.execute(ctx);

            assertTrue(result.isSuccess());
            verify(validationResultRepository).save(any(ValidationResult.class));
        }

        @Test
        void execute_withFailingRule_shouldAddValidationError() {
            ValidationRule rule = new ValidationRule();
            rule.setRuleCode("TEST002");
            rule.setRuleExpression("CONTAINS:NonExistentElement");
            rule.setErrorCode("ERR002");
            rule.setErrorMessage("Element niet gevonden");

            when(validationRuleRepository.findByMessageTypeAndIsActiveTrue("ALLOCATION_SERIES"))
                    .thenReturn(List.of(rule));
            when(validationRuleRepository.findByIsActiveTrue()).thenReturn(List.of());

            PipelineContext ctx = new PipelineContext(createMessage(VALID_XML));
            step.execute(ctx);

            assertFalse(ctx.getValidationErrors().isEmpty());
            assertEquals("ERR002", ctx.getValidationErrors().get(0).code());
        }

        @Test
        void execute_withNotContainsRule_shouldEvaluateCorrectly() {
            ValidationRule rule = new ValidationRule();
            rule.setRuleCode("TEST003");
            rule.setRuleExpression("NOT_CONTAINS:ForbiddenElement");
            rule.setErrorCode("ERR003");
            rule.setErrorMessage("Forbidden found");

            when(validationRuleRepository.findByMessageTypeAndIsActiveTrue("ALLOCATION_SERIES"))
                    .thenReturn(List.of(rule));
            when(validationRuleRepository.findByIsActiveTrue()).thenReturn(List.of());

            PipelineContext ctx = new PipelineContext(createMessage(VALID_XML));
            step.execute(ctx);

            // Rule should pass because ForbiddenElement is not in the XML
            assertTrue(ctx.getValidationErrors().isEmpty());
        }
    }

    @Nested
    @DisplayName("Step 3E: Tijdvenster validaties")
    class Step3eTests {

        private Step3eTijdvenster step;

        @BeforeEach
        void setUp() {
            step = new Step3eTijdvenster();
        }

        @Test
        void stepCode_shouldBeStep3E() {
            assertEquals(StepCode.STEP_3E, step.getStepCode());
        }

        @Test
        void execute_withValidDates_shouldNotAddErrors() {
            MarketMessage msg = createMessage(VALID_XML);
            msg.setStartDateTime(LocalDateTime.now().minusDays(1));
            msg.setEndDateTime(LocalDateTime.now());

            PipelineContext ctx = new PipelineContext(msg);
            step.execute(ctx);

            long tvlErrors = ctx.getValidationErrors().stream()
                    .filter(e -> e.code().equals("663") || e.code().equals("772") || e.code().equals("763"))
                    .count();
            assertEquals(0, tvlErrors);
        }

        @Test
        void execute_withEndBeforeStart_shouldAddError() {
            MarketMessage msg = createMessage(VALID_XML);
            msg.setStartDateTime(LocalDateTime.now());
            msg.setEndDateTime(LocalDateTime.now().minusDays(1));

            PipelineContext ctx = new PipelineContext(msg);
            step.execute(ctx);

            boolean hasTvl001 = ctx.getValidationErrors().stream()
                    .anyMatch(e -> e.code().equals("663"));
            assertTrue(hasTvl001);
        }

        @Test
        void execute_withFutureStartDate_shouldAddError() {
            MarketMessage msg = createMessage(VALID_XML);
            msg.setStartDateTime(LocalDateTime.now().plusDays(5));
            msg.setEndDateTime(LocalDateTime.now().plusDays(6));

            PipelineContext ctx = new PipelineContext(msg);
            step.execute(ctx);

            boolean hasTvl002 = ctx.getValidationErrors().stream()
                    .anyMatch(e -> e.code().equals("772"));
            assertTrue(hasTvl002);
        }

        @Test
        void execute_withOldMessage_shouldAddError() {
            // E_763: bericht ontvangen meer dan maxDeliveryDelayHours na periode-einde
            MarketMessage msg = createMessage(VALID_XML);
            // Set endDateTime 10 days ago, message received now (well after SLA window)
            msg.setEndDateTime(LocalDateTime.now().minusDays(10));
            msg.setReceivedAt(LocalDateTime.now());

            PipelineContext ctx = new PipelineContext(msg);
            step.execute(ctx);

            boolean hasTvl003 = ctx.getValidationErrors().stream()
                    .anyMatch(e -> e.code().equals("763"));
            assertTrue(hasTvl003);
        }
    }

    @Nested
    @DisplayName("Step 3F: Volgordelijkheid")
    class Step3fTests {

        private Step3fVolgordelijkheid step;

        @BeforeEach
        void setUp() {
            step = new Step3fVolgordelijkheid();
        }

        @Test
        void stepCode_shouldBeStep3F() {
            assertEquals(StepCode.STEP_3F, step.getStepCode());
        }

        @Test
        void execute_withCorrectPositions_shouldNotAddErrors() {
            PipelineContext ctx = new PipelineContext(createMessage(VALID_XML));
            step.execute(ctx);

            long volErrors = ctx.getValidationErrors().stream()
                    .filter(e -> e.code().equals("782") || e.code().equals("676"))
                    .count();
            assertEquals(0, volErrors);
        }

        @Test
        void execute_withIncorrectPositions_shouldAddError() {
            String xml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <AllocationSeries>
                        <position>1</position><quantity>100.000</quantity>
                        <position>3</position><quantity>200.000</quantity>
                    </AllocationSeries>
                    """;
            PipelineContext ctx = new PipelineContext(createMessage(xml));
            step.execute(ctx);

            boolean hasVol001 = ctx.getValidationErrors().stream()
                    .anyMatch(e -> e.code().equals("782"));
            assertTrue(hasVol001);
        }
    }

    @Nested
    @DisplayName("Step 3G: Herbruikbare regels")
    class Step3gTests {

        @Mock
        private MarketMessageRepository marketMessageRepository;
        private Step3gHerbruikbareRegels step;

        @BeforeEach
        void setUp() {
            step = new Step3gHerbruikbareRegels(marketMessageRepository);
        }

        @Test
        void stepCode_shouldBeStep3G() {
            assertEquals(StepCode.STEP_3G, step.getStepCode());
        }

        @Test
        void execute_withUniqueExternalMessageId_shouldNotAddE669() {
            // E_669 is now a DB uniqueness check, not UUID format
            MarketMessage msg = createMessage(VALID_XML);
            msg.setExternalMessageId("unique-ext-id-001");
            when(marketMessageRepository.existsByExternalMessageIdAndIdNot("unique-ext-id-001", 1L))
                    .thenReturn(false);

            PipelineContext ctx = new PipelineContext(msg);
            step.execute(ctx);

            boolean hasE669 = ctx.getValidationErrors().stream()
                    .anyMatch(e -> e.code().equals("669"));
            assertFalse(hasE669);
        }

        @Test
        void execute_withDuplicateExternalMessageId_shouldAddE669() {
            // E_669: bericht met dit kenmerk al ontvangen (duplicate in DB)
            MarketMessage msg = createMessage(VALID_XML);
            msg.setExternalMessageId("duplicate-ext-id-001");
            when(marketMessageRepository.existsByExternalMessageIdAndIdNot("duplicate-ext-id-001", 1L))
                    .thenReturn(true);

            PipelineContext ctx = new PipelineContext(msg);
            step.execute(ctx);

            boolean hasE669 = ctx.getValidationErrors().stream()
                    .anyMatch(e -> e.code().equals("669"));
            assertTrue(hasE669);
        }

        @Test
        void execute_withValidDateTimeFormat_shouldNotAddError() {
            PipelineContext ctx = new PipelineContext(createMessage(VALID_XML));
            step.execute(ctx);

            boolean hasHbr002 = ctx.getValidationErrors().stream()
                    .anyMatch(e -> e.code().equals("999"));
            assertFalse(hasHbr002);
        }

        @Test
        void execute_withInvalidDateTimeFormat_shouldAddError() {
            String xml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <AllocationSeries>
                        <mRID>a1b2c3d4-e5f6-7890-abcd-ef1234567890</mRID>
                        <startDateTime>01-01-2025 00:00:00</startDateTime>
                    </AllocationSeries>
                    """;
            PipelineContext ctx = new PipelineContext(createMessage(xml));
            step.execute(ctx);

            boolean hasHbr002 = ctx.getValidationErrors().stream()
                    .anyMatch(e -> e.code().equals("999"));
            assertTrue(hasHbr002);
        }
    }
}
