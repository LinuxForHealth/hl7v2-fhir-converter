/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;


import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.Assert;
import org.junit.Test;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

public class Hl7ORUMessageTest {
  private static FHIRContext context = new FHIRContext();
  private static final ConverterOptions OPTIONS = new Builder().withValidateResource().build();
  private static final ConverterOptions OPTIONS_PRETTYPRINT = new Builder()
          .withBundleType(BundleType.COLLECTION)
          .withValidateResource()
          .withPrettyPrint()
          .build();

  @Test
  public void test_oru() throws IOException {
    String hl7message =
        "MSH|^~\\\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|ORU^R01|MSGID000005|T|2.6\r"
            + "PID||45483|45483||SMITH^SUZIE^||20160813|M|||123 MAIN STREET^^SCHENECTADY^NY^12345||(123)456-7890|||||^^^T||||||||||||\r"
            + "OBR|1||986^IA PHIMS Stage^2.16.840.1.114222.4.3.3.5.1.2^ISO|1051-2^New Born Screening^LN|||20151009173644|||||||||||||002|||||F|||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^||||3065^Mahoney^Paul^J|\r"
            + "OBX|1|TX|TS-F-01-002^Endocrine Disorders^L||obs report||||||F\r"
            + "OBX|2|TX|GA-F-01-024^Galactosemia^L||ECHOCARDIOGRAPHIC REPORT||||||F\r";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    assertThat(json).isNotBlank();
    System.out.println(json);
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();
    List<Resource> patientResource =
        e.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(patientResource).hasSize(1);

    List<Resource> obsResource =
        e.stream().filter(v -> ResourceType.Observation == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(obsResource).hasSize(2);

    List<Resource> physicianresource =
        e.stream().filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(physicianresource).hasSize(1);



    List<Resource> diagnosticresource =
        e.stream().filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(diagnosticresource).hasSize(1);

    DiagnosticReport diag = getResource(diagnosticresource.get(0));
    Reference patRef = diag.getSubject();
    assertThat(patRef.isEmpty()).isFalse();
    List<Reference> obsRef = diag.getResult();
    assertThat(obsRef.isEmpty()).isFalse();
    assertThat(obsRef).hasSize(2);
    assertThat(obsRef.get(0).isEmpty()).isFalse();
    assertThat(diag.getEffectiveDateTimeType().asStringValue())
        .isEqualTo("2015-10-09T17:36:44+08:00");
    assertThat(diag.getStatus().toCode()).isEqualTo("final");
    List<Reference> performerRef = diag.getResultsInterpreter();
    assertThat(performerRef.get(0).isEmpty()).isFalse();
  }



  @Test
  public void test_oru_multiple() throws IOException {
    String hl7message =
        "MSH|^~\\\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|ORU^R01|MSGID000005|T|2.6\r"
            + "PID||45483|45483||SMITH^SUZIE^||20160813|M|||123 MAIN STREET^^SCHENECTADY^NY^12345||(123)456-7890|||||^^^T||||||||||||\r"
            + "OBR|1||986^IA PHIMS Stage^2.16.840.1.114222.4.3.3.5.1.2^ISO|112^Final Echocardiogram Report|||20151009173644|||||||||||||002|||||F|||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^||||3068^JOHN^Paul^J|\r"
            + "OBX|1|TX|TS-F-01-007^Endocrine Disorders 7^L||obs report||||||F\r"
            + "OBX|2|TX|TS-F-01-008^Endocrine Disorders 8^L||ECHOCARDIOGRAPHIC REPORT||||||F\r"
            + "OBR|1||98^IA PHIMS Stage^2.16.840.1.114222.4.3.3.5.1.2^ISO|113^Echocardiogram Report|||20151009173644|||||||||||||002|||||F|||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^||||3065^Mahoney^Paul^J|\r"
            + "OBX|1|CWE|625-4^Bacteria identified in Stool by Culture^LN^^^^2.33^^result1|1|27268008^Salmonella^SCT^^^^20090731^^Salmonella species|||A^A^HL70078^^^^2.5|||P|||20120301|||^^^^^^^^Bacterial Culture||201203140957||||State Hygienic Laboratory^L^^^^IA Public HealthLab&2.16.840.1.114222.4.1.10411&ISO^FI^^^16D0648109|State Hygienic Laboratory^UI Research Park -Coralville^Iowa City^IA^52242-5002^USA^B^^19103|^Atchison^Christopher^^^^^^^L\r"
            + "OBX|2|TX|TS-F-01-002^Endocrine Disorders^L||ECHOCARDIOGRAPHIC REPORT Grroup 2||||||F\r";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message);

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
    assertThat(obsResource).hasSize(4);

    List<Resource> physicianresource =
        e.stream().filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(physicianresource).hasSize(2);


    List<Resource> diagnosticresource =
        e.stream().filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(diagnosticresource).hasSize(2);

    DiagnosticReport enc = getResource(diagnosticresource.get(0));
    Reference ref = enc.getSubject();
    assertThat(ref.isEmpty()).isFalse();
  }

