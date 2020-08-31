package com.ibm.whi.hl7.message;

import java.io.IOException;
import org.junit.Test;
import org.python.google.common.collect.Lists;
import com.ibm.whi.hl7.resource.ResourceModel;
import ca.uhn.hl7v2.HL7Exception;


public class Hl7MessageTest {

  @Test
  public void test_patient() throws HL7Exception, IOException {

    ResourceModel rsm = ResourceModel.generateResourceModel("resource/Patient");
    FHIRResource patient = new FHIRResource("Patient", "PID", rsm, 0);
    Hl7Message message = new Hl7Message("ADT", Lists.newArrayList(patient));
    String hl7message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||";
    System.out.println(message.convertMessageToFHIRResource(hl7message));
  }


  @Test
  public void test_patient_encounter() throws HL7Exception, IOException {

    ResourceModel rsm= ResourceModel.generateResourceModel("resource/Patient");
    FHIRResource patient = new FHIRResource("Patient", "PID", rsm, 0);
    ResourceModel encounter = ResourceModel.generateResourceModel("resource/Encounter");
    FHIRResource encounterFH =
        new FHIRResource("Encounter", "PV1", encounter, 0, false, Lists.newArrayList("PV2"));



    Hl7Message message = new Hl7Message("ADT", Lists.newArrayList(patient, encounterFH));
    String hl7message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||";
    System.out.println(message.convertMessageToFHIRResource(hl7message));
  }

}
