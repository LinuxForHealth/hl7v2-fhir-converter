/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.data;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import ca.uhn.hl7v2.model.v26.datatype.CWE;
import ca.uhn.hl7v2.model.Varies;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceCategory;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceCriticality;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Immunization.ImmunizationStatus;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Specimen.SpecimenStatus;
import org.hl7.fhir.r4.model.codesystems.V3MaritalStatus;
import org.hl7.fhir.r4.model.codesystems.ConditionCategory;
import org.hl7.fhir.r4.model.codesystems.MessageReasonEncounter;
import org.hl7.fhir.r4.model.codesystems.NameUse;
import org.hl7.fhir.r4.model.codesystems.V3ReligiousAffiliation;
import org.hl7.fhir.r4.model.codesystems.DiagnosisRole;
import org.hl7.fhir.r4.model.codesystems.ConditionClinical;
import org.hl7.fhir.r4.model.codesystems.ConditionVerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.linuxforhealth.api.ResourceValue;
import io.github.linuxforhealth.core.terminology.Hl7v2Mapping;
import io.github.linuxforhealth.core.terminology.SimpleCode;
import io.github.linuxforhealth.core.terminology.TerminologyLookup;
import io.github.linuxforhealth.core.terminology.UrlLookup;
import io.github.linuxforhealth.hl7.data.date.DateUtil;



