package nl.engie.allocation.config;

import nl.engie.allocation.model.entity.BrpRegisterEntry;
import nl.engie.allocation.model.entity.ValidationRule;
import nl.engie.allocation.model.enums.ErrorCode;
import nl.engie.allocation.repository.BrpRegisterRepository;
import nl.engie.allocation.repository.ValidationRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Initializes the database with sample data on first startup.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final BrpRegisterRepository brpRegisterRepository;
    private final ValidationRuleRepository validationRuleRepository;

    public DataInitializer(BrpRegisterRepository brpRegisterRepository, ValidationRuleRepository validationRuleRepository) {
        this.brpRegisterRepository = brpRegisterRepository;
        this.validationRuleRepository = validationRuleRepository;
    }

    @Override
    public void run(String... args) {
        initBrpRegister();
        initValidationRules();
        log.info("Database initialisatie voltooid");
    }

    private void initBrpRegister() {
        if (brpRegisterRepository.count() > 0) return;

        // Sample BRP register entries
        brpRegisterRepository.save(BrpRegisterEntry.builder()
                .eanCode("871686700000000001")
                .partyName("Engie Energie NL")
                .marketRole("DDK")
                .isActive(true)
                .validFrom(LocalDate.of(2020, 1, 1))
                .build());

        brpRegisterRepository.save(BrpRegisterEntry.builder()
                .eanCode("871686700000000002")
                .partyName("Vattenfall NL")
                .marketRole("DDK")
                .isActive(true)
                .validFrom(LocalDate.of(2020, 1, 1))
                .build());

        brpRegisterRepository.save(BrpRegisterEntry.builder()
                .eanCode("871686700000000003")
                .partyName("Essent BV")
                .marketRole("DDQ")
                .isActive(true)
                .validFrom(LocalDate.of(2020, 1, 1))
                .build());

        brpRegisterRepository.save(BrpRegisterEntry.builder()
                .eanCode("871686700000000004")
                .partyName("Liander NB")
                .marketRole("DDM")
                .isActive(true)
                .validFrom(LocalDate.of(2020, 1, 1))
                .build());

        brpRegisterRepository.save(BrpRegisterEntry.builder()
                .eanCode("871686700000000005")
                .partyName("Stedin NB")
                .marketRole("DDM")
                .isActive(true)
                .validFrom(LocalDate.of(2020, 1, 1))
                .build());

        log.info("BRP register geïnitialiseerd met {} entries", brpRegisterRepository.count());
    }

    private void initValidationRules() {
        if (validationRuleRepository.count() > 0) return;

        // Validation rules based on official error codes from specification
        validationRuleRepository.save(ValidationRule.builder()
                .ruleCode("XML_SCHEMA")
                .ruleName("XML root element aanwezig")
                .ruleDescription("Controleert of het XML document een root element heeft")
                .ruleExpression("CONTAINS:<?xml")
                .errorCode(ErrorCode.E_999.getCode())
                .errorMessage(ErrorCode.E_999.getFoutmelding())
                .isActive(true)
                .build());

        validationRuleRepository.save(ValidationRule.builder()
                .ruleCode("PRODUCTSOORT")
                .ruleName("Productsoort elektriciteit")
                .ruleDescription("Controleert of productsoort elektriciteit (023) wordt gebruikt")
                .messageType("ALLOCATION_SERIES")
                .ruleExpression("CONTAINS:023")
                .errorCode(ErrorCode.E_999.getCode())
                .errorMessage("Productsoort moet elektriciteit (023) zijn")
                .isActive(true)
                .build());

        validationRuleRepository.save(ValidationRule.builder()
                .ruleCode("RESOLUTIE")
                .ruleName("Resolutie PT15M")
                .ruleDescription("Controleert of de resolutie PT15M is voor elektriciteit")
                .ruleExpression("CONTAINS:PT15M")
                .errorCode(ErrorCode.E_773.getCode())
                .errorMessage(ErrorCode.E_773.getFoutmelding())
                .isActive(true)
                .build());

        validationRuleRepository.save(ValidationRule.builder()
                .ruleCode("ALLOCATIEGROEP")
                .ruleName("Allocatiegroep aanwezig")
                .ruleDescription("Controleert of een geldige allocatiegroep is meegegeven")
                .messageType("AGGREGATED_ALLOCATION_SERIES")
                .ruleExpression("REGEX:.*(?:PRF|TMT|SMA|NVL|DIM).*")
                .errorCode(ErrorCode.E_764.getCode())
                .errorMessage(ErrorCode.E_764.getFoutmelding())
                .isActive(true)
                .build());

        validationRuleRepository.save(ValidationRule.builder()
                .ruleCode("RCF_DATUM")
                .ruleName("RCF datumversie aanwezig")
                .ruleDescription("Controleert of datumversie RCF aanwezig is voor AllocationFactorSeries")
                .messageType("ALLOCATION_FACTOR_SERIES")
                .ruleExpression("CONTAINS:dateRCF_version")
                .errorCode(ErrorCode.E_999.getCode())
                .errorMessage("Datumversie RCF is verplicht voor RCF/Profielfracties berichten")
                .isActive(true)
                .build());

        validationRuleRepository.save(ValidationRule.builder()
                .ruleCode("EAN_18")
                .ruleName("EAN-18 codelijst")
                .ruleDescription("Controleert of EAN-18 code geldig is (18 cijfers)")
                .ruleExpression("REGEX:\\\\d{18}")
                .errorCode(ErrorCode.E_651.getCode())
                .errorMessage(ErrorCode.E_651.getFoutmelding())
                .isActive(true)
                .build());

        validationRuleRepository.save(ValidationRule.builder()
                .ruleCode("EAN_13")
                .ruleName("EAN-13 code meetpunt")
                .ruleDescription("Controleert of EAN-13 code geldig is (13 cijfers)")
                .ruleExpression("REGEX:\\\\d{13}")
                .errorCode(ErrorCode.E_758.getCode())
                .errorMessage(ErrorCode.E_758.getFoutmelding())
                .isActive(true)
                .build());

        validationRuleRepository.save(ValidationRule.builder()
                .ruleCode("BRP_BEKENDHEID")
                .ruleName("BRP bekendheid controle")
                .ruleDescription("Controleert of de BRP/leverancier bekend is in het register")
                .ruleExpression("LOOKUP:BRP_REGISTER")
                .errorCode(ErrorCode.E_765.getCode())
                .errorMessage(ErrorCode.E_765.getFoutmelding())
                .isActive(true)
                .build());

        validationRuleRepository.save(ValidationRule.builder()
                .ruleCode("VOLUME_DECIMALEN")
                .ruleName("Volume decimalen controle")
                .ruleDescription("Controleert of volumes exact 3 decimalen hebben")
                .ruleExpression("REGEX:-?\\\\d+\\\\.\\\\d{3}")
                .errorCode(ErrorCode.E_776.getCode())
                .errorMessage(ErrorCode.E_776.getFoutmelding())
                .isActive(true)
                .build());

        validationRuleRepository.save(ValidationRule.builder()
                .ruleCode("NEGATIEF_VOLUME")
                .ruleName("Negatief volume controle")
                .ruleDescription("Controleert of volumes niet negatief zijn (behalve PRF)")
                .ruleExpression("CHECK:VOLUME_POSITIVE")
                .errorCode(ErrorCode.E_686.getCode())
                .errorMessage(ErrorCode.E_686.getFoutmelding())
                .isActive(true)
                .build());

        log.info("Validatieregels geïnitialiseerd met {} regels", validationRuleRepository.count());
    }
}
