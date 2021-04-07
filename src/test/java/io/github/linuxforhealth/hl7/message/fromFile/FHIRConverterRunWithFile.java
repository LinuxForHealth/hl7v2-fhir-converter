package io.github.linuxforhealth.hl7.message.fromFile;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

public class FHIRConverterRunWithFile {
    //private static final ConverterOptions OPTIONS = new Builder().withValidateResource().withPrettyPrint().build();

    public static void main(String[] args) throws IOException {
        String filename = System.getProperty("hl7.filename");
        System.out.println("Converting file: " + filename);
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String everything = "";
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            everything = sb.toString();
        } finally {
            br.close();
        }

        String hl7message = 
//        		"MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
                //+ "EVN||201209122222\r"
//                + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
//                + "PV1|1|ff|yyy|EL|ABC||200^ATTEND_DOC_FAMILY_TEST^ATTEND_DOC_GIVEN_TEST|201^REFER_DOC_FAMILY_TEST^REFER_DOC_GIVEN_TEST|202^CONSULTING_DOC_FAMILY_TEST^CONSULTING_DOC_GIVEN_TEST|MED|||||B6|E|272^ADMITTING_DOC_FAMILY_TEST^ADMITTING_DOC_GIVEN_TEST||48390|||||||||||||||||||||||||201409122200|20150206031726\r"
                // Create 1 observation
                //                + "OBX|1|TX|1324||ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^TRDSE^Janetary~2913^MRTTE^Darren^F~3065^MGHOBT^Paul^J~4723^LOTHDEW^Robert^L|\r"
                //                + "OBX|2|TX|||[PII] Emergency Department||||||F|||20200802124455\r"
                //                + "OBX|3|TX|||ED Encounter Arrival Date: [ADDRESS] [PERSONALNAME]:||||||F|||20200802124455\r";

                // Create 0 observations
                //                + "OBX|1|TX|||ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^TRDSE^Janetary~2913^MRTTE^Darren^F~3065^MGHOBT^Paul^J~4723^LOTHDEW^Robert^L|\r"
                //                + "OBX|2|TX|||[PII] Emergency Department||||||F|||20200802124455\r"
                //                + "OBX|3|TX|||ED Encounter Arrival Date: [ADDRESS] [PERSONALNAME]:||||||F|||20200802124455\r";

                // Creates 3 observations
//                + "OBX|1|TX|1324||ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^TRDSE^Janetary~2913^MRTTE^Darren^F~3065^MGHOBT^Paul^J~4723^LOTHDEW^Robert^L|\r"
//                + "OBX|2|TX|1234||[PII] Emergency Department||||||F|||20200802124455\r"
//                + "OBX|3|TX|1234||ED Encounter Arrival Date: [ADDRESS] [PERSONALNAME]:||||||F|||20200802124455\r";

        // Creates 3 observations, and a diagnostic report pointing back to those
//        "MSH|^~\\&|WHI_Automation|IBM_Toronto_Lab|IMAGING_REPORT|Hartland|20200802124455||ORU^R01^ORU_R01|MSGID00231|T|2.6\r"
//        + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
//        + "PV1|1|ff|yyy|EL|ABC||200^ATTEND_DOC_FAMILY_TEST^ATTEND_DOC_GIVEN_TEST|201^REFER_DOC_FAMILY_TEST^REFER_DOC_GIVEN_TEST|202^CONSULTING_DOC_FAMILY_TEST^CONSULTING_DOC_GIVEN_TEST|MED|||||B6|E|272^ADMITTING_DOC_FAMILY_TEST^ADMITTING_DOC_GIVEN_TEST||48390|||||||||||||||||||||||||201409122200|20150206031726\r"
//        + "OBR|1|PON_0d70c6c8^LAB|FON_0d70c6c8^LAB|1487^ECHO CARDIOGRAM COMPLETE||20200802124455|20200802124455|||||||||OP_0d70c6c8^SINGH^BALDEV||||||||CT|F|||COP_0d70c6c8^GARCIA^LUIS\r"
//        + "OBX|1|TX|1324||ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^TRDSE^Janetary~2913^MRTTE^Darren^F~3065^MGHOBT^Paul^J~4723^LOTHDEW^Robert^L|\r"
//        + "OBX|2|TX|1234||[PII] Emergency Department||||||F|||20200802124455\r"
//        + "OBX|3|TX|1234||ED Encounter Arrival Date: [ADDRESS] [PERSONALNAME]:||||||F|||20200802124455\r";
        
