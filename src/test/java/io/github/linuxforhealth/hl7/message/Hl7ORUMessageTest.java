/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.segments.util.DatatypeUtils;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

public class Hl7ORUMessageTest {
    private static FHIRContext context = new FHIRContext();
    private static final Logger LOGGER = LoggerFactory.getLogger(Hl7ORUMessageTest.class);
    private static final ConverterOptions OPTIONS = new Builder().withValidateResource().build();
    private static final ConverterOptions OPTIONS_PRETTYPRINT = new Builder()
            .withBundleType(BundleType.COLLECTION)
            .withValidateResource()
            .withPrettyPrint()
            .build();

    // DiagnosticReports are only created from ORU messages so the test of DiagnosticReport content is included in this test module.

    @Test
    // Test the minimum scenario where the least possible segments and resource info are provided in the message
    public void test_oru_patient_diagReport() throws IOException {
        String hl7message = "MSH|^~\\\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|ORU^R01|MSGID000005|T|2.6\r"
                + "PID||45483|45483||SMITH^SUZIE^||20160813|M|||123 MAIN STREET^^SCHENECTADY^NY^12345||(123)456-7890|||||^^^T||||||||||||\r"
                + "OBR|1||986^IA PHIMS Stage^2.16.840.1.114222.4.3.3.5.1.2^ISO|1051-2^New Born Screening^LN|||20151009173644|||||||||||||002|||||F|||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^|||||\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Verify correct resources created
        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> diagnosticReport = e.stream()
                .filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(diagnosticReport).hasSize(1);

        List<Resource> servReqResource = e.stream()
                .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(servReqResource).hasSize(1);

        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(3);

        ///////////////////////////////////////////
        // Now confirm content of the diagnosticReport because we don't have separate tests for DiagnosticReport
        ///////////////////////////////////////////
        DiagnosticReport diag = ResourceUtils.getResourceDiagnosticReport(diagnosticReport.get(0), context);
        assertThat(diag.getStatus().toCode()).isEqualTo("final"); // Verify status from OBR.25
        assertThat(diag.getCategory().size()).isZero(); // Verify category from OBR.24

        // Verify code from OBR.4
        assertThat(diag.hasCode()).isTrue();
        List<Coding> codings = diag.getCode().getCoding();
        assertThat(codings.size()).isEqualTo(1);
        Coding coding = codings.get(0);
        assertThat(coding.hasDisplay()).isTrue();
        assertThat(coding.getDisplay()).hasToString("New Born Screening");
        assertThat(coding.hasCode()).isTrue();
        assertThat(coding.getCode()).hasToString("1051-2");
        assertThat(coding.hasSystem()).isTrue();
        assertThat(coding.getSystem()).hasToString("http://loinc.org");

        assertThat(diag.getEncounter().isEmpty()).isTrue(); // Verify encounter reference
        assertThat(diag.getSubject().isEmpty()).isFalse(); // Verify subject reference

        // Verify effectiveDateTime from OBR.7 and OBR.8
        assertThat(diag.getEffectiveDateTimeType().asStringValue()).isEqualTo("2015-10-09T17:36:44+08:00"); // This also verifies the type, confirming effectiveDateTime was set rather than effectivePeriod

        assertThat(diag.getIssued()).isNull(); // Verify issued from OBR.22
        assertThat(diag.getResultsInterpreter()).isEmpty(); // Verify resultsInterpreter from OBR.32
        assertThat(diag.getBasedOn()).hasSize(1); // Verify basedOn is ref to the ServiceRequest created for ORC or OBR
        assertThat(diag.getBasedOn().get(0).getReference().substring(0, 15)).isEqualTo("ServiceRequest/");

        assertThat(diag.getSpecimen()).isEmpty(); // Verify specimen reference
        assertThat(diag.getResult()).isEmpty(); // Verify result reference

        // Verify presentedForm from OBX of type TX - In this case no attachments created since there are no OBX with type TX
        List<Attachment> attachments = diag.getPresentedForm();
        Assertions.assertEquals(0, attachments.size(), "Unexpected number of attachments");

    }

