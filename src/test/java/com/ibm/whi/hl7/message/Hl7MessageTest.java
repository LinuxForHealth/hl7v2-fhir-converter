/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.message;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.Test;
import com.google.common.collect.Lists;
import com.ibm.whi.core.resource.ResourceModel;
import com.ibm.whi.fhir.FHIRContext;
import com.ibm.whi.hl7.resource.ResourceModelReader;

public class Hl7MessageTest {
  private static HL7MessageEngine engine = new HL7MessageEngine();
  @Test
  public void test_patient() throws IOException {

    ResourceModel rsm = ResourceModelReader.getInstance().generateResourceModel("resource/Patient");
    HL7FHIRResource patient = new HL7FHIRResource("Patient", "PID", rsm, 0, false);
    HL7MessageModel message = new HL7MessageModel("ADT", Lists.newArrayList(patient));
    String hl7message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||";
    String json = message.convert(hl7message, engine);
    assertThat(json).isNotBlank();


    IBaseResource bundleResource = FHIRContext.getIParserInstance().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> patientResource =
        e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(patientResource).hasSize(1);

  }


  @Test
  public void test_patient_encounter() throws IOException {

    ResourceModel rsm = ResourceModelReader.getInstance().generateResourceModel("resource/Patient");
    HL7FHIRResource patient = new HL7FHIRResource("Patient", "PID", rsm, 0, false);
    ResourceModel encounter =
        ResourceModelReader.getInstance().generateResourceModel("resource/Encounter");
    HL7FHIRResource encounterFH =
        new HL7FHIRResource("Encounter", "PV1", encounter, 0, false, Lists.newArrayList("PV2"));



    HL7MessageModel message = new HL7MessageModel("ADT", Lists.newArrayList(patient, encounterFH));
    String hl7message = "MSH|^~\\&|SE050|050|PACS|050|201209121212||ADT^A01|102|T|2.7|||AL|NE\r"
        + "EVN||201209122222\r"
        + "PID|||1234^^^^SR^20021212^20200120~1234-12^^^^LR^~3872^^^^MR~221345671^^^^SS^~430078856^^^^MA^||KENNEDY^JOHN^FITZGERALD^JR^^^L| BOUVIER^^^^^^M|19900607|M|KENNEDY^BABYBOY^^^^^^B|2106-3^WHITE^HL70005|123 MAIN ST^APT 3B^LEXINGTON^MA^00210^^M^MSA CODE^MA034~345 ELM\r"
        + "PV1|1|ff|yyy|EL|||200^ATTEND_DOC_FAMILY_TEST^ATTEND_DOC_GIVEN_TEST|201^REFER_DOC_FAMILY_TEST^REFER_DOC_GIVEN_TEST|202^CONSULTING_DOC_FAMILY_TEST^CONSULTING_DOC_GIVEN_TEST|MED|||||B6|E|272^ADMITTING_DOC_FAMILY_TEST^ADMITTING_DOC_GIVEN_TEST||48390|||||||||||||||||||||||||201409122200|20000206031726\r"
        + "AL1|0001|DA|98798^problem|SV|sneeze|20120808\r";
    String json = message.convert(hl7message, engine);
    assertThat(json).isNotBlank();



    IBaseResource bundleResource = FHIRContext.getIParserInstance().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> patientResource =
        e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(patientResource).hasSize(1);

    List<Resource> encounterResource =
        e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(encounterResource).hasSize(1);
  }


  @Test
  public void test_patient_encounter_only() throws IOException {


    ResourceModel encounter =
        ResourceModelReader.getInstance().generateResourceModel("resource/Encounter");
    HL7FHIRResource encounterFH =
        new HL7FHIRResource("Encounter", "PV1", encounter, 0, false, Lists.newArrayList("PV2"));



    HL7MessageModel message = new HL7MessageModel("ADT", Lists.newArrayList(encounterFH));
    String hl7message = "MSH|^~\\&|SE050|050|PACS|050|201209121212||ADT^A01|102|T|2.7|||AL|NE\r"
        + "EVN||201209122222\r"
        + "PID|0010||ADTNew^^^1231||ADT01New||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
        + "PV1|1|ff|Location|EL|||200^ATTEND_DOC_FAMILY_TEST^ATTEND_DOC_GIVEN_TEST|201^REFER_DOC_FAMILY_TEST^REFER_DOC_GIVEN_TEST|202^CONSULTING_DOC_FAMILY_TEST^CONSULTING_DOC_GIVEN_TEST|MED|||||B6|E|272^ADMITTING_DOC_FAMILY_TEST^ADMITTING_DOC_GIVEN_TEST||48390|||||||||||||||||||||||||201409122200|20000206031726\r"
        + "AL1|0001|DA|98798^problem|SV|sneeze|20120808\r";
    String json = message.convert(hl7message, engine);
    assertThat(json).isNotBlank();



    IBaseResource bundleResource = FHIRContext.getIParserInstance().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> encounterResource =
        e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(encounterResource).hasSize(1);
  }


