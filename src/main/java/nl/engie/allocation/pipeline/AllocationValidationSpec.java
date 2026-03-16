package nl.engie.allocation.pipeline;

import nl.engie.allocation.model.enums.MessageType;

import java.util.Map;
import java.util.Set;

/**
 * Central specification mapping for process types and receiver-role constraints.
 */
public final class AllocationValidationSpec {

    private AllocationValidationSpec() {
    }

    public static final Set<String> PROCESS_TYPES_BRP = Set.of(
            "N101", "N102", "N111", "N121", "N131", "N132", "N141", "N142", "N151"
    );

    public static final Set<String> PROCESS_TYPES_LNB = Set.of(
            "N101", "N102", "N111", "N121", "N132", "N142", "N151"
    );

    public static final Map<MessageType, Set<String>> PROCESS_TYPES_BY_MESSAGE_TYPE = Map.of(
            MessageType.ALLOCATION_SERIES, Set.of("N101", "N131", "N141"),
            MessageType.AGGREGATED_ALLOCATION_SERIES, Set.of("N102", "N111", "N121", "N132", "N142"),
            MessageType.ALLOCATION_FACTOR_SERIES, Set.of("N151")
    );

    // Spec §4.1/4.2/4.3: productsoort "023" (elektriciteit) en product ID "8716867000030" (actieve energie, ebIX Code list)
    public static final Set<String> VALID_PRODUCT_CODES = Set.of("023", "8716867000030");

    // Spec §4.1/4.2: "Energie eenheid ... Toegestane waarde actieve energie: kWh KWH" — alleen KWH voor allocatieberichten
    public static final Set<String> VALID_ENERGY_UNITS = Set.of("KWH");

    public static boolean isAllowedForMessageType(MessageType messageType, String processType) {
        if (messageType == null || processType == null || processType.isBlank()) {
            return true;
        }
        Set<String> allowed = PROCESS_TYPES_BY_MESSAGE_TYPE.get(messageType);
        return allowed == null || allowed.contains(processType);
    }

    public static boolean isAllowedForReceiverRole(String receiverRole, String processType) {
        if (receiverRole == null || receiverRole.isBlank() || processType == null || processType.isBlank()) {
            return true;
        }
        return switch (receiverRole.trim().toUpperCase()) {
            case "BRP" -> PROCESS_TYPES_BRP.contains(processType);
            case "LNB" -> PROCESS_TYPES_LNB.contains(processType);
            default -> true;
        };
    }
}
