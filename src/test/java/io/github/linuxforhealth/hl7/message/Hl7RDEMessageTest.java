/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hl7RDEMessageTest {
  private static FHIRContext context = new FHIRContext();
  private static final ConverterOptions OPTIONS = new Builder().withValidateResource().build();
  private static final Logger LOGGER = LoggerFactory.getLogger(Hl7RDEMessageTest.class);


  @Test
  public void test_RDEO11_patient_encounter_present() throws IOException {
	  String hl7message =
		        "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O11|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\n"
		        + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
		        + "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a4d960d-c528-45c9-bb10-7e9929968247|||||||||||||||||||||||||20210407191342\n"
		        + "ORC|RE|ACCESSION_62fd10ea-522a-4261-837a-30381a5aa04a|ACCESSION_62fd10ea-522a-4261-837a-30381a5aa04a|3200|||^Every 24 hours&1500^^20210330150000^^ROU||20210407191342|2739^BY^ENTERED|2799^BY^VERIFIED|3122^PROVIDER^ORDERING||(696)901-1300|20210407191342||||||ORDERING FAC NAME|ADDR^^CITY^STATE^ZIP^USA|(515)-290-8888|9999^^CITY^STATE^ZIP^CAN|||||I\n"
		        + "RXO|DEFAULTMED\n"
		        + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||12345^LNAME^FNAME\n"
		        + "RXA|0|23|20210407191342|20210407191342|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|||||12345^LNAME^FNAME|BN6T3|100^mL/hr||||||||RateChange||20210407191342\n"
		        + "ZXC||00409-7335-03^CEFTRIAXONE SODIUM 2 G IJ SOLR^REPNDC\n"
		        + "ZCL|hcm-prod1862018998731\n";
                        // TODO: RXA, ZXC, ZCL are not in RDE_o11.

      HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
      String json = ftv.convert(hl7message, OPTIONS);
      assertThat(json).isNotBlank();
      IBaseResource bundleResource = context.getParser().parseResource(json);
      assertThat(bundleResource).isNotNull();
      Bundle b = (Bundle) bundleResource;
      List<BundleEntryComponent> e = b.getEntry();
      
      List<Resource> patientResource = e.stream()
              .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(patientResource).hasSize(1);

      List<Resource> encounterResource = e.stream()
              .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(encounterResource).hasSize(1);

      // TODO: Add check for Medication Request

  }

  
  @Test
  public void test_RDEO11_patient_observation_present() throws IOException {
	  String hl7message =
		        "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O11|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\n"
		        + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
		        + "ORC|RE|111|111|3200|||^Every 24 hours&1500^^20210330150000^^ROU||20210407191342|2739^BY^ENTERED|2799^BY^VERIFIED|3122^PROVIDER^ORDERING||(696)901-1300|20210407191342||||||ORDERING FAC NAME|ADDR^^CITY^STATE^ZIP^USA|(515)-290-8888|9999^^CITY^STATE^ZIP^CAN|||||I\n"
		        + "RXO|DEFAULTMED\n"
		        + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||12345^LNAME^FNAME\n"
		        + "RXA|0|23|20210407191342|20210407191342|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|||||12345^LNAME^FNAME|BN6T3|100^mL/hr||||||||RateChange||20210407191342\n"
		        + "OBX|1|NM|Most Current Weight^Most current measured weight (actual)||90|kg";
                        // TODO: RXA is not in RDE_o11 messages
                        // TODO: Add check for MedicationRequest, ORC and RXE are required, and RXO is optional.  ORC + RXE is repeatable
		        
      HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
      String json = ftv.convert(hl7message, OPTIONS);
      assertThat(json).isNotBlank();
      IBaseResource bundleResource = context.getParser().parseResource(json);
      assertThat(bundleResource).isNotNull();
      Bundle b = (Bundle) bundleResource;
      List<BundleEntryComponent> e = b.getEntry();

      List<Resource> patientResource = e.stream()
              .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(patientResource).hasSize(1);

      List<Resource> observationResource = e.stream()
              .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(observationResource).hasSize(1);
  }

  @Test
  public void test_RDEO11_medrequest_present() throws IOException {
	  String hl7message =
		        "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O11|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\n"
		        + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
		        + "ORC|RE|ACCESSION_62fd10ea-522a-4261-837a-30381a5aa04a|ACCESSION_62fd10ea-522a-4261-837a-30381a5aa04a|3200|||^Every 24 hours&1500^^20210330150000^^ROU||20210407191342|2739^BY^ENTERED|2799^BY^VERIFIED|3122^PROVIDER^ORDERING||(696)901-1300|20210407191342||||||ORDERING FAC NAME|ADDR^^CITY^STATE^ZIP^USA|(515)-290-8888|9999^^CITY^STATE^ZIP^CAN|||||I\n"
		        + "RXO|DEFAULTMED\n"
		        + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||12345^LNAME^FNAME\n";

      HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
      String json = ftv.convert(hl7message, OPTIONS);
      assertThat(json).isNotBlank();
      IBaseResource bundleResource = context.getParser().parseResource(json);
      assertThat(bundleResource).isNotNull();
      Bundle b = (Bundle) bundleResource;
      List<BundleEntryComponent> e = b.getEntry();

      List<Resource> resourceList = e.stream()
              .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(resourceList).hasSize(1);

  }

  @Test
  public void test_RDEO25_patient_encounter_present() throws IOException {
	  String hl7message =
		        "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O25|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\n"
		        + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
		        + "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a4d960d-c528-45c9-bb10-7e9929968247|||||||||||||||||||||||||20210407191342\n"
		        + "ORC|RE|ACCESSION_62fd10ea-522a-4261-837a-30381a5aa04a|ACCESSION_62fd10ea-522a-4261-837a-30381a5aa04a|3200|||^Every 24 hours&1500^^20210330150000^^ROU||20210407191342|2739^BY^ENTERED|2799^BY^VERIFIED|3122^PROVIDER^ORDERING||(696)901-1300|20210407191342||||||ORDERING FAC NAME|ADDR^^CITY^STATE^ZIP^USA|(515)-290-8888|9999^^CITY^STATE^ZIP^CAN|||||I\n"
		        + "RXO|DEFAULTMED\n"
		        + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||12345^LNAME^FNAME\n";

      HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
      String json = ftv.convert(hl7message, OPTIONS);
      assertThat(json).isNotBlank();
      LOGGER.info("FHIR json result:\n" + json);
      IBaseResource bundleResource = context.getParser().parseResource(json);
      assertThat(bundleResource).isNotNull();
      Bundle b = (Bundle) bundleResource;
      List<BundleEntryComponent> e = b.getEntry();
      List<Resource> patientResource = e.stream()
              .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(patientResource).hasSize(1);

      List<Resource> encounterResource = e.stream()
              .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(encounterResource).hasSize(1);

  }

  @Test
  public void test_RDEO25_medrequest_present() throws IOException {
	  String hl7message =
		        "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O25|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\n"
		        + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
		        + "ORC|RE|ACCESSION_62fd10ea-522a-4261-837a-30381a5aa04a|ACCESSION_62fd10ea-522a-4261-837a-30381a5aa04a|3200|||^Every 24 hours&1500^^20210330150000^^ROU||20210407191342|2739^BY^ENTERED|2799^BY^VERIFIED|3122^PROVIDER^ORDERING||(696)901-1300|20210407191342||||||ORDERING FAC NAME|ADDR^^CITY^STATE^ZIP^USA|(515)-290-8888|9999^^CITY^STATE^ZIP^CAN|||||I\n"
		        + "RXO|DEFAULTMED\n"
		        + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||12345^LNAME^FNAME\n";

      HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
      String json = ftv.convert(hl7message, OPTIONS);
      assertThat(json).isNotBlank();
      LOGGER.info("FHIR json result:\n" + json);
      IBaseResource bundleResource = context.getParser().parseResource(json);
      assertThat(bundleResource).isNotNull();
      Bundle b = (Bundle) bundleResource;
      List<BundleEntryComponent> e = b.getEntry();
      
      List<Resource> resourceList = e.stream()
              .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(resourceList).hasSize(1);
  }
  
  @Test
  public void test_RDEO25_patient_observation_present() throws IOException {
	  String hl7message =
		        "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210407191342|25739|RDE^O25|MSGID_f209e83f-20db-474d-a7ae-82e5c3894273|T|2.6\n"
		        + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
		        + "ORC|RE|111|111|3200|||^Every 24 hours&1500^^20210330150000^^ROU||20210407191342|2739^BY^ENTERED|2799^BY^VERIFIED|3122^PROVIDER^ORDERING||(696)901-1300|20210407191342||||||ORDERING FAC NAME|ADDR^^CITY^STATE^ZIP^USA|(515)-290-8888|9999^^CITY^STATE^ZIP^CAN|||||I\n"
		        + "RXE|^Q24H&0600^^20210407191342^^ROU|DEFAULTMED^cefTRIAXone (ROCEPHIN) 2 g in sodium chloride 0.9 % 50 mL IVPB|2||g||||||||12345^LNAME^FNAME\n"
		        + "OBX|1|NM|Most Current Weight^Most current measured weight (actual)||90|kg\n";

      HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
      String json = ftv.convert(hl7message, OPTIONS);
      assertThat(json).isNotBlank();
      LOGGER.info("FHIR json result:\n" + json);
      IBaseResource bundleResource = context.getParser().parseResource(json);
      assertThat(bundleResource).isNotNull();
      Bundle b = (Bundle) bundleResource;
      List<BundleEntryComponent> e = b.getEntry();
      List<Resource> patientResource = e.stream()
              .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(patientResource).hasSize(1);

      List<Resource> observationResource = e.stream()
              .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(observationResource).hasSize(1);
  }
}
