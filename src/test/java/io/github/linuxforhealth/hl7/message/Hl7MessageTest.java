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
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Condition.ConditionEvidenceComponent;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Specimen;
import org.junit.jupiter.api.Test;
import com.google.common.collect.Lists;
import io.github.linuxforhealth.api.ResourceModel;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.resource.ResourceReader;

public class Hl7MessageTest {
  private static FHIRContext context = new FHIRContext(true, false);
  private static HL7MessageEngine engine = new HL7MessageEngine(context);

  @Test
  public void test_patient() throws IOException {

    ResourceModel rsm = ResourceReader.getInstance().generateResourceModel("resource/Patient");

    HL7FHIRResourceTemplateAttributes attributes = new HL7FHIRResourceTemplateAttributes.Builder()
        .withResourceName("Patient").withResourceModel(rsm).withSegment("PID")
        .withIsReferenced(false).withRepeats(false).build();

    HL7FHIRResourceTemplate patient = new HL7FHIRResourceTemplate(attributes);



    HL7MessageModel message = new HL7MessageModel("ADT", Lists.newArrayList(patient));
    String hl7message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||";
    String json = message.convert(hl7message, engine);
    assertThat(json).isNotBlank();

    IBaseResource bundleResource = context.getParser().parseResource(json);
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

    ResourceModel rsm = ResourceReader.getInstance().generateResourceModel("resource/Patient");
    HL7FHIRResourceTemplateAttributes attributes = new HL7FHIRResourceTemplateAttributes.Builder()
        .withResourceName("Patient").withResourceModel(rsm).withSegment("PID")
        .withIsReferenced(true).withRepeats(false).build();

    HL7FHIRResourceTemplate patient = new HL7FHIRResourceTemplate(attributes);


    ResourceModel encounter =
        ResourceReader.getInstance().generateResourceModel("resource/Encounter");
    HL7FHIRResourceTemplateAttributes attributesEncounter =
        new HL7FHIRResourceTemplateAttributes.Builder().withResourceName("Encounter")
            .withResourceModel(encounter).withSegment("PV1").withIsReferenced(true)
            .withRepeats(false).withAdditionalSegments(Lists.newArrayList("PV2")).build();

    HL7FHIRResourceTemplate encounterFH = new HL7FHIRResourceTemplate(attributesEncounter);



    HL7MessageModel message = new HL7MessageModel("ADT", Lists.newArrayList(patient, encounterFH));
    String hl7message = "MSH|^~\\&|SE050|050|PACS|050|201209121212||ADT^A01|102|T|2.7|||AL|NE\r"
        + "EVN||201209122222\r"
        + "PID|||1234^^^^SR^20021212^20200120~1234-12^^^^LR^~3872^^^^MR~221345671^^^^SS^~430078856^^^^MA^||KENNEDY^JOHN^FITZGERALD^JR^^^L| BOUVIER^^^^^^M|19900607|M|KENNEDY^BABYBOY^^^^^^B|2106-3^linuxforhealthTE^HL70005|123 MAIN ST^APT 3B^LEXINGTON^MA^00210^^M^MSA CODE^MA034~345 ELM\r"
        + "PV1|1|ff|yyy|EL|||200^ATTEND_DOC_FAMILY_TEST^ATTEND_DOC_GIVEN_TEST|201^REFER_DOC_FAMILY_TEST^REFER_DOC_GIVEN_TEST|202^CONSULTING_DOC_FAMILY_TEST^CONSULTING_DOC_GIVEN_TEST|MED|||||B6|E|272^ADMITTING_DOC_FAMILY_TEST^ADMITTING_DOC_GIVEN_TEST||48390|||||||||||||||||||||||||201409122200|20000206031726\r"
        + "AL1|0001|DA|98798^problem|SV|sneeze|20120808\r";
    String json = message.convert(hl7message, engine);
    assertThat(json).isNotBlank();



    IBaseResource bundleResource = context.getParser().parseResource(json);
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
    Encounter enc = getResourceEncounter(encounterResource.get(0));
    Reference ref = enc.getSubject();
    assertThat(ref.isEmpty()).isFalse();

  }



