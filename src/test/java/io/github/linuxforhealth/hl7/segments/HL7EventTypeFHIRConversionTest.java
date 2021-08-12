/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

public class HL7EventTypeFHIRConversionTest {

  private static final ConverterOptions OPTIONS = new Builder().withValidateResource().withPrettyPrint().build();

  @Test
  public void validate_evn_segment () {

    String hl7message = "MSH|^~\\&|||||||ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6||||||\r" +
                        "EVN||||851||20210319134735|\r"+
                        "PV1|1|I||R|||||||||R|1||||||||||||||||||||||||||||||||||||||";                    
                        

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message , OPTIONS);
    assertThat(json).isNotBlank();

    FHIRContext context = new FHIRContext(true, false);
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();

    // Find the encounter from the FHIR bundle.
    List<Resource> encounterResource = e.stream()
        .filter(v -> ResourceType.Encounter == v.getResource().getResourceType()).map(BundleEntryComponent::getResource)
        .collect(Collectors.toList());
    assertThat(encounterResource).hasSize(1);

    // ENV.4 is used for reasonCode
    String reasonCode = encounterResource.get(0).getNamedProperty("reasonCode").getValues().get(0).getNamedProperty("text").getValues().get(0).toString();
    assertThat(reasonCode).isEqualTo("851");

    Base period = encounterResource.get(0).getNamedProperty("period").getValues().get(0);
    // EVN.6 is used for start period (with no end) if there is no PV1.44 
    String startPeriod = period.getNamedProperty("start").getValues().get(0).toString();
    int endPeriodSize = period.getNamedProperty("end").getValues().size();
    assertThat(startPeriod).isEqualTo("DateTimeType[2021-03-19T13:47:35+08:00]");
    assertThat(endPeriodSize).isZero();
  }

@Test
public void validate_evn_segment_no_period_override() {

  String hl7message = "MSH|^~\\&|||||||ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6||||||\r" +
                      "EVN||||7525||20210319134735|\r"+
                      "PV1|1|I||R|||||||||R|1||||||||||||||||||||||||||||||200603150624|200603150625|||||||";                    
                      

  HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
  String json = ftv.convert(hl7message , OPTIONS);
  assertThat(json).isNotBlank();

  FHIRContext context = new FHIRContext(true, false);
  IBaseResource bundleResource = context.getParser().parseResource(json);
  assertThat(bundleResource).isNotNull();
  Bundle b = (Bundle) bundleResource;
  List<BundleEntryComponent> e = b.getEntry();

  // Find the encounter from the FHIR bundle.
  List<Resource> encounterResource = e.stream()
      .filter(v -> ResourceType.Encounter == v.getResource().getResourceType()).map(BundleEntryComponent::getResource)
      .collect(Collectors.toList());
  assertThat(encounterResource).hasSize(1);

  // ENV.4 is used for reasonCode
  String reasonCode = encounterResource.get(0).getNamedProperty("reasonCode").getValues().get(0).getNamedProperty("text").getValues().get(0).toString();
  assertThat(reasonCode).isEqualTo("7525");

  Base period = encounterResource.get(0).getNamedProperty("period").getValues().get(0);
  // EVN.6 is used for start period if there is no PV1.44 but since we have a PV1.44 it should use that not EVN.6
  String startPeriod = period.getNamedProperty("start").getValues().get(0).toString();
  // And use PV1.45 for end period.
  String endPeriod = period.getNamedProperty("end").getValues().get(0).toString();
  assertThat(startPeriod).isEqualTo("DateTimeType[2006-03-15T06:24:00+08:00]");
  assertThat(endPeriod).isEqualTo("DateTimeType[2006-03-15T06:25:00+08:00]");
  
}

}