        // original message (corrected with IDs '1234')
        "MSH|^~\\&|WHI_Automation|IBM_Toronto_Lab|IMAGING_REPORT|Hartland|20200802124455||ORU^R01^ORU_R01|MSGID00231|T|2.6\r"
        + "PID|1||0d70c6c8^^^MRN||Patient^Autogenerated||19630306|M||Caucasian|^^^^L6G 1C7~^^^ON~^^Unionville~&Warden Av~8200~^^^^^^B||^^^^^^4042808~^^^^^905~^^^^001|||Married|Baptist|Account_0d70c6c8\r"
        + "NTE|1||Created for MRN: 0d70c6c8\r"
        + "PV1|1|I|^^^Toronto^^^8200 Warden Av|EM|||2905^Langa^Albert^J^IV||0007^SINGH^BALDEV||||||||5755^Kuczma^Sean^^Jr||Visit_0d70c6c8|||||||||||||||||||||||||20200802124455||||||||ABC\r"
        + "OBR|1|PON_0d70c6c8^LAB|FON_0d70c6c8^LAB|1487^ECHO CARDIOGRAM COMPLETE||20200802124455|20200802124455|||||||||OP_0d70c6c8^SINGH^BALDEV||||||||CT|F|||COP_0d70c6c8^GARCIA^LUIS\r"
        + "OBX|1|TX|1234||||||||F|||20200802124455\r"
        + "OBX|2|TX|1324||[PII] Emergency Department||||||F|||20200802124455\r"
        + "OBX|3|TX|1234||ED Encounter Arrival Date: [ADDRESS] [PERSONALNAME]:||||||F|||20200802124455\r";
        
        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        ConverterOptions options = new Builder()
                .withBundleType(BundleType.COLLECTION)
                .withValidateResource()
                .withPrettyPrint()
                .build();
        String json = ftv.convert(hl7message, options);
        //String json = ftv.convert(everything, options);
        //String json = ftv.convert(new File(filename), options);
        System.out.println("----------------");
        System.out.println(json);
        System.out.println("----------------");

        //        ObjectMapper mapper = new ObjectMapper();
        //        Object jsonObject = mapper.readValue(json, Object.class);
        //        String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        //        System.out.println("----------------");
        //        System.out.println(prettyJson);
        //        System.out.println("----------------");

        verifyResult(json, BundleType.COLLECTION, true);
    }

    private static void verifyResult(String json, BundleType expectedBundleType,
            boolean messageHeaderExpected) {
        FHIRContext context = new FHIRContext();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        Bundle b = (Bundle) bundleResource;
        if (b.getType() != expectedBundleType) {
            System.out.println("Bundle type not expected:" + b.getType());
        }
        b.getId();
        b.getMeta().getLastUpdated();

        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        //        assertThat(patientResource).hasSize(1);
        //
        List<Resource> encounterResource = e.stream()
                .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        //        assertThat(encounterResource).hasSize(1);
        List<Resource> obsResource = e.stream()
                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        //        assertThat(obsResource).hasSize(1);
        List<Resource> pracResource = e.stream()
                .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        //        assertThat(pracResource).hasSize(4);
        //
        List<Resource> allergyResources = e.stream()
                .filter(v -> ResourceType.AllergyIntolerance == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        //        assertThat(allergyResources).hasSize(2);
        //
        if (messageHeaderExpected) {
            List<Resource> messageHeader = e.stream()
                    .filter(v -> ResourceType.MessageHeader == v.getResource().getResourceType())
                    .map(BundleEntryComponent::getResource).collect(Collectors.toList());
            //            assertThat(messageHeader).hasSize(1);
        }
    }

}
