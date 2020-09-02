package com.ibm.whi;

import java.io.IOException;
import org.junit.Test;
import ca.uhn.hl7v2.HL7Exception;


public class FHIRConverterTest {

  @Test
  public void test_patient_encounter() throws HL7Exception, IOException {

    String hl7message = "MSH|^~\\&|SE050|050|PACS|050|20120912||ADT^A01|102|T|2.7|||AL|NE\r"
        + "EVN||201209122222\r"
        + "PID|0010||ADTNew^^^1231||ADT01New||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
        + "PV1|1|ff|yyy|EL|||200^ATTEND_DOC_FAMILY_TEST^ATTEND_DOC_GIVEN_TEST|201^REFER_DOC_FAMILY_TEST^REFER_DOC_GIVEN_TEST|202^CONSULTING_DOC_FAMILY_TEST^CONSULTING_DOC_GIVEN_TEST|MED|||||B6|E|272^ADMITTING_DOC_FAMILY_TEST^ADMITTING_DOC_GIVEN_TEST||48390|||||||||||||||||||||||||201409122200|20000206031726\r"
        + "AL1|0001|DA|98798^problem|SV|sneeze|20120808\r";

    FHIRConverter ftv = new FHIRConverter();
    System.out.println(ftv.convert(hl7message));
  }


}