    @Test
    // Test multiple OBX (non TX) put into Observation resources. 
    // Observation resources are created instead of attachments in the diagReport because they are not type TX.
    public void test_oru_with_multiple_observations() throws IOException {
        String hl7message = "MSH|^~\\\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|ORU^R01|MSGID000005|T|2.6\r"
                + "PID||45483|45483||SMITH^SUZIE^||20160813|M|||123 MAIN STREET^^SCHENECTADY^NY^12345||(123)456-7890|||||^^^T||||||||||||\r"
                // OBR.24 creates a DiagnosticReport.category
                + "OBR|1||986^IA PHIMS Stage^2.16.840.1.114222.4.3.3.5.1.2^ISO|1051-2^New Born Screening^LN|||20151009173644|||||||||||||002||||CUS|F|||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^||||3065^Mahoney^Paul^J|\r"
                + "OBX|1|ST|TS-F-01-002^Endocrine Disorders^L||obs report||||||F\r"
                + "OBX|2|ST|GA-F-01-024^Galactosemia^L||ECHOCARDIOGRAPHIC REPORT||||||F\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Verify correct resources created
        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> obsResource = e.stream()
                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(obsResource).hasSize(2);

        List<Resource> practitionerResource = e.stream()
                .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(practitionerResource).hasSize(1);

        List<Resource> diagnosticReport = e.stream()
                .filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(diagnosticReport).hasSize(1);

        List<Resource> servReqResource = e.stream()
                .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(servReqResource).hasSize(1);

        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(6);

        ///////////////////////////////////////////
        // Now confirm content of the diagnosticReport because we don't have separate tests for DiagnosticReport
        ///////////////////////////////////////////
        DiagnosticReport diag = ResourceUtils.getResourceDiagnosticReport(diagnosticReport.get(0), context);
        assertThat(diag.getStatus().toCode()).isEqualTo("final"); // Verify status from OBR.25
        assertThat(diag.getCategory().size()).isEqualTo(1); // Verify category from OBR.24
        DatatypeUtils.checkCommonCodeableConceptAssertions(diag.getCategoryFirstRep(), "CUS", "Cardiac Ultrasound",
                "http://terminology.hl7.org/CodeSystem/v2-0074", "CUS");

        // Verify code from OBR.4
        assertThat(diag.hasCode()).isTrue();
        List<Coding> codings = diag.getCode().getCoding();
        assertThat(codings.size()).isEqualTo(1);
        Coding coding = codings.get(0);
        assertThat(coding.hasDisplay()).isTrue();
        assertThat(coding.getDisplay()).hasToString("New Born Screening");
        assertThat(coding.hasCode()).isTrue();
        assertThat(coding.getCode()).hasToString("1051-2");
        assertThat(coding.hasSystem()).isTrue();
        assertThat(coding.getSystem()).hasToString("http://loinc.org");

        assertThat(diag.getEncounter().isEmpty()).isTrue(); // Verify encounter reference
        assertThat(diag.getSubject().isEmpty()).isFalse(); // Verify subject reference

        // Verify effectiveDateTime from OBR.7 and OBR.8
        assertThat(diag.getEffectiveDateTimeType().asStringValue())
                .isEqualTo("2015-10-09T17:36:44+08:00"); // This also verifies the type, confirming effectiveDateTime was set rather than effectivePeriod

        assertThat(diag.getIssued()).isNull(); // Verify issued from OBR.22
        assertThat(diag.getResultsInterpreter()).hasSize(1); // Verify resultsInterpreter from OBR.32
        assertThat(diag.getBasedOn()).hasSize(1); // Verify basedOn is ref to the ServiceRequest created for ORC or OBR
        assertThat(diag.getBasedOn().get(0).getReference().substring(0, 15)).isEqualTo("ServiceRequest/");
        assertThat(diag.getSpecimen()).isEmpty(); // Verify specimen reference

        // Verify result reference
        List<Reference> obsRef = diag.getResult();
        assertThat(obsRef.isEmpty()).isFalse();
        assertThat(obsRef).hasSize(2);
        assertThat(obsRef.get(0).isEmpty()).isFalse();
        assertThat(obsRef.get(1).isEmpty()).isFalse();

        // Verify presentedForm from OBX of type TX - In this case no attachments created because the OBX of type TX have ids.
        List<Attachment> attachments = diag.getPresentedForm();
        Assertions.assertEquals(0, attachments.size(), "Unexpected number of attachments");

        ////////////////////////////////////
        // Verify the references that aren't covered in Observation tests
        ////////////////////////////////////
        for (Resource res : obsResource) {
            // Verify encounter reference is not set
            Observation obs = (Observation) res;
            assertThat(obs.getEncounter().isEmpty()).isTrue();

            //Verify subject reference to Patient exists
            assertThat(obs.getSubject().isEmpty()).isFalse();
            assertThat(obs.getSubject().getReference().substring(0, 8)).isEqualTo("Patient/");
        }

    }

