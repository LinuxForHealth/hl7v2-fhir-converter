/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.codesystems.V3MaritalStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.segments.util.PatientUtils;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

class Hl7PatientFHIRConversionTest {
    private HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();

    private static FHIRContext context = new FHIRContext(true, false);
    private static final Logger LOGGER = LoggerFactory.getLogger(Hl7PatientFHIRConversionTest.class);

    // Tests the PD1 segment with all supported message types.
    @ParameterizedTest
    @ValueSource(strings = { "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.6|\r",
            // "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A02|||2.6|\r",
            // "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A03|||2.6|\r",
            // "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A04|||2.6|\r",
            "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A08|||2.6|\r",
            // "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A28|||2.6|\r",
            // "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A31|||2.6|\r",
            "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A34|||2.6|\r",
            "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A40|||2.6|\r",
            // MDM messages are not tested here because they do not contain PD1 segments
            "MSH|^~\\&|hl7Integration|hl7Integration|||||OMP^O09|||2.6|\r",
            "MSH|^~\\&|hl7Integration|hl7Integration|||||ORM^O01|||2.6|\r",
            "MSH|^~\\&|hl7Integration|hl7Integration|||||ORU^R01|||2.6|\r",
            "MSH|^~\\&|hl7Integration|hl7Integration|||||RDE^O11|||2.6|\r",
            "MSH|^~\\&|hl7Integration|hl7Integration|||||RDE^O25|||2.6|\r",
            "MSH|^~\\&|hl7Integration|hl7Integration|||||VXU^V04|||2.6|\r",
    })
    void test_patient_additional_demographics(String msh) {
        String hl7message = msh
                + "PID|1||1234^^^AssigningAuthority^MR||TEST^PATIENT|\r"
                + "PD1|||Sample Family Practice^^2222|1111^LastName^ClinicianFirstName^^^^Title||||||||||||A|\r";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(ftv, hl7message);

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);
        Patient patient = getResourcePatient(patientResource.get(0));
        List<Reference> refs = patient.getGeneralPractitioner();
        assertThat(refs.size()).isPositive();