  @Test
  public void test_oru_spm() throws IOException {
    String hl7message =
        "MSH|^~\\\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|ORU^R01|MSGID000005|T|2.6\r"
            + "PID||45483|45483||SMITH^SUZIE^||20160813|M|||123 MAIN STREET^^SCHENECTADY^NY^12345||(123)456-7890|||||^^^T||||||||||||\r"
            + "OBR|1||986^IA PHIMS Stage^2.16.840.1.114222.4.3.3.5.1.2^ISO|1051-2^New Born Screening^LN|||20151009173644|||||||||||||002|||||F|||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^||||3065^Mahoney^Paul^J|\r"
            + "OBX|1|TX|TS-F-01-002^Endocrine Disorders^L||obs report||||||F\r"
            + "SPM|1|SpecimenID||BLD|||||||P||||||201410060535|201410060821||Y||||||1\r";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, OPTIONS);
    assertThat(json).isNotBlank();
    System.out.println(json);
    IBaseResource bundleResource = context.getParser().parseResource(json);
    assertThat(bundleResource).isNotNull();
    Bundle b = (Bundle) bundleResource;
    List<BundleEntryComponent> e = b.getEntry();

    List<Resource> diagnosticresource =
        e.stream().filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
            .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(diagnosticresource).hasSize(1);