public class SimpleDataValueResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDataValueResolver.class);
    private static final String INVALID_CODE_MESSAGE_FULL = "Invalid input: code '%s' could not be mapped to values in system '%s' with original display '%s' and version '%s'.";
    private static final String INVALID_CODE_MESSAGE_SHORT = "Invalid input: code '%s' could not be mapped to values in system '%s'.";


    public static final ValueExtractor<Object, String> DATE = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        if (val != null) {
            return DateUtil.formatToDate(val);
        }
        return null;
    };

    public static final ValueExtractor<Object, String> DATE_TIME = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        if (val != null) {
            return DateUtil.formatToDateTimeWithZone(val);
        }
        return null;
    };

    public static final ValueExtractor<Object, String> STRING = (Object value) -> {
        return Hl7DataHandlerUtil.getStringValue(value);
    };

    public static final ValueExtractor<Object, String> STRING_ALL = (Object value) -> {
        return Hl7DataHandlerUtil.getStringValue(value, true);
    };

    public static final ValueExtractor<Object, String> INSTANT = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        if (val != null) {
            return DateUtil.formatToZonedDateTime(val);
        }
        return null;
    };

    public static final ValueExtractor<Object, URI> URI_VAL = (Object value) -> {
        try {
            String val = Hl7DataHandlerUtil.getStringValue(value);
            if (val != null && isValidUUID(val)) {
                return new URI("urn", "uuid", val);
            } else {
                return null;
            }
        } catch (IllegalArgumentException | URISyntaxException e) {
            LOGGER.warn("Value not valid URI, value: {}", value, e);
            return null;
        }
    };

    public static final ValueExtractor<Object, String> ADMINISTRATIVE_GENDER_CODE_FHIR = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String code = getFHIRCode(val, AdministrativeGender.class);
        if (code != null) {
            return code;
        } else if (val == null) {
            return null;
        } else {
            return AdministrativeGender.UNKNOWN.toCode();
        }
    };

    public static final ValueExtractor<Object, String> MEDREQ_STATUS_CODE_FHIR = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String code = getFHIRCode(val, MedicationRequest.class);
        if (code != null) {
            return code;
        }
        else return "unknown"; // when the HL7 status codes get mapped in v2toFhirMapping, we will return code. "unknown" is being returned because the hl7 message is not mapped to fhir yet.
    };

    public static final ValueExtractor<Object, String> OBSERVATION_STATUS_CODE_FHIR = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String code = getFHIRCode(val, ObservationStatus.class);
        if (code != null) {
            return code;
        } else {
            return null;
        }

    };

    public static final ValueExtractor<Object, SimpleCode> OBSERVATION_STATUS_FHIR = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String code = getFHIRCode(val, ObservationStatus.class);
        if (code != null) {
            ObservationStatus status = ObservationStatus.fromCode(code);
            return new SimpleCode(code, status.getSystem(), status.getDisplay());
        } else {
            // If code is null, it means the code wasn't known in our table, and can't be looked up.
            // Make a message in the display.
            String theSystem = ObservationStatus.REGISTERED.getSystem();  
            return new SimpleCode(null, theSystem, String.format(INVALID_CODE_MESSAGE_SHORT, val, theSystem));
        }
    };

    public static final ValueExtractor<Object, SimpleCode> CONDITION_CATEGORY_CODES = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        if (val != null) {
            ConditionCategory status = ConditionCategory.fromCode(val);
            return new SimpleCode(val, status.getSystem(), status.getDisplay());
        } else {
            return null;
        }
    };


    public static final ValueExtractor<Object, SimpleCode> RELIGIOUS_AFFILIATION_FHIR_CC =
        (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String code = getFHIRCode(val, V3ReligiousAffiliation.class);
        String text = Hl7DataHandlerUtil.getOriginalDisplayText(value);
        String version = Hl7DataHandlerUtil.getVersion(value);

        if (code != null) {
            V3ReligiousAffiliation status = V3ReligiousAffiliation.fromCode(code);
            return new SimpleCode(code, status.getSystem(), status.getDisplay());
        } else {
            // // If code is null, it means the code wasn't known in our table, and can't be looked up.
            // // Make a message in the display.
            String religionSystem = V3ReligiousAffiliation._1028.getSystem(); 
            return new SimpleCode(null, religionSystem, String.format(INVALID_CODE_MESSAGE_FULL, val, religionSystem, text, version));
          }
    };

    public static final ValueExtractor<Object, SimpleCode> DIAGNOSIS_USE =
        (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String code = getFHIRCode(val, DiagnosisRole.class);
        if (code != null) {
            DiagnosisRole use = DiagnosisRole.fromCode(code);
            return new SimpleCode(code, use.getSystem(), use.getDisplay());
        } else {
            return new SimpleCode(val, null, null);
        }
    };

    public static final ValueExtractor<Object, SimpleCode> CONDITION_CLINICAL_STATUS_FHIR =
        (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);

        //Test to see if val is a valid code.
        ConditionClinical use = null;
        try {
            use = ConditionClinical.fromCode(val);
            LOGGER.info("Found ConditionClinical code for '{}'.",val);
        }
        catch(FHIRException e) {
            LOGGER.warn("Could not find ConditionClinical code for '{}'.",val);
        }

        if (use != null) { // if it is then setup the simple code
            return new SimpleCode(val, use.getSystem(), use.getDisplay());
        } else { // otherwise we don't want the code at all
            return null;
        }
    };

    public static final ValueExtractor<Object, SimpleCode> CONDITION_VERIFICATION_STATUS_FHIR =
        (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);

         //Test to see if val is a valid code.
        ConditionVerStatus use = null;
        try{
            use = ConditionVerStatus.fromCode(val);
            LOGGER.info("Found ConditionVerStatus code for '{}'.",val);
        }
        catch(FHIRException e) {
            LOGGER.warn("Could not find ConditionVerStatus code for '{}'.",val);
        }
        if (use != null) { // if it is then setup the simple code
            return new SimpleCode(val, use.getSystem(), use.getDisplay());
        } else { // otherwise we don't want the code at all
            return null;
        }
    };

    public static final ValueExtractor<Object, String> IMMUNIZATION_STATUS_CODES = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String code = getFHIRCode(val, ImmunizationStatus.class);
        if (code != null) {
            return code;
        } else {
            return null;
        }
    };

    public static final ValueExtractor<Object, String> SPECIMEN_STATUS_CODE_FHIR = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String code = getFHIRCode(val, SpecimenStatus.class);
        if (code != null) {
            return code;
        } else {
            return null;
        }
    };

    public static final ValueExtractor<Object, String> NAME_USE_CODE_FHIR = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String code = getFHIRCode(val, NameUse.class);
        if (code != null) {
            return code;
        } else {
            return null;
        }
    };

  public static final ValueExtractor<Object, SimpleCode> MARITAL_STATUS =
      (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String text = Hl7DataHandlerUtil.getOriginalDisplayText(value);
        String code = getFHIRCode(val, V3MaritalStatus.class);
        String version = Hl7DataHandlerUtil.getVersion(value);
        if(code != null){
            V3MaritalStatus mar = V3MaritalStatus.fromCode(code);
            return new SimpleCode(code, mar.getSystem(), mar.getDisplay(), version);
        } else {
          // If code is null, it means the code wasn't known in our table, and can't be looked up.
          // Make a message in the display.
          String theSystem = V3MaritalStatus.M.getSystem(); 
          return new SimpleCode(null, theSystem, String.format(INVALID_CODE_MESSAGE_FULL, val, theSystem, text, version));
        }
  };
  
    public static final ValueExtractor<Object, Boolean> BOOLEAN = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        if (null == val) {
            return false;
        }
        return BooleanUtils.toBoolean(val);

    };

    public static final ValueExtractor<Object, Integer> INTEGER = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        if (null == val) {
            return null;
        }
        if (NumberUtils.isCreatable(val)) {
            return NumberUtils.createInteger(val);
        } else {
            LOGGER.warn("Value {} for INTEGER is not a valid number so returning null.", value);
            return null;
        }
    };

    public static final ValueExtractor<Object, Float> FLOAT = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        if (null == val) {
            return null;
        }
        if (NumberUtils.isCreatable(val)) {
            return NumberUtils.createFloat(val);
        } else {
            LOGGER.warn("Value {} for DECIMAL is not a valid number so returning null.", value);
            return null;
        }
    };

    public static final ValueExtractor<Object, UUID> UUID_VAL = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        return getUUID(val);
    };

    public static final ValueExtractor<Object, String> BASE64_BINARY = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        return Base64.getEncoder().encodeToString(val.getBytes());
    };

    public static final ValueExtractor<Object, Object> OBJECT = (Object value) -> {
        return value;
    };

    public static final ValueExtractor<Object, SimpleCode> CODING_SYSTEM_V2_ALTERNATE = (Object value) -> {
        value = checkForAndUnwrapVariesObject(value);
        // ensure we have a CWE
        if (value instanceof CWE) {
            CWE cwe = (CWE) value;
            String table = Hl7DataHandlerUtil.getStringValue(cwe.getCwe6_NameOfAlternateCodingSystem());
            String code = Hl7DataHandlerUtil.getStringValue(cwe.getCwe4_AlternateIdentifier()); 
            String text = Hl7DataHandlerUtil.getStringValue(cwe.getCwe5_AlternateText());
            String version = Hl7DataHandlerUtil.getStringValue(cwe.getCwe8_AlternateCodingSystemVersionID());
            return commonCodingSystemV2(table, code, text, version);
        } 
        return null;
    };

    public static final ValueExtractor<Object, SimpleCode> CODING_SYSTEM_V2 = (Object value) -> {
        value = checkForAndUnwrapVariesObject(value);
        String table = Hl7DataHandlerUtil.getTableNumber(value);
        String code = Hl7DataHandlerUtil.getStringValue(value);
        String text = Hl7DataHandlerUtil.getOriginalDisplayText(value);
        String version = Hl7DataHandlerUtil.getVersion(value);
        return commonCodingSystemV2(table, code, text, version);
    };

    // For OBX.5 and other dynamic encoded fields, the real class is wrapped in the Varies class, and must be extracted from data
    private static final Object checkForAndUnwrapVariesObject(Object value) {
        if (value instanceof Varies) {
            Varies v = (Varies)value;
            value = v.getData();
        }
        return value;    
    }

    private static final SimpleCode commonCodingSystemV2 (String table, String code, String text, String version) {
        if (table != null && code != null) {
            // Found table and a code. Try looking it up.
            SimpleCode coding = TerminologyLookup.lookup(table, code);
            if (coding != null) {
                String display = coding.getDisplay();
                // Successful display confirms a valid code and system 
                if (display != null ) {

                    if (display.isEmpty()) {
                        // We have a table, code, but unknown display, so we can't tell if it's good, use the original display text
                        coding = new SimpleCode(coding.getCode(), coding.getSystem(), text, version);
                    }
                    coding.setVersion(version);
                    // We have a table, code, and display, so code was valid
                    return coding;
                } else {
                    // Display was not found; create an error message in the display text
                    return new SimpleCode(null, coding.getSystem(), String.format(INVALID_CODE_MESSAGE_FULL, code, coding.getSystem(), text, version));
                }
            } else { 
                // No success looking up the code, build our own fall-back system using table name
                return new SimpleCode(code, "urn:id:"+table, text, version) ;
            }
        } else if (code != null) {
            // A code but no system: build a simple systemless code
            return new SimpleCode(code, null, null);
        } else {
            return null;
        }
    }

    public static final ValueExtractor<Object, String> BUILD_IDENTIFIER_FROM_CWE = (Object value) -> {
        CWE newValue = ((CWE) value);
        String identifier = newValue.getCwe1_Identifier().toString();
        String text = newValue.getCwe2_Text().toString();
        String codingSystem = newValue.getCwe3_NameOfCodingSystem().toString();
        if (identifier != null ) {
            if (codingSystem != null) {
                String join = identifier + "-" + codingSystem;
                return join;
            }
            else {
                return identifier;
            }
        }
        else return text;
    };

    public static final ValueExtractor<Object, String> ALLERGY_INTOLERANCE_CRITICALITY_CODE_FHIR = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String code = getFHIRCode(val, AllergyIntoleranceCriticality.class);
        if (code != null) {
            return code;
        } else {
            return null;
        }
    };

    public static final ValueExtractor<Object, String> ALLERGY_INTOLERANCE_CATEGORY_CODE_FHIR = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String code = getFHIRCode(val, AllergyIntoleranceCategory.class);
        if (code != null) {
            return code;
        } else {
            return null;
        }
    };

    public static final ValueExtractor<Object, String> SYSTEM_URL = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
      return UrlLookup.getAssociatedUrl(val);
    };


    // Convert an authority string to a valid system value
    // Prepend "urn:id:"; convert any spaces to underscores
    public static final ValueExtractor<Object, String> SYSTEM_ID = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        if (val != null && val.length() > 0) {
            return "urn:id:" + val.replace(" ", "_");
        } else {
            return null;
        }

    };

    public static final ValueExtractor<Object, List<?>> ARRAY = (Object value) -> {
        if (value != null) {
            List list = new ArrayList<>();
            list.add(value);
            return list;
        }
        return null;
    };

    public static final ValueExtractor<Object, String> RELATIVE_REFERENCE = (Object value) -> {
        Map<String, Object> mapValue = null;
        if (value instanceof Map) {
            mapValue = (Map<String, Object>) value;
        } else if (value instanceof ResourceValue) {
            ResourceValue rv = (ResourceValue) value;
            mapValue = rv.getResource();
        }
        if (mapValue != null) {
            String type = Hl7DataHandlerUtil.getStringValue(mapValue.get("resourceType"));
            String refId = Hl7DataHandlerUtil.getStringValue(mapValue.get("id"));
            if (StringUtils.isNotBlank(type) && StringUtils.isNotBlank(refId)) {
                return type + "/" + refId;
            }
        }
        return null;
    };

    public static final ValueExtractor<Object, String> DIAGNOSTIC_REPORT_STATUS_CODES = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String code = getFHIRCode(val, DiagnosticReportStatus.class);
        if (code != null) {
            return code;
        } else {
            return DiagnosticReportStatus.UNKNOWN.toCode();
        }
    };

    public static final ValueExtractor<Object, UUID> NAMED_UUID = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        if (StringUtils.isNotBlank(val)) {
            return UUID.nameUUIDFromBytes(val.getBytes());
        }
        return null;
    };

    public static final ValueExtractor<Object, SimpleCode> MESSAGE_REASON_ENCOUNTER = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String code = getFHIRCode(val, MessageReasonEncounter.class);
        if (code != null) {
            MessageReasonEncounter en = MessageReasonEncounter.fromCode(code);
            return new SimpleCode(code, en.getSystem(), en.getDisplay());
        } else {
          // If code is null, it means the code wasn't known in our table, and can't be looked up.
          // Make a message in the display.
          String theSystem = MessageReasonEncounter.ADMIT.getSystem();  
          return new SimpleCode(null, theSystem, String.format(INVALID_CODE_MESSAGE_SHORT, val, theSystem));
        }
    };

    private SimpleDataValueResolver() {
    }

    private static UUID getUUID(String value) {
        if (value != null) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Value not valid UUID, value: {}  failure reason {}", value, e.getMessage());
                LOGGER.debug("Value not valid UUID, value: {}", value, e);
                return null;
            }
        } else {
            LOGGER.info("Value for  UUID is null, value: {}", value);
            return null;
        }
    }

    private static boolean isValidUUID(String val) {
        try {
            UUID.fromString(val);
            return true;
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Not a valid UUID reason {} ", e.getMessage());
            LOGGER.debug("Not a valid UUID ", e);
            return false;
        }
    }

    private static String getFHIRCode(String hl7Value, Class<?> fhirConceptClassName) {
        if (hl7Value != null) {
            Map<String, String> mapping = Hl7v2Mapping.getMapping(fhirConceptClassName.getSimpleName());
            if (mapping != null && !mapping.isEmpty()) {
                return mapping.get(StringUtils.upperCase(hl7Value, Locale.ENGLISH));
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

}
