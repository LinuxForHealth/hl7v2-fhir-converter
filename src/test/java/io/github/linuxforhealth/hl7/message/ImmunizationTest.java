/*
 * (C) Copyright IBM Corp. 2020, 2021
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
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import com.google.common.collect.Lists;
import io.github.linuxforhealth.api.ResourceModel;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.resource.ResourceReader;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

public class ImmunizationTest {
    private static FHIRContext context = new FHIRContext(true, false);
    private static final ConverterOptions OPTIONS = new Builder().withValidateResource().build();

    private static HL7MessageEngine engine = new HL7MessageEngine(context);

    @Test
    public void test_vxuv04_patient_encounter_present() throws IOException {
  	  String hl7message =
  		        "MSH|^~\\&|EHR|12345^SiteName|MIIS|99990|20140701041038||VXU^V04^VXU_V04|MSG.Valid_01|P|2.6|||\n"
  		        + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
  		        + "NK1|1|mother^patient|MTH^Mother^HL70063|5 elm st^^boston^MA^01234^^P|781-999-9999^PRN^PH^^1^781^9999999|||||||||||||||||01^No reminder/recall^HL70215\n"
  		        + "PV1|1|R||||||||||||||||||V01^20120901041038\n"
  		        + "IN1|1||8|Aetna Inc\n"
  		        + "ORC|RE||4242546^NameSpaceID||||||||||||||\n"
  		        + "RXA|0|1|20140701041038|20140701041038|48^HPV, quadrivalent^CVX|0.5|ml^MilliLiter [SI Volume Units]^UCUM||00^New Immunization^NIP001|NPI001^LastName^ClinicianFirstName^^^^Title^^AssigningAuthority|14509||||L987||MSD^Merck^MVX|||CP||20120901041038\n"
  		        + "RXR|C28161^Intramuscular^NCIT|LA^Leftarm^HL70163\n"
  		        + "OBX|1|CE|30963-3^ VACCINE FUNDING SOURCE^LN|1|VXC2^STATE FUNDS^HL70396||||||F|||20120901041038\n"
  		        + "OBX|2|CE|64994-7^Vaccine funding program eligibility category^LN|1|V01^Not VFC^HL70064||||||F|||20140701041038\n"
  		        + "OBX|3|TS|29768-9^DATE VACCINE INFORMATION STATEMENT PUBLISHED^LN|1|20010711||||||F|||20120720101321\n"
  		        + "OBX|4|TS|29769-7^DATE VACCINE INFORMATION STATEMENT PRESENTED^LN|1|19901207||||||F|||20140701041038";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS);
        assertThat(json).isNotBlank();
        System.out.println(json);
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