  @Test
  public void test_patient_encounter_only() throws IOException {

    ResourceModel encounter =
        ResourceReader.getInstance().generateResourceModel("resource/Encounter");
    HL7FHIRResourceTemplateAttributes attributesEncounter =
        new HL7FHIRResourceTemplateAttributes.Builder().withResourceName("Encounter")
            .withResourceModel(encounter).withSegment("PV1").withIsReferenced(false)
            .withRepeats(false).withAdditionalSegments(Lists.newArrayList("PV2")).build();

    HL7FHIRResourceTemplate encounterFH = new HL7FHIRResourceTemplate(attributesEncounter);



    HL7MessageModel message = new HL7MessageModel("ADT", Lists.newArrayList(encounterFH));
    String hl7message = "MSH|^~\\&|SE050|050|PACS|050|201209121212||ADT^A01|102|T|2.7|||AL|NE\r"
        + "EVN||201209122222\r"
        + "PID|0010||ADTNew^^^1231||ADT01New||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
        + "PV1|1|ff|Location|EL|||200^ATTEND_DOC_FAMILY_TEST^ATTEND_DOC_GIVEN_TEST|201^REFER_DOC_FAMILY_TEST^REFER_DOC_GIVEN_TEST|202^CONSULTING_DOC_FAMILY_TEST^CONSULTING_DOC_GIVEN_TEST|MED|||||B6|E|272^ADMITTING_DOC_FAMILY_TEST^ADMITTING_DOC_GIVEN_TEST||48390|||||||||||||||||||||||||201409122200|20000206031726\r"
        + "AL1|0001|DA|98798^problem|SV|sneeze|20120808\r";
    String json = message.convert(hl7message, engine);
    assertThat(json).isNotBlank();



    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> encounterResource =
        e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(encounterResource).hasSize(1);
  }

