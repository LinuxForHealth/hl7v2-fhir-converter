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
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Test;
import com.google.common.collect.Lists;
import io.github.linuxforhealth.api.ResourceModel;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.resource.ResourceReader;

public class DifferentObservationValueTest {
  private static FHIRContext context = new FHIRContext();
  private static HL7MessageEngine engine = new HL7MessageEngine(context);

  private String baseMessage = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.6|\r"
      + "EVN|A01|20130617154644\r"
      + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
      + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
      + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r";


  private ResourceModel rsm =
      ResourceReader.getInstance().generateResourceModel("resource/Observation");

  HL7FHIRResourceTemplateAttributes attributes = new HL7FHIRResourceTemplateAttributes.Builder()
      .withResourceName("Observation").withResourceModel(rsm).withSegment("OBX")
      .withIsReferenced(false).withRepeats(true).build();

  HL7FHIRResourceTemplate observation = new HL7FHIRResourceTemplate(attributes);



  private HL7MessageModel message = new HL7MessageModel("ADT", Lists.newArrayList(observation));



  @Test
  public void test_observation_NM_result() throws IOException {

    String hl7message = baseMessage + "OBX|1|NM|0135–4^TotalProtein||7.3|gm/dl|5.9-8.4||||F";
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
    Quantity q = obs.getValueQuantity();
    assertThat(q.getUnit()).isEqualTo("gm/dl");
    assertThat(q.getValue().floatValue()).isEqualTo(7.3f);

  }


  @Test
  public void test_observation_TX_result() throws IOException {

    String hl7message = baseMessage
        + "OBX|1|TX|^Type of protein feed^L||Fourth Line: HYPERDYNAMIC LV SYSTOLIC FUNCTION, VISUAL EF 80%||||||F||||Alex||";
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
    assertThat(obs.getValueStringType()).isNotNull();
    StringType q = obs.getValueStringType();
    assertThat(q.asStringValue())
        .isEqualTo("Fourth Line: HYPERDYNAMIC LV SYSTOLIC FUNCTION, VISUAL EF 80%");


  }


  @Test
  public void test_observation_TX_multiple_parts_result() throws IOException {

    String hl7message = baseMessage
        + "OBX|1|TX|^Type of protein feed^L||HYPERDYNAMIC LV SYSTOLIC FUNCTION, VISUAL EF 80%~Fifth line, as part of a repeated field||||||F||";
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
    assertThat(obs.getValueStringType()).isNotNull();
    StringType q = obs.getValueStringType();
    assertThat(q.asStringValue()).isEqualTo(
        "HYPERDYNAMIC LV SYSTOLIC FUNCTION, VISUAL EF 80%. Fifth line, as part of a repeated field.");


  }

  @Test
  public void test_observation_CE_result_unknown_system() throws IOException {

    String hl7message =
        baseMessage + "OBX|1|CE|93000&CMP^LIN^CPT4|11|1305^No significant change was found^MEIECG";
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
    assertThat(obs.getValueCodeableConcept()).isNotNull();
    assertThat(obs.getStatus()).isNotNull();
    CodeableConcept cc = obs.getValueCodeableConcept();
    assertThat(cc.getCoding()).isNotNull();
    assertThat(cc.getCoding().get(0)).isNotNull();
    assertThat(cc.getCoding().get(0).getSystem()).isNull();
    assertThat(cc.getCoding().get(0).getCode()).isEqualTo("1305");
    assertThat(cc.getCoding().get(0).getDisplay()).isEqualTo("No significant change was found");

    assertThat(cc.getText()).isEqualTo("No significant change was found");


  }


  @Test
  public void test_observation_CE_result_known_system() throws IOException {

    String hl7message =
        baseMessage + "OBX|1|CE|93000&CMP^LIN^CPT4|11|1305^No significant change was found^LN";
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
    assertThat(obs.getValueCodeableConcept()).isNotNull();
    assertThat(obs.getStatus()).isNotNull();
    CodeableConcept cc = obs.getValueCodeableConcept();
    assertThat(cc.getCoding()).isNotNull();
    assertThat(cc.getCoding().get(0)).isNotNull();
    assertThat(cc.getCoding().get(0).getSystem()).isEqualTo("http://loinc.org");
    assertThat(cc.getCoding().get(0).getCode()).isEqualTo("1305");
    assertThat(cc.getCoding().get(0).getDisplay()).isEqualTo("No significant change was found");

    assertThat(cc.getText()).isEqualTo("No significant change was found");


  }



}