    DiagnosticReport diag = getResource(diagnosticresource.get(0));
    List<Reference> spmRef = diag.getSpecimen();
    assertThat(spmRef.isEmpty()).isFalse();
    assertThat(spmRef).hasSize(1);
    assertThat(spmRef.get(0).isEmpty()).isFalse();
  }

  /**
   * 
   * ORU messages with an OBR and multiple OBX segments that do not have an id represent a report
   * 
   * The OBX segments should be grouped together and added the presentedForm as an attachment for the diagnostic report
   * @throws IOException 
   */
  @Test
  public void multipleOBXWithNoId() throws IOException {
  	HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
      String json = ftv.convert(new File("src/test/resources/ORU-multiline-short.hl7"), OPTIONS_PRETTYPRINT);
      
      //Verify conversion
      FHIRContext context = new FHIRContext();
      IBaseResource bundleResource = context.getParser().parseResource(json);
      Bundle b = (Bundle) bundleResource;
      
      Assert.assertTrue("Bundle type not expected", b.getType() == BundleType.COLLECTION);
      b.getId();
      b.getMeta().getLastUpdated();

      List<BundleEntryComponent> e = b.getEntry();
     
      List<Resource> patientResource = e.stream()
              .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(patientResource).hasSize(1);
     
      List<Resource> organizationResource = e.stream()
              .filter(v -> ResourceType.Organization == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(organizationResource).hasSize(1);

      List<Resource> messageHeader = e.stream()
              .filter(v -> ResourceType.MessageHeader == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(messageHeader).hasSize(1);
      
      //Verify no observations are created
      List<Resource> obsResource = e.stream()
              .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(obsResource).hasSize(0);
      
      //Verify Diagnostic Report is created as expected
      List<Resource> reportResource = e.stream()
              .filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(reportResource).hasSize(1);
         
      DiagnosticReport report = (DiagnosticReport) reportResource.get(0);
      
      List<Attachment> attachments = report.getPresentedForm();
      Assert.assertTrue("Unexpected number of attachments", attachments.size() == 1);
      
      //Verify attachment to diagnostic report
      Attachment a = attachments.get(0);
      Assert.assertTrue("Incorrect content type", a.getContentType().equalsIgnoreCase("text"));
      Assert.assertTrue("Incorrect language", a.getLanguage().equalsIgnoreCase("en"));
      
      //Verify data attachment after decoding
      String decoded = new String(Base64.getDecoder().decode(a.getDataElement().getValueAsString()));
      Assert.assertTrue("Incorrect data", decoded.equals("~[PII] Emergency Department~ED Encounter Arrival Date: [ADDRESS] [PERSONALNAME]:~"));

      Assert.assertTrue("Incorrect title", a.getTitle().equalsIgnoreCase("ECHO CARDIOGRAM COMPLETE"));

      //Verify creation data is persisted correctly - 2020-08-02T12:44:55+08:00
      Calendar c = Calendar.getInstance();
      c.clear();  // needed to completely clear out calendar object
      c.set(2020, 7, 2, 12, 44, 55);
      c.setTimeZone(TimeZone.getTimeZone(ZoneId.of("+08:00")));
      
      Date d = c.getTime();
      Assert.assertTrue("Incorrect creation date", a.getCreation().equals(d));
      
  }
   
  /**
   * 
   * Verifies ORU messages with mixed OBX types 
   * 
   * @throws IOException 
   */
  @Test
  public void multipleOBXWithMixedType() throws IOException {
  	HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
      String json = ftv.convert(new File("src/test/resources/ORU-multiline-short-mixed.hl7"), OPTIONS_PRETTYPRINT);
      
      //Verify conversion
      FHIRContext context = new FHIRContext();
      IBaseResource bundleResource = context.getParser().parseResource(json);
      Bundle b = (Bundle) bundleResource;
      
      Assert.assertTrue("Bundle type not expected", b.getType() == BundleType.COLLECTION);
      b.getId();
      b.getMeta().getLastUpdated();

      List<BundleEntryComponent> e = b.getEntry();
     
      List<Resource> patientResource = e.stream()
              .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(patientResource).hasSize(1);
     
      List<Resource> organizationResource = e.stream()
              .filter(v -> ResourceType.Organization == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(organizationResource).hasSize(1);

      List<Resource> messageHeader = e.stream()
              .filter(v -> ResourceType.MessageHeader == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(messageHeader).hasSize(1);
      
      //Verify one observations is created
      List<Resource> obsResource = e.stream()
              .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(obsResource).hasSize(1);
      
      //Verify Diagnostic Report is created as expected
      List<Resource> reportResource = e.stream()
              .filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
              .map(BundleEntryComponent::getResource).collect(Collectors.toList());
      assertThat(reportResource).hasSize(1);
         
      DiagnosticReport report = (DiagnosticReport) reportResource.get(0);
      
      //No attachment created since OBX with TX and no id is not first
      List<Attachment> attachments = report.getPresentedForm();
      Assert.assertTrue("Unexpected number of attachments", attachments.size() == 0);
  }
  
  private static DiagnosticReport getResource(Resource resource) {
    String s = context.getParser().encodeResourceToString(resource);
    Class<? extends IBaseResource> klass = DiagnosticReport.class;
    return (DiagnosticReport) context.getParser().parseResource(klass, s);
  }
}
