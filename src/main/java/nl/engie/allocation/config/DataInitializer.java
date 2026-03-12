package nl.engie.allocation.config;

import nl.engie.allocation.model.entity.BrpRegisterEntry;
import nl.engie.allocation.model.entity.ValidationRule;
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

        // Generic validation rules
        validationRuleRepository.save(ValidationRule.builder()
                .ruleCode("GEN001")
                .ruleName("XML root element aanwezig")
                .ruleDescription("Controleert of het XML document een root element heeft")
                .ruleExpression("CONTAINS:<?xml")
                .errorCode("001")
                .errorMessage("XML header ontbreekt")
                .isActive(true)
                .build());

        validationRuleRepository.save(ValidationRule.builder()
                .ruleCode("GEN002")
                .ruleName("Productsoort elektriciteit")
                .ruleDescription("Controleert of productsoort elektriciteit (023) wordt gebruikt")
                .messageType("ALLOCATION_SERIES")
                .ruleExpression("CONTAINS:023")
                .errorCode("002")
                .errorMessage("Productsoort moet elektriciteit (023) zijn")
                .isActive(true)
                .build());

        validationRuleRepository.save(ValidationRule.builder()
                .ruleCode("GEN003")
                .ruleName("Resolutie PT15M")
                .ruleDescription("Controleert of de resolutie PT15M is voor elektriciteit")
                .ruleExpression("CONTAINS:PT15M")
                .errorCode("003")
                .errorMessage("Resolutie moet PT15M zijn voor elektriciteit")
                .isActive(true)
                .build());

        validationRuleRepository.save(ValidationRule.builder()
                .ruleCode("AGG001")
                .ruleName("Allocatiegroep aanwezig")
                .ruleDescription("Controleert of een geldige allocatiegroep is meegegeven")
                .messageType("AGGREGATED_ALLOCATION_SERIES")
                .ruleExpression("REGEX:.*(?:PRF|TMT|SMA|NVL|DIM).*")
                .errorCode("010")
                .errorMessage("Geldige allocatiegroep (PRF/TMT/SMA/NVL/DIM) is vereist")
                .isActive(true)
                .build());

        validationRuleRepository.save(ValidationRule.builder()
                .ruleCode("RCF001")
                .ruleName("RCF datumversie aanwezig")
                .ruleDescription("Controleert of datumversie RCF aanwezig is voor AllocationFactorSeries")
                .messageType("ALLOCATION_FACTOR_SERIES")
                .ruleExpression("CONTAINS:dateRCF_version")
                .errorCode("020")
                .errorMessage("Datumversie RCF is verplicht voor RCF/Profielfracties berichten")
                .isActive(true)
                .build());

        log.info("Validatieregels geïnitialiseerd met {} regels", validationRuleRepository.count());
    }
}
