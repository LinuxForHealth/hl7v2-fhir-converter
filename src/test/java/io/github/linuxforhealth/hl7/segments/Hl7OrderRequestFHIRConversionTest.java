/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.segments.util.PatientUtils;
import io.github.linuxforhealth.hl7.segments.util.DatatypeUtils;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hl7OrderRequestFHIRConversionTest {

  private static FHIRContext context = new FHIRContext(true, false);
  private static final Logger LOGGER = LoggerFactory.getLogger(Hl7MedicationRequestFHIRConversionTest.class);

  // Read comments carefully.  Tests
  @Test
  public void testBroadORCFields() {

    String hl7message =

        "MSH|^~\\&|||||20180924152907|34001|ORU^R01^ORU_R01|213|T|2.6|||||||||||\n"
            + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
            + "PV1|1|E|||||||||||||||||||||||||||||||||||||||||||\n"
            //  NOTE: ORC is optional; OBR is required.
            //  Key input data set up:
            //  1. Checking fields ORC.4 to ServiceRequest.requisition
            //  1b. ORC.5 to ServiceRequest.status 
            //  2. ORC.9 to ServiceRequest.authoredOn (overrides secondary OBR.6)
            //  3. ORC.12 to ServiceRequest.requester AND Practitioner object and reference to Practictioner
            //  4. ORC.15 to ServiceRequest.occurrenceDateTime (overrides secondary OBR.7)
            //  5. ORC.16 is set to a reason code (secondary to OBR.31, which is purposely not present in this case)
            + "ORC|RE|248648498^|248648498^|ML18267-C00001^Beaker|SC||||20120628071200|||5742200012^Radon^Nicholas^^^^^^NPI^L^^^NPI|||20170917151717|042^Human immunodeficiency virus [HIV] disease [42]^I9CDX^^^^29|||||||||||||||\n"
            //  10. OBR.16 is here as a test to show that it is ignored as Practitioner because ORC.12 has priority 
            //  11. OBR.31 is omitted purposely so the ORC.16 is used for the reason code
            //  12. OBR.6 is purposely present, but will be ignored because ORC.9 has a value and is preferred.
            //  13. OBR.7 is purposely present, but will be ignored because ORC.15 has a value and is preferred.
            + "OBR|1|248648498^|248648498^|83036E^HEMOGLOBIN A1C^PACSEAP^^^^^^HEMOGLOBIN A1C||20120606120606|20120606120606||||L|||||54321678^SCHMIDT^FRIEDA^^MD^^^^^^^^^NPI||||||20180924152900|||F||^^^20120613071200|||||||||||||||||||||||\n";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
    assertThat(json).isNotBlank();

    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle bundle = (Bundle) bundleResource;
    List<BundleEntryComponent> e = bundle.getEntry();

    List<Resource> serviceRequestList = e.stream()
        .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    // Important that we have exactly one service request (no duplication).  OBR creates it as a reference.        
    assertThat(serviceRequestList).hasSize(1);
    ServiceRequest serviceRequest = ResourceUtils.getResourceServiceRequest(serviceRequestList.get(0), context);

    // ORC.4 should create a requisition in the serviceRequest.
    assertThat(serviceRequest.hasRequisition()).isTrue();
    assertThat(serviceRequest.getRequisition().hasSystem()).isTrue();
    assertThat(serviceRequest.getRequisition().getSystem()).isEqualToIgnoringCase("urn:id:Beaker");
    assertThat(serviceRequest.getRequisition().hasValue()).isTrue();
    assertThat(serviceRequest.getRequisition().getValue()).isEqualToIgnoringCase("ML18267-C00001");

    // // ORC.5 creates the serviceRequest.status()
    assertThat(serviceRequest.hasStatus()).isTrue();
    assertThat(serviceRequest.getStatusElement().getCode()).isEqualTo("active");   

    // ORC.9 should create an serviceRequest.authoredOn date 
    assertThat(serviceRequest.hasAuthoredOn()).isTrue();
    assertThat(serviceRequest.getAuthoredOnElement().toString()).containsPattern("2012-06-28T07:12:00");

    // ORC.15 should create an ServiceRequest.occurrenceDateTime date
    assertThat(serviceRequest.hasOccurrenceDateTimeType()).isTrue();
    assertThat(serviceRequest.getOccurrenceDateTimeType().toString()).containsPattern("2017-09-17T15:17:17");

    // ORC.16 should create a ServiceRequest.reasonCode CWE
    assertThat(serviceRequest.hasReasonCode()).isTrue();
    assertThat(serviceRequest.getReasonCode()).hasSize(1);
    DatatypeUtils.checkCommonCodeableConceptAssertions(serviceRequest.getReasonCodeFirstRep(), "042",
        "Human immunodeficiency virus [HIV] disease [42]",
        "http://terminology.hl7.org/CodeSystem/ICD-9CM-diagnosiscodes",
        "Human immunodeficiency virus [HIV] disease [42]");

    // ORC.12 should create an ServiceRequest.requester reference
    assertThat(serviceRequest.hasRequester()).isTrue();
    String requesterRef = serviceRequest.getRequester().getReference();

    Practitioner pract = ResourceUtils.getSpecificPractitionerFromBundle(bundle, requesterRef);
    // Confirm that the matching practitioner by ID has the correct content (simple validation)
    // Should be ORC.12.1 and NOT OBR.16.1
    assertThat(pract.getIdentifierFirstRep().getValue()).isEqualTo("5742200012");

    // Get the DiagnosticReport and see that it's basedOn cross-references to the ServiceRequest object
    List<Resource> diagnosticReportList = e.stream()
        .filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(diagnosticReportList).hasSize(1);
    DiagnosticReport diagnosticReport = ResourceUtils.getResourceDiagnosticReport(diagnosticReportList.get(0), context);

    assertThat(diagnosticReport.hasBasedOn()).isTrue();
    assertThat(diagnosticReport.getBasedOn()).hasSize(1);
    //Check that the cross reference is equal to the serviceRequest id
    assertThat(diagnosticReport.getBasedOnFirstRep().getReference()).hasToString(serviceRequest.getId());

  }

  // This test is a companion to testBroadORCFields.   ORC and OBR records often have repeated data; one taking priority over the other.
  // Read comments carefully.  This sometimes tests the secondary value and may be the opposite to the tests in testBroadORCFields.
  @Test
  public void testBroadORCPlusOBRFields() {

    String hl7message = "MSH|^~\\&|||||20180924152907|34001|ORU^R01^ORU_R01|213|T|2.6|||||||||||\n"
        + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
        + "PV1|1|E|||||||||||||||||||||||||||||||||||||||||||\n"
        //  NOTE: ORC is optional; OBR is required.
        //  Key input data set up:
        //  1. Map OBR.7 to ServiceRequest.occurrenceDateTime, because ORC.15 is empty
        //  2.  Map OBR.7 to DiagnosticReport.effectiveDateTime 
        //  3. Leave ORC.9 empty so that OBR.6 is used for ServiceRequest.authoredOn
        //  4. OBR.22 used and DiagnosticReport.issued
        //  5. Leave ORC.12 empty so OBR.16 is used for Practitioner reference
        //  6. ORC.16 is set to a reason code (but it is ignored because it is secondary to OBR.31, which is present in this case and therefore overrides ORC.16)
        + "ORC|RE|248648498^|248648498^|||||||||||||042^Human immunodeficiency virus [HIV] disease [42]^I9CDX^^^^29|||||||||||||||\n"
        //  10. OBR.32 will be turned into a Practioner and referenced and the DiagnositicReport.resultsInterpreter
        //  11. OBR.4 maps to both ServiceRequest.code and DiagnosticReport.code
        //  12. OBR.16 creates a ServiceRequest.requester reference
        //  13. OBR.22 creates a DiagnosticReport.status 
        //  14. OBR.6 creates ServiceRequest.authoredOn
        + "OBR|1|248648498^|248648498^|83036E^HEMOGLOBIN A1C^PACSEAP^^^^^^HEMOGLOBIN A1C||20120606120606|20170707150707||||L|||||54321678^SCHMIDT^FRIEDA^^MD^^^^^^^^^NPISER||||||20180924152900|||F||||||HIV^HIV/Aids^L^^^^V1|323232^Mahoney^Paul^J||||||||||||||||||\n";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
    assertThat(json).isNotBlank();

    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle bundle = (Bundle) bundleResource;
    List<BundleEntryComponent> e = bundle.getEntry();

    List<Resource> serviceRequestList = e.stream()
        .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    // Important that we have exactly one service request (no duplication).  OBR creates it as a reference.        
    assertThat(serviceRequestList).hasSize(1);
    ServiceRequest serviceRequest = ResourceUtils.getResourceServiceRequest(serviceRequestList.get(0), context);
    assertThat(serviceRequest.hasStatus()).isTrue();

    // ORC.4 checked in testBroadORCFields
    // OBR.6 should create authoredOn because ORC.9 is not filled in
    assertThat(serviceRequest.hasAuthoredOn()).isTrue();
    assertThat(serviceRequest.getAuthoredOnElement().toString()).containsPattern("2012-06-06T12:06:06");

    // OBR.7 is used to create an ServiceRequest.occurrenceDateTime date because ORC.15 is empty
    assertThat(serviceRequest.hasOccurrenceDateTimeType()).isTrue();
    assertThat(serviceRequest.getOccurrenceDateTimeType().toString()).containsPattern("2017-07-07T15:07:07");

    // OBR.31 should create the ServiceRequest.reasonCode CWE
    assertThat(serviceRequest.hasReasonCode()).isTrue();
    assertThat(serviceRequest.getReasonCode()).hasSize(1);
    DatatypeUtils.checkCommonCodeableConceptAssertions(serviceRequest.getReasonCodeFirstRep(), "HIV", "HIV/Aids",
        "urn:id:L", "HIV/Aids");

    // OBR.16 should create an ServiceRequest.requester reference
    assertThat(serviceRequest.hasRequester()).isTrue();
    String requesterRef = serviceRequest.getRequester().getReference();
    Practitioner pract = ResourceUtils.getSpecificPractitionerFromBundle(bundle, requesterRef);
    // Confirm that the matching practitioner by ID has the correct content (simple validation)
    // Should be OBR.16 because ORC.12 is empty. Check OBR.16.1 which is the ID.
    assertThat(pract.getIdentifierFirstRep().getValue()).isEqualTo("54321678");

    // OBR.4 maps to ServiceRequest.code.  Verify resulting CodeableConcept.
    assertThat(serviceRequest.hasCode()).isTrue();
    DatatypeUtils.checkCommonCodeableConceptAssertions(serviceRequest.getCode(), "83036E", "HEMOGLOBIN A1C",
        "urn:id:PACSEAP", "HEMOGLOBIN A1C");

    List<Resource> diagnosticReportList = e.stream()
        .filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(diagnosticReportList).hasSize(1);
    DiagnosticReport diagnosticReport = ResourceUtils.getResourceDiagnosticReport(diagnosticReportList.get(0), context);

    // OBR.4 ALSO maps to DiagnosticReport.code.  Verify resulting CodeableConcept.
    assertThat(diagnosticReport.hasCode()).isTrue();
    DatatypeUtils.checkCommonCodeableConceptAssertions(diagnosticReport.getCode(), "83036E", "HEMOGLOBIN A1C",
        "urn:id:PACSEAP", "HEMOGLOBIN A1C");

    assertThat(diagnosticReport.hasBasedOn()).isTrue();
    assertThat(diagnosticReport.getBasedOn()).hasSize(1);

    // OBR.7 maps to create an DiagnosticReport.effectiveDateTime
    assertThat(diagnosticReport.hasEffectiveDateTimeType()).isTrue();
    assertThat(diagnosticReport.getEffectiveDateTimeType().toString()).containsPattern("2017-07-07T15:07:07");

    // Check for DiagnosticReport.issued instant of OBR.22
    assertThat(diagnosticReport.hasIssued()).isTrue();
    // NOTE: The data is kept as an InstantType, and is extracted with .toInstant
    // thus results in a different format than other time stamps that are based on DateTimeType
    assertThat(diagnosticReport.getIssued().toInstant().toString()).contains("2018-09-24T07:29:00Z");

    // Get the diagnosticReport.resultsInterpreter, which should match the Practitioner data from OBR.32
    assertThat(diagnosticReport.hasResultsInterpreter()).isTrue();
    assertThat(diagnosticReport.getResultsInterpreter()).hasSize(1);
    String resultsInterpreterRef = diagnosticReport.getResultsInterpreter().get(0).getReference();
    pract = ResourceUtils.getSpecificPractitionerFromBundle(bundle, resultsInterpreterRef);
    // Confirm that the matching practitioner by ID has the correct content (simple validation)
    // Should be the value OBR.32
    assertThat(pract.getIdentifierFirstRep().getValue()).isEqualTo("323232");

    // Check for OBR.25 mapped to DiagnosticReport.status
    assertThat(diagnosticReport.hasStatus()).isTrue();
    assertThat(diagnosticReport.getStatusElement().getCode()).isEqualTo("final");   
  }

  @Test
  public void testBroadORCPlusOBRFields2() {

    String hl7message = "MSH|^~\\&|||||20180924152907|34001|ORU^R01^ORU_R01|213|T|2.6|||||||||||\n"
            + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
            + "PV1|1|E|||||||||||||||||||||||||||||||||||||||||||\n"
            //  ORC.15 is empty, OBR.7 is empty, so use OBR-27[0].4 as ServiceRequest.occurrenceDateTime
            //  ORC.5 with purposely bad code to see that 'unknown' is result
            + "ORC|RE|248648498^|248648498^||ZZ||||20120628071200||||||||||||||||||||||\n"
            + "OBR|1|248648498^|248648498^|83036E^HEMOGLOBIN A1C^PACSEAP^^^^^^HEMOGLOBIN A1C|||||||L||||||||||||||F||^^^20120606120606|||||||||||||||||||||||\n";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
    assertThat(json).isNotBlank();

    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle bundle = (Bundle) bundleResource;
    List<BundleEntryComponent> e = bundle.getEntry();

    List<Resource> serviceRequestList = e.stream()
        .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    // Important that we have exactly one service request (no duplication).  OBR creates it as a reference.        
    assertThat(serviceRequestList).hasSize(1);
    ServiceRequest serviceRequest = ResourceUtils.getResourceServiceRequest(serviceRequestList.get(0), context);
    assertThat(serviceRequest.hasStatus()).isTrue();

    // OBR.27[0].4 should create an ServiceRequest.occurrenceDateTime date
    assertThat(serviceRequest.hasOccurrenceDateTimeType()).isTrue();
    assertThat(serviceRequest.getOccurrenceDateTimeType().toString()).containsPattern("2012-06-06T12:06:06");

    // // ORC.5 creates the serviceRequest.status() purposely an unknown code
    assertThat(serviceRequest.hasStatus()).isTrue();
    assertThat(serviceRequest.getStatusElement().getCode()).isEqualTo("unknown");   
  }

  @Test
  public void testAdditionalOBRFields() {

    String hl7message =

        "MSH|^~\\&|Epic|ATRIUS|||20180924152907|34001|ORU^R01^ORU_R01|213|T|2.6|||||||||PHLabReport-Ack^^2.16.840.1.114222.4.10.3^ISO||\n"
        + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
        + "PV1|1|E|||||||||||||||||||||||||||||||||||||||||||\n"
        //  ORC not needed for this OBR test
        //  1. Map OBR.7 and OBR.8 together to DiagnosticReport.effectivePeriod
        + "OBR|1|248648498^|248648498^|83036E^HEMOGLOBIN A1C^PACSEAP^^^^^^HEMOGLOBIN A1C|||20170707120707|20180808120808|||||||||||||||||F|||||||||||||||||||||||||\n";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
    assertThat(json).isNotBlank();

    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle bundle = (Bundle) bundleResource;
    List<BundleEntryComponent> e = bundle.getEntry();

    List<Resource> diagnosticReportList = e.stream()
        .filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(diagnosticReportList).hasSize(1);
    DiagnosticReport diagnosticReport = ResourceUtils.getResourceDiagnosticReport(diagnosticReportList.get(0), context);

    // OBR.7 and OBR.8 together map to DiagnosticReport.effectivePeriod
    assertThat(diagnosticReport.hasEffectivePeriod()).isTrue();
    assertThat(diagnosticReport.getEffectivePeriod().getStartElement().toString()).containsPattern("2017-07-07T12:07:07");
    assertThat(diagnosticReport.getEffectivePeriod().getEndElement().toString()).containsPattern("2018-08-08T12:08:08");
  }

  // This test assures the MDM_T02 is properly enabled. 
  // It focuses on _differences_ in MDM not tested above, then does general confirmation of other fields
  @Test
  public void testMDMT02ServiceRequest() {

    String hl7message =
        "MSH|^~\\&|Epic|PQA|WHIA|IBM|20170920141233||MDM^T02^MDM_T02|M1005|D|2.6\r"
        +"EVN|T02|20170920141233|||\r"
        +"PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
        +"PV1|1|I|||||||||||||||||||||||||||||||||||||\r"
        // ORC.4 is used for requisition and for PGN identifier
        // ORC.5 is status
        // ORC.9 is authoredOn
        // ORC.15 is occurenceDateTime
        // ORC.12 is tested for requester
        +"ORC|NW|P1005|F1005|P1005|SC|D|1||20170920141233|||1212^docProvider^docOrdering|||20170920141233|\r"
        // OBR.4 is the code
        // OBR.31 used for reason code
        +"OBR|1|P1005|F1005|71260^CT Chest without contrast^ICD10|||||||||||||||||||||||||||exam reason ID^PREAURICULAR EDEMA text||||\r";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
    assertThat(json).isNotBlank();

    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle bundle = (Bundle) bundleResource;
    List<BundleEntryComponent> e = bundle.getEntry();

    List<Resource> serviceRequestList = e.stream()
        .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(serviceRequestList).hasSize(1);
    ServiceRequest serviceRequest = ResourceUtils.getResourceServiceRequest(serviceRequestList.get(0), context);

    // Expect 4 identifiers (VN, PLAC, FILL, PGN)
    assertThat(serviceRequest.hasIdentifier()).isTrue();
    assertThat(serviceRequest.getIdentifier()).hasSize(4);

    // ORC.4.1 value should create a requisition in the serviceRequest
    assertThat(serviceRequest.hasRequisition()).isTrue();
    assertThat(serviceRequest.getRequisition().hasValue()).isTrue();
    assertThat(serviceRequest.getRequisition().getValue()).isEqualToIgnoringCase("P1005");
    assertThat(serviceRequest.getRequisition().hasSystem()).isFalse();
    assertThat(serviceRequest.getRequisition().hasType()).isTrue();
    DatatypeUtils.checkCommonCodeableConceptAssertions(serviceRequest.getRequisition().getType(), "PGN", "Placer Group Number", "http://terminology.hl7.org/2.1.0/CodeSystem/v2-0203", "Placer Group Number");

    // ORC.12 should create an ServiceRequest.requester reference
    assertThat(serviceRequest.hasRequester()).isTrue();
    assertThat(serviceRequest.getRequester().hasDisplay()).isTrue();   
    assertThat(serviceRequest.getRequester().getDisplay()).isEqualTo("docOrdering docProvider");  
    assertThat(serviceRequest.getRequester().hasReference()).isTrue();    
    String requesterRef = serviceRequest.getRequester().getReference();

    Practitioner pract = ResourceUtils.getSpecificPractitionerFromBundle(bundle, requesterRef);
    // Confirm that the matching practitioner by ID has the correct content (simple validation)
    // Should be ORC.12.1
    assertThat(pract.getIdentifierFirstRep().getValue()).isEqualTo("1212");

    // OBR.31 is the reason code
    assertThat(serviceRequest.hasReasonCode()).isTrue();
    assertThat(serviceRequest.getReasonCode()).hasSize(1);
    DatatypeUtils.checkCommonCodeableConceptAssertions(serviceRequest.getReasonCodeFirstRep(), "exam reason ID", "PREAURICULAR EDEMA text", null, "PREAURICULAR EDEMA text");

    // OBR.4 is the  code
    assertThat(serviceRequest.hasCode()).isTrue();
    DatatypeUtils.checkCommonCodeableConceptAssertions(serviceRequest.getCode(), "71260", "CT Chest without contrast",
        "http://hl7.org/fhir/sid/icd-10-cm", "CT Chest without contrast");    

    // General over valdation of presence of fields:
    assertThat(serviceRequest.hasStatus()).isTrue();  // ORC.4
    assertThat(serviceRequest.hasIntent()).isTrue();
    assertThat(serviceRequest.hasSubject()).isTrue();  // ORC.9
    assertThat(serviceRequest.hasAuthoredOn()).isTrue();  // ORC.15
  }


}
