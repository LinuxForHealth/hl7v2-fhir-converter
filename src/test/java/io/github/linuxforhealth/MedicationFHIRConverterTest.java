/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

public class MedicationFHIRConverterTest {

    private static final ConverterOptions OPTIONS = new Builder().withValidateResource().withPrettyPrint().build();

    @Test
    public void test_OMP_O09_message() {
        String omp_msg = "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|OMP^O09^OMP_O09|1|P^I|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "PID|||555444222111^^^MPI&GenHosp^MR||smith^john||19600614|M||C|99 Oakland #106^^Toronto^ON^44889||||||||343132266|||N\r"
                + "PD1|S|A|patientpfacisXON|primarycareXCN||||F|Y|duppatient|pubcode|Y|20060316|placeofworshipXON|DNR|A|20060101|20050101|USA||ACT\r"
                + "NTE|1|P|comment after PD1\r"
                + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON,CARL|0148^ADDISON,JAMES||SUR|||||||0148^ANDERSON,CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
                + "PV2|priorpendingpoint|accomcode|admitreason|transferreason|patientvalu|patientvaluloc|PH|20060315|20060316|2|4|visit description|referral1~referral2|20050411|Y|P|20060415|FP|Y|1|F|Y|clinicorgname|AI|2|20060316|05|20060318|20060318|chargecode|RC|Y|20060315|Y|Y|Y|Y|P|K|AC|A|A|N|Y|DNR|20060101|20060401|200602140900|O\r"
                + "IN1|1|insuranceplanA|insurancecoB\r"
                + "IN2|Insuredempid|||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||||Y|N|Y\r"
                + "GT1|6357||gname|gspousename|gaddress|||20060308|F|GT||111-22-3456|20060101|20060101|23|gempname|||||||||N||||||||M\r"
                + "AL1|1|DA|allergydescription|SV|allergyreasonxx\r" + "ORC|NW|1000^OE|9999999^RX|||E|40^QID^D10^^^R\r"
                + "TQ1||||||30^M|199401060930|199401061000\r"
                + "TQ2|1|S|relatedplacer|relatedfiller|placergroupnumber|EE|*|10^min|4|N\r"
                + "RXO|RX2111^Polycillin 500 mg TAB|500||MG|reqdosgform|providerspharm~prpharm2|providersadmin1~providersadmin2||G||40|dispenseunits|2|orderDEAisXCN|pharmverifidXCN|Y|day|3|units|indication|rate|rateunits\r"
                + "NTE|1|P|comment after RXO\r" + "RXR|^PO|\r" + "RXC|B|D5/.45NACL|1000|ML\r"
                + "NTE|1|P|comment after RXC\r" + "OBX|1|TX|^hunchback|1|Increasing||||||S\r"
                + "NTE|1|P|comment after OBX\r"
                + "FT1|1|||199805291115||CG|00378112001^VerapamilHydrochloride 120 mgTAB^NDC|||1|5&USD^TP\r"
                + "BLG|D|CH|accountid|01";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(omp_msg, OPTIONS);
        int expectedMedicationResourceCount = 1;
        verifyResult(json, Constants.DEFAULT_BUNDLE_TYPE, expectedMedicationResourceCount);
    }

