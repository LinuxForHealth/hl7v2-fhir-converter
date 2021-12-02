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

import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.UnsupportedTemporalTypeException;

import ca.uhn.hl7v2.model.v26.datatype.CWE;
import ca.uhn.hl7v2.model.v26.datatype.PPN;
import ca.uhn.hl7v2.model.v26.datatype.XCN;
import ca.uhn.hl7v2.model.v26.segment.PV1;
import ca.uhn.hl7v2.model.v26.datatype.DTM;

import ca.uhn.hl7v2.model.Varies;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hl7.fhir.dstu3.model.codesystems.MedicationRequestCategory;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceCategory;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceCriticality;
import org.hl7.fhir.r4.model.AllergyIntolerance.AllergyIntoleranceType;
import org.hl7.fhir.r4.model.DiagnosticReport.DiagnosticReportStatus;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Immunization.ImmunizationStatus;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.hl7.fhir.r4.model.ServiceRequest.ServiceRequestStatus;
import org.hl7.fhir.r4.model.Specimen.SpecimenStatus;
import org.hl7.fhir.r4.model.codesystems.V3ActCode;
import org.hl7.fhir.r4.model.codesystems.V3MaritalStatus;
import org.hl7.fhir.r4.model.codesystems.ConditionCategory;
import org.hl7.fhir.r4.model.codesystems.MessageReasonEncounter;
import org.hl7.fhir.r4.model.codesystems.NameUse;
import org.hl7.fhir.r4.model.codesystems.V3ReligiousAffiliation;
import org.hl7.fhir.r4.model.codesystems.DiagnosisRole;
import org.hl7.fhir.r4.model.codesystems.ConditionClinical;
import org.hl7.fhir.r4.model.codesystems.ConditionVerStatus;
import org.hl7.fhir.r4.model.codesystems.CompositionStatus;
import org.hl7.fhir.r5.model.Enumerations;

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

    // Special extractor only for use with PV1 records.
    // Extract the admit and discharge time and calculate duration length.
    // Returns null if for any reason the data is not usable, which
    // allows use of secondary values or to stop display.
    public static final ValueExtractor<Object, String> PV1_DURATION_LENGTH = (Object value) -> {
        if (value instanceof PV1) {
            PV1 pv1 = (PV1) value;
            DTM start = pv1.getAdmitDateTime();
            DTM end = pv1.getDischargeDateTime();

            try {
                String sdate1 = Hl7DataHandlerUtil.getStringValue(start);
                String sdate2 = Hl7DataHandlerUtil.getStringValue(end);
                if (sdate1 != null && sdate2 != null) {
                    Temporal date1 = DateUtil.getTemporal(DateUtil.formatToDateTimeWithZone(sdate1));
                    Temporal date2 = DateUtil.getTemporal(DateUtil.formatToDateTimeWithZone(sdate2));
                    LOGGER.info("computing temporal dates");
                    LOGGER.debug("temporal dates start: {} , end: {} ", date1, date2);
                    if (date1 != null && date2 != null) {
                        return String.valueOf(ChronoUnit.MINUTES.between(date1, date2));
                    }
                }
            } catch (UnsupportedTemporalTypeException e) {
                LOGGER.warn("Cannot evaluate time difference.");
                LOGGER.debug("Cannot evaluate time difference for start: {} , end: {} reason {} ", start, end, e.getMessage());
                return null;
            }
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
            LOGGER.warn("Value not valid URI, value: {}", value);
            LOGGER.debug("Value not valid URI, value: {}", value, e);
            return null;
        }
    };

    // Creates a display name; currently only handles XCN and PPN as input
    public static final ValueExtractor<Object, String> PERSON_DISPLAY_NAME = (Object value) -> {
        StringBuilder sb = new StringBuilder();
        String valprefix = null;
        String valfirst = null;
        String valmiddle = null;
        String valfamily = null;
        String valsuffix = null;

        if (value instanceof XCN) {
            XCN xcn = (XCN) value;
            valprefix = Hl7DataHandlerUtil.getStringValue(xcn.getPrefixEgDR());
            valfirst = Hl7DataHandlerUtil.getStringValue(xcn.getGivenName());
            valmiddle = Hl7DataHandlerUtil.getStringValue(xcn.getSecondAndFurtherGivenNamesOrInitialsThereof());
            valfamily = Hl7DataHandlerUtil.getStringValue(xcn.getFamilyName());
            valsuffix = Hl7DataHandlerUtil.getStringValue(xcn.getSuffixEgJRorIII());

        }
        else if (value instanceof PPN) {
            PPN ppn = (PPN) value;
            valprefix = Hl7DataHandlerUtil.getStringValue(ppn.getPrefixEgDR());
            valfirst = Hl7DataHandlerUtil.getStringValue(ppn.getGivenName());
            valmiddle = Hl7DataHandlerUtil.getStringValue(ppn.getSecondAndFurtherGivenNamesOrInitialsThereof());
            valfamily = Hl7DataHandlerUtil.getStringValue(ppn.getFamilyName());
            valsuffix = Hl7DataHandlerUtil.getStringValue(ppn.getSuffixEgJRorIII());
        }

        if (valprefix != null) {
            sb.append(valprefix).append(" ");
        }
        if (valfirst != null) {
            sb.append(valfirst).append(" ");
        }
        if (valmiddle != null) {
            sb.append(valmiddle).append(" ");
        }
        if (valfamily != null) {
            sb.append(valfamily).append(" ");
        }
        if (valsuffix != null) {
            sb.append(valsuffix).append(" ");
        }
        String name = sb.toString();
        if (StringUtils.isNotBlank(name)) {
            return name.trim();
        }
        else return null;
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
        String code = getFHIRCode(val, MedicationRequest.MedicationRequestStatus.class);
        if (code != null) {
            return code;
        }
        return "unknown"; // when the HL7 status codes get mapped in v2toFhirMapping, we will return code. "unknown" is being returned because the hl7 message is not mapped to fhir yet.
    };

    public static final ValueExtractor<Object, SimpleCode> MEDREQ_CATEGORY_CODE_FHIR = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String code = getFHIRCode(val, MedicationRequestCategory.class);
        if (code != null) {
            MedicationRequestCategory category = MedicationRequestCategory.fromCode(code);
            return new SimpleCode(code, "http://terminology.hl7.org/CodeSystem/medicationrequest-category", category.getDisplay()); // category.getSystem() returns http://hl7.org/fhir/medication-request-category which is invalid so we hardcode the system with the proper url
        } else
            return null;
    };

    public static final ValueExtractor<Object, String> OBSERVATION_STATUS_CODE_FHIR = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        return getFHIRCode(val, ObservationStatus.class);
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
    
    public static final ValueExtractor<Object, String> SERVICE_REQUEST_STATUS = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        return getFHIRCode(val, ServiceRequestStatus.class);
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

    public static final ValueExtractor<Object, SimpleCode> ACT_ENCOUNTER_CODE_FHIR = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String code = getFHIRCode(val, V3ActCode.class);
        String version = Hl7DataHandlerUtil.getVersion(value);
        if(code != null){
            V3ActCode act = V3ActCode.fromCode(code);
            return new SimpleCode( code , act.getSystem(), act.getDisplay(), version);
        } else if (val != null) { // if code does not map but is present, use the "val" as the code
            return new SimpleCode(val, null,  null);
        }
        else return null;
    };

    public static final ValueExtractor<Object, String> IMMUNIZATION_STATUS_CODES = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        return getFHIRCode(val, ImmunizationStatus.class);
    };

    public static final ValueExtractor<Object, String> SPECIMEN_STATUS_CODE_FHIR = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        return getFHIRCode(val, SpecimenStatus.class);
    };

    public static final ValueExtractor<Object, String> DOC_REF_DOC_STATUS_CODE_FHIR = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        return getFHIRCode(val, CompositionStatus.class);
    };

    public static final ValueExtractor<Object, String> NAME_USE_CODE_FHIR = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        return getFHIRCode(val, NameUse.class);
    };


    public static final ValueExtractor<Object, String> ENCOUNTER_MODE_ARRIVAL_DISPLAY = (Object value) -> {
        return getFHIRCode(Hl7DataHandlerUtil.getStringValue(value), "EncounterModeOfArrivalDisplay");
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

    // Special case of a SYSTEM V2.  Identifiers allow unknown codes.
    // When an unknown code is detected, return a null so that the text is displayed instead.
    // Only a known code returns a coding.
    public static final ValueExtractor<Object, SimpleCode> CODING_SYSTEM_V2_IDENTIFIER = (Object value) -> {
        value = checkForAndUnwrapVariesObject(value);
        String table = Hl7DataHandlerUtil.getTableNumber(value);
        String code = Hl7DataHandlerUtil.getStringValue(value);
        if (table != null && code != null) {
            // Found table and a code. Try looking it up.
            SimpleCode coding = TerminologyLookup.lookup(table, code);
            // A non-null, non-empty value in display means a good code from lookup.
            if (coding != null && coding.getDisplay()!=null && !coding.getDisplay().isEmpty()) {
                return coding;
            } 
        }    
        // All other situations return null.  
        return null;
    };

    // Special case of a SYSTEM V2.  User-defined tables IS allow unknown codes.
    // When an unknown code is detected, return a coding with the code but no system, 
    // so the code is retained. Only a known code returns a system.
    // Compare function CODING_SYSTEM_V2_IDENTIFIER for slightly different behavior 
    public static final ValueExtractor<Object, SimpleCode> CODING_SYSTEM_V2_IS_USER_DEFINED_TABLE = (Object value) -> {
        value = checkForAndUnwrapVariesObject(value);
        String table = Hl7DataHandlerUtil.getTableNumber(value);
        String code = Hl7DataHandlerUtil.getStringValue(value);
        if (table != null && code != null) {
            // Found table and a code. Try looking it up.
            SimpleCode coding = TerminologyLookup.lookup(table, code);
            // A non-null, non-empty value in display means a good code from lookup.
            if (coding != null && coding.getDisplay()!=null && !coding.getDisplay().isEmpty()) {
                return coding;
            } else { 
                // Otherwise, must be a user-defined code, so return the code without a system
                return new SimpleCode(code, null, null, null) ;
            }
        } else if (table == null && code != null) {
            // Handle a code with unknown table
            return new SimpleCode(code, null, null, null);
        }
        // All other situations return null.  
        return null;
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
            return new SimpleCode(code, null, text);
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
        return getFHIRCode(val, AllergyIntoleranceCategory.class);
    };
    
    public static final ValueExtractor<Object, String> ALLERGY_INTOLERANCE_TYPE_CODE_FHIR = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        return getFHIRCode(val, AllergyIntoleranceType.class);
    };

    public static final ValueExtractor<Object, String> DOSE_SYSTEM = (Object value) -> {
        String val = Hl7DataHandlerUtil.getStringValue(value);
        String url = UrlLookup.getAssociatedUrl(val);
        if (url != null){
            return url;
        }
        else {
            if (val != null && val.length() > 0) {
                return "urn:id:" + val.replace(" ", "_");
            }
        }
        return null;
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
                LOGGER.warn("Value not valid UUID");
                LOGGER.debug("Value not valid UUID, value: {}", value, e);
                return null;
            }
        } else {
            LOGGER.info("Value for UUID is null");
            LOGGER.debug("Value for UUID is null, value: {}", value);
            return null;
        }
    }

    private static boolean isValidUUID(String val) {
        try {
            UUID.fromString(val);
            return true;
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Could not extract valid UUID - not a valid UUID");
            LOGGER.debug("Not a valid UUID", e);
            return false;
        }
    }

    public static String getFHIRCode(String hl7Value, Class<?> fhirConceptClassName) {
    	return getFHIRCode(hl7Value,fhirConceptClassName.getSimpleName());
    }

    public static String getFHIRCode(String hl7Value, String fhirMappingConceptName) {
        if (hl7Value != null) {
            Map<String, String> mapping = Hl7v2Mapping.getMapping(fhirMappingConceptName);
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