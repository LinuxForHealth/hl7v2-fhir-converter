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
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceCategory;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceCriticality;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Immunization.ImmunizationStatus;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.Specimen.SpecimenStatus;
import org.hl7.fhir.r4.model.codesystems.V3MaritalStatus;
import org.hl7.fhir.r4.model.codesystems.ConditionCategory;
import org.hl7.fhir.r4.model.codesystems.MessageReasonEncounter;
import org.hl7.fhir.r4.model.codesystems.NameUse;
import org.hl7.fhir.r4.model.codesystems.V3ReligiousAffiliation;
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
            return null;
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

    public static final ValueExtractor<Object, CodeableConcept> RELIGIOUS_AFFILIATION_FHIR_CC = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String code = getFHIRCode(val, V3ReligiousAffiliation.class);
        if (code != null) {
            V3ReligiousAffiliation rel = V3ReligiousAffiliation.fromCode(code);
            CodeableConcept codeableConcept = new CodeableConcept();
            codeableConcept.addCoding(new Coding(rel.getSystem(), code, rel.getDisplay()));
            codeableConcept.setText(rel.getDisplay());
            return codeableConcept;
        } else {
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

  public static final ValueExtractor<Object, Object> MARITAL_STATUS =
      (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String code = getFHIRCode(val, V3MaritalStatus.class);
        if(code != null){
            V3MaritalStatus mar = V3MaritalStatus.fromCode(code);
            return new SimpleCode(code, mar.getSystem(), mar.getDisplay() );
        } else {
          return null;
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

    public static final ValueExtractor<Object, Object> CODING_SYSTEM_V2 = (Object value) -> {
        String table = Hl7DataHandlerUtil.getTableNumber(value);
        String val = Hl7DataHandlerUtil.getStringValue(value);
        if (table != null && val != null) {
            return TerminologyLookup.lookup(table, val);
        } else if (val != null) {
            return new SimpleCode(val, null, null);
        } else {
            return null;
        }
    };

    public static final ValueExtractor<Object, String> JOIN_FILLER = (Object value) -> {
        System.out.println(value);
        // CWE newValue = ((CWE) value);
        return "";
    };
    public static final ValueExtractor<Object, String> BUILD_FROM_CWE = (Object value) -> {
       System.out.println(value);


       CWE newValue = ((CWE) value);


        String dot1 = newValue.getCwe1_Identifier().toString();
        String dot2 = newValue.getCwe2_Text().toString();
        String dot3 = newValue.getCwe3_NameOfCodingSystem().toString();

        if (dot1 != null ) {
            if (dot3 != null) {
                String join = dot1 + "-" + dot3;
                return join;
            }
            else {
                return dot1;
            }
        }
        else return dot2;

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
        return UrlLookup.getSystemUrl(val);
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

    public static final ValueExtractor<Object, Object> MESSAGE_REASON_ENCOUNTER = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String code = getFHIRCode(val, MessageReasonEncounter.class);
        if (code != null) {
            MessageReasonEncounter en = MessageReasonEncounter.fromCode(code);
            return new SimpleCode(code, en.getSystem(), en.getDisplay());
        } else {
            return null;
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