        List<Resource> practitionerResource = ResourceUtils.getResourceList(e, ResourceType.Practitioner);
        assertThat(practitionerResource).hasSize(1);
        Practitioner doc = getResourcePractitioner(practitionerResource.get(0));
        String lastName = doc.getName().get(0).getFamily();
        assertThat(lastName).isEqualTo("LastName");
    }

    /**
     * In order to generate messageHeader resource, MSH should have MSH.24.2 as this is required
     * attribute for source attribute, and source is required for MessageHeader resource.
     * 
     * 
     */

    @Test
    void patient_deceased_conversion_test1() {

        String patientMsgDeceasedEmpty = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA||||\n";
        String patientMsgNotDeadBooleanN = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA||||N\n";
        String patientMsgDeceasedDateOnlyYYYY = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA|||2006|\n";
        String patientMsgDeceasedDateOnlyYYYYMM = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA|||200611|\n";

        Patient patientObjDeceasedEmpty = PatientUtils.createPatientFromHl7Segment(ftv, patientMsgDeceasedEmpty);
        assertThat(patientObjDeceasedEmpty.hasDeceased()).isFalse();
        assertThat(patientObjDeceasedEmpty.hasDeceasedBooleanType()).isFalse();
        assertThat(patientObjDeceasedEmpty.hasDeceasedDateTimeType()).isFalse();

        Patient patientObjNotDeadBooleanN = PatientUtils.createPatientFromHl7Segment(ftv, patientMsgNotDeadBooleanN);
        assertThat(patientObjNotDeadBooleanN.hasDeceased()).isTrue();
        assertThat(patientObjNotDeadBooleanN.hasDeceasedBooleanType()).isTrue();
        assertThat(patientObjNotDeadBooleanN.getDeceasedBooleanType().booleanValue()).isFalse();

        Patient patientObjDeceasedDateOnlyYYYY = PatientUtils
                .createPatientFromHl7Segment(ftv, patientMsgDeceasedDateOnlyYYYY);
        assertThat(patientObjDeceasedDateOnlyYYYY.hasDeceased()).isTrue();
        assertThat(patientObjDeceasedDateOnlyYYYY.hasDeceasedDateTimeType()).isTrue();
        assertThat(patientObjDeceasedDateOnlyYYYY.hasDeceasedBooleanType()).isFalse();
        assertThat(patientObjDeceasedDateOnlyYYYY.getDeceasedDateTimeType().asStringValue()).isEqualTo("2006");

        Patient patientObjDeceasedDateOnlyYYYYMM = PatientUtils
                .createPatientFromHl7Segment(ftv, patientMsgDeceasedDateOnlyYYYYMM);
        assertThat(patientObjDeceasedDateOnlyYYYYMM.hasDeceased()).isTrue();
        assertThat(patientObjDeceasedDateOnlyYYYYMM.hasDeceasedDateTimeType()).isTrue();
        assertThat(patientObjDeceasedDateOnlyYYYYMM.hasDeceasedBooleanType()).isFalse();
        assertThat(patientObjDeceasedDateOnlyYYYYMM.getDeceasedDateTimeType().asStringValue()).isEqualTo("2006-11");

        // More related tests in patient_deceased_conversion_test2
    }

    @Test
    void patient_deceased_conversion_test2() {

        String patientMsgDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA|||20061120115930+0100|\n";

        String patientMsgDeceasedBooleanYOnly = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA||||Y\n";
        String patientMsgDeceasedDateAndBooleanY = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^|Mother^Micky|20060504|M|Alias^Alias|2106-3^White^ HL70005|12345 testing ave^^Minneapolis^MN^55407^^^^MN053|USAA|^PRN^^^PH^555^5555555|^PRN^^^PH^555^666666|english|married|bhuddist|1234567_account|111-22-3333|||2186-5^not Hispanic or Latino^CDCREC|Born in USA|Y|2|USA|||20061120|Y\n";

        Patient patientObjDeceasedBooleanYOnly = PatientUtils
                .createPatientFromHl7Segment(ftv, patientMsgDeceasedBooleanYOnly);
        assertThat(patientObjDeceasedBooleanYOnly.hasDeceased()).isTrue();
        assertThat(patientObjDeceasedBooleanYOnly.hasDeceasedDateTimeType()).isFalse();
        assertThat(patientObjDeceasedBooleanYOnly.hasDeceasedBooleanType()).isTrue();
        assertThat(patientObjDeceasedBooleanYOnly.getDeceasedBooleanType().booleanValue()).isTrue();

        Patient patientObjDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ = PatientUtils
                .createPatientFromHl7Segment(ftv, patientMsgDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ);
        assertThat(patientObjDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ.hasDeceased()).isTrue();
        assertThat(patientObjDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ.hasDeceasedDateTimeType()).isTrue();
        assertThat(patientObjDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ.hasDeceasedBooleanType()).isFalse();
        assertThat(patientObjDeceasedDateOnlyYYYYMMDDHHMMSSZZZZ.getDeceasedDateTimeType().asStringValue())
                .isEqualTo("2006-11-20T11:59:30+01:00");

        Patient patientObjDeceasedDateAndBooleanY = PatientUtils
                .createPatientFromHl7Segment(ftv, patientMsgDeceasedDateAndBooleanY);
        assertThat(patientObjDeceasedDateAndBooleanY.hasDeceased()).isTrue();
        assertThat(patientObjDeceasedDateAndBooleanY.hasDeceasedDateTimeType()).isTrue();
        assertThat(patientObjDeceasedDateAndBooleanY.hasDeceasedBooleanType()).isFalse();
        assertThat(patientObjDeceasedDateAndBooleanY.getDeceasedDateTimeType().asStringValue()).isEqualTo("2006-11-20"); //DateUtil.formatToDate
    }

    @Test
    void patient_multiple_birth_conversion_test() {

        /**
         * Simplified logic for multiple birth
         * 
         * Y + number = number
         * N + number = N
         * Y + blank = Y
         * N + blank = N
         * blank + number = number
         * blank + blank = nothing.
         * 
         */

        String patientMsgEmptyMultiple = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^||||||||||||||||||Born in USA|||USA||||\n";
        Patient patientObjEmptyMultiple = PatientUtils.createPatientFromHl7Segment(ftv, patientMsgEmptyMultiple);
        assertThat(patientObjEmptyMultiple.hasMultipleBirth()).isFalse();
        assertThat(patientObjEmptyMultiple.hasMultipleBirthIntegerType()).isFalse();
        assertThat(patientObjEmptyMultiple.hasMultipleBirthBooleanType()).isFalse();

        String patientMsgMultipleN = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^||||||||||||||||||Born in USA|N||USA||||\n";
        Patient patientObjMultipleN = PatientUtils.createPatientFromHl7Segment(ftv, patientMsgMultipleN);
        assertThat(patientObjMultipleN.hasMultipleBirth()).isTrue();
        assertThat(patientObjMultipleN.hasMultipleBirthIntegerType()).isFalse();
        assertThat(patientObjMultipleN.hasMultipleBirthBooleanType()).isTrue();
        assertThat(patientObjMultipleN.getMultipleBirthBooleanType().booleanValue()).isFalse();

        String patientMsgMultipleNumberOnly = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^||||||||||||||||||Born in USA||2|USA||||\n";
        // A number when the boolean is missing presumes the number has meaning.  An integer is created.
        Patient patientObjMultipleNumberOnly = PatientUtils.createPatientFromHl7Segment(ftv,
                patientMsgMultipleNumberOnly);
        assertThat(patientObjMultipleNumberOnly.hasMultipleBirth()).isTrue();
        assertThat(patientObjMultipleNumberOnly.hasMultipleBirthIntegerType()).isTrue();
        assertThat(patientObjMultipleNumberOnly.hasMultipleBirthBooleanType()).isFalse();
        assertThat(patientObjMultipleNumberOnly.getMultipleBirthIntegerType().asStringValue()).isEqualTo("2");

        String patientMsgMultipleBooleanYOnly = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^||||||||||||||||||Born in USA|Y||USA||||\n";
        Patient patientObjMultipleBooleanYOnly = PatientUtils
                .createPatientFromHl7Segment(ftv, patientMsgMultipleBooleanYOnly);
        assertThat(patientObjMultipleBooleanYOnly.hasMultipleBirth()).isTrue();
        assertThat(patientObjMultipleBooleanYOnly.hasMultipleBirthIntegerType()).isFalse();
        assertThat(patientObjMultipleBooleanYOnly.hasMultipleBirthBooleanType()).isTrue();
        assertThat(patientObjMultipleBooleanYOnly.getMultipleBirthBooleanType().booleanValue()).isTrue();

        String patientMsgMultipleNumberAndBooleanY = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^||||||||||||||||||Born in USA|Y|3|USA||||\n";
        Patient patientObjMultipleNumberAndBooleanY = PatientUtils
                .createPatientFromHl7Segment(ftv, patientMsgMultipleNumberAndBooleanY);
        assertThat(patientObjMultipleNumberAndBooleanY.hasMultipleBirth()).isTrue();
        assertThat(patientObjMultipleNumberAndBooleanY.hasMultipleBirthIntegerType()).isTrue();
        assertThat(patientObjMultipleNumberAndBooleanY.hasMultipleBirthBooleanType()).isFalse();
        assertThat(patientObjMultipleNumberAndBooleanY.getMultipleBirthIntegerType().asStringValue()).isEqualTo("3"); //DateUtil.formatToDate

        String patientMsgMultipleN16 = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^||||||||||||||||||Born in USA|N|16|USA||||\n";
        Patient patientObjMultipleN16 = PatientUtils.createPatientFromHl7Segment(ftv, patientMsgMultipleN16);
        assertThat(patientObjMultipleN16.hasMultipleBirth()).isTrue();
        assertThat(patientObjMultipleN16.hasMultipleBirthIntegerType()).isFalse();
        assertThat(patientObjMultipleN16.hasMultipleBirthBooleanType()).isTrue();
        assertThat(patientObjMultipleN16.getMultipleBirthBooleanType().booleanValue()).isFalse();
    }

    @Test
    void patient_use_name_conversion_test() {
        String patientUseName = "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r"
                +
                "PID|1||PA123456^^^MYEMR^MR||TestPatient^John^M^^^^B|MILLER^MARTHA^G^^^^M|20140227|M||2106-3^WHITE^CDCREC|1234 W FIRST ST^^BEVERLY HILLS^CA^90210^^H||^PRN^PH^^^555^5555555||ENG^English^HL70296|||||||2186-5^ not Hispanic or Latino^CDCREC||Y|2\r";

        Patient patientObjUsualName = PatientUtils.createPatientFromHl7Segment(ftv, patientUseName);

        java.util.List<org.hl7.fhir.r4.model.HumanName> name = patientObjUsualName.getName();
        HumanName.NameUse useName = name.get(0).getUse();
        assertThat(useName).isEqualTo(HumanName.NameUse.OFFICIAL);

    }

    @Test
    void patientNameTest() {
        String patientHasMiddleName = "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r"
                // PID 5 fields (name) are extracted and tested
                + "PID|1||PA123456^^^MYEMR^MR||JONES^GEORGE^Q^III^MR^^B||||||||||||||||||||\r";

        Patient patientObjUsualName = PatientUtils.createPatientFromHl7Segment(ftv, patientHasMiddleName);

        java.util.List<org.hl7.fhir.r4.model.HumanName> name = patientObjUsualName.getName();
        List<StringType> givenName = name.get(0).getGiven();
        List<StringType> suffixes = name.get(0).getSuffix();
        assertThat(suffixes).hasSize(1);
        List<StringType> prefixes = name.get(0).getPrefix();
        assertThat(prefixes).hasSize(1);
        String fullName = name.get(0).getText();
        assertThat(prefixes.get(0).toString()).hasToString("MR");
        assertThat(givenName.get(0).toString()).hasToString("GEORGE");
        assertThat(givenName.get(1).toString()).hasToString("Q");
        assertThat(suffixes.get(0).toString()).hasToString("III");
        assertThat(fullName).isEqualTo("MR GEORGE Q JONES III");

    }

    @Test
    void patientGenderTest() {
        String patientEmptyGenderField = "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r"
                +
                "PID|0010||||DOE^JOHN^A^|||||||\r";

        String patientWithGenderField = "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r"
                +
                "PID|0010||||DOE^JOHN^A^|||M|||||||\r";

        Patient patientObjNoGender = PatientUtils.createPatientFromHl7Segment(ftv, patientEmptyGenderField);
        Enumerations.AdministrativeGender gender = patientObjNoGender.getGender();
        assertThat(gender).isNull();

        Patient patientObjGender = PatientUtils.createPatientFromHl7Segment(ftv, patientWithGenderField);
        Enumerations.AdministrativeGender gen = patientObjGender.getGender();
        assertThat(gen).isNotNull().isEqualTo(Enumerations.AdministrativeGender.MALE);
    }

    @Test
    void patientMaritalStatusTest() {
        String marriedPatientWithVersion = "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r"
                +
                "PID|1||12345678^^^MRN||TestPatient^Jane|||||||||||M^^^^^^47||||||\r";

        String singlePatientWithVersionAndOriginalText = "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r"
                +
                "PID|1||12345678^^^MRN||TestPatient^Jane|||||||||||S^unmarried^^^^^1.1||||||\r";

        Patient patientObjMarried = PatientUtils.createPatientFromHl7Segment(ftv, marriedPatientWithVersion);
        assertThat(patientObjMarried.hasMaritalStatus()).isTrue();
        assertThat(patientObjMarried.getMaritalStatus().hasText()).isFalse();
        assertThat(patientObjMarried.getMaritalStatus().getCoding()).hasSize(1);
        Coding coding = patientObjMarried.getMaritalStatus().getCodingFirstRep();
        assertThat(coding.getDisplay()).isEqualTo(V3MaritalStatus.M.getDisplay());
        assertThat(coding.getSystem()).isEqualTo(V3MaritalStatus.M.getSystem());
        assertThat(coding.getVersion()).isEqualTo("47");

        Patient patientObjMarriedAltText = PatientUtils
                .createPatientFromHl7Segment(ftv, singlePatientWithVersionAndOriginalText);
        assertThat(patientObjMarriedAltText.hasMaritalStatus()).isTrue();
        assertThat(patientObjMarriedAltText.getMaritalStatus().getText()).isEqualTo("unmarried");
        assertThat(patientObjMarriedAltText.getMaritalStatus().getCoding()).hasSize(1);
        coding = patientObjMarriedAltText.getMaritalStatus().getCodingFirstRep();
        assertThat(coding.getDisplay()).isEqualTo(V3MaritalStatus.S.getDisplay());
        assertThat(coding.getSystem()).isEqualTo(V3MaritalStatus.S.getSystem());
        assertThat(coding.getVersion()).isEqualTo("1.1");

    }

    @Test
    void patientCommunicationLanguage() {

        String patientSpeaksEnglishWithSystem = "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r"
                +
                "PID|1||PA123456^^^MYEMR^MR||DOE^JOHN|||M|||||||ENG^English^HL70296|||||||||Y|2\r";

        String patientEnglishNoSystem = //NO coding system given in the CWE
                "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r"
                        +
                        "PID|1||PA123456^^^MYEMR^MR||DOE^JANE|||M|||||||ENG^English|||||||||Y|2\r";

        String patientEnglishCodeOnly = //NO coding system given in the CWE
                "MSH|^~\\&|MyEMR|DE-000001| |CAIRLO|20160701123030-0700||VXU^V04^VXU_V04|CA0001|P|2.6|||ER|AL|||||Z22^CDCPHINVS|DE-000001\r"
                        +
                        "PID|1||PA123456^^^MYEMR^MR||DOE^JANE|||M|||||||ENG|||||||||Y|2\r";

        Patient patientObjEnglish = PatientUtils.createPatientFromHl7Segment(ftv, patientSpeaksEnglishWithSystem);
        assertThat(patientObjEnglish.hasCommunication()).isTrue();
        assertThat(patientObjEnglish.getCommunication().get(0).getPreferred()).isTrue();
        assertThat(patientObjEnglish.getCommunication()).hasSize(1);
        Patient.PatientCommunicationComponent cc = patientObjEnglish.getCommunication().get(0);
        assertThat(cc.getPreferred()).isTrue();
        assertThat(cc.getLanguage().getText()).isEqualTo("English");
        Coding code = cc.getLanguage().getCodingFirstRep();
        assertThat(code.getCode()).isEqualTo("ENG");
        assertThat(code.getSystem()).isEqualTo("urn:id:v2-0296");
        assertThat(code.getDisplay()).isEqualTo("English");

        Patient patientObjNoSystem = PatientUtils.createPatientFromHl7Segment(ftv, patientEnglishNoSystem);
        assertThat(patientObjNoSystem.hasCommunication()).isTrue();
        assertThat(patientObjNoSystem.getCommunication().get(0).getPreferred()).isTrue();
        assertThat(patientObjNoSystem.getCommunication()).hasSize(1);
        Patient.PatientCommunicationComponent ccNoCode = patientObjNoSystem.getCommunication().get(0);
        assertThat(ccNoCode.getPreferred()).isTrue();
        assertThat(ccNoCode.getLanguage().getText()).isEqualTo("English");
        Coding codeNo = ccNoCode.getLanguage().getCodingFirstRep();
        assertThat(codeNo.getCode()).isEqualTo("ENG");
        assertThat(code.getDisplay()).isEqualTo("English");
        assertThat(codeNo.hasDisplay()).isTrue();

        Patient patientObjCodeOnly = PatientUtils.createPatientFromHl7Segment(ftv, patientEnglishCodeOnly);
        assertThat(patientObjCodeOnly.hasCommunication()).isTrue();
        assertThat(patientObjCodeOnly.getCommunication().get(0).getPreferred()).isTrue();
        assertThat(patientObjCodeOnly.getCommunication()).hasSize(1);
        Patient.PatientCommunicationComponent ccCodeOnly = patientObjCodeOnly.getCommunication().get(0);
        assertThat(ccCodeOnly.getPreferred()).isTrue();
        assertThat(ccCodeOnly.getLanguage().hasText()).isFalse();
        Coding coding = ccCodeOnly.getLanguage().getCodingFirstRep();
        assertThat(coding.getCode()).isEqualTo("ENG");
        assertThat(coding.getSystem()).isNull();
        assertThat(coding.getDisplay()).isNull();

    }

    private Patient getResourcePatient(Resource resource) {
        String s = context.getParser().encodeResourceToString(resource);
        Class<? extends IBaseResource> klass = Patient.class;
        return (Patient) context.getParser().parseResource(klass, s);
    }

    private static Practitioner getResourcePractitioner(Resource resource) {
        String s = context.getParser().encodeResourceToString(resource);
        Class<? extends IBaseResource> klass = Practitioner.class;
        return (Practitioner) context.getParser().parseResource(klass, s);
    }

    private void validate_lineage_json(List<Extension> extensions, String messageType, boolean millis) {
        assertThat(extensions.size()).isEqualTo(7);
        for (Extension extension : extensions) {
            // Get the URL
            String url = extension.getUrl();
            LOGGER.debug("URL:" + url);

            // Get the value
            String value = extension.getValue().toString();

            //If the value is a codeable concept and not a simple value - parse the value out of the codeable concept.
            if (value.indexOf("CodeableConcept") >= 0) {
                String codeableConceptValue = extension.getValue().getChildByName("coding").getValues().get(0)
                        .getNamedProperty("code").getValues().get(0).toString();
                LOGGER.debug("CodeableConceptValue:" + codeableConceptValue.toString());
                value = codeableConceptValue.toString();
            }
            //Get the Name from the URL
            String name = url.substring(url.lastIndexOf("/") + 1, url.length());
            LOGGER.debug("Name:" + name);
            LOGGER.debug("Value:" + value);

            LOGGER.debug("Message Type:" + messageType);

            String[] messageParts = messageType.split("\\^");

            // test value based off the name.
            switch (name) {
                case "source-event-timestamp":
                    if (millis)
                        assertThat(value).isEqualTo("DateTimeType[2006-09-15T21:00:00.567+08:00]");
                    else
                        assertThat(value).isEqualTo("DateTimeType[2006-09-15T21:00:00+08:00]");
                    break;
                case "source-record-id":
                    assertThat(value).isEqualTo("1473973200100600");
                    break;
                case "source-data-model-version":
                    assertThat(value).isEqualTo("2.3");
                    break;
                case "process-client-id":
                    assertThat(value).isEqualTo("SendingApplication");
                    break;
                case "source-event-trigger":
                    assertThat(value).isEqualTo(messageParts[1]);
                    break;
                case "source-record-type":
                    assertThat(value).isEqualTo(messageParts[0]);
                    break;
                case "process-timestamp":
                    // this is the current time the message was converted
                    assertThat(value).contains("DateTimeType");
                    break;
                default:
                    // this shouldn't happen
                    LOGGER.debug("Not found");
                    Assertions.fail();
                    break;
            }
        }
    }

    private void validate_data_lineage(String hl7message, String messageType) {
        validate_data_lineage(hl7message, messageType, false);
    }

    private void validate_data_lineage(String hl7message, String messageType, boolean millis) {
        String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.debug(json);

        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Get bundle meta extensions *not using these currently*
        // Meta bundleMeta = b.getMeta();
        // List<Extension> bundleMetaExtensions = bundleMeta.getExtension();

        LOGGER.debug("Found {} resources.", e.stream().count());

        e.stream().forEach(bec -> {
            LOGGER.debug("Validating " + bec.getResource().getResourceType());
            Meta meta = bec.getResource().getMeta();
            List<Extension> extensions = meta.getExtension();
            LOGGER.debug("Found " + extensions.size() + " meta extensions");
            validate_lineage_json(extensions, messageType, millis);
        });
    }

    // Tests Data lineage for a ORU^R01 message
    @Test
    void verify_data_lineage_ORU() {

        String hl7message = "MSH|^~\\&|SendingApplication|Sending^Facility|Receiving-Application|ReceivingFacility|20060915210000||ORU^R01|1473973200100600|P|2.3|||NE|NE\n"
                + "PID|1||1234^^^AssigningAuthority^MR||TEST^PATIENT|\n"
                + "PD1|||Sample Family Practice^^2222|1111^LastName^ClinicianFirstName^^^^Title||||||||||||A|";
        validate_data_lineage(hl7message, "ORU^R01");
    }

    // Tests Data lineage for a message with milliseconds in MSH-7
    @Test
    void verify_data_lineage_milliseconds_timestamp() {
        String hl7message = "MSH|^~\\&|SendingApplication|Sending^Facility|Receiving-Application|ReceivingFacility|20060915210000.567||ORU^R01|1473973200100600|P|2.3|||NE|NE\n"
                + "PID|1||1234^^^AssigningAuthority^MR||TEST^PATIENT|\n"
                + "PD1|||Sample Family Practice^^2222|1111^LastName^ClinicianFirstName^^^^Title||||||||||||A|";
        validate_data_lineage(hl7message, "ORU^R01", true);
    }

    // Tests Data lineage for a ADT^A01 message
    @Test
    void verify_data_lineage_ADT() {
        String hl7message = "MSH|^~\\&|SendingApplication|hl7Integration|||20060915210000||ADT^A01|1473973200100600||2.3|\r"
                + "EVN|A01|20130617154644\r"
                + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
                + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
                + "PV1|1|E|Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r"
                + "OBX|1|TX|1234||First line: ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^|\r";
        validate_data_lineage(hl7message, "ADT^A01");
    }

    // Tests Data lineage for a VXU^V04 message
    @Test
    void verify_data_lineage_VXU() {
        String hl7message = "MSH|^~\\&|SendingApplication|RI88140101|KIDSNET_IFL|RIHEALTH|20060915210000||VXU^V04|1473973200100600|P|2.3|||NE|AL||||||RI543763\r"
                + "PID|1||432155^^^^MR||Patient^Johnny^New^^^^L|Smith^Sally|20130414|M||2106-3^White^HL70005|123 Any St^^Somewhere^WI^54000^^M\r"
                + "NK1|1|Patient^Sally|MTH^mother^HL70063|123 Any St^^Somewhere^WI^54000^^M|^PRN^PH^^^608^5551212|||||||||||19820517||||eng^English^ISO639\r"
                + "ORC|RE||197027|||||||^Clerk^Myron||MD67895^Pediatric^MARY^^^^MD^^RIA|||||RI2050\r"
                + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|0.5|ML^^ISO+||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX|||CP|A\r"
                + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r"
                + "OBX|1|CE|64994-7^vaccine fund pgm elig cat^LN|1|V02^VFC eligible Medicaid/MedicaidManaged Care^HL70064||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r"
                + "OBX|2|CE|30956-7^Vaccine Type^LN|2|48^HIB PRP-T^CVX||||||F|||20130531\r"
                + "OBX|3|TS|29768-9^VIS Publication Date^LN|2|19981216||||||F|||20130531\r"
                + "OBX|4|TS|59785-6^VIS Presentation Date^LN|2|20130531||||||F|||20130531\r"
                + "OBX|5|ST|48767-8^Annotation^LN|2|Some text from doctor||||||F|||20130531\r";
        validate_data_lineage(hl7message, "VXU^V04");
    }

    @ParameterizedTest
    @ValueSource(strings={"PRN", "VHN", "WPN", "PRS", ""})
    void testPatientContactPointUse(String use) {
        String hl7message = "MSH|^~\\&|SendingApplication|RI88140101|KIDSNET_IFL|RIHEALTH|20060915210000||VXU^V04|1473973200100600|P|2.3|||NE|AL||||||RI543763\r"
                + "PID||B987654^^^CLINIC^ABC|B987655^^^CLINIC^XYZ|B987656^^^CLINIC^PQR|Doe^John^Allen^Jr.^Mr.^L||19800101|M|||123 Main St^Apt 4B^Metropolis^NY^10001^USA^H^NY001||(555)555-1234^"+use+"^BP\r";

        Patient patient = PatientUtils.createPatientFromHl7Segment(ftv, hl7message);
        assertThat(patient.hasTelecom()).isTrue();
        assertThat(patient.getTelecom()).hasSize(1);
        if (use.isEmpty()) {
            assertThat(patient.getTelecom().get(0).hasUse()).isFalse();
        } else {
            assertThat(patient.getTelecom().get(0).hasUse()).isTrue();
        }
        switch (use) {
            case "PRN":
            case "VHN":
                assertThat(patient.getTelecom().get(0).getUse()).isEqualTo(ContactPoint.ContactPointUse.HOME);
                break;
            case "WPN":
                assertThat(patient.getTelecom().get(0).getUse()).isEqualTo(ContactPoint.ContactPointUse.WORK);
                break;
            case "PRS":
                assertThat(patient.getTelecom().get(0).getUse()).isEqualTo(ContactPoint.ContactPointUse.MOBILE);
                break;
            default:
                assertThat(patient.getTelecom().get(0).getUse()).isNull();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "PH", "FX", "CP", "BP", "Internet", "X.400", "MD", "TDD", "TTY", "SAT", ""})
    void testPatientContactPointSystem(String system) {
        String hl7message = "MSH|^~\\&|SendingApplication|RI88140101|KIDSNET_IFL|RIHEALTH|20060915210000||VXU^V04|1473973200100600|P|2.3|||NE|AL||||||RI543763\r"
                + "PID||B987654^^^CLINIC^ABC|B987655^^^CLINIC^XYZ|B987656^^^CLINIC^PQR|Doe^John^Allen^Jr.^Mr.^L||19800101|M|||123 Main St^Apt 4B^Metropolis^NY^10001^USA^H^NY001||(555)555-1234^PRN^"+system+"\r";
        Patient patient = PatientUtils.createPatientFromHl7Segment(ftv, hl7message);
        assertThat(patient.hasTelecom()).isTrue();
        assertThat(patient.getTelecom()).hasSize(1);
        if(system.isEmpty()) {
            assertThat(patient.getTelecom().get(0).hasSystem()).isFalse();
        } else {
            assertThat(patient.getTelecom().get(0).hasSystem()).isTrue();
        }
        switch (system) {
            case "PH":
            case "CP":
                assertThat(patient.getTelecom().get(0).getSystem()).isEqualTo(ContactPoint.ContactPointSystem.PHONE);
                break;
            case "FX":
                assertThat(patient.getTelecom().get(0).getSystem()).isEqualTo(ContactPoint.ContactPointSystem.FAX);
                break;
            case "BP":
                assertThat(patient.getTelecom().get(0).getSystem()).isEqualTo(ContactPoint.ContactPointSystem.PAGER);
                break;
            case "Internet":
            case "X.400":
                assertThat(patient.getTelecom().get(0).getSystem()).isEqualTo(ContactPoint.ContactPointSystem.EMAIL);
                break;
            case "MD":
            case "SAT":
            case "TTY":
            case "TDD":
                assertThat(patient.getTelecom().get(0).getSystem()).isEqualTo(ContactPoint.ContactPointSystem.OTHER);
                break;
            default:
                assertThat(patient.getTelecom().get(0).getSystem()).isNull();
                break;
        }
    }
}