    @Test
    public void test_orur01_with_encounter_present() throws IOException {
        String hl7message = "MSH|^~\\&|PROSLOV|MYHOSPITAL|WHIA|IBM|20180520230000||ORU^R01|MSGID006552|T|2.6\r"
                + "PID|1||000065432^^^MRN^MR||ROSTENKOWSKI^BERNADETTE^||19840823|Female||1002-5|382 OTHERSTREET AVE^^PASADENA^LA^223343||4582143248||^French|S||53811||||U|||||||\r"
                + "PV1|1|O|||||9905^Adams^John|9906^Yellow^William^F|9907^Blue^Oren^J||||||||9908^Green^Mircea^||2462201|||||||||||||||||||||||||20180520230000\r"
                + "OBR|1||bbf1993ab|1122^Final Echocardiogram Report|||20180520230000|||||||||||||002|||||F|||550469^Tsadok550469^Janetary~660469^Merrit660469^Darren^F~770469^Das770469^Surjya^P~880469^Winter880469^Oscar^||||770469&Das770469&Surjya&P^^^6N^1234^A|\r"
                + "OBX|1|NM|2552^HRTRTMON|1|115||||||F|||20180520230000|||\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Verify that the right resources are created
        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = e.stream()
                .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> obsResource = e.stream()
                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(obsResource).hasSize(1);

        List<Resource> diagnosticReport = e.stream()
                .filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(diagnosticReport).hasSize(1);

        List<Resource> servReqResource = e.stream()
                .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(servReqResource).hasSize(1);

        List<Resource> practitionerResource = e.stream()
                .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(practitionerResource).hasSize(5);

        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(10);

        ///////////////////////////////////////////
        // Now confirm content of the diagnosticReport because we don't have separate tests for DiagnosticReport
        ///////////////////////////////////////////
        DiagnosticReport diag = ResourceUtils.getResourceDiagnosticReport(diagnosticReport.get(0), context);
        assertThat(diag.getStatus().toCode()).isEqualTo("final"); // Verify status from OBR.25
        assertThat(diag.getCategory().size()).isZero(); // Verify category from OBR.24

        // Verify code from OBR.4
        assertThat(diag.hasCode()).isTrue();
        List<Coding> codings = diag.getCode().getCoding();
        assertThat(codings.size()).isEqualTo(1);
        Coding coding = codings.get(0);
        assertThat(coding.hasDisplay()).isTrue();
        assertThat(coding.getDisplay()).hasToString("Final Echocardiogram Report");
        assertThat(coding.hasCode()).isTrue();
        assertThat(coding.getCode()).hasToString("1122");
        assertThat(coding.hasSystem()).isFalse();

        assertThat(diag.getEncounter().isEmpty()).isFalse(); // Verify encounter reference
        assertThat(diag.getSubject().isEmpty()).isFalse(); // Verify subject reference

        // Verify effectiveDateTime from OBR.7 and OBR.8
        assertThat(diag.getEffectiveDateTimeType().asStringValue()).isEqualTo("2018-05-20T23:00:00+08:00"); // This also verifies the type, confirming effectiveDateTime was set rather than effectivePeriod

        assertThat(diag.getIssued()).isNull(); // Verify issued from OBR.22
        assertThat(diag.getResultsInterpreter()).hasSize(1); // Verify resultsInterpreter from OBR.32
        assertThat(diag.getBasedOn()).hasSize(1); // Verify basedOn is ref to the ServiceRequest created for ORC or OBR
        assertThat(diag.getBasedOn().get(0).getReference().substring(0, 15)).isEqualTo("ServiceRequest/");

        assertThat(diag.getSpecimen()).isEmpty(); // Verify specimen reference

        // Verify result reference
        List<Reference> obsRef = diag.getResult();
        assertThat(obsRef.isEmpty()).isFalse();
        assertThat(obsRef).hasSize(1);
        assertThat(obsRef.get(0).isEmpty()).isFalse();

        // Verify presentedForm from OBX of type TX - In this case no attachments created because the OBX is not of type TX.
        List<Attachment> attachments = diag.getPresentedForm();
        Assertions.assertEquals(0, attachments.size(), "Unexpected number of attachments");

        ////////////////////////////////////
        // Verify the references that aren't covered in Observation tests
        ////////////////////////////////////        
        for (Resource res : obsResource) {
            // Verify encounter reference exists
            Observation obs = (Observation) res;
            assertThat(obs.getEncounter().isEmpty()).isFalse();
            assertThat(obs.getEncounter().getReference().substring(0, 10)).isEqualTo("Encounter/");

            //Verify subject reference to Patient exists
            assertThat(obs.getSubject().isEmpty()).isFalse();
            assertThat(obs.getSubject().getReference().substring(0, 8)).isEqualTo("Patient/");
        }

    }

