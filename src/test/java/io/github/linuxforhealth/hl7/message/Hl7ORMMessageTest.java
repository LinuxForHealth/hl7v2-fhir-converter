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

import io.github.linuxforhealth.hl7.segments.Hl7EncounterFHIRConversionTest;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hl7ORMMessageTest {
  private static FHIRContext context = new FHIRContext();
  private static final ConverterOptions OPTIONS = new Builder().withValidateResource().build();
  private static final Logger LOGGER = LoggerFactory.getLogger(Hl7ORMMessageTest.class);



    @Test@Disabled
  public void test_ORMO01_patient_encounter_present() throws IOException {
	  String hl7message =
		        "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB|IBMWATSON_LAB|IBM|20210407191758||ORM^O01|MSGID_e30a3471-7afd-4aa2-a3d5-e93fd89d24b3|T|2.3\n"
		        + "PID|1||0a1f7838-4230-4752-b8f6-948b07c38b25^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator||9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900|||||Account_0a1f7838-4230-4752-b8f6-948b07c38b25|123-456-7890||||BIRTH PLACE\n"
		        + "PV1||IP|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting|IP^I|Visit_0a1f7838-4230-4752-b8f6-948b07c38b25|||||||||||||||||||||||||20210407191758\n"
		        + "PV2|||^|||X-5546||20210407191758|||||||||||||||\n"
		        + "ORC|SN|ACCESSION_a42990b7-4155-4404-81ef-e85158caed72|ACCESSION_a42990b7-4155-4404-81ef-e85158caed72|2950|||||20210407191758|2739^BY^ENTERED|2799^BY^VERIFIED|3122^PROVIDER^ORDERING||(696)901-1300|20210407191758||||||ORDERING FAC NAME|ADDR^^CITY^STATE^ZIP^USA|(515)-290-8888|9999^^CITY^STATE^ZIP^CAN\n"
		        + "OBR|1|ACCESSION_a42990b7-4155-4404-81ef-e85158caed72|ACCESSION_a42990b7-4155-4404-81ef-e85158caed72|4916^Diffusion-weighted imaging||20210331214400|20210407191758|20210407191758||||||20210331214600||1234^SOURCE^SPECIMEN^LNAME^FNAME^^^^^^^^^LABNAME||||W18562||||P|||^^^^^POCPR|660600^Doctor^FYI||||Result Interpreter\n";

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

}