    @Test
    public void test_RDE_O11_message() {
        String rde_msg = "MSH|^~\\&|PROSLOV|MYHOSPITAL|WHIA|IBM|20170215080000||RDE^O11^RDE_O11|MSGID005520|T|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                + "PID|1||000054321^^^MRN||COOPER^SHELDON^||19820512|M|||765 SOMESTREET RD UNIT 3A^^PASADENA^LA^|||||S||78654|\r"
                + "PV1||I|6N^1234^A^GENERAL HOSPITAL2||||0100^ANDERSON,CARL|0148^ADDISON,JAMES||SUR|||||||0148^ANDERSON,CARL|S|8846511|A|||||||||||||||||||SF|K||||20170215080000\r"
                + "PV2|priorpendingpoint|accomcode|admitreason^Prescribed Medications|transferreason|patientvalu|patientvaluloc|PH|20170315|20060316|2|4|visit description|referral1~referral2|20170411|Y|P|20170415|FP|Y|1|F|Y|General Hospital^^^^^^^GH|AI|2|20170316|05|20170318|20170318|chargecode|RC|Y|20170315|Y|Y|Y|Y|P|K|AC|A|A|N|Y|DNR|20170101|20170401|201702140900|O\r"
                + "ORC|NW|PON001^OE|CD2017071101^RX|||E|10^BID^D4^^^R||20170215080000\r"
                + "TQ1|||BID|||10^MG|20170215080000|20170228080000\r"
                + "RXO|RX700001^DOCUSATE SODIUM 100 MG CAPSULE|100||mg|||||G||10||5|\r" + "RXR|^MTH\r"
                + "RXC|B|Ampicillin 250|250|MG\r"
                + "RXE|^^^20170923230000^^R|999^Ampicillin 250 MG TAB^NDC|100||mg|123^test^ABC||||10||5|"
                + "TQ1|||BID|||10^MG|20170215080000|20170228080000\r" + "RXR|^MTH\r"
                + "ORC|NW|F700002^OE|P100002^RX|||E|40^QID^D10^^^R||20170215080000\r"
                + "TQ1|||QID|||40^MG|20170215080000|20170222080000\r"
                + "RXO|RX700002^colchicine 0.6 mg capsule|0.6||mg|||||G||40||2|||Y|day|3|units|indication|rate|rateunits\r"
                + "RXR|^NS\r" + "RXC|B|D5/.45NACL|1000|ML\r"
                + "RXE|^^^20170923230000^^R|999^D5/.45NACL|1000||ml|||||40||2|\r"
                + "TQ1|||QID|||40^MG|20170215080000|20170222080000\r" + "RXR|^NS\r"
                + "ORC|NW|F700003^OE|P100003^RX|||E|20^QSHIFT^D10^^^R||20170202080000\r"
                + "TQ1|||QSHIFT|||20^MG|20170202080000|20170204080000\r"
                + "RXO|RX700003^thyroxine 0.05 MG|0.05||mg|||||G||20||2|||Y|day|3|units|indication|rate|rateunits\r"
                + "RXR|^PR\r" + "RXE|^^^20170923230000^^R|999^thyroxine 0.05 MG|0.05||mg|||||20||2|\r"
                + "TQ1|||QSHIFT|||20^MG|20170202080000|20170204080000\r" + "RXR|^PR\r"
                + "ORC|NW|F700004^OE|P100004^RX|||E|5^QHS^D4^^^R||20170125080000\r"
                + "TQ1|||QHS|||5^ML|20170125080000|20170325080000\r"
                + "RXO|RX700004^metformin 850 ml orally|850||ml|||||G||5||5|\r" + "RXR|^PO\r"
                + "RXE|^^^20170923230000^^R|999^metformin 850 ml|850||ml|||||3||2|\r"
                + "TQ1|||QHS|||5^ML|20170125080000|20170325080000\r" + "RXR|^PO\r"
                + "ORC|NW|F700005^OE|P100005^RX|||E|8^TID^D3^^^R||20170125080000\r"
                + "TQ1|||TID|||8^MG|20170125080000|20171125230000\r"
                + "RXO|RX700005^OxyContin 20 MG CAPSULE|20||mg|||||G||8||3|\r" + "RXR|^EP\r"
                + "RXE|^^^20170923230000^^R|999^OxyContin 20 MG CAPSULE|20||mg|||||5||3|\r"
                + "TQ1|||TID|||8^MG|20170125080000|20171125230000\r" + "RXR|^EP";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(rde_msg, OPTIONS);
        int expectedMedicationResourceCount = 5;
        verifyResult(json, Constants.DEFAULT_BUNDLE_TYPE, expectedMedicationResourceCount);
    }

