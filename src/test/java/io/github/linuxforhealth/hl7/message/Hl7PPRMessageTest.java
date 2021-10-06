/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hl7PPRMessageTest {
  private static FHIRContext context = new FHIRContext();
  private static final ConverterOptions OPTIONS = new Builder().withValidateResource().build();
  private static final Logger LOGGER = LoggerFactory.getLogger(Hl7PPRMessageTest.class);


  @Test
  public void test_ppr_pc1() throws IOException {
    String hl7message =
        "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson||19600614|M||C|99 Oakland #106^^qwerty^OH^44889||^^^^^626^5641111|^^^^^626^5647654|||||343132266|||N\r"
            + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"
            + "NTE|1|P|Problem Comments\r" + "VAR|varid1|200603150610\r"
            + "OBX|1|TX|^Type of protein feed^L||ECHOCARDIOGRAPHIC REPORT||||||F|||20150930164100|||\r"
            + "OBX|2|TX|^SOMEd^L||NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH||||||F|||20150930165100|||";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);

    assertThat(json).isNotBlank();
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> patientResource =
        e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(patientResource).hasSize(1);

    List<Resource> obsResource =
        e.stream().filter(v -> ResourceType.Observation == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(obsResource).hasSize(2);

    List<Resource> encounterResource =
        e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(encounterResource).hasSize(1);

    List<Resource> conditionresource =
        e.stream().filter(v -> ResourceType.Condition == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(conditionresource).hasSize(1);
  }

  @Test
  public void testPprPc1ServiceRequestPresentDocumentReferenceDetails() throws IOException {
    String hl7message =
        "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|202101010000|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\n"
    		+ "PID|||1234^^^^MR||DOE^JANE^|||F|||||||||||||||||||||\n"
            + "PV1||I|6N^1234^A^GENHOS|||||||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\n"
            + "PRB|AD||202101010000|aortic stenosis|53692||2|||202101010000\n"
            + "OBX|1|NM|111^TotalProtein||7.5|gm/dl|5.9-8.4||||F\n"
            + "NTE|1|P|Problem Comments\n"
            + "ORC|NW|1000^OE|9999999^RX|||E|^Q6H^D10^^^R\n"
            // OBR.7 is used for the timestamp (because no TXA in a PPR_PC1 message)
            + "OBR|1|TESTID|TESTID|||201801180346|201801180347||||||||||||||||||F||||||WEAKNESS||||||||||||\n"
            // Next three lines create an attachment
            + "OBX|1|TX|||ECHOCARDIOGRAPHIC REPORT||||||F|||202101010000|||\n"
            + "OBX|2|TX|||NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH||||||F|||202101010000|||\n"
            + "OBX|3|TX|||HYPERDYNAMIC LV SYSTOLIC FUNCTION, VISUAL EF 80%||||||F|||202101010000|||\n";
            
    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    assertThat(json).isNotBlank();
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> patientResource =
        e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(patientResource).hasSize(1);
    
    // OBX under PRB (the PROBLEM.PROBLEM_OBSERVATION.OBSERVATION) creates an Observation resource
    List<Resource> obsResource =
            e.stream().filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(obsResource).hasSize(1);

    List<Resource> encounterResource =
        e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(encounterResource).hasSize(1);

    List<Resource> serviceRequestResource =
            e.stream().filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(serviceRequestResource).hasSize(1);

    List<Resource> documentRefResource =
            e.stream().filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(documentRefResource).hasSize(1);

    DocumentReference documentRef = ResourceUtils.getResourceDocumentReference(documentRefResource.get(0), context);
    DocumentReference.DocumentReferenceContentComponent content = documentRef.getContentFirstRep();
    assertThat(content.getAttachment().getContentType()).isEqualTo("text/plain"); // Currently always defaults to text/plain
    // TODO: why can't this OBR.7 date be found?
    // assertThat(content.getAttachment().getCreationElement().toString()).containsPattern("2021-01-01T01:00:00"); // OBR.7 date
    assertThat(content.getAttachment().hasData()).isTrue();
    String decodedData = new String(Base64.getDecoder().decode(content.getAttachment().getDataElement().getValueAsString()));
    assertThat(decodedData).isEqualTo("ECHOCARDIOGRAPHIC REPORT\nNORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH\nHYPERDYNAMIC LV SYSTOLIC FUNCTION, VISUAL EF 80%\n");


  }
 
  
  @Test
  @Disabled("PPR_PC2 not supported yet")
  public void test_ppr_pc2_patient_encounter_present() throws IOException {
	  String hl7message =
		        "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC2|1|P^I|2.6||||||ASCII||\r"
		            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson||19600614|M||C|99 Oakland #106^^qwerty^OH^44889||^^^^^626^5641111|^^^^^626^5647654|||||343132266|||N\r"
		            + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
		            + "PRB|UP|200603150625|aortic stenosis|53692||2||200603150625\r"
		            + "NTE|1|P|Problem Comments\r" + "VAR|varid1|200603150610\r"
		            + "OBX|1|TX|^Type of protein feed^L||ECHOCARDIOGRAPHIC REPORT||||||F|||20150930164100|||\r"
		            + "OBX|2|TX|^SOMEd^L||NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH||||||F|||20150930165100|||";

      HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
      String json = ftv.convert(hl7message, OPTIONS);
      assertThat(json).isNotBlank();
      LOGGER.info("FHIR json result:\n" + json);
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

  }


  @Test
  @Disabled("PPR_PC2 not supported yet")
  public void test_ppr_pc2_service_request_present() throws IOException {
    String hl7message =
            "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|202101010000|security|PPR^PC2^PPR_PC1|1|P^I|2.6||||||ASCII||\n"
        		+ "PID|||1234^^^^MR||DOE^JANE^|||F|||||||||||||||||||||\n"
                + "PRB|AD||202101010000|aortic stenosis|53692||2|||202101010000\n"
                + "ORC|NW|1000^OE|9999999^RX|||E|^Q6H^D10^^^R\n"
                + "OBR|1|TESTID|TESTID|||202101010000|202101010000||||||||||||||||||F||||||WEAKNESS||||||||||||\n"
                + "OBX|1|TX|||ECHOCARDIOGRAPHIC REPORT||||||F|||202101010000|||\n"
                + "OBX|2|TX|||NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH||||||F|||202101010000|||\n";
        
        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS);

        assertThat(json).isNotBlank();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> patientResource =
            e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> documentRefResource =
        	e.stream().filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(documentRefResource).hasSize(1);
      
        List<Resource> obsResource =
                e.stream().filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                    .map(BundleEntryComponent::getResource).collect(Collectors.toList());
            assertThat(obsResource).hasSize(0);

        List<Resource> encounterResource =
            e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(encounterResource).hasSize(1);

        List<Resource> serviceRequestResource =
            e.stream().filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(serviceRequestResource).hasSize(1);
      }


  @Test
  @Disabled("PPR_PC3 not supported yet")
  public void test_ppr_pc3_service_request_present() throws IOException {
	    String hl7message =
	        "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|202101010000|security|PPR^PC3^PPR_PC1|1|P^I|2.6||||||ASCII||\n"
	    		+ "PID|||1234^^^^MR||DOE^JANE^|||F|||||||||||||||||||||\n"
	            + "PRB|AD||202101010000|aortic stenosis|53692||2|||202101010000\n"
	            + "ORC|NW|1000^OE|9999999^RX|||E|^Q6H^D10^^^R\n"
	            + "OBR|1|TESTID|TESTID|||202101010000|202101010000||||||||||||||||||F||||||WEAKNESS||||||||||||\n"
	            + "OBX|1|TX|||ECHOCARDIOGRAPHIC REPORT||||||F|||202101010000|||\n"
	            + "OBX|2|TX|||NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH||||||F|||202101010000|||\n";
	    
		    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
		    String json = ftv.convert(hl7message, OPTIONS);

		    assertThat(json).isNotBlank();
		    IBaseResource bundleResource = context.getParser().parseResource(json);
		    assertThat(bundleResource).isNotNull();
		    Bundle b = (Bundle) bundleResource;
		    assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
		    List<BundleEntryComponent> e = b.getEntry();
		    List<Resource> patientResource =
		        e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
		            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
		    assertThat(patientResource).hasSize(1);

		    List<Resource> documentRefResource =
		    	e.stream().filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
		        	.map(BundleEntryComponent::getResource).collect(Collectors.toList());
		    assertThat(documentRefResource).hasSize(1);
		  
		    List<Resource> obsResource =
		            e.stream().filter(v -> ResourceType.Observation == v.getResource().getResourceType())
		                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
		    assertThat(obsResource).hasSize(0);

		    List<Resource> encounterResource =
		        e.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
		            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
		    assertThat(encounterResource).hasSize(1);

		    List<Resource> serviceRequestResource =
		            e.stream().filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
		                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
		    assertThat(serviceRequestResource).hasSize(1);
		  }

}
