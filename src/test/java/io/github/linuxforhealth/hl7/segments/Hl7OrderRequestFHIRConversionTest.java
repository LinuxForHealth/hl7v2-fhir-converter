/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hl7OrderRequestFHIRConversionTest {

  private static FHIRContext context = new FHIRContext(true, false);
  private static final Logger LOGGER = LoggerFactory.getLogger(Hl7MedicationRequestFHIRConversionTest.class);

  @Test
  public void testBroadORCFields() {

    String hl7message =

        "MSH|^~\\&|Epic|ATRIUS|||20180924152907|34001|ORU^R01^ORU_R01|213|T|2.6|||||||||PHLabReport-Ack^^2.16.840.1.114222.4.10.3^ISO||\n"
            + "SFT|Epic Systems Corporation^L^^^^ANSI&1.2.840&ISO^XX^^^1.2.840.114350|Epic 2015 |Bridges|8.2.2.0||20160605043244\n"
            + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
            + "PV1|1|||||||||||||||||||||||||||||||||||||||||||20180924152707|\n"
            //  NOTE: ORC is optional; OBR is required.
            //  Key input data set up:
            //  1. Checking fields ORC.4 to ServiceRequest.requisition
            //  2. ORC.9 to ServiceRequest.authoredOn
            //  3. ORC.12 to ServiceRequest.requester AND Practitioner object and reference to Practictioner
            //  4. ORC.15 to ServiceRequest.occurrenceDateTime
            //  5. ORC.16 is set to a reason code (secondary to OBR.31, which is purposely not present in this case)
            + "ORC|RE|248648498^|248648498^|ML18267-C00001^Beaker|||||20120628071200|||5742200012^Radon^Nicholas^^^^^^NPI^L^^^NPI||(781)849-2400^^^^^781^8492400|20170917151717|042^Human immunodeficiency virus [HIV] disease [42]^I9CDX^^^^29|||||ATRIUS HEALTH, INC^D^^^^POCFULL^XX^^^1020|P.O. BOX 415432^^BOSTON^MA^02241-5432^^B|898-7980^^8^^^800^8987980|111 GROSSMAN DRIVE^^BRAINTREE^MA^02184^USA^C^^NORFOLK|||||||\n"
            //  10. ORB.16 is here as a test to show that it is ignored as Practitioner because ORC.12 has priority 
            //  11. ORB.31 is omitted purposely so the ORC.16 is used for the reason code
            + "OBR|1|248648498^|248648498^|83036E^HEMOGLOBIN A1C^PACSEAP^^^^^^HEMOGLOBIN A1C|||||||L||E11.9^Type 2 diabetes mellitus without complications^ICD-10-CM^^^^^^Type 2 diabetes mellitus without complications|||54321678^SCHMIDT^FRIEDA^^MD^^^^^^^^^NPI|(781)849-2400^^^^^781^8492400|||||20180924152900|||F||^^^20120613071200|||||&Roache&Gerard&&||||||||||||||||||\n"
            + "TQ1|1||||||20180924152721|20180924235959|R\n"
            + "OBX|1|NM|17985^GLYCOHEMOGLOBIN HGB A1C^LRR^^^^^^GLYCOHEMOGLOBIN HGB A1C||5.6|%|<6.0||||F|||20180924152700||9548^ROACHE^GERARD^^|||20180924152903||||HVMA DEPARTMENT OF PATHOLOGY AND LAB MEDICINE^D|152 SECOND AVE^^NEEDHAM^MA^02494-2809^^B|\n"
            + "SPM|1|||^^^^^^^^Blood|||||||||||||20180924152700|20180924152755||||||\n";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
    assertThat(json).isNotBlank();

    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();

    List<Resource> serviceRequestList = e.stream()
        .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    // Important that we have exactly one service request (no duplication).  OBR creates it as a reference.        
    assertThat(serviceRequestList).hasSize(1);
    ServiceRequest serviceRequest = getServiceRequest(serviceRequestList.get(0));
    assertThat(serviceRequest.hasStatus()).isTrue();

    // ORC.4 should create a requisition in the serviceRequest.
    assertThat(serviceRequest.hasRequisition()).isTrue();
    assertThat(serviceRequest.getRequisition().hasSystem()).isTrue();
    assertThat(serviceRequest.getRequisition().getSystem()).isEqualToIgnoringCase("urn:id:Beaker");
    assertThat(serviceRequest.getRequisition().hasValue()).isTrue();
    assertThat(serviceRequest.getRequisition().getValue()).isEqualToIgnoringCase("ML18267-C00001");

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
        "Human immunodeficiency virus [HIV] disease [42]", "http://terminology.hl7.org/CodeSystem/ICD-9CM-diagnosiscodes",
        "Human immunodeficiency virus [HIV] disease [42]");

    // ORC.12 should create an ServiceRequest.requester reference
    assertThat(serviceRequest.hasRequester()).isTrue();
    String requesterRef = serviceRequest.getRequester().getReference();
    // assertThat(serviceRequest.getOccurrenceDateTimeType().toString()).containsPattern("2017-09-17T15:17:17");

    // Find the practitioner resources from the FHIR bundle.
    List<Resource> practitioners = e.stream()
        .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(practitioners.size()).isPositive(); // Confirm there is at least one practitioner
    // Find all practitioners with matching Id's in the list of practitioners.
    List<Resource> matchingPractitioners = new ArrayList<Resource>();
    for (int i = 0; i < practitioners.size(); i++) {
      if (practitioners.get(i).getId().equalsIgnoreCase(requesterRef)) {
        matchingPractitioners.add(practitioners.get(i));
      }
    }
    assertThat(matchingPractitioners).hasSize(1); // Count must be exactly 1.  
    // Confirm that the matching practitioner by ID has the correct content (simple validation)
    // Should be ORC.12.1 and NOT OBR.16.1
    Practitioner pract = (Practitioner) matchingPractitioners.get(0);
    assertThat(pract.getIdentifierFirstRep().getValue()).isEqualTo("5742200012");

    // Get the DiagnosticReport and see that it's basedOn cross-references to the ServiceRequest object
    List<Resource> diagnosticReportList = e.stream()
        .filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(diagnosticReportList).hasSize(1);
    DiagnosticReport diagnosticReport = getDiagnosticReport(diagnosticReportList.get(0));

    assertThat(diagnosticReport.hasBasedOn()).isTrue();
    assertThat(diagnosticReport.getBasedOn()).hasSize(1);
    //Check that the cross reference is equal to the serviceRequest id
    assertThat(diagnosticReport.getBasedOnFirstRep().getReference()).hasToString(serviceRequest.getId());

  }

  // This test is a companion to testBroadORCFields.   ORC and OBR records often have repeated data; one taking priority over the other.
  // Read comments carefully.  This often test the other condition and even the opposite to the tests in testBroadORCFields.
  @Test
  public void testBroadORCPlusOBRFields() {

    String hl7message = "MSH|^~\\&|Epic|ATRIUS|||20180924152907|34001|ORU^R01^ORU_R01|213|T|2.6|||||||||PHLabReport-Ack^^2.16.840.1.114222.4.10.3^ISO||\n"
        + "SFT|Epic Systems Corporation^L^^^^ANSI&1.2.840&ISO^XX^^^1.2.840.114350|Epic 2015 |Bridges|8.2.2.0||20160605043244\n"
        + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
        + "PV1|1|||||||||||||||||||||||||||||||||||||||||||20180924152707|\n"
        //  NOTE: ORC is optional; OBR is required.
        //  Key input data set up:
        //  1. ORC.15 is empty, so OBR.7 used as ServiceRequest.occurrenceDateTime
        //  2. Leave ORC.9 empty so that OBR.6 is used for ServiceRequest.authoredOn
        //  3. OBR.22 used and DiagnosticReport.issued
        //  4. Leave ORC.12 empty so OBR.16 is used for Practitioner reference
        //  5. ORC.16 is set to a reason code (but it is ignored because it is secondary to OBR.31, which is present in this case and therefore overrides ORC.16)
        + "ORC|RE|248648498^|248648498^|ML18267-C00001^Beaker||||||||||(781)849-2400^^^^^781^8492400||042^Human immunodeficiency virus [HIV] disease [42]^I9CDX^^^^29|||||ATRIUS HEALTH, INC^D^^^^POCFULL^XX^^^1020|P.O. BOX 415432^^BOSTON^MA^02241-5432^^B|898-7980^^8^^^800^8987980|111 GROSSMAN DRIVE^^BRAINTREE^MA^02184^USA^C^^NORFOLK|||||||\n"
        + "OBR|1|248648498^|248648498^|83036E^HEMOGLOBIN A1C^PACSEAP^^^^^^HEMOGLOBIN A1C||20120606120606|20170707150707||||L||E11.9^Type 2 diabetes mellitus without complications^ICD-10-CM^^^^^^Type 2 diabetes mellitus without complications|||54321678^SCHMIDT^FRIEDA^^MD^^^^^^^^^NPISER|(781)849-2400^^^^^781^8492400|||||20180924152900|||F||^^^20120613071200||||HIV^HIV/Aids^L^^^^V1|&Roache&Gerard&&||||||||||||||||||\n"
        + "TQ1|1||||||20180924152721|20180924235959|R\n"
        + "OBX|1|NM|17985^GLYCOHEMOGLOBIN HGB A1C^LRR^^^^^^GLYCOHEMOGLOBIN HGB A1C||5.6|%|<6.0||||F|||20180924152700||9548^ROACHE^GERARD^^|||20180924152903||||HVMA DEPARTMENT OF PATHOLOGY AND LAB MEDICINE^D|152 SECOND AVE^^NEEDHAM^MA^02494-2809^^B|\n"
        + "SPM|1|||^^^^^^^^Blood|||||||||||||20180924152700|20180924152755||||||\n";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
    assertThat(json).isNotBlank();

    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();

    List<Resource> serviceRequestList = e.stream()
        .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    // Important that we have exactly one service request (no duplication).  OBR creates it as a reference.        
    assertThat(serviceRequestList).hasSize(1);
    ServiceRequest serviceRequest = getServiceRequest(serviceRequestList.get(0));
    assertThat(serviceRequest.hasStatus()).isTrue();

    // ORC.4 checked in testBroadORCFields
    // OBR.5 should create basedOn because ORC.9 is not filled in
    assertThat(serviceRequest.hasAuthoredOn()).isTrue();
    assertThat(serviceRequest.getAuthoredOnElement().toString()).containsPattern("2012-06-06T12:06:06");

    // ORC.15 should create an ServiceRequest.occurrenceDateTime date
    assertThat(serviceRequest.hasOccurrenceDateTimeType()).isTrue();
    assertThat(serviceRequest.getOccurrenceDateTimeType().toString()).containsPattern("2017-07-07T15:07:07");

    // OBR.31 should create the ServiceRequest.reasonCode CWE
    assertThat(serviceRequest.hasReasonCode()).isTrue();
    assertThat(serviceRequest.getReasonCode()).hasSize(1);
    DatatypeUtils.checkCommonCodeableConceptAssertions(serviceRequest.getReasonCodeFirstRep(), "HIV", "HIV/Aids",
        "urn:id:L", "HIV/Aids");

    // OBR.22 should create an ServiceRequest.requester reference
    assertThat(serviceRequest.hasRequester()).isTrue();
    String requesterRef = serviceRequest.getRequester().getReference();

    // Find the practitioner resources from the FHIR bundle.
    List<Resource> practitioners = e.stream()
        .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(practitioners.size()).isPositive(); // Confirm there is at least one practitioner
    // Find all practitioners with matching Id's in the list of practitioners.
    List<Resource> matchingPractitioners = new ArrayList<Resource>();
    for (int i = 0; i < practitioners.size(); i++) {
      if (practitioners.get(i).getId().equalsIgnoreCase(requesterRef)) {
        matchingPractitioners.add(practitioners.get(i));
      }
    }
    assertThat(matchingPractitioners).hasSize(1); // Count must be exactly 1.  
    // Confirm that the matching practitioner by ID has the correct content (simple validation)
    // Should be OBR.16.1 because ORC.12 is empty.
    Practitioner pract = (Practitioner) matchingPractitioners.get(0);
    assertThat(pract.getIdentifierFirstRep().getValue()).isEqualTo("54321678");

    List<Resource> diagnosticReportList = e.stream()
        .filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(diagnosticReportList).hasSize(1);
    DiagnosticReport diagnosticReport = getDiagnosticReport(diagnosticReportList.get(0));

    assertThat(diagnosticReport.hasBasedOn()).isTrue();
    assertThat(diagnosticReport.getBasedOn()).hasSize(1);

    // Check for issued instant of OBR.22
    assertThat(diagnosticReport.hasIssued()).isTrue();
    assertThat(diagnosticReport.getIssued().toInstant().toString()).contains("2018-09-24T07:29:00Z");
  }

  @Test
  public void testBroadORCPlusOBRFields2() {

    String hl7message =

        "MSH|^~\\&|Epic|ATRIUS|||20180924152907|34001|ORU^R01^ORU_R01|213|T|2.6|||||||||PHLabReport-Ack^^2.16.840.1.114222.4.10.3^ISO||\n"
            + "SFT|Epic Systems Corporation^L^^^^ANSI&1.2.840&ISO^XX^^^1.2.840.114350|Epic 2015 |Bridges|8.2.2.0||20160605043244\n"
            + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
            + "PV1|1|||||||||||||||||||||||||||||||||||||||||||20180924152707|\n"
            //  ORC.15 is empty, OBR.7 is empty, so use OBR-27[0].4 as ServiceRequest.occurrenceDateTime
            + "ORC|RE|248648498^|248648498^|ML18267-C00001^Beaker|||||20120628071200|||1457352338^TEITLEMAN^CHRISTOPHER^A^MD^^^^^^^^^NPISER||(781)849-2400^^^^^781^8492400|||||||ATRIUS HEALTH, INC^D^^^^POCFULL^XX^^^1020|P.O. BOX 415432^^BOSTON^MA^02241-5432^^B|898-7980^^8^^^800^8987980|111 GROSSMAN DRIVE^^BRAINTREE^MA^02184^USA^C^^NORFOLK|||||||\n"
            + "OBR|1|248648498^|248648498^|83036E^HEMOGLOBIN A1C^PACSEAP^^^^^^HEMOGLOBIN A1C|||||||L||E11.9^Type 2 diabetes mellitus without complications^ICD-10-CM^^^^^^Type 2 diabetes mellitus without complications|||1457352338^TEITLEMAN^CHRISTOPHER^A^MD^^^^^^^^^NPISER|(781)849-2400^^^^^781^8492400|||||20180924152900|||F||^^^20120606120606|||||&Roache&Gerard&&||||||||||||||||||\n"
            + "TQ1|1||||||20180924152721|20180924235959|R\n"
            + "OBX|1|NM|17985^GLYCOHEMOGLOBIN HGB A1C^LRR^^^^^^GLYCOHEMOGLOBIN HGB A1C||5.6|%|<6.0||||F|||20180924152700||9548^ROACHE^GERARD^^|||20180924152903||||HVMA DEPARTMENT OF PATHOLOGY AND LAB MEDICINE^D|152 SECOND AVE^^NEEDHAM^MA^02494-2809^^B|\n"
            + "SPM|1|||^^^^^^^^Blood|||||||||||||20180924152700|20180924152755||||||\n";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, PatientUtils.OPTIONS);
    assertThat(json).isNotBlank();

    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();

    List<Resource> serviceRequestList = e.stream()
        .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    // Important that we have exactly one service request (no duplication).  OBR creates it as a reference.        
    assertThat(serviceRequestList).hasSize(1);
    ServiceRequest serviceRequest = getServiceRequest(serviceRequestList.get(0));
    assertThat(serviceRequest.hasStatus()).isTrue();

    // OBR.27[0].4 should create an ServiceRequest.occurrenceDateTime date
    assertThat(serviceRequest.hasOccurrenceDateTimeType()).isTrue();
    assertThat(serviceRequest.getOccurrenceDateTimeType().toString()).containsPattern("2012-06-06T12:06:06");

  }


  private static ServiceRequest getServiceRequest(Resource resource) {
    String s = context.getParser().encodeResourceToString(resource);
    Class<? extends IBaseResource> klass = ServiceRequest.class;
    return (ServiceRequest) context.getParser().parseResource(klass, s);
  }

  private static DiagnosticReport getDiagnosticReport(Resource resource) {
    String s = context.getParser().encodeResourceToString(resource);
    Class<? extends IBaseResource> klass = DiagnosticReport.class;
    return (DiagnosticReport) context.getParser().parseResource(klass, s);
  }

}