  @Test
  public void test_observation() throws IOException {

    ResourceModel rsm =
        ResourceModelReader.getInstance().generateResourceModel("resource/Observation");
    HL7FHIRResource observation = new HL7FHIRResource("Observation", "OBX", rsm, 0, false);
    HL7MessageModel message = new HL7MessageModel("ADT", Lists.newArrayList(observation));
    String hl7message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r"
        + "OBX|1|TX|1234||First line: ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^|\r"
        + "OBX|2|TX|||Second Line: NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH\\.br\\Third Line in the same field, after the escape character for line break.||||||F||\r"
        + "OBX|3|TX|||Fourth Line: HYPERDYNAMIC LV SYSTOLIC FUNCTION, VISUAL EF 80%~Fifth line, as part of a repeated field||||||F||||Alex||";
    String json = message.convert(hl7message, engine);

    IBaseResource bundleResource = FHIRContext.getIParserInstance().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> obsResource = e.stream()
        .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
        .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(obsResource).hasSize(1);
    List<Resource> pracResource =
        e.stream().filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(pracResource).hasSize(5);

  }


  @Test
  public void test_observation_multiple() throws IOException {

    ResourceModel rsm =
        ResourceModelReader.getInstance().generateResourceModel("resource/Observation");
    HL7FHIRResource observation =
        new HL7FHIRResource("Observation", "OBX", rsm, 0, true, new ArrayList<>());
    HL7MessageModel message = new HL7MessageModel("ADT", Lists.newArrayList(observation));
    String hl7message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r"
        + "OBX|1|TX|1234||First line: ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^|\r"
        + "OBX|2|TX|||Second Line: NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH\\.br\\Third Line in the same field, after the escape character for line break.||||||F||\r"
        + "OBX|3|TX|||Fourth Line: HYPERDYNAMIC LV SYSTOLIC FUNCTION, VISUAL EF 80%~Fifth line, as part of a repeated field||||||F||";
    String json = message.convert(hl7message, engine);
    IBaseResource bundleResource = FHIRContext.getIParserInstance().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> obsResource =
        e.stream().filter(v -> ResourceType.Observation == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(obsResource).hasSize(3);
    List<Resource> pracResource =
        e.stream().filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(pracResource).hasSize(5);
  }


  @Test
  public void test_observation_NM_result() throws IOException {

    ResourceModel rsm =
        ResourceModelReader.getInstance().generateResourceModel("resource/Observation");
    HL7FHIRResource observation =
        new HL7FHIRResource("Observation", "OBX", rsm, 0, true, new ArrayList<>());
    HL7MessageModel message = new HL7MessageModel("ADT", Lists.newArrayList(observation));
    String hl7message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r"
        + "OBX|1|NM|0135–4^TotalProtein||7.3|gm/dl|5.9-8.4||||F";
    String json = message.convert(hl7message, engine);
    IBaseResource bundleResource = FHIRContext.getIParserInstance().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> obsResource =
        e.stream().filter(v -> ResourceType.Observation == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(obsResource).hasSize(1);
    Observation obs = (Observation) obsResource.get(0);
    assertThat(obs.getValueQuantity()).isNotNull();

  }


  @Test
  public void test_allergy_single() throws IOException {

    ResourceModel rsm =
        ResourceModelReader.getInstance().generateResourceModel("resource/AllergyIntolerance");
    HL7FHIRResource observation =
        new HL7FHIRResource("AllergyIntolerance", "AL1", rsm, 0, false, new ArrayList<>());
    HL7MessageModel message = new HL7MessageModel("ADT", Lists.newArrayList(observation));
    String hl7message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r"
        + "AL1|1|DA|^PENICILLIN|MO|PRODUCES HIVES~RASH|MO\r" //
        + "AL1|2|AA|^CAT DANDER|SV";
    String json = message.convert(hl7message, engine);
    IBaseResource bundleResource = FHIRContext.getIParserInstance().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> allergyRes =
        e.stream().filter(v -> ResourceType.AllergyIntolerance == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(allergyRes).hasSize(1);
    AllergyIntolerance allergy = (AllergyIntolerance) allergyRes.get(0);
    assertThat(allergy.getCriticality().toCode()).isEqualTo("low");
    assertThat(allergy.getCategory().get(0).getCode()).isEqualTo("medication");
    assertThat(allergy.getCode().getText()).isEqualTo("PENICILLIN");
    assertThat(allergy.getReaction().get(0).getManifestation()).extracting(m -> m.getText())
        .containsExactlyInAnyOrder("PRODUCES HIVES", "RASH");

  }



  @Test
  public void test_allergy_multiple() throws IOException {

    ResourceModel rsm =
        ResourceModelReader.getInstance().generateResourceModel("resource/AllergyIntolerance");
    HL7FHIRResource observation =
        new HL7FHIRResource("AllergyIntolerance", "AL1", rsm, 0, true, new ArrayList<>());
    HL7MessageModel message = new HL7MessageModel("ADT", Lists.newArrayList(observation));
    String hl7message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r"
        + "AL1|1|DA|^PENICILLIN|MO|PRODUCES HIVES~RASH|MO\r" //
        + "AL1|2|AA|^CAT DANDER|SV";
    String json = message.convert(hl7message, engine);
    IBaseResource bundleResource = FHIRContext.getIParserInstance().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> obsResource =
        e.stream().filter(v -> ResourceType.AllergyIntolerance == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(obsResource).hasSize(2);


  }

}
