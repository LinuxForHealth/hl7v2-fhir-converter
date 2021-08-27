/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Assertions;

import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterParticipantComponent;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hl7EncounterFHIRConversionTest {

  private static FHIRContext context = new FHIRContext(true, false);
  private static final Logger LOGGER = LoggerFactory.getLogger(Hl7EncounterFHIRConversionTest.class);
  private static final ConverterOptions OPTIONS = new Builder().withValidateResource().build();

  @Test
  public void test_encounter_visitdescription_present() {
    String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A02|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
    		+ "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
    		+ "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
    		+ "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n"
    		+ "PV2||TEL||||X-5546||20210330144208|20210309|||<This field> should be found \"Encounter.text\" \\T\\ formatted as xhtml with correct escaped characters.\\R\\HL7 newline should be processed as well|||||||||n|N|South Shore Hosptial Weymouth^SSHW^^^^^^SSH-WEYMOUTH|||||||||N||||||AMBULATORY\n";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    assertThat(json).isNotBlank();
    System.out.println(json);
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> patientResource = e.stream()
            .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(patientResource).hasSize(1);

    List<Resource> encounterResource = e.stream()
            .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(encounterResource).hasSize(1);
    
    Encounter encounter = getResourceEncounter(encounterResource.get(0));

    //
    // "text": {
    //   "status": "additional",
    //    "div": "<div xmlns=\"http://www.w3.org/1999/xhtml\"><p>&lt;This field&gt; should be found &quot;Encounter.text&quot; &amp; formatted as xhtml with correct escaped characters.<br/>HL7 newline should be processed as well</p></div>"
    //  }
    //
    
    Narrative encText = encounter.getText();
    assertNotNull(encText);
    assertEquals("additional", encText.getStatusAsString());
    assertEquals("<div xmlns=\"http://www.w3.org/1999/xhtml\"><p>&lt;This field&gt; should be found &quot;Encounter.text&quot; &amp; formatted as xhtml with correct escaped characters.<br/>HL7 newline should be processed as well</p></div>", encText.getDivAsString());
 
  }

  @Test
  public void test_encounter_visitdescription_missing() {
    String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A02|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
    		+ "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
    		+ "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
    		+ "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n"
    		+ "PV2||TEL||||X-5546||20210330144208|20210309||||||||||||n|N|South Shore Hosptial Weymouth^SSHW^^^^^^SSH-WEYMOUTH|||||||||N||||||AMBULATORY\n";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    assertThat(json).isNotBlank();
    System.out.println(json);
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> patientResource = e.stream()
            .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(patientResource).hasSize(1);

    List<Resource> encounterResource = e.stream()
            .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(encounterResource).hasSize(1);
    
    Encounter encounter = getResourceEncounter(encounterResource.get(0));
 
    Narrative encText = encounter.getText();
    assertNull(encText.getStatus());
    assertThat(encText.getDiv().getChildNodes()).isEmpty();
   
  }

  @Test
  public void test_encounter_PV2segment_missing() {
    String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A02|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
    		+ "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
    		+ "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
    		+ "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    assertThat(json).isNotBlank();
    System.out.println(json);
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> patientResource = e.stream()
            .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(patientResource).hasSize(1);

    List<Resource> encounterResource = e.stream()
            .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(encounterResource).hasSize(1);
    
    Encounter encounter = getResourceEncounter(encounterResource.get(0));
 
    Narrative encText = encounter.getText();
    assertNull(encText.getStatus());
    assertThat(encText.getDiv().getChildNodes()).isEmpty();
   
  }
  @Test
  public void test_encounter_participant_list() {
    String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A02|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
    		+ "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
    		+ "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
    		+ "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    assertThat(json).isNotBlank();
    System.out.println(json);
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> patientResource = e.stream()
            .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(patientResource).hasSize(1);

    List<Resource> encounterResource = e.stream()
            .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(encounterResource).hasSize(1);
    
    Encounter encounter = getResourceEncounter(encounterResource.get(0));
    
    List<EncounterParticipantComponent>  encParticipantList = encounter.getParticipant();
    assertThat(encParticipantList).hasSize(4);

    List<Resource> practioners = e.stream()
            .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(practioners).hasSize(4);
    
    HashMap<String, String> practionerMap = new HashMap<String, String>();
    //Make sure that practitioners found are matching the HL7
    List<String> practionerIds = Arrays.asList("2905", "5755", "770542", "59367");
    for(Resource r : practioners) {
    	Practitioner p = getResourcePractitioner(r);
    	assertThat(p.getIdentifier()).hasSize(1);
    	String value = p.getIdentifier().get(0).getValue();
    	assertThat(practionerIds).contains(value);
    	switch (value)
    	{
    	case "2905":
    		practionerMap.put("ATND",  p.getId());
    		break;
    	case "5755":
			practionerMap.put("REF",  p.getId());
    		break;
    	case "770542":
			practionerMap.put("CON",  p.getId());
    		break;
    	case "59367":
			practionerMap.put("ADM",  p.getId());
    		break;
    	}
    }
    
    //Make sure that practitioners are correctly mapped within the Encounter
    for(EncounterParticipantComponent component : encParticipantList)
    {
    	String code = component.getType().get(0).getCoding().get(0).getCode();
        assertEquals(practionerMap.get(code), component.getIndividual().getReference());
    }   
  }
  @Test
  public void test_encounter_participant_missing() {
    String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A02|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
    		+ "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
    		+ "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
    		+ "PV1||I|^^^Toronto^^5642 Hilly Av||||||||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    assertThat(json).isNotBlank();
    System.out.println(json);
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> patientResource = e.stream()
            .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(patientResource).hasSize(1);

    List<Resource> encounterResource = e.stream()
            .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(encounterResource).hasSize(1);
    
    Encounter encounter = getResourceEncounter(encounterResource.get(0));
    
    List<EncounterParticipantComponent>  encParticipantList = encounter.getParticipant();
    assertThat(encParticipantList).hasSize(1);

    List<Resource> practioners = e.stream()
            .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(practioners).hasSize(1);
    
    HashMap<String, String> practionerMap = new HashMap<String, String>();
    //Make sure that practitioners found are matching the HL7
    List<String> practionerIds = Arrays.asList( "5755",  "59367");
    for(Resource r : practioners) {
    	Practitioner p = getResourcePractitioner(r);
    	assertThat(p.getIdentifier()).hasSize(1);
    	String value = p.getIdentifier().get(0).getValue();
    	assertThat(practionerIds).contains(value);
    	switch (value)
    	{
    	case "2905":
    		practionerMap.put("ATND",  p.getId());
    		break;
    	case "5755":
			practionerMap.put("REF",  p.getId());
    		break;
    	case "770542":
			practionerMap.put("CON",  p.getId());
    		break;
    	case "59367":
			practionerMap.put("ADM",  p.getId());
    		break;
    	}
    }
    
    //Make sure that practitioners are correctly mapped within the Encounter
    for(EncounterParticipantComponent component : encParticipantList)
    {
    	String code = component.getType().get(0).getCoding().get(0).getCode();
        assertEquals(practionerMap.get(code), component.getIndividual().getReference());
    }   
  }
  private Encounter getResourceEncounter(Resource resource) {
	    String s = context.getParser().encodeResourceToString(resource);
	    Class<? extends IBaseResource> klass = Encounter.class;
	    return (Encounter) context.getParser().parseResource(klass, s);
  }

  private Practitioner getResourcePractitioner(Resource resource) {
	    String s = context.getParser().encodeResourceToString(resource);
	    Class<? extends IBaseResource> klass = Practitioner.class;
	    return (Practitioner) context.getParser().parseResource(klass, s);
  }

}