    @Disabled("Need to find test RDE_O25 message")
    @Test
    public void test_RDE_O25_message() {
        String rde_msg = "TODO";
        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(rde_msg, OPTIONS);
        int expectedMedicationResourceCount = 1;
        verifyResult(json, Constants.DEFAULT_BUNDLE_TYPE, expectedMedicationResourceCount);
    }

    private void verifyResult(String json, BundleType expectedBundleType, int expectedNumMedicationResources) {
        verifyResult(json, expectedBundleType, expectedNumMedicationResources, true);
    }

    private void verifyResult(String json, BundleType expectedBundleType, int expectedNumMedicationResources,
            boolean messageHeaderExpected) {

        FHIRContext context = new FHIRContext();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        assertThat(b.getType()).isEqualTo(expectedBundleType);
        assertThat(b.getId()).isNotNull();
        assertThat(b.getMeta().getLastUpdated()).isNotNull();

        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = e.stream()
                .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> medication = e.stream().filter(v -> ResourceType.Medication == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(medication).hasSize(expectedNumMedicationResources);

        if (messageHeaderExpected) {
            List<Resource> messageHeader = e.stream()
                    .filter(v -> ResourceType.MessageHeader == v.getResource().getResourceType())
                    .map(BundleEntryComponent::getResource).collect(Collectors.toList());
            assertThat(messageHeader).hasSize(1);
        }

    }

    // Tests that onset[x] is correctly set to PRB.17 if we have no PRB.16
    @Test
    public void practitonerCreatedFor() {

        String hl7message = "MSH|^~\\&|EHR|12345^SiteName|MIIS|99990|20140701041038||VXU^V04^VXU_V04|MSG.Valid_01|P|2.6|||\r"
        + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
        + "NK1|1|mother^patient|MTH^Mother^HL70063|5 elm st^^boston^MA^01234^^P|781-999-9999^PRN^PH^^1^781^9999999|||||||||||||||||01^No reminder/recall^HL70215\r"
        + "PV1|1|R||||||||||||||||||V01^20120901041038\r"
        + "IN1|1||8|Aetna Inc\r"
        + "ORC|RE||4242546^NameSpaceID||||||||||||||\r"
        + "RXA|0|1|20140701041038|20140701041038|48^HPV, quadrivalent^CVX|0.5|ml^MilliLiter [SI Volume Units]^UCUM||00^New Immunization^NIP001|NPI001^LastName^ClinicianFirstName^^^^Title^^AssigningAuthority|14509||||L987||MSD^Merck^MVX|||CP||20120901041038\r"
        + "RXR|C28161^Intramuscular^NCIT|LA^Leftarm^HL70163\r"
        + "OBX|1|CE|30963-3^ VACCINE FUNDING SOURCE^LN|1|VXC2^STATE FUNDS^HL70396||||||F|||20120901041038\r"
        + "OBX|2|CE|64994-7^Vaccine funding program eligibility category^LN|1|V01^Not VFC^HL70064||||||F|||20140701041038\r"
        + "OBX|3|TS|29768-9^DATE VACCINE INFORMATION STATEMENT PUBLISHED^LN|1|20010711||||||F|||20120720101321\r"
        + "OBX|4|TS|29769-7^DATE VACCINE INFORMATION STATEMENT PRESENTED^LN|1|19901207||||||F|||20140701041038\r";

        List<BundleEntryComponent> e =ResourceUtils.createHl7Segment(hl7message);

        // Find the practitioner from the FHIR bundle.
        List<Resource> practitionerResource = e.stream()
        .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        // Verify we have onw practitioner
        assertThat(practitionerResource).hasSize(1);

        // // Get practitioner Resource
        // Resource practitioner = practitionerResource.get(0);

    }

}
