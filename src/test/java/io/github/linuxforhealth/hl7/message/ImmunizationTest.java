/*
 * (C) Copyright IBM Corp. 2020
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
import org.junit.Test;
import com.google.common.collect.Lists;
import io.github.linuxforhealth.api.ResourceModel;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.resource.ResourceReader;

public class ImmunizationTest {
  private static FHIRContext context = new FHIRContext(true);
  private static HL7MessageEngine engine = new HL7MessageEngine(context);


  @Test
  public void test_immunization() throws IOException {

    ResourceModel rsm = ResourceReader.getInstance().generateResourceModel("resource/Immunization");

    HL7FHIRResourceTemplateAttributes attributes = new HL7FHIRResourceTemplateAttributes.Builder()
        .withResourceName("Immunization").withResourceModel(rsm).withGroup("ORDER")
        .withSegment(".RXA").withAdditionalSegments(Lists.newArrayList(".OBSERVATION.OBX", ".ORC"))
        .withRepeats(true).build();

    HL7FHIRResourceTemplate immunization = new HL7FHIRResourceTemplate(attributes);
    HL7MessageModel message = new HL7MessageModel("VXU_V04", Lists.newArrayList(immunization));
    String hl7VUXmessageRep =
        "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.5.1|||NE|AL||||||RI543763\r"
            + "PID|1||432155^^^^MR||Patient^Johnny^New^^^^L|Smith^Sally|20130414|M||2106-3^White^HL70005|123 Any St^^Somewhere^WI^54000^^M\r"
            + "NK1|1|Patient^Sally|MTH^mother^HL70063|123 Any St^^Somewhere^WI^54000^^M|^PRN^PH^^^608^5551212|||||||||||19820517||||eng^English^ISO639\r"
            + "ORC|RE||197027|||||||^Clerk^Myron||MD67895^Pediatric^MARY^^^^MD^^RIA|||||RI2050\r"
            + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|0.5|ML^^ISO+||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX|||CP|A\r"
            + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r"
            + "OBX|1|CE|64994-7^vaccine fund pgm elig cat^LN|1|V02^VFC eligible Medicaid/MedicaidManaged Care^HL70064||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r"
            + "OBX|2|CE|30956-7^Vaccine Type^LN|2|48^HIB PRP-T^CVX||||||F|||20130531\r"
            + "OBX|3|TS|29768-9^VIS Publication Date^LN|2|19981216||||||F|||20130531\r"
            + "OBX|4|TS|59785-6^VIS Presentation Date^LN|2|20130531||||||F|||20130531\r";
    String json = message.convert(hl7VUXmessageRep, engine);
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> immu =
        e.stream().filter(v -> ResourceType.Immunization == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(immu).hasSize(1);


  }



}