    @Test
    public void test_oru_with_multiple_reports() throws IOException {
        String hl7message = "MSH|^~\\\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|ORU^R01|MSGID000005|T|2.6\r"
                + "PID||45483|45483||SMITH^SUZIE^||20160813|M|||123 MAIN STREET^^SCHENECTADY^NY^12345||(123)456-7890|||||^^^T||||||||||||\r"
                + "OBR|1||986^IA PHIMS Stage^2.16.840.1.114222.4.3.3.5.1.2^ISO|112^Final Echocardiogram Report|||20151009173644|||||||||||||002|||||F|||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^||||3068^JOHN^Paul^J|\r"
                + "OBX|1|ST|TS-F-01-007^Endocrine Disorders 7^L||obs report||||||F\r"
                + "OBX|2|ST|TS-F-01-008^Endocrine Disorders 8^L||ECHOCARDIOGRAPHIC REPORT||||||F\r"
                + "OBR|1||98^IA PHIMS Stage^2.16.840.1.114222.4.3.3.5.1.2^ISO|113^Echocardiogram Report|||20151009173644|||||||||||||002|||||F|||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^||||3065^Mahoney^Paul^J|\r"
                + "OBX|1|CWE|625-4^Bacteria identified in Stool by Culture^LN^^^^2.33^^result1|1|27268008^Salmonella^SCT^^^^20090731^^Salmonella species|||A^A^HL70078^^^^2.5|||P|||20120301|||^^^^^^^^Bacterial Culture||201203140957||||||\r"
                + "OBX|2|ST|TS-F-01-002^Endocrine Disorders^L||ECHOCARDIOGRAPHIC REPORT Group 2||||||F\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
        List<BundleEntryComponent> e = b.getEntry();

        // Verify that the right resources are being created
        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> obsResource = e.stream()
                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(obsResource).hasSize(4);

        List<Resource> practitionerResource = e.stream()
                .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(practitionerResource).hasSize(2);

        List<Resource> diagnosticReport = e.stream()
                .filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(diagnosticReport).hasSize(2);

        List<Resource> servReqResource = e.stream()
                .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(servReqResource).hasSize(2);

        // Expecting only the above resources, no extras!
        assertThat(e.size()).isEqualTo(11);

        ///////////////////////////////////////////
        // Now confirm content of the FIRST diagnosticReport because we don't have separate tests for DiagnosticReport
        ///////////////////////////////////////////
        DiagnosticReport diag = ResourceUtils.getResourceDiagnosticReport(diagnosticReport.get(0), context);
        assertThat(diag.getStatus().toCode()).isEqualTo("final"); // Verify status from OBR.25
        assertThat(diag.getCategory().size()).isZero(); // Verify category from OBR.24

        // Verify code from OBR.4
        assertThat(diag.hasCode()).isTrue();
        List<Coding> codings = diag.getCode().getCoding();
        assertThat(codings.size()).isEqualTo(1);
        Coding coding = codings.get(0);
        assertThat(coding.hasDisplay()).isTrue();
        assertThat(coding.getDisplay()).hasToString("Final Echocardiogram Report");
        assertThat(coding.hasCode()).isTrue();
        assertThat(coding.getCode()).hasToString("112");
        assertThat(coding.hasSystem()).isFalse();

        assertThat(diag.getEncounter().isEmpty()).isTrue(); // Verify encounter reference
        assertThat(diag.getSubject().isEmpty()).isFalse(); // Verify subject reference

        // Verify effectiveDateTime from OBR.7 and OBR.8
        assertThat(diag.getEffectiveDateTimeType().asStringValue()).isEqualTo("2015-10-09T17:36:44+08:00"); // This also verifies the type, confirming effectiveDateTime was set rather than effectivePeriod

        assertThat(diag.getIssued()).isNull(); // Verify issued from OBR.22
        assertThat(diag.getResultsInterpreter()).hasSize(1); // Verify resultsInterpreter from OBR.32
        assertThat(diag.getBasedOn()).hasSize(1); // Verify basedOn is ref to the ServiceRequest created for ORC or OBR
        assertThat(diag.getBasedOn().get(0).getReference().substring(0, 15)).isEqualTo("ServiceRequest/");
        assertThat(diag.getSpecimen()).isEmpty(); // Verify specimen reference

        // Verify result reference
        List<Reference> obsRef = diag.getResult();
        assertThat(obsRef.isEmpty()).isFalse();
        assertThat(obsRef).hasSize(2);
        assertThat(obsRef.get(0).isEmpty()).isFalse();

        // Verify presentedForm from OBX of type TX - In this case no attachments created because the OBX is not of type TX.
        List<Attachment> attachments = diag.getPresentedForm();
        Assertions.assertEquals(0, attachments.size(), "Unexpected number of attachments");

        ///////////////////////////////////////////
        // Now confirm content of the SECOND diagnosticReport because we don't have separate tests for DiagnosticReport
        ///////////////////////////////////////////
        DiagnosticReport diag2 = ResourceUtils.getResourceDiagnosticReport(diagnosticReport.get(0), context);
        assertThat(diag2.getStatus().toCode()).isEqualTo("final"); // Verify status from OBR.25
        assertThat(diag2.getCategory().size()).isZero(); // Verify category from OBR.24

        // Verify code from OBR.4
        assertThat(diag2.hasCode()).isTrue();
        List<Coding> codings2 = diag2.getCode().getCoding();
        assertThat(codings2.size()).isEqualTo(1);
        Coding coding2 = codings.get(0);
        assertThat(coding2.hasDisplay()).isTrue();
        assertThat(coding2.getDisplay()).hasToString("Final Echocardiogram Report");
        assertThat(coding2.hasCode()).isTrue();
        assertThat(coding2.getCode()).hasToString("112");
        assertThat(coding2.hasSystem()).isFalse();

        assertThat(diag2.getEncounter().isEmpty()).isTrue(); // Verify encounter reference
        assertThat(diag2.getSubject().isEmpty()).isFalse(); // Verify subject reference

        // Verify effectiveDateTime from OBR.7 and OBR.8
        assertThat(diag2.getEffectiveDateTimeType().asStringValue()).isEqualTo("2015-10-09T17:36:44+08:00"); // This also verifies the type, confirming effectiveDateTime was set rather than effectivePeriod

        assertThat(diag2.getIssued()).isNull(); // Verify issued from OBR.22
        assertThat(diag2.getResultsInterpreter()).hasSize(1); // Verify resultsInterpreter from OBR.32
        assertThat(diag.getBasedOn()).hasSize(1); // Verify basedOn is ref to the ServiceRequest created for ORC or OBR
        assertThat(diag.getBasedOn().get(0).getReference().substring(0, 15)).isEqualTo("ServiceRequest/");
        assertThat(diag2.getSpecimen()).isEmpty(); // Verify specimen reference

        // Verify result reference
        List<Reference> obsRef2 = diag2.getResult();
        assertThat(obsRef2.isEmpty()).isFalse();
        assertThat(obsRef2).hasSize(2);
        assertThat(obsRef2.get(0).isEmpty()).isFalse();

        // Verify presentedForm from OBX of type TX - In this case no attachments created because the OBX is not type TX.
        List<Attachment> attachments2 = diag2.getPresentedForm();
        Assertions.assertEquals(0, attachments2.size(), "Unexpected number of attachments");

        ////////////////////////////////////        
        // Check the references that aren't covered in Observation tests
        ////////////////////////////////////
        for (Resource res : obsResource) {
            // Verify encounter reference is not set
            Observation obs = (Observation) res;
            assertThat(obs.getEncounter().isEmpty()).isTrue();

            // Verify subject reference to Patient exists
            Base subject = ResourceUtils.getValue(obs, "subject");
            assertThat(ResourceUtils.getValueAsString(subject, "reference").substring(0, 8)).isEqualTo("Patient/");
        }
    }

