/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

public class IssueFixTest {

  // Note: test oru_issue_81 has been removed. It is superceded by testBroadORCPlusOBRFields which specifically tests that 
  // DiagnosticReport.issued instant comes from OBR.22    

  /**
   * In order to generate messageHeader resource, MSH should have MSH.24.2 as this is required
   * attribute for source attribute, and source is required for MessageHeader resource.
   * 
   * @throws IOException
   */
  @Test
  public void message_header_issue_76() throws IOException {

    String hl7message = "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE|764|ASCII||||||\r"
        + "EVN||201209122222\r"
        + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
        + "OBX|1|TX|1234||ECHOCARDIOGRAPHIC REPORT||||||F||||||\r"
        + "AL1|1|DRUG|00000741^OXYCODONE||HYPOTENSION\r"
        + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625";

    List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);
    List<Resource> messageHeaders = ResourceUtils.getResourceList(e, ResourceType.MessageHeader);
    assertThat(messageHeaders).isEmpty();
  }

  @Test
  public void vxu_issue_75() {
    String hl7vxu = "MSH|^~\\&|||||201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
        + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
        + "ORC|RE||IZ-783278^NDA||||||||||||||MIIC^MIIC clinic^HL70362\n"
        // Purposely incorrect date in RXA.3 (see below)
        + "RXA|0|1|201501011|20150101|141^Influenza^CVX|1|mL||00^NEW IMMUNIZATIONRECORD^NIP001||^^^MIICSHORTCODE||||ABC1234|20211201|SKB^GlaxoSmithKline^MVX|||CP|A\n"
        + "OBX|4|CE|31044-1^reaction^LN|4|||||||F\n"
        + "ORC|RE||IZ-783280^NDA|||||||||||||||\n"
        // Purposely correct date in RXA.3 (see below)
        + "RXA|0|1|20170512||998^No vaccine given^CVX|999||||||||||||||CP|\n"
        + "OBX|1|CE|30945-0^contraindication^LN|1|M4^Medical exemption: Influenza^NIP||||||F|||20120916  \n";

    List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7vxu);
    List<Resource> immunizations = ResourceUtils.getResourceList(e, ResourceType.Immunization);
    // Since "RXA|0|1|201501011| " RXA.3 has incorrect date, no immunization resource is generated
    // as occurrenceDateTime is required field and is extracted from RXA.3
    // But the second RXA.3 is correctly processed
    assertThat(immunizations).hasSize(1);

  }

  // NOTE:  name_issue_92 was superceded by testing in patientNameTest, and is removed.

}
