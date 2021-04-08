package io.github.linuxforhealth.hl7;




import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Arrays;


public class HapiValidationTest {

    private HapiContext ctx = new DefaultHapiContext();

    @Test
    public void testValidMessage() throws HL7Exception {

        String validMsg = "MSH|^~\\&|PROSLOV|MYHOSPITAL|WHIA|IBM|20170215080000||RDE^O11^RDE_O11|MSGID005520|T|2.6\r" +
                "PID|1||000054321^^^MRN||COOPER^SHELDON^||19820512|M|||765 SOMESTREET RD UNIT 3A^^PASADENA^LA^|||||S||78654|\r" +
                "PV1||I|6N^1234^A^GENERAL HOSPITAL2||||0100^ANDERSON,CARL|0148^ADDISON,JAMES||SUR|||||||0148^ANDERSON,CARL|S|8846511|A|||||||||||||||||||SF|K||||20170215080000\r" +
                "PV2|priorpendingpoint|accomcode|admitreason^Prescribed Medications|transferreason|patientvalu|patientvaluloc|PH|20170315|20060316|2|4|visit description|referral1~referral2|20170411|Y|P|20170415|FP|Y|1|F|Y|General Hospital^^^^^^^GH|AI|2|20170316|05|20170318|20170318|chargecode|RC|Y|20170315|Y|Y|Y|Y|P|K|AC|A|A|N|Y|DNR|20170101|20170401|201702140900|O\r" +
                "ORC|NW|PON001^OE|CD2017071101^RX|||E|10^BID^D4^^^R||20170215080000\r" +
                "TQ1|||BID|||10^MG|20170215080000|20170228080000\r" +
                "RXO|RX700001^DOCUSATE SODIUM 100 MG CAPSULE|100||mg|||||G||10||5|\r" +
                "RXR|^MTH\r" +
                "RXC|B|Ampicillin 250|250|MG\r" +
                "RXE|^^^20170923230000^^R|999^Ampicillin 250 MG TAB^NDC|100||mg|||||10||5|\r" +
                "TQ1|||BID|||10^MG|20170215080000|20170228080000\r" +
                "RXR|^MTH\r" +
                "ORC|NW|F700002^OE|P100002^RX|||E|40^QID^D10^^^R||20170215080000\r" +
                "TQ1|||QID|||40^MG|20170215080000|20170222080000\r" +
                "RXO|RX700002^colchicine 0.6 mg capsule|0.6||mg|||||G||40||2|||Y|day|3|units|indication|rate|rateunits\r" +
                "RXR|^NS\r" +
                "RXC|B|D5/.45NACL|1000|ML\r" +
                "RXE|^^^20170923230000^^R|999^D5/.45NACL|1000||ml|||||40||2|\r" +
                "TQ1|||QID|||40^MG|20170215080000|20170222080000\r" +
                "RXR|^NS\r" +
                "ORC|NW|F700003^OE|P100003^RX|||E|20^QSHIFT^D10^^^R||20170202080000\r" +
                "TQ1|||QSHIFT|||20^MG|20170202080000|20170204080000\r" +
                "RXO|RX700003^thyroxine 0.05 MG|0.05||mg|||||G||20||2|||Y|day|3|units|indication|rate|rateunits\r" +
                "RXR|^PR\r" +
                "RXE|^^^20170923230000^^R|999^thyroxine 0.05 MG|0.05||mg|||||20||2|\r" +
                "TQ1|||QSHIFT|||20^MG|20170202080000|20170204080000\r" +
                "RXR|^PR\r" +
                "ORC|NW|F700004^OE|P100004^RX|||E|5^QHS^D4^^^R||20170125080000\r" +
                "TQ1|||QHS|||5^ML|20170125080000|20170325080000\r" +
                "RXO|RX700004^metformin 850 ml orally|850||ml|||||G||5||5|\r" +
                "RXR|^PO\r" +
                "RXE|^^^20170923230000^^R|999^metformin 850 ml|850||ml|||||3||2|\r" +
                "TQ1|||QHS|||5^ML|20170125080000|20170325080000\r" +
                "RXR|^PO\r" +
                "ORC|NW|F700005^OE|P100005^RX|||E|8^TID^D3^^^R||20170125080000\r" +
                "TQ1|||TID|||8^MG|20170125080000|20171125230000\r" +
                "RXO|RX700005^OxyContin 20 MG CAPSULE|20||mg|||||G||8||3|\r" +
                "RXR|^EP\r" +
                "RXE|^^^20170923230000^^R|999^OxyContin 20 MG CAPSULE|20||mg|||||5||3|\r" +
                "TQ1|||TID|||8^MG|20170125080000|20171125230000\r" +
                "RXR|^EP";

        String m = "MSH|^~\\&|NISTEHRAPP|NISTEHRFAC|NISTIISAPP|NISTIISFAC|20150625072816.601-0500||VXU^V04^VXU_V04|NIST-IZ-AD-10.1_Send_V04_Z22|P|2.5.1|||ER|AL|||||Z22^CDCPHINVS|NISTEHRFAC|NISTIISFAC" +
                "PID|1||21142^^^NIST-MPI-1^MR||Vasquez^Manuel^Diego^^^^L||19470215|M||2106-3^White^CDCREC|227 Park Ave^^Bozeman^MT^59715^USA^P||^PRN^PH^^^406^5555815~^NET^^Manuel.Vasquez@isp.com|||||||||2135-2^Hispanic or Latino^CDCREC||N|1|||||N\r" +
                "PD1|||||||||||01^No reminder/recall^HL70215|N|20150625|||A|20150625|20150625ORC|RE||31165^NIST-AA-IZ-2|||||||7824^Jackson^Lily^Suzanne^^^^^NIST-PI-1^L^^^PRN|||||||NISTEHRFAC^NISTEHRFacility^HL70362\r" +
                "RXA|0|1|20141021||152^Pneumococcal Conjugate, unspecified formulation^CVX|999|||01^Historical Administration^NIP001|||||||||||CP|A";

        Message msg = ctx.getGenericParser().parse(validMsg);
        //System.out.println(msg.get("ORDER").getName());


        for(String c: msg.getNames()) {
            System.out.println(c);
        }


        String[] expectedNames = new String[] {"MSH", "EVN", "PID", "PV1", "PV2", "IN1"};

    }
}