    @Test
    public void test_oru_with_specimen() throws IOException {
        String hl7message = "MSH|^~\\\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|ORU^R01|MSGID000005|T|2.6\r"
                + "PID||45483|45483||SMITH^SUZIE^||20160813|M|||123 MAIN STREET^^SCHENECTADY^NY^12345||(123)456-7890|||||^^^T||||||||||||\r"
                + "OBR|1||986^IA PHIMS Stage^2.16.840.1.114222.4.3.3.5.1.2^ISO|1051-2^New Born Screening^LN|||20151009173644|||||||||||||002|||||F|||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^||||3065^Mahoney^Paul^J|\r"
                + "OBX|1|ST|TS-F-01-002^Endocrine Disorders^L||obs report||||||F\r"
                + "SPM|1|SpecimenID||BLD|||||||P||||||201410060535|201410060821||Y||||||1\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Verify that the right resources are created
        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> diagnosticReport = e.stream()
                .filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(diagnosticReport).hasSize(1);

        List<Resource> servReqResource = e.stream()
                .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(servReqResource).hasSize(1);

        List<Resource> obsResource = e.stream()
                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(obsResource).hasSize(1);

        List<Resource> practitionerResource = e.stream()
                .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(practitionerResource).hasSize(1);

        List<Resource> specimenResource = e.stream()
                .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(specimenResource).hasSize(1);

        // Expecting only the above resources, no extras! 
        assertThat(e.size()).isEqualTo(6);

        ///////////////////////////////////////////
        // Now confirm content of the diagnosticReport because we don't have separate tests for DiagnosticReport
        ///////////////////////////////////////////
        DiagnosticReport diag = ResourceUtils.getResourceDiagnosticReport(diagnosticReport.get(0), context);
        assertThat(diag.getStatus().toCode()).isEqualTo("final"); // Verify status from OBR.25
        assertThat(diag.getCategory().size()).isZero(); // Verify category from OBR.24

        // Verify code from OBR.4
        assertThat(diag.hasCode()).isTrue();
        List<Coding> codings = diag.getCode().getCoding();
        assertThat(codings.size()).isEqualTo(1);
        Coding coding = codings.get(0);
        assertThat(coding.hasDisplay()).isTrue();
        assertThat(coding.getDisplay()).hasToString("New Born Screening");
        assertThat(coding.hasCode()).isTrue();
        assertThat(coding.getCode()).hasToString("1051-2");
        assertThat(coding.hasSystem()).isTrue();
        assertThat(coding.getSystem()).hasToString("http://loinc.org");

        assertThat(diag.getEncounter().isEmpty()).isTrue(); // Verify encounter reference
        assertThat(diag.getSubject().isEmpty()).isFalse(); // Verify subject reference

        // Verify effectiveDateTime from OBR.7 and OBR.8
        assertThat(diag.getEffectiveDateTimeType().asStringValue()).isEqualTo("2015-10-09T17:36:44+08:00"); // This also verifies the type, confirming effectiveDateTime was set rather than effectivePeriod
        assertThat(diag.getIssued()).isNull(); // Verify issued from OBR.22
        assertThat(diag.getResultsInterpreter()).hasSize(1); // Verify resultsInterpreter from OBR.32
        assertThat(diag.getBasedOn()).hasSize(1); // Verify basedOn is ref to the ServiceRequest created for ORC or OBR
        assertThat(diag.getBasedOn().get(0).getReference().substring(0, 15)).isEqualTo("ServiceRequest/");

        // Verify specimen reference
        List<Reference> spmRef = diag.getSpecimen();
        assertThat(spmRef.isEmpty()).isFalse();
        assertThat(spmRef).hasSize(1);
        assertThat(spmRef.get(0).isEmpty()).isFalse();

        // Verify result reference
        List<Reference> obsRef = diag.getResult();
        assertThat(obsRef.isEmpty()).isFalse();
        assertThat(obsRef).hasSize(1);
        assertThat(obsRef.get(0).isEmpty()).isFalse();
        // Verify presentedForm from OBX of type ST - No attachments expected because OBX of type not TX creates an Observation.
        List<Attachment> attachments = diag.getPresentedForm();
        Assertions.assertEquals(0, attachments.size(), "Unexpected number of attachments");

        ////////////////////////////////////
        // Verify the references that aren't covered in Observation tests
        ////////////////////////////////////        
        for (Resource res : obsResource) {
            // Verify encounter reference
            Observation obs = (Observation) res;
            assertThat(obs.getEncounter().isEmpty()).isTrue();

            //Verify subject reference to Patient exists
            assertThat(obs.getSubject().isEmpty()).isFalse();
            assertThat(obs.getSubject().getReference().substring(0, 8)).isEqualTo("Patient/");
        }
    }

