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
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.Test;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

public class Hl7ORUMessageTest {
  private static FHIRContext context = new FHIRContext();

  @Test
  public void test_oru() throws IOException {
    String hl7message =
        "MSH|^~\\\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|ORU^R01|MSGID000005|T|2.6\r"
            + "PID||45483|45483||SMITH^SUZIE^||20160813|M|||123 MAIN STREET^^SCHENECTADY^NY^12345||(123)456-7890|||||^^^T||||||||||||\r"
            + "OBR|1||986^IA PHIMS Stage^2.16.840.1.114222.4.3.3.5.1.2^ISO|112^Final Echocardiogram Report|||20151009173644|||||||||||||002|||||F|||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^||||3065^Mahoney^Paul^J|\r"
            + "OBX|1|TX|||obs report||||||F\r"
            + "OBX|2|TX|||ECHOCARDIOGRAPHIC REPORT||||||F\r";

    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(hl7message, true, BundleType.COLLECTION);
    assertThat(json).isNotBlank();

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
    assertThat(diag.getEffectiveDateTimeType().asStringValue()).isEqualTo("2015-10-09T17:36:44");
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
            + "OBX|1|TX|||obs report||||||F\r"
            + "OBX|2|TX|||ECHOCARDIOGRAPHIC REPORT||||||F\r"
            + "OBR|1||98^IA PHIMS Stage^2.16.840.1.114222.4.3.3.5.1.2^ISO|113^Echocardiogram Report|||20151009173644|||||||||||||002|||||F|||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^||||3065^Mahoney^Paul^J|\r"
            + "OBX|1|CWE|625-4^Bacteria identified in Stool by Culture^LN^^^^2.33^^result1|1|27268008^Salmonella^SCT^^^^20090731^^Salmonella species|||A^A^HL70078^^^^2.5|||P|||20120301|||^^^^^^^^Bacterial Culture||201203140957||||State Hygienic Laboratory^L^^^^IA Public HealthLab&2.16.840.1.114222.4.1.10411&ISO^FI^^^16D0648109|State Hygienic Laboratory^UI Research Park -Coralville^Iowa City^IA^52242-5002^USA^B^^19103|^Atchison^Christopher^^^^^^^L\r"
            + "OBX|2|TX|||ECHOCARDIOGRAPHIC REPORT Grroup 2||||||F\r";

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

  private static DiagnosticReport getResource(Resource resource) {
    String s = context.getParser().encodeResourceToString(resource);
    Class<? extends IBaseResource> klass = (Class<? extends IBaseResource>) DiagnosticReport.class;
    return (DiagnosticReport) context.getParser().parseResource(klass, s);
  }




}
