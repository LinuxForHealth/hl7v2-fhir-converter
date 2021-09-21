/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;

import java.math.BigDecimal;
import java.util.List;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Duration;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Encounter.EncounterParticipantComponent;
import org.hl7.fhir.r4.model.Observation.ObservationReferenceRangeComponent;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Extension;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.segments.util.DatatypeUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hl7EncounterFHIRConversionTest {

  private static FHIRContext context = new FHIRContext(true, false);
  private static final Logger LOGGER = LoggerFactory.getLogger(Hl7EncounterFHIRConversionTest.class);
  private static final ConverterOptions OPTIONS = new Builder().withValidateResource().withPrettyPrint().build();

  @Test@Disabled
  public void test_encounter_visitdescription_present() {
    String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A02|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
    		+ "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
    		+ "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
    		+ "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n"
    		+ "PV2||TEL||||X-5546||20210330144208|20210309|||<This field> should be found \"Encounter.text\" \\T\\ formatted as xhtml with correct escaped characters.\\R\\HL7 newline should be processed as well|||||||||n|N|South Shore Hosptial Weymouth^SSHW^^^^^^SSH-WEYMOUTH|||||||||N||||||\n";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    assertThat(json).isNotBlank();
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

  @Test@Disabled
  public void test_encounter_visitdescription_missing() {
    String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A02|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
    		+ "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
    		+ "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
    		+ "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n"
    		+ "PV2||TEL||||X-5546||20210330144208|20210309||||||||||||n|N|South Shore Hosptial Weymouth^SSHW^^^^^^SSH-WEYMOUTH|||||||||N||||||\n";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    assertThat(json).isNotBlank();
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

  @Test@Disabled
  public void test_encounter_PV2_serviceProvider() {
    String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A02|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
    		+ "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
    		+ "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
    		+ "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n"
    		+ "PV2||TEL||||X-5546||20210330144208|20210309||||||||||||n|N|South Shore Hosptial Weymouth^SSHW^^^^^^SSH_WEYMOUTH|||||||||N||||||\n";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    assertThat(json).isNotBlank();
    LOGGER.info("FHIR json result:\n" + json);

    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> encounterResource = e.stream()
            .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(encounterResource).hasSize(1);
    
    Encounter encounter = getResourceEncounter(encounterResource.get(0));
    Reference serviceProvider = encounter.getServiceProvider();
    assertThat(serviceProvider).isNotNull();
    String providerString =serviceProvider.getReference();
    assertThat(providerString).isEqualTo("Organization/SSH.WEYMOUTH");
    

    List<Resource> organizations = e.stream()
            .filter(v -> ResourceType.Organization == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(organizations).hasSize(1);
    
    Organization orgResource = getResourceOrganization(organizations.get(0));
    assertThat(orgResource.getId()).isEqualTo(providerString);
    assertThat(orgResource.getName()).isEqualTo("South Shore Hosptial Weymouth");
  }

  @Test@Disabled
  public void test_encounter_PV2_serviceProvider_idfix() {
    String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A02|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
    		+ "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
    		+ "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
    		+ "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n"
    		+ "PV2||TEL||||X-5546||20210330144208|20210309||||||||||||n|N|South Shore Hosptial Weymouth^SSHW^^^^^^SSH*WEYMOUTH WEST_BUILD-7.F|||||||||N||||||\n";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    assertThat(json).isNotBlank();
    LOGGER.info("FHIR json result:\n" + json);

    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> encounterResource = e.stream()
            .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(encounterResource).hasSize(1);
    
    Encounter encounter = getResourceEncounter(encounterResource.get(0));
    Reference serviceProvider = encounter.getServiceProvider();
    assertThat(serviceProvider).isNotNull();
    String providerString =serviceProvider.getReference();
    assertThat(providerString).isEqualTo("Organization/SSH.WEYMOUTH.WEST.BUILD-7.F");
    

    List<Resource> organizations = e.stream()
            .filter(v -> ResourceType.Organization == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(organizations).hasSize(1);
    
    Organization orgResource = getResourceOrganization(organizations.get(0));
    assertThat(orgResource.getId()).isEqualTo(providerString);
    assertThat(orgResource.getName()).isEqualTo("South Shore Hosptial Weymouth");
  }

  @Test@Disabled
  public void test_encounter_PV1_serviceProvider() {
    String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A02|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
    		+ "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
    		+ "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
    		+ "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n"
    		+ "PV2||TEL||||X-5546||20210330144208|20210309||||||||||||n|N|South Shore Hosptial Weymouth|||||||||N||||||\n";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    assertThat(json).isNotBlank();
    LOGGER.info("FHIR json result:\n" + json);

    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> encounterResource = e.stream()
            .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(encounterResource).hasSize(1);
    
    Encounter encounter = getResourceEncounter(encounterResource.get(0));
    Reference serviceProvider = encounter.getServiceProvider();
    assertThat(serviceProvider).isNotNull();
    String providerString =serviceProvider.getReference();
    assertThat(providerString).isEqualTo("Organization/Toronto");
    

    List<Resource> organizations = e.stream()
            .filter(v -> ResourceType.Organization == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(organizations).hasSize(1);
    
    Organization orgResource = getResourceOrganization(organizations.get(0));
    assertThat(orgResource.getId()).isEqualTo(providerString);
    assertThat(orgResource.getName()).isEqualTo("South Shore Hosptial Weymouth");
  }
  @Test
  public void test_encounter_class() {
      // PV1.2 has mapped value and should returned fhir value
      String hl7message = "MSH|^~\\&|PROSOLV|SENTARA|WHIA|IBM|20151008111200|S1|ADT^A01^ADT_A01|MSGID000001|T|2.6|10092|PRPA008|AL|AL|100|8859/1|ENGLISH|ARM|ARM5007\n"
              + "EVN|A04|20151008111200|20171013152901|O|OID1006|20171013153621|EVN1009\n"
              + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
              + "PV1|1|E|SAN JOSE|A|10089|MILPITAS|2740^Torres^Callie|2913^Grey^Meredith^F|3065^Sloan^Mark^J|CAR|FOSTER CITY|AD|R|1|A4|VI|9052^Shepeard^Derek^|AH|10019181|FIC1002|IC|CC|CR|CO|20161012034052|60000|6|AC|GHBR|20160926054052|AC5678|45000|15000|D|20161016154413|DCD|SAN FRANCISCO|VEG|RE|O|AV|FREMONT|CALIFORNIA|20161013154626|20161014154634|10000|14000|2000|4000|POL8009|V|PHY6007\n";
      Encounter encounter = ResourceUtils.getEncounter(hl7message);

      assertThat(encounter.hasClass_()).isTrue();
      Coding encounterClass = encounter.getClass_();
      assertThat(encounterClass.getCode()).isEqualTo("EMER");
      assertThat(encounterClass.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v3-ActCode");
      assertThat(encounterClass.getDisplay()).isEqualTo("emergency");
      assertThat(encounterClass.getVersion()).isNull();

      // Should return "unknown"  if not a mapped value
      hl7message = "MSH|^~\\&|PROSOLV|SENTARA|WHIA|IBM|20151008111200|S1|ADT^A01^ADT_A01|MSGID000001|T|2.6|10092|PRPA008|AL|AL|100|8859/1|ENGLISH|ARM|ARM5007\n"
              + "EVN|A04|20151008111200|20171013152901|O|OID1006|20171013153621|EVN1009\n"
              + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
              + "PV1|1|L|SAN JOSE|A|10089|MILPITAS|2740^Torres^Callie|2913^Grey^Meredith^F|3065^Sloan^Mark^J|CAR|FOSTER CITY|AD|R|1|A4|VI|9052^Shepeard^Derek^|AH|10019181|FIC1002|IC|CC|CR|CO|20161012034052|60000|6|AC|GHBR|20160926054052|AC5678|45000|15000|D|20161016154413|DCD|SAN FRANCISCO|VEG|RE|O|AV|FREMONT|CALIFORNIA|20161013154626|20161014154634|10000|14000|2000|4000|POL8009|V|PHY6007\n";
      encounter = ResourceUtils.getEncounter(hl7message);

      assertThat(encounter.hasClass_()).isTrue();
      encounterClass = encounter.getClass_();
      assertThat(encounterClass.getCode()).isEqualTo("unknown");
      assertThat(encounterClass.getSystem()).isNull();
      assertThat(encounterClass.getDisplay()).isNull();
      assertThat(encounterClass.getVersion()).isNull();

  }

  @Test
  public void test_encounter_reason_code() {
      //Checks EVN.4 for reason Code
      String hl7message = "MSH|^~\\&|PROSOLV|SENTARA|WHIA|IBM|20151008111200|S1|ADT^A01^ADT_A01|MSGID000001|T|2.6|10092|PRPA008|AL|AL|100|8859/1|ENGLISH|ARM|ARM5007\n"
              + "EVN|A04|20151008111200|20171013152901|O|OID1006|20171013153621|EVN1009\n"
              + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
              + "PV1|1|E|SAN JOSE|A|10089|MILPITAS|2740^Torres^Callie|2913^Grey^Meredith^F|3065^Sloan^Mark^J|CAR|FOSTER CITY|AD|R|1|A4|VI|9052^Shepeard^Derek^|AH|10019181|FIC1002|IC|CC|CR|CO|20161012034052|60000|6|AC|GHBR|20160926054052|AC5678|45000|15000|D|20161016154413|DCD|SAN FRANCISCO|VEG|RE|O|AV|FREMONT|CALIFORNIA|20161013154626|20161014154634|10000|14000|2000|4000|POL8009|V|PHY6007\n"
              + "PV2|SAN BRUNO|AC4567|vomits|less equipped|purse|SAN MATEO|HO|20171014154626|20171018154634|4|3|DIAHHOREA|RSA456|20161013154626|Y|D|20191026001640|O|Y|1|F|Y|KAISER|AI|2|20161013154626|ED|20171018001900|20161013154626|10000|RR|Y|20171108002129|Y|Y|N|N|C^Car^HL70430\n";

      Encounter encounter = ResourceUtils.getEncounter(hl7message);

      assertThat(encounter.hasReasonCode()).isTrue();
      CodeableConcept encounterReason = encounter.getReasonCodeFirstRep();
      Coding encounterReasonCoding = encounterReason.getCodingFirstRep();
      assertThat(encounterReasonCoding.getCode()).isEqualTo("O");
      assertThat(encounterReasonCoding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0062");
      assertThat(encounterReasonCoding.getDisplay()).isEqualTo("Other");
      assertThat(encounterReasonCoding.getVersion()).isNull();

      //Checks PV2.3 for reason code
      hl7message = "MSH|^~\\&|PROSOLV|SENTARA|WHIA|IBM|20151008111200|S1|ADT^A01^ADT_A01|MSGID000001|T|2.6|10092|PRPA008|AL|AL|100|8859/1|ENGLISH|ARM|ARM5007\n"
              + "EVN|A04|20151008111200|20171013152901||OID1006|20171013153621|EVN1009\n"
              + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
              + "PV1|1|L|SAN JOSE|A|10089|MILPITAS|2740^Torres^Callie|2913^Grey^Meredith^F|3065^Sloan^Mark^J|CAR|FOSTER CITY|AD|R|1|A4|VI|9052^Shepeard^Derek^|AH|10019181|FIC1002|IC|CC|CR|CO|20161012034052|60000|6|AC|GHBR|20160926054052|AC5678|45000|15000|D|20161016154413|DCD|SAN FRANCISCO|VEG|RE|O|AV|FREMONT|CALIFORNIA|20161013154626|20161014154634|10000|14000|2000|4000|POL8009|V|PHY6007\n"
              + "PV2|SAN BRUNO|AC4567|vomits|less equipped|purse|SAN MATEO|HO|20171014154626|20171018154634|4|3|DIAHHOREA|RSA456|20161013154626|Y|D|20191026001640|O|Y|1|F|Y|KAISER|AI|2|20161013154626|ED|20171018001900|20161013154626|10000|RR|Y|20171108002129|Y|Y|N|N|C^Car^HL70430\n";

      encounter = ResourceUtils.getEncounter(hl7message);

      assertThat(encounter.hasReasonCode()).isTrue();
      encounterReason = encounter.getReasonCodeFirstRep();
      encounterReasonCoding = encounterReason.getCodingFirstRep();
      assertThat(encounterReasonCoding.getCode()).isEqualTo("vomits");
      assertThat(encounterReasonCoding.getSystem()).isNull();
      assertThat(encounterReasonCoding.getDisplay()).isNull();
      assertThat(encounterReasonCoding.getVersion()).isNull();

  }

    @Test
    public void test_encounter_length() {
        //When length between encounters is a day or more apart the units should be "Days"
        String hl7message = "MSH|^~\\&|PROSOLV|SENTARA|WHIA|IBM|20151008111200|S1|ADT^A01^ADT_A01|MSGID000001|T|2.6|10092|PRPA008|AL|AL|100|8859/1|ENGLISH|ARM|ARM5007\n"
                + "EVN|A04|20151008111200|20171013152901|O|OID1006|20171013153621|EVN1009\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1|1|E|SAN JOSE|A|10089|MILPITAS|2740^Torres^Callie|2913^Grey^Meredith^F|3065^Sloan^Mark^J|CAR|FOSTER CITY|AD|R|1|A4|VI|9052^Shepeard^Derek^|AH|10019181|FIC1002|IC|CC|CR|CO|20161012034052|60000|6|AC|GHBR|20160926054052|AC5678|45000|15000|D|20161016154413|DCD|SAN FRANCISCO|VEG|RE|O|AV|FREMONT|CALIFORNIA|20161013154626|20161014154634|10000|14000|2000|4000|POL8009|V|PHY6007\n";

        Encounter encounter = ResourceUtils.getEncounter(hl7message);

        assertThat(encounter.hasLength()).isTrue();
        Duration encounterLength = encounter.getLength();
        assertThat(encounterLength.getValue()).isEqualTo((BigDecimal.valueOf(1)));
        assertThat(encounterLength.getUnit()).isEqualTo("Days");


        //When length between encounters is a less than a apart the units should be "Minutes" and test PV2 segment
        hl7message = "MSH|^~\\&|PROSOLV|SENTARA|WHIA|IBM|20151008111200|S1|ADT^A01^ADT_A01|MSGID000001|T|2.6|10092|PRPA008|AL|AL|100|8859/1|ENGLISH|ARM|ARM5007\n"
                + "EVN|A04|20151008111200|20171013152901||OID1006|20171013153621|EVN1009\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                + "PV1|1|L|SAN JOSE|A|10089|MILPITAS|2740^Torres^Callie|2913^Grey^Meredith^F|3065^Sloan^Mark^J|CAR|FOSTER CITY|AD|R|1|A4|VI|9052^Shepeard^Derek^|AH|10019181|FIC1002|IC|CC|CR|CO|20161012034052|60000|6|AC|GHBR|20160926054052|AC5678|45000|15000|D|20161016154413|DCD|SAN FRANCISCO|VEG|RE|O|AV|FREMONT|CALIFORNIA||20171018154634|10000|14000|2000|4000|POL8009|V|PHY6007\n"
                + "PV2|SAN BRUNO|AC4567|vomits|less equipped|purse|SAN MATEO|HO|20171014154626|20171018154634|4|3|DIAHHOREA|RSA456|20161013154626|Y|D|20191026001640|O|Y|1|F|Y|KAISER|AI|2|20161013154626|ED|20171018001900|20161013154626|10000|RR|Y|20171108002129|Y|Y|N|N|C^Car^HL70430\n";

        encounter = ResourceUtils.getEncounter(hl7message);

        assertThat(encounter.hasLength()).isTrue();
        encounterLength = encounter.getLength();
        assertThat(encounterLength.getValue()).isEqualTo((BigDecimal.valueOf(3)));
        assertThat(encounterLength.getUnit()).isEqualTo("Days");

    }

  @Test
  public void test_encounter_modeOfarrival() {
    String hl7message = "MSH|^~\\&|PROSOLV|SENTARA|WHIA|IBM|20151008111200|S1|ADT^A01^ADT_A01|MSGID000001|T|2.6|10092|PRPA008|AL|AL|100|8859/1|ENGLISH|ARM|ARM5007\n"
    		+ "EVN|A04|20151008111200|20171013152901|O|OID1006|20171013153621|EVN1009\n"
    		+ "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
    		+ "PV1|1|E|SAN JOSE|A|10089|MILPITAS|2740^Torres^Callie|2913^Grey^Meredith^F|3065^Sloan^Mark^J|CAR|FOSTER CITY|AD|R|1|A4|VI|9052^Shepeard^Derek^|AH|10019181|FIC1002|IC|CC|CR|CO|20161012034052|60000|6|AC|GHBR|20160926054052|AC5678|45000|15000|D|20161016154413|DCD|SAN FRANCISCO|VEG|RE|O|AV|FREMONT|CALIFORNIA|20161013154626|20161014154634|10000|14000|2000|4000|POL8009|V|PHY6007\n"
    		+ "PV2|SAN BRUNO|AC4567|vomits|less equipped|purse|SAN MATEO|HO|20171014154626|20171018154634|4|3|DIAHHOREA|RSA456|20161013154626|Y|D|20191026001640|O|Y|1|F|Y|KAISER|AI|2|20161013154626|ED|20171018001900|20161013154626|10000|RR|Y|20171108002129|Y|Y|N|N|C^Car^HL70430\n";

    Encounter encounter = ResourceUtils.getEncounter(hl7message);

    List<Extension> extensionList = encounter.getExtension();
    assertNotNull(extensionList);
    assertThat(extensionList).hasSize(1);


    boolean extFound=false;
    for (Extension ext : extensionList)
    {
    	if (ext.getUrl().equals("http://hl7.org/fhir/StructureDefinition/encounter-modeOfArrival"))
    	{
    		extFound = true;
    		assertTrue(ext.getValue() instanceof Coding);

    		Coding valueCoding = (Coding) ext.getValue();

    		assertThat(valueCoding.getCode()).isEqualTo("C");
    		assertThat(valueCoding.getDisplay()).isEqualTo("Car");
    		assertThat(valueCoding.getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0430");

    		break;
    	}
    }
    assertTrue(extFound, "modeOfArrival extension not found");
   
  }

  @Test@Disabled
  public void test_encounter_modeOfarrival_invalid_singlevalue() {
    String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A02|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
    		+ "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
    		+ "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
    		+ "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n"
    		+ "PV2||TEL||||X-5546||20210330144208|20210309||||||||||||n|N|South Shore Hosptial Weymouth^SSHW^^^^^^SSH-WEYMOUTH|||||||||N||||||AMBULATORY\n";

    Encounter encounter = ResourceUtils.getEncounter(hl7message);


    List<Extension> extensionList = encounter.getExtension();
    assertNotNull(extensionList);
    assertThat(extensionList).isNotEmpty();
    
    
    boolean extFound=false;
    for (Extension ext : extensionList)
    {
    	if (ext.getUrl().equals("http://hl7.org/fhir/StructureDefinition/encounter-modeOfArrival"))
    	{
    		extFound = true;
    		assertTrue(ext.getValue() instanceof Coding);
    
    		Coding valueCoding = (Coding) ext.getValue();
    		
    		assertThat(valueCoding.getCode()).isEqualTo("AMBULATORY");
    		assertThat(valueCoding.getDisplay()).isNull();
    		assertThat(valueCoding.getSystem()).isNull();
    		
    		break;
    	}
    }
    assertTrue(extFound, "modeOfArrival extension not found");
   
   
  }

  @Test@Disabled
  public void test_encounter_modeOfarrival_invalid_with_codeAndDisplay() {
    String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A02|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
    		+ "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
    		+ "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
    		+ "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n"
    		+ "PV2||TEL||||X-5546||20210330144208|20210309||||||||||||n|N|South Shore Hosptial Weymouth^SSHW^^^^^^SSH-WEYMOUTH|||||||||N||||||AMB^AMBULATORY\n";

    Encounter encounter = ResourceUtils.getEncounter(hl7message);


    List<Extension> extensionList = encounter.getExtension();
    assertNotNull(extensionList);
    assertThat(extensionList).isNotEmpty();
    
    
    boolean extFound=false;
    for (Extension ext : extensionList)
    {
    	if (ext.getUrl().equals("http://hl7.org/fhir/StructureDefinition/encounter-modeOfArrival"))
    	{
    		extFound = true;
    		assertTrue(ext.getValue() instanceof Coding);
    
    		Coding valueCoding = (Coding) ext.getValue();
    		
    		assertThat(valueCoding.getCode()).isEqualTo("AMB");
    		assertThat(valueCoding.getDisplay()).isEqualTo("AMBULATORY");
    		assertThat(valueCoding.getSystem()).isNull();
    		
    		break;
    	}
    }
    assertTrue(extFound, "modeOfArrival extension not found");
   
   
  }

  @Test@Disabled
  public void test_encounter_modeOfarrival_invalid_with_system() {
    String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A02|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
    		+ "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
    		+ "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
    		+ "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n"
    		+ "PV2||TEL||||X-5546||20210330144208|20210309||||||||||||n|N|South Shore Hosptial Weymouth^SSHW^^^^^^SSH-WEYMOUTH|||||||||N||||||AMB^AMBULATORY^FUNKY\n";

    Encounter encounter = ResourceUtils.getEncounter(hl7message);

    List<Extension> extensionList = encounter.getExtension();
    assertNotNull(extensionList);
    assertThat(extensionList).isNotEmpty();
    
    
    boolean extFound=false;
    for (Extension ext : extensionList)
    {
    	if (ext.getUrl().equals("http://hl7.org/fhir/StructureDefinition/encounter-modeOfArrival"))
    	{
    		extFound = true;
    		assertTrue(ext.getValue() instanceof Coding);
    
    		Coding valueCoding = (Coding) ext.getValue();
    		
    		assertThat(valueCoding.getCode()).isEqualTo("AMB");
    		assertThat(valueCoding.getDisplay()).isEqualTo("AMBULATORY");
    		assertThat(valueCoding.getSystem()).isEqualTo("urn:id:FUNKY");
    		
    		break;
    	}
    }
    assertTrue(extFound, "modeOfArrival extension not found");
   
   
  }
  @Test@Disabled
  public void test_encounter_PV2segment_missing() {
    String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A02|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
    		+ "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
    		+ "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
    		+ "PV1||I|^^^^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n";

    Encounter encounter = ResourceUtils.getEncounter(hl7message);

    Narrative encText = encounter.getText();
    assertNull(encText.getStatus());
    assertThat(encText.getDiv().getChildNodes()).isEmpty();
    
    List<Extension> extensionList = encounter.getExtension();
    assertNotNull(extensionList);
    assertThat(extensionList).isEmpty();
    
    Reference serviceProvider = encounter.getServiceProvider();
    assertThat(serviceProvider).isNotNull();
    assertThat(serviceProvider.getReference()).isNull();
   
  }

  @Disabled("type is not yet implmemented. tracking down issue")
  @Test
  public void test_encounter_type_PV1_4() {
    String hl7message = "MSH|^~\\\\&|SE050|050|PACS|050|20120912011230||ADT^A01|102|T|2.6|||AL|NE|764|||||||^4086::132:2A57:3C28^IPv6\n"
    		+ "PID|0010||PID1234^5^M11^A^MR^HOSP~1234568965^^^USA^SS||DOE^JOHN^A^||19800202|F||W|111 TEST_STREET_NAME^^TEST_CITY^NY^111-1111^USA||(905)111-1111|||S|ZZ|12^^^124|34-13-312||||TEST_BIRTH_PLACE\n"
    		+ "PV1|1|ff|yyy|EL|ABC||200^ATTEND_DOC_FAMILY_TEST^ATTEND_DOC_GIVEN_TEST|201^REFER_DOC_FAMILY_TEST^REFER_DOC_GIVEN_TEST|202^CONSULTING_DOC_FAMILY_TEST^CONSULTING_DOC_GIVEN_TEST|MED|||||B6|E|272^ADMITTING_DOC_FAMILY_TEST^ADMITTING_DOC_GIVEN_TEST||48390|||||||||||||||||||||||||201409122200|";

    Encounter encounter = ResourceUtils.getEncounter(hl7message);

    List<CodeableConcept> types = encounter.getType();
    assertThat(types).hasSize(1);
    
    assertThat(types.get(0).getText()).isEqualTo("E");
   
  }
  @Test@Disabled
  public void test_encounter_participant_list() {
    String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A02|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
    		+ "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
    		+ "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
    		+ "PV1||I|^^^Toronto^^5642 Hilly Av||||2905^Doctor^Attending^M^IV^^M.D|5755^Doctor^Referring^^Sr|770542^Doctor^Consulting^Jr||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    assertThat(json).isNotBlank();
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
  @Test@Disabled
  public void test_encounter_participant_missing() {
    String hl7message = "MSH|^~\\&|WHI_LOAD_GENERATOR|IBM_TORONTO_LAB||IBM|20210330144208|8078780|ADT^A02|MSGID_4e1c575f-6c6d-47b2-ab9f-829f20c96db2|T|2.3\n"
    		+ "EVN||20210330144208||ADT_EVENT|007|20210309140700\n"
    		+ "PID|1||0a8a1752-e336-43e1-bf7f-0c8f6f437ca3^^^MRN||Patient^Load^Generator||19690720|M|Patient^Alias^Generator|AA|9999^^CITY^STATE^ZIP^CAN|COUNTY|(866)845-0900||ENGLISH^ENGLISH|SIN|NONE|Account_0a8a1752-e336-43e1-bf7f-0c8f6f437ca3|123-456-7890|||N|BIRTH PLACE|N||||||N\n"
    		+ "PV1||I|^^^Toronto^^5642 Hilly Av||||||||||||||59367^Doctor^Admitting||Visit_0a3be81e-144b-4885-9b4e-c5cd33c8f038|||||||||||||||||||||||||20210407191342\n";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    assertThat(json).isNotBlank();
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
    List<String> practionerIds = Arrays.asList("59367");
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
  
  /**
   * Testing Encounter correctly references Observation
   * 
   * @throws IOException
   */
  @Test
  public void testEncounterReferencesObservation() throws IOException {
      String hl7message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.6|\n"
              + "PID|||1234^^^^MR||DOE^JANE^|||F|||||||||||||||||||||\n"
              + "PV1|1|O|Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\n"
              + "OBX|1|SN|24467-3^CD3+CD4+ (T4 helper) cells [#/volume] in Blood^LN||=^440|{Cells}/uL^cells per microliter^UCUM|649-1346 cells/mcL|L|||F\r";

      HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
      String json = ftv.convert(hl7message, OPTIONS);
      IBaseResource bundleResource = context.getParser().parseResource(json);
      assertThat(bundleResource).isNotNull();
      Bundle b = (Bundle) bundleResource;
      List<BundleEntryComponent> e = b.getEntry();
      List<Resource> obsResource = e.stream()
              .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(obsResource).hasSize(1);
      
      List<Resource> encounterResource = e.stream()
              .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(encounterResource).hasSize(1);
      
      Encounter enc = (Encounter) encounterResource.get(0);
      List<Reference> reasonRefs = enc.getReasonReference();
      assertEquals(1, reasonRefs.size());
      assertTrue(reasonRefs.get(0).getReference().contains("Observation"));
  }

  /**
   * Testing Encounter correctly references Observation AND Diagnosis when both are present.
   * 
   * @throws IOException
   */
  @Test
  public void testEncounterReferencesObservationAndDiagnosis() throws IOException {
      String hl7message = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.6|\n"
              + "PID|||1234^^^^MR||DOE^JANE^|||F|||||||||||||||||||||\n"
              + "PV1|1|O|Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\n"
              + "OBX|1|SN|24467-3^CD3+CD4+ (T4 helper) cells [#/volume] in Blood^LN||=^440|{Cells}/uL^cells per microliter^UCUM|649-1346 cells/mcL|L|||F\r"
              + "DG1|1|ICD10|^Ovarian Cancer|||||||||||||||||||||\r";

      HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
      String json = ftv.convert(hl7message, OPTIONS);
      IBaseResource bundleResource = context.getParser().parseResource(json);
      assertThat(bundleResource).isNotNull();
      Bundle b = (Bundle) bundleResource;
      List<BundleEntryComponent> e = b.getEntry();
      List<Resource> obsResource = e.stream()
              .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(obsResource).hasSize(1);
      
      List<Resource> encounterResource = e.stream()
              .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(encounterResource).hasSize(1);
      
      Encounter enc = (Encounter) encounterResource.get(0);
      List<Reference> reasonRefs = enc.getReasonReference();
      assertEquals(2, reasonRefs.size());
      // Guess at the order of the references
      Reference refObservation = reasonRefs.get(0);
      Reference refCondition = reasonRefs.get(1);
      // If guessed wrong, reverse them
      if (!refObservation.getReference().contains("Observation")){
        refObservation = reasonRefs.get(1);
        refCondition = reasonRefs.get(0);   
      }
      assertTrue(refObservation.getReference().contains("Observation"));
      assertTrue(refCondition.getReference().contains("Condition"));
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

  private Organization getResourceOrganization(Resource resource) {
	    String s = context.getParser().encodeResourceToString(resource);
	    Class<? extends IBaseResource> klass = Organization.class;
	    return (Organization) context.getParser().parseResource(klass, s);
  }
}