    /**
     * 
     * ORU messages with an OBR and multiple OBX segments create records only for non TX type OBX
     * The OBX type TX are added to the presentedForm as an attachment for the diagnostic
     * @throws IOException
     */
    @Test
    public void test_oru_multipleOBXofDifferentTypes() throws IOException {
        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(new File("src/test/resources/ORU-multiline-short.hl7"), OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);

        // Verify conversion
        FHIRContext context = new FHIRContext();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        Assertions.assertSame(BundleType.COLLECTION, b.getType(), "Bundle type not expected");
        List<BundleEntryComponent> e = b.getEntry();

        // Verify that the right resources have been created
        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = e.stream()
                .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> organizationResource = e.stream()
                .filter(v -> ResourceType.Organization == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        // We expect an organization created from an Encounter.serviceProvider reference 
        assertThat(organizationResource).hasSize(1);

        List<Resource> practitionerResource = e.stream()
                .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(practitionerResource).hasSize(4);

        List<Resource> messageHeader = e.stream()
                .filter(v -> ResourceType.MessageHeader == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(messageHeader).hasSize(1);

        //Verify Diagnostic Report is created as expected
        List<Resource> reportResource = e.stream()
                .filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(reportResource).hasSize(1);

        List<Resource> servReqResource = e.stream()
                .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(servReqResource).hasSize(1);

        // Verify there are no extra resources created
        assertThat(e.size()).isEqualTo(10);

        //Verify no observations are created
        List<Resource> obsResource = e.stream()
                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(obsResource).hasSize(0); // TODO: When NTE is implemented, then update this to one.

        ///////////////////////////////////////////
        // Now confirm content of the diagnosticReport because we don't have separate tests for DiagnosticReport
        ///////////////////////////////////////////
        DiagnosticReport diag = ResourceUtils.getResourceDiagnosticReport(reportResource.get(0), context);
        assertThat(diag.getStatus().toCode()).isEqualTo("final"); // Verify status from OBR.25
        assertThat(diag.getCategory().size()).isEqualTo(1); // Verify category from OBR.24
        DatatypeUtils.checkCommonCodeableConceptAssertions(diag.getCategoryFirstRep(), "CT", "CAT Scan",
                "http://terminology.hl7.org/CodeSystem/v2-0074", "CT");

        // Verify code from OBR.4; This tests scenario of the code not being in the default loinc system.
        assertThat(diag.hasCode()).isTrue();
        List<Coding> codings = diag.getCode().getCoding();
        assertThat(codings.size()).isEqualTo(1);
        Coding coding = codings.get(0);
        assertThat(coding.hasDisplay()).isTrue();
        assertThat(coding.getDisplay()).hasToString("ECHO CARDIOGRAM COMPLETE");
        assertThat(coding.hasCode()).isTrue();
        assertThat(coding.getCode()).hasToString("1487");
        assertThat(coding.hasSystem()).isFalse();

        assertThat(diag.getEncounter().isEmpty()).isFalse(); // Verify encounter reference
        assertThat(diag.getSubject().isEmpty()).isFalse(); // Verify subject reference

        // Verify effectiveDateTime from OBR.7 and OBR.8
        assertThat(diag.getEffectiveDateTimeType().asStringValue()).isEqualTo("2020-08-02T12:44:55+08:00"); // This also verifies the type, confirming effectiveDateTime was set rather than effectivePeriod
        assertThat(diag.getIssued()).isNull(); // Verify issued from OBR.22
        assertThat(diag.getResultsInterpreter()).isEmpty(); // Verify resultsInterpreter from OBR.32
        assertThat(diag.getBasedOn()).hasSize(1); // Verify basedOn is ref to the ServiceRequest created for ORC or OBR
        assertThat(diag.getBasedOn().get(0).getReference().substring(0, 15)).isEqualTo("ServiceRequest/");

        // Verify specimen reference
        List<Reference> spmRef = diag.getSpecimen();
        assertThat(spmRef).isEmpty();

        // Verify result reference
        List<Reference> obsRef = diag.getResult();
        assertThat(obsRef).isEmpty();

        //Verify attachment to diagnostic report
        List<Attachment> attachments = diag.getPresentedForm();
        Assertions.assertEquals(1, attachments.size(), "Unexpected number of attachments");
        Attachment a = attachments.get(0);
        Assertions.assertTrue(a.getContentType().equalsIgnoreCase("text/plain"), "Incorrect content type");
        Assertions.assertTrue(a.getLanguage().equalsIgnoreCase("en"), "Incorrect language");
        //Verify data attachment after decoding
        String decoded = new String(Base64.getDecoder().decode(a.getDataElement().getValueAsString()));
        System.out.println("Decoded: '" + decoded + "'");
        Assertions.assertEquals("\n[PII] Emergency Department\nED Encounter Arrival Date: [ADDRESS] [PERSONALNAME]:",
                decoded, "Incorrect data");
        Assertions.assertTrue(a.getTitle().equalsIgnoreCase("ECHO CARDIOGRAM COMPLETE"), "Incorrect title");
        //Verify creation data is persisted correctly - 2020-08-02T12:44:55+08:00
        Calendar c = Calendar.getInstance();
        c.clear(); // needed to completely clear out calendar object
        c.set(2020, 7, 2, 12, 44, 55);
        c.setTimeZone(TimeZone.getTimeZone(ZoneId.of("+08:00")));
        Date d = c.getTime();
        Assertions.assertEquals(d, a.getCreation(), "Incorrect creation date");

        ////////////////////////////////////
        // Verify the references that aren't covered in Observation tests
        ////////////////////////////////////        
        for (Resource res : obsResource) {
            // Verify encounter reference exists
            Observation obs = (Observation) res;
            assertThat(obs.getEncounter().isEmpty()).isFalse();
            assertThat(obs.getEncounter().getReference().substring(0, 10)).isEqualTo("Encounter/");

            //Verify subject reference to Patient exists
            assertThat(obs.getSubject().isEmpty()).isFalse();
            assertThat(obs.getSubject().getReference().substring(0, 8)).isEqualTo("Patient/");
        }
    }

    /**
     * 
     * Verifies ORU messages with mixed OBX types
     * 
     * @throws IOException
     */
    @Test
    public void test_oru_multipleOBXWithMixedType() throws IOException {
        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(new File("src/test/resources/ORU-multiline-short-mixed.hl7"), OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        // Verify conversion
        FHIRContext context = new FHIRContext();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        Bundle b = (Bundle) bundleResource;
        assertThat(bundleResource).isNotNull();
        Assertions.assertSame(BundleType.COLLECTION, b.getType(), "Bundle type not expected");
        List<BundleEntryComponent> e = b.getEntry();

        // Verify that the right resources have been created
        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = e.stream()
                .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> organizationResource = e.stream()
                .filter(v -> ResourceType.Organization == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        // We expect an organization created from an Encounter.serviceProvider reference 
        assertThat(organizationResource).hasSize(1);

        List<Resource> practitionerResource = e.stream()
                .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(practitionerResource).hasSize(4);

        List<Resource> messageHeader = e.stream()
                .filter(v -> ResourceType.MessageHeader == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(messageHeader).hasSize(1);

        // Verify one Observation is created (from the ST, not the TX)
        List<Resource> obsResource = e.stream()
                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(obsResource).hasSize(1); // TODO: When NTE is implemented, then update this.

        // Verify Diagnostic Report is created as expected
        List<Resource> reportResource = e.stream()
                .filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(reportResource).hasSize(1);

        List<Resource> servReqResource = e.stream()
                .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(servReqResource).hasSize(1);

        // Verify there are no extra resources created
        assertThat(e.size()).isEqualTo(11);

        ///////////////////////////////////////////
        // Now confirm content of the diagnosticReport because we don't have separate tests for DiagnosticReport
        ///////////////////////////////////////////
        DiagnosticReport diag = ResourceUtils.getResourceDiagnosticReport(reportResource.get(0), context);
        assertThat(diag.getStatus().toCode()).isEqualTo("final"); // Verify status from OBR.25
        assertThat(diag.getCategory().size()).isEqualTo(1); // Verify category from OBR.24
        assertThat(diag.getCategory().get(0).getCoding().size()).isEqualTo(1);
        assertThat(diag.getCategory().get(0).getCoding().get(0).getSystem())
                .isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0074");
        assertThat(diag.getCategory().get(0).getCoding().get(0).getCode()).isEqualTo("CT");
        assertThat(diag.getCategory().get(0).getCoding().get(0).getDisplay()).isEqualTo("CAT Scan");
        assertThat(diag.getCategory().get(0).getText()).isEqualTo("CT");

        // Verify code from OBR.4; This tests scenario of the code not being in the default loinc system.
        assertThat(diag.hasCode()).isTrue();
        List<Coding> codings = diag.getCode().getCoding();
        assertThat(codings.size()).isEqualTo(1);
        Coding coding = codings.get(0);
        assertThat(coding.hasDisplay()).isTrue();
        assertThat(coding.getDisplay()).hasToString("ECHO CARDIOGRAM COMPLETE");
        assertThat(coding.hasCode()).isTrue();
        assertThat(coding.getCode()).hasToString("1487");
        assertThat(coding.hasSystem()).isFalse();

        assertThat(diag.getEncounter().isEmpty()).isFalse(); // Verify encounter reference
        assertThat(diag.getSubject().isEmpty()).isFalse(); // Verify subject reference

        // Verify effectiveDateTime from OBR.7 and OBR.8
        assertThat(diag.getEffectiveDateTimeType().asStringValue()).isEqualTo("2020-08-02T12:44:55+08:00"); // This also verifies the type, confirming effectiveDateTime was set rather than effectivePeriod
        assertThat(diag.getIssued()).isNull(); // Verify issued from OBR.22
        assertThat(diag.getResultsInterpreter()).isEmpty(); // Verify resultsInterpreter from OBR.32
        assertThat(diag.getBasedOn()).hasSize(1); // Verify basedOn is ref to the ServiceRequest created for ORC or OBR
        assertThat(diag.getBasedOn().get(0).getReference().substring(0, 15)).isEqualTo("ServiceRequest/");

        // Verify specimen reference
        List<Reference> spmRef = diag.getSpecimen();
        assertThat(spmRef).isEmpty();

        // Verify result reference
        List<Reference> obsRef = diag.getResult();
        assertThat(obsRef).isNotEmpty();
        assertThat(obsRef).hasSize(1);
        assertThat(obsRef.get(0).isEmpty()).isFalse();
        // No attachment created since OBX with TX and no id is not first
        List<Attachment> attachments = diag.getPresentedForm();
        Assertions.assertEquals(0, attachments.size(), "Unexpected number of attachments");

        ////////////////////////////////////
        // Verify the references that aren't covered in Observation tests
        ////////////////////////////////////        
        for (Resource res : obsResource) {
            // Verify encounter reference exists
            Observation obs = (Observation) res;
            assertThat(obs.getEncounter().isEmpty()).isFalse();
            assertThat(obs.getEncounter().getReference().substring(0, 10)).isEqualTo("Encounter/");

            //Verify subject reference to Patient exists
            assertThat(obs.getSubject().isEmpty()).isFalse();
            assertThat(obs.getSubject().getReference().substring(0, 8)).isEqualTo("Patient/");
        }
    }

    @Test
    public void test_ORU_r01_without_status() throws IOException {
        String ORU_r01 = "MSH|^~\\&|NIST Test Lab APP|NIST Lab Facility||NIST EHR Facility|20150926140551||ORU^R01|NIST-LOI_5.0_1.1-NG|T|2.5.1|||AL|AL|||||\r"
                + "PID|1||PATID5421^^^NISTMPI^MR||Wilson^Patrice^Natasha^^^^L||19820304|F||2106-3^White^HL70005|144 East 12th Street^^Los Angeles^CA^90012^^H||^PRN^PH^^^203^2290210|||||||||N^Not Hispanic or Latino^HL70189\r"
                + "ORC|NW|ORD448811^NIST EHR|R-511^NIST Lab Filler||||||20120628070100|||5742200012^Radon^Nicholas^^^^^^NPI^L^^^NPI\r"
                + "OBR|1|ORD448811^NIST EHR|R-511^NIST Lab Filler|1000^Hepatitis A B C Panel^99USL|||20120628070100|||||||||5742200012^Radon^Nicholas^^^^^^NPI^L^^^NPI\r"
                + "OBX|1|CWE|22314-9^Hepatitis A virus IgM Ab [Presence] in Serum^LN^HAVM^Hepatitis A IgM antibodies (IgM anti-HAV)^L^2.52||260385009^Negative (qualifier value)^SCT^NEG^NEGATIVE^L^201509USEd^^Negative (qualifier value)||Negative|N|||F|||20150925|||||201509261400\r"
                + "OBX|2|CWE|20575-7^Hepatitis A virus Ab [Presence] in Serum^LN^HAVAB^Hepatitis A antibodies (anti-HAV)^L^2.52||260385009^Negative (qualifier value)^SCT^NEG^NEGATIVE^L^201509USEd^^Negative (qualifier value)||Negative|N|||F|||20150925|||||201509261400\r"
                + "OBX|3|NM|22316-4^Hepatitis B virus core Ab [Units/volume] in Serum^LN^HBcAbQ^Hepatitis B core antibodies (anti-HBVc) Quant^L^2.52||0.70|[IU]/mL^international unit per milliliter^UCUM^IU/ml^^L^1.9|<0.50 IU/mL|H|||F|||20150925|||||201509261400";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(ORU_r01, OPTIONS);

        FHIRContext context = new FHIRContext();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> diagnosticReport = e.stream()
                .filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(diagnosticReport).hasSize(1);

        String s = context.getParser().encodeResourceToString(diagnosticReport.get(0));
        Class<? extends IBaseResource> klass = DiagnosticReport.class;
        DiagnosticReport expectStatusUnknown = (DiagnosticReport) context.getParser().parseResource(klass, s);
        DiagnosticReport.DiagnosticReportStatus status = expectStatusUnknown.getStatus();

        assertThat(expectStatusUnknown.hasStatus()).isTrue();
        assertThat(status).isEqualTo(DiagnosticReport.DiagnosticReportStatus.UNKNOWN);
    }
}