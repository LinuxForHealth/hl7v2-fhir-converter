/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.io.IOException;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests focused on Service Requests details created by different message events
 *
 */
public class Hl7ServiceRequestFHIRConversionTest {

  private static FHIRContext context = new FHIRContext(true, false);
  private static final Logger LOGGER = LoggerFactory.getLogger(Hl7ServiceRequestFHIRConversionTest.class);
  private static final ConverterOptions OPTIONS = new Builder().withValidateResource().build();

  @Test
  public void test_ppr_pc1_service_request_status() throws IOException {
    String hl7message =
        "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|202101010000|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\n"
    		+ "PID|||1234^^^^MR||DOE^JANE^|||F|||||||||||||||||||||\n"
            + "PV1||I|6N^1234^A^GENHOS|||||||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\n"
            + "PRB|AD||202101010000|aortic stenosis|53692||2|||202101010000\n"
            + "OBX|1|NM|111^TotalProtein||7.5|gm/dl|5.9-8.4||||W\n"
            + "NTE|1|P|Problem Comments\n"
            + "ORC|NW|1000^OE|9999999^RX|||E|^Q6H^D10^^^R\n"
            + "OBR|1|TESTID|TESTID|||202101010000|202101010000||||||||||||||||||F||||||WEAKNESS||||||||||||\n"
            + "OBX|1|TX|||ECHOCARDIOGRAPHIC REPORT||||||F|||202101010000|||\n"
            + "OBX|2|TX|||NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH||||||F|||202101010000|||\n"
            + "OBX|3|TX|||HYPERDYNAMIC LV SYSTOLIC FUNCTION, VISUAL EF 80%||||||F|||202101010000|||\n";
    
    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    assertThat(json).isNotBlank();
    IBaseResource bundleResource = context.getParser().parseResource(json);
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    
    // OBX under PRB (the PROBLEM.PROBLEM_OBSERVATION.OBSERVATION) creates an Observation resource
    // Verifying this OBX to assure ServiceRequest is using correct OBX information
    List<Resource> obsResource =
            e.stream().filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    Observation o = (Observation) obsResource.get(0);
    assertEquals("111", o.getCode().getCodingFirstRep().getCode());
    assertEquals("TotalProtein", o.getCode().getText());
    assertEquals("Entered in Error", o.getStatus().getDisplay()); //From OBX.11 'W'
    
    List<Resource> serviceRequestResource =
            e.stream().filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    ServiceRequest sr = (ServiceRequest) serviceRequestResource.get(0);
   
    assertEquals("Completed", sr.getStatus().getDisplay()); //From OBX.11 'F'
  }
  
  @Test
  public void test_ppr_pc1_service_request_status_defaults_unknown() throws IOException {
    String hl7message =
        "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|202101010000|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\n"
    		+ "PID|||1234^^^^MR||DOE^JANE^|||F|||||||||||||||||||||\n"
            + "PV1||I|6N^1234^A^GENHOS|||||||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\n"
            + "PRB|AD||202101010000|aortic stenosis|53692||2|||202101010000\n"
            + "NTE|1|P|Problem Comments\n"
            + "ORC|NW|1000^OE|9999999^RX|||E|^Q6H^D10^^^R\n";
    
    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    assertThat(json).isNotBlank();
    IBaseResource bundleResource = context.getParser().parseResource(json);
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    
    List<Resource> serviceRequestResource =
            e.stream().filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    ServiceRequest sr = (ServiceRequest) serviceRequestResource.get(0);
   
    assertEquals("Unknown", sr.getStatus().getDisplay()); //Default since no OBX in message
  }

}