  // This test buids the encounter via code instead of using a message event. Helps with debugging.
  @Test
  public void test_patient_multipe_diagnoses_only_not_using_message() throws IOException {

    ResourceModel encounter =
        ResourceReader.getInstance().generateResourceModel("resource/Encounter");
    HL7FHIRResourceTemplateAttributes attributesEncounter =
        new HL7FHIRResourceTemplateAttributes.Builder().withResourceName("Encounter")
            .withResourceModel(encounter).withSegment("PV1").withIsReferenced(false)
            .withRepeats(false).withAdditionalSegments(Lists.newArrayList("DG1")).build();

    HL7FHIRResourceTemplate encounterFH = new HL7FHIRResourceTemplate(attributesEncounter);

    HL7MessageModel message = new HL7MessageModel("ADT", Lists.newArrayList(encounterFH));
    String hl7message = "MSH|^~\\&|PROSOLV|SENTARA|WHIA|IBM|20151008111200|S1|ADT^A01^ADT_A01|MSGID000001|T|2.6|10092|PRPA008|AL|AL|100|8859/1|ENGLISH|ARM|ARM5007\r"
    + "EVN|A04|20151008111200|20171013152901|O|OID1006|20171013153621|EVN1009\r"
    + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
    + "PV1|1|O|SAN JOSE|A|10089|MILPITAS|2740^Torres^Callie|2913^Grey^Meredith^F|3065^Sloan^Mark^J|CAR|FOSTER CITY|AD|R|FR|A4|VI|9052^Shepeard^Derek^|AH|10019181|FIC1002|IC|CC|CR|CO|20161012034052|60000|6|AC|GHBR|20160926054052|AC5678|45000|15000|D|20161016154413|DCD|SAN FRANCISCO|VEG|RE|O|AV|FREMONT|CALIFORNIA|20161013154626|20161014154634|10000|14000|2000|4000|POL8009|V|PHY6007\r"
    + "PV2|SAN BRUNO|AC4567|vomits|less equipped|purse|SAN MATEO|HO|20171014154626|20171018154634|4|3|DIAHHOREA|RSA456|20161013154626|Y|D|20191026001640|O|Y|1|F|Y|KAISER|AI|2|20161013154626|ED|20171018001900|20161013154626|10000|RR|Y|20171108002129|Y|Y|N|N|A^Ambulance^HL70430\r"
    + "DG1|1|D1|V72.83^Other specified pre-operative examination^ICD-9^^^|Other specified pre-operative examination|20151008111200|A\r"
    + "DG1|2|D2|R00.0^Tachycardia, unspecified^ICD-10^^^|Tachycardia, unspecified|20150725201300|A\r"
    + "DG1|3|D3|R06.02^Shortness of breath^ICD-10^^^|Shortness of breath||A\r"
    + "DG1|4|D4|Q99.9^Chromosomal abnormality, unspecified^ICD-10^^^|Chromosomal abnormality, unspecified||A\r"
    + "DG1|5|D5|I34.8^Arteriosclerosis^ICD-10^^^|Arteriosclerosis||A\r"
    + "DG1|6|D6|I34.0^Mitral valve regurgitation^ICD-10^^^|Mitral valve regurgitation||A\r"
    + "DG1|6|D7|I05.9^Mitral valve disorder in childbirth^ICD-10^^^|Mitral valve disorder in childbirth||A\r"
    + "DG1|7|D8|J45.909^Unspecified asthma, uncomplicated^ICD-10^^^|Unspecified asthma, uncomplicated||A\r";
    String json = message.convert(hl7message, engine);
    assertThat(json).isNotBlank();
    System.out.println(json);

    IBaseResource bundleResource = context.getParser().parseResource(json);
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
    ResourceModel rsm = ResourceReader.getInstance().generateResourceModel("resource/Observation");

    HL7FHIRResourceTemplateAttributes attributes = new HL7FHIRResourceTemplateAttributes.Builder()
        .withResourceName("Observation").withResourceModel(rsm).withSegment("OBX")
        .withIsReferenced(true).withRepeats(false).build();

    HL7FHIRResourceTemplate observation = new HL7FHIRResourceTemplate(attributes);

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

    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> obsResource =
        e.stream().filter(v -> ResourceType.Observation == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(obsResource).hasSize(1);
    List<Resource> pracResource =
        e.stream().filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(pracResource).hasSize(5);

  }


  @Test
  public void test_observation_multiple() throws IOException {


    ResourceModel rsm = ResourceReader.getInstance().generateResourceModel("resource/Observation");

    HL7FHIRResourceTemplateAttributes attributes = new HL7FHIRResourceTemplateAttributes.Builder()
        .withResourceName("Observation").withResourceModel(rsm).withSegment("OBX")
        .withIsReferenced(true).withRepeats(true).build();

    HL7FHIRResourceTemplate observation = new HL7FHIRResourceTemplate(attributes);


    HL7MessageModel message = new HL7MessageModel("ADT", Lists.newArrayList(observation));
    String hl7message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r"
        + "OBX|1|TX|1234||First line: ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^|\r"
        + "OBX|2|TX|TS-F-01-005^Endocrine Disorders new^L||Second Line: NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH\\.br\\Third Line in the same field, after the escape character for line break.||||||F||\r"
        + "OBX|3|TX|TS-F-01-002^Endocrine Disorders^L||Fourth Line: HYPERDYNAMIC LV SYSTOLIC FUNCTION, VISUAL EF 80%~Fifth line, as part of a repeated field||||||F||";
    String json = message.convert(hl7message, engine);

    IBaseResource bundleResource = context.getParser().parseResource(json);
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

    ResourceModel rsm = ResourceReader.getInstance().generateResourceModel("resource/Observation");

    HL7FHIRResourceTemplateAttributes attributes = new HL7FHIRResourceTemplateAttributes.Builder()
        .withResourceName("Observation").withResourceModel(rsm).withSegment("OBX")
        .withIsReferenced(true).withRepeats(false).build();

    HL7FHIRResourceTemplate observation = new HL7FHIRResourceTemplate(attributes);


    HL7MessageModel message = new HL7MessageModel("ADT", Lists.newArrayList(observation));
    String hl7message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r"
        + "OBX|1|NM|0135–4^TotalProtein||7.3|gm/dl|5.9-8.4||||F";
    String json = message.convert(hl7message, engine);
    IBaseResource bundleResource = context.getParser().parseResource(json);
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
  public void test_condition() throws IOException {

    ResourceModel rsm = ResourceReader.getInstance().generateResourceModel("resource/Condition");

    HL7FHIRResourceTemplateAttributes attributes = new HL7FHIRResourceTemplateAttributes.Builder()
        .withResourceName("Condition").withResourceModel(rsm).withSegment("PRB")
        .withIsReferenced(true).withRepeats(false).build();

    HL7FHIRResourceTemplate conditionTemplate = new HL7FHIRResourceTemplate(attributes);
    HL7MessageModel message = new HL7MessageModel("ADT", Lists.newArrayList(conditionTemplate));



    String hl7message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.3|\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r"
        + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625";
    String json = message.convert(hl7message, engine);
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> cond =
        e.stream().filter(v -> ResourceType.Condition == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(cond).hasSize(1);


  }



  @Test
  public void test_observation_condition() throws IOException {



    ResourceModel obsModel =
        ResourceReader.getInstance().generateResourceModel("resource/Observation");
    HL7FHIRResourceTemplateAttributes attributesObs =
        new HL7FHIRResourceTemplateAttributes.Builder().withResourceName("Observation")
            .withResourceModel(obsModel).withSegment(".PROBLEM_OBSERVATION.OBX")
            .withIsReferenced(true).withRepeats(true).withGroup("PROBLEM").build();

    HL7FHIRResourceTemplate obsTemplate = new HL7FHIRResourceTemplate(attributesObs);



    ResourceModel condModel =
        ResourceReader.getInstance().generateResourceModel("resource/Condition");

    HL7FHIRResourceTemplateAttributes attributesCond =
        new HL7FHIRResourceTemplateAttributes.Builder().withResourceName("Condition")
            .withResourceModel(condModel).withSegment(".PRB").withIsReferenced(false)
            .withRepeats(true).withGroup("PROBLEM").build();

    HL7FHIRResourceTemplate conditionTemplate = new HL7FHIRResourceTemplate(attributesCond);


    HL7MessageModel message =
        new HL7MessageModel("ADT", Lists.newArrayList(obsTemplate, conditionTemplate));
    String hl7message =
        "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson||19600614|M||C|99 Oakland #106^^qwerty^OH^44889||^^^^^626^5641111|^^^^^626^5647654|||||343132266|||N\r"
            + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"
            + "NTE|1|P|Problem Comments\r" + "VAR|varid1|200603150610\r"
            + "OBX|1|TX|TS-FR-01-002^Some report^L||ECHOCARDIOGRAPHIC REPORT||||||F|||20150930164100|||\r"
            + "OBX|2|TX|TS-F-01-002^Endocrine Disorders^L||NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH||||||F|||20150930164100|||";
    String json = message.convert(hl7message, engine);
    assertThat(json).isNotBlank();



    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> observationResource =
        e.stream().filter(v -> ResourceType.Observation == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(observationResource).hasSize(2);
    List<String> ids =
        observationResource.stream().map(m -> m.getId()).collect(Collectors.toList());
    List<Resource> conditionResource =
        e.stream().filter(v -> ResourceType.Condition == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(conditionResource).hasSize(1);
    Condition cond = getResourceCondition(conditionResource.get(0));
    List<ConditionEvidenceComponent> evidences = cond.getEvidence();
    assertThat(evidences).isNotEmpty();
    assertThat(evidences.get(0).hasDetail()).isTrue();
    assertThat(evidences.get(0).getDetail().get(0).getReference()).isIn(ids);
    assertThat(evidences.get(1).getDetail().get(0).getReference()).isIn(ids);
    assertThat(evidences.get(0).getDetail().get(0).getReference())
        .isNotEqualTo(evidences.get(1).getDetail().get(0).getReference());
  }



  @Test
  public void test_messageHeader_with_ADT() throws IOException {

    ResourceModel rsm =
        ResourceReader.getInstance().generateResourceModel("resource/MessageHeader");

    HL7FHIRResourceTemplateAttributes attributes = new HL7FHIRResourceTemplateAttributes.Builder()
        .withResourceName("MessageHeader").withResourceModel(rsm).withSegment("MSH")
        .withIsReferenced(false).withRepeats(false).build();

    HL7FHIRResourceTemplate messageHeaderTemplate = new HL7FHIRResourceTemplate(attributes);



    HL7MessageModel message = new HL7MessageModel("ADT", Lists.newArrayList(messageHeaderTemplate));
    String hl7message =
        "MSH|^~\\&|Amalga HIS|BUM|New Tester|MS|20111121103141||ADT^A01|2847970-201111211031|P|2.6|||AL|NE|764|||||||^4086::132:2A57:3C28^IPv6\r"
            + "EVN|A01|20130617154644\r"
            + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
            + "NK1|1|Wood^John^^^MR|Father||999-9999\r"
            + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
            + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||";
    String json = message.convert(hl7message, engine);
    assertThat(json).isNotBlank();

    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> messageHeader =
        e.stream().filter(v -> ResourceType.MessageHeader == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(messageHeader).hasSize(1);

    MessageHeader msgH = getResourceMessageHeader(messageHeader.get(0));

    assertThat(msgH.getId()).isNotNull();
    assertThat(msgH.getEventCoding()).isNotNull();
    assertThat(msgH.getEventCoding().getSystem())
        .isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0003");
    assertThat(msgH.getEventCoding().getCode()).isEqualTo("A01");
    assertThat(msgH.getEventCoding().getDisplay())
        .isEqualTo("ADT/ACK - Admit/visit notification");
    assertThat(msgH.getDestination()).hasSize(1);
    assertThat(msgH.getDestinationFirstRep().getName()).isEqualTo("New Tester");
    assertThat(msgH.getDestinationFirstRep().getEndpoint()).isEqualTo("MS");

    assertThat(msgH.getSource()).isNotNull();
    assertThat(msgH.getSource().getName()).isEqualTo("Amalga HIS");

    assertThat(msgH.getReason()).isNotNull();
    assertThat(msgH.getReason().getCoding().get(0).getSystem())
        .isEqualTo("http://terminology.hl7.org/CodeSystem/message-reasons-encounter");
    assertThat(msgH.getReason().getCoding().get(0).getCode()).isEqualTo("admit");
    assertThat(msgH.getReason().getCoding().get(0).getDisplay()).isEqualTo("Admit");



  }



  @Test
  public void test_messageHeader_with_ORU() throws IOException {

    ResourceModel rsm =
        ResourceReader.getInstance().generateResourceModel("resource/MessageHeader");

    HL7FHIRResourceTemplateAttributes attributes = new HL7FHIRResourceTemplateAttributes.Builder()
        .withResourceName("MessageHeader").withResourceModel(rsm).withSegment("MSH")
        .withAdditionalSegments(Lists.newArrayList("EVN"))
        .withIsReferenced(false).withRepeats(false).build();

    HL7FHIRResourceTemplate messageHeaderTemplate = new HL7FHIRResourceTemplate(attributes);


    HL7MessageModel message = new HL7MessageModel("ORU", Lists.newArrayList(messageHeaderTemplate));
    String hl7message =
        "MSH|^~\\&|Amalga HIS|BUM|New Tester|MS|20111121103141||ORU^R01|2847970-201111211031|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
            + "EVN|A01|20130617154644||01\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||";
    String json = message.convert(hl7message, engine);
    assertThat(json).isNotBlank();

    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> messageHeader =
        e.stream().filter(v -> ResourceType.MessageHeader == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(messageHeader).hasSize(1);

    MessageHeader msgH = getResourceMessageHeader(messageHeader.get(0));

    assertThat(msgH.getId()).isNotNull();
    assertThat(msgH.getEventCoding()).isNotNull();
    assertThat(msgH.getEventCoding().getSystem())
        .isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0003");
    assertThat(msgH.getEventCoding().getCode()).isEqualTo("R01");
    assertThat(msgH.getEventCoding().getDisplay())
        .isEqualTo("ORU/ACK - Unsolicited transmission of an observation message");
    assertThat(msgH.getDestination()).hasSize(1);
    assertThat(msgH.getDestinationFirstRep().getName()).isEqualTo("New Tester");
    assertThat(msgH.getDestinationFirstRep().getEndpoint()).isEqualTo("MS");

    assertThat(msgH.getSource()).isNotNull();
    assertThat(msgH.getSource().getName()).isEqualTo("Amalga HIS");

    assertThat(msgH.getReason()).isNotNull();
    assertThat(msgH.getReason().getCoding().get(0).getSystem())
        .isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0062");
    assertThat(msgH.getReason().getCoding().get(0).getCode()).isEqualTo("01");
    assertThat(msgH.getReason().getCoding().get(0).getDisplay()).isEqualTo("Patient request");

  }

  @Test
  public void test_specimen() throws IOException {
    ResourceModel rsm = ResourceReader.getInstance().generateResourceModel("resource/Specimen");

    HL7FHIRResourceTemplateAttributes attributes = new HL7FHIRResourceTemplateAttributes.Builder()
        .withResourceName("Specimen").withResourceModel(rsm).withSegment("SPM")
        .withIsReferenced(true).withRepeats(false).build();

    HL7FHIRResourceTemplate specimen = new HL7FHIRResourceTemplate(attributes);

    HL7MessageModel message = new HL7MessageModel("ORU", Lists.newArrayList(specimen));

    String hl7message = "MSH|^~\\&|Amalga HIS|BUM|New Tester|MS|20111121103141||ORU^R01|2847970-201111211031|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r"
        + "OBX|1|TX|1234||First line: ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^|\r"
        + "SPM|1|SpecimenID||BLOOD^Blood^^87612001^BLOOD^SCT^^||||Cord Art^Blood, Cord Arterial^^^^^^^|||P||||||201110060535|201110060821||Y||||||1";
    String json = message.convert(hl7message, engine);

    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> spmResource =
        e.stream().filter(v -> ResourceType.Specimen == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(spmResource).hasSize(1);
  }


  @Test
  public void test_specimen_multiple() throws IOException {


    ResourceModel rsm = ResourceReader.getInstance().generateResourceModel("resource/Specimen");

    HL7FHIRResourceTemplateAttributes attributes = new HL7FHIRResourceTemplateAttributes.Builder()
        .withResourceName("Specimen").withResourceModel(rsm).withSegment("SPM")
        .withIsReferenced(true).withRepeats(true).build();

    HL7FHIRResourceTemplate specimen = new HL7FHIRResourceTemplate(attributes);


    HL7MessageModel message = new HL7MessageModel("ORU", Lists.newArrayList(specimen));
    String hl7message = "MSH|^~\\&|Amalga HIS|BUM|New Tester|MS|20111121103141||ORU^R01|2847970-201111211031|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r"
        + "OBX|1|TX|1234||First line: ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^|\r"
        + "SPM|1|SpecimenID||BLOOD^Blood^^87612001^BLOOD^SCT^^||||Cord Art^Blood, Cord Arterial^^^^^^^|||P||||||201110060535|201110060821||Y||||||1\r"
        + "SPM|2|SpecimenID||BLD|||||||P||||||201110060535|201110060821||Y||||||1";
    String json = message.convert(hl7message, engine);

    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> spmResource =
        e.stream().filter(v -> ResourceType.Specimen == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(spmResource).hasSize(2);
  }


  @Test
  public void test_specimen_multiple_type_coding() throws IOException {

    ResourceModel rsm = ResourceReader.getInstance().generateResourceModel("resource/Specimen");

    HL7FHIRResourceTemplateAttributes attributes = new HL7FHIRResourceTemplateAttributes.Builder()
        .withResourceName("Specimen").withResourceModel(rsm).withSegment("SPM")
        .withIsReferenced(true).withRepeats(false).build();

    HL7FHIRResourceTemplate observation = new HL7FHIRResourceTemplate(attributes);


    HL7MessageModel message = new HL7MessageModel("ORU", Lists.newArrayList(observation));
    String hl7message = "MSH|^~\\&|Amalga HIS|BUM|New Tester|MS|20111121103141||ORU^R01|2847970-201111211031|P|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
        + "EVN|A01|20130617154644\r"
        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r"
        + "OBX|1|NM|0135–4^TotalProtein||7.3|gm/dl|5.9-8.4||||F\r"
        + "SPM|1|SpecimenID||BLOOD^Blood^^87612001^BLOOD^SCT^^||||Cord Art^Blood, Cord Arterial^^^^^^^|||P||||||201110060535|201110060821||Y||||||1";
    String json = message.convert(hl7message, engine);
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> spmResource =
        e.stream().filter(v -> ResourceType.Specimen == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(spmResource).hasSize(1);
    Specimen spm = (Specimen) spmResource.get(0);
    assertThat(spm.getType().getCoding()).hasSize(2);

  }

  @Test
  public void test_encouner_with_observation() throws IOException {

    ResourceModel rsm = ResourceReader.getInstance().generateResourceModel("resource/Patient");
    HL7FHIRResourceTemplateAttributes attributes = new HL7FHIRResourceTemplateAttributes.Builder()
        .withResourceName("Patient").withResourceModel(rsm).withSegment("PID")
        .withIsReferenced(true).withRepeats(false).build();

    HL7FHIRResourceTemplate patient = new HL7FHIRResourceTemplate(attributes);


    ResourceModel encounter =
        ResourceReader.getInstance().generateResourceModel("resource/Encounter");
    HL7FHIRResourceTemplateAttributes attributesEncounter =
        new HL7FHIRResourceTemplateAttributes.Builder().withResourceName("Encounter")
            .withResourceModel(encounter).withSegment("PV1").withIsReferenced(true)
            .withRepeats(false).withAdditionalSegments(Lists.newArrayList("PV2", "OBX")).build();

    HL7FHIRResourceTemplate encounterFH = new HL7FHIRResourceTemplate(attributesEncounter);

    ResourceModel obsModel =
        ResourceReader.getInstance().generateResourceModel("resource/Observation");
    HL7FHIRResourceTemplateAttributes attributesObs =
        new HL7FHIRResourceTemplateAttributes.Builder().withResourceName("Observation")
            .withResourceModel(obsModel).withSegment("OBX").withIsReferenced(true).withRepeats(true)
            .build();

    HL7FHIRResourceTemplate obsTemplate = new HL7FHIRResourceTemplate(attributesObs);



    HL7MessageModel message = new HL7MessageModel("ADT",
        Lists.newArrayList(patient, encounterFH, obsTemplate));
    String hl7message =
        "MSH|^~\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE|764|ASCII||||||^4086::132:2A57:3C28^IPv6\r"
            + "EVN||201209122222\r"
            + "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\r"
            + "PV1|1|ff|yyy|EL|ABC||200^ATTEND_DOC_FAMILY_TEST^ATTEND_DOC_GIVEN_TEST|201^REFER_DOC_FAMILY_TEST^REFER_DOC_GIVEN_TEST|202^CONSULTING_DOC_FAMILY_TEST^CONSULTING_DOC_GIVEN_TEST|MED|||||B6|E|272^ADMITTING_DOC_FAMILY_TEST^ADMITTING_DOC_GIVEN_TEST||48390|||||||||||||||||||||||||201409122200|20150206031726\r"
            + "OBX|1|TX|1234||ECHOCARDIOGRAPHIC REPORT||||||F|||||2740^TRDSE^Janetary~2913^MRTTE^Darren^F~3065^MGHOBT^Paul^J~4723^LOTHDEW^Robert^L|\r"
            + "AL1|1|DRUG|00000741^OXYCODONE||HYPOTENSION\r"
            + "AL1|2|DRUG|00001433^TRAMADOL||SEIZURES~VOMITING\r";
    String json = message.convert(hl7message, engine);
    System.out.println(json);
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> obsResource =
        e.stream().filter(v -> ResourceType.Observation == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(obsResource).hasSize(1);
    List<Resource> pracResource =
        e.stream().filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(pracResource).hasSize(4);

    List<Resource> encounterResource =
        e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(encounterResource).hasSize(1);

    Encounter encounterRes = getResourceEncounter(encounterResource.get(0));
    Reference patRef = encounterRes.getSubject();
    assertThat(patRef.isEmpty()).isFalse();
    List<Reference> obsRef = encounterRes.getReasonReference();
    assertThat(obsRef).hasSize(1);



  }



  private MessageHeader getResourceMessageHeader(Resource resource) {
    String s = context.getParser().encodeResourceToString(resource);
    Class<? extends IBaseResource> klass = MessageHeader.class;
    return (MessageHeader) context.getParser().parseResource(klass, s);
  }
  
  private Condition getResourceCondition(Resource resource) {
    String s = context.getParser().encodeResourceToString(resource);
    Class<? extends IBaseResource> klass = Condition.class;
    return (Condition) context.getParser().parseResource(klass, s);
  }

  private static Encounter getResourceEncounter(Resource resource) {
	String s = context.getParser().encodeResourceToString(resource);
    Class<? extends IBaseResource> klass = Encounter.class;
    return (Encounter) context.getParser().parseResource(klass, s);
  }

}
