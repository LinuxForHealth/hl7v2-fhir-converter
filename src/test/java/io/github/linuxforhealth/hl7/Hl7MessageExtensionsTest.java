/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.linuxforhealth.hl7;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import java.util.Map;
import java.io.FileOutputStream;
import java.util.Properties;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.apache.commons.io.IOUtils;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.model.v26.message.ADT_A01;
import ca.uhn.hl7v2.model.v26.message.ADT_A09;
import ca.uhn.hl7v2.model.v26.message.PPR_PC1;
import ca.uhn.hl7v2.model.v26.message.MDM_T02;
import ca.uhn.hl7v2.model.v26.message.ORU_R01;

import ca.uhn.hl7v2.model.Message;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Hl7InputStreamMessageStringIterator;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.config.ConverterConfiguration;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.parsing.HL7DataExtractor;
import io.github.linuxforhealth.hl7.parsing.HL7HapiParser;
import io.github.linuxforhealth.hl7.resource.ResourceReader;
import io.github.linuxforhealth.hl7.segments.util.DatatypeUtils;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

public class Hl7MessageExtensionsTest {

    private static final String HL7_FILE_UNIX_NEWLINE = "src/test/resources/sample_unix.hl7";
    private static final String HL7_FILE_WIN_NEWLINE = "src/test/resources/sample_win.hl7";
    private static final String HL7_FILE_WIN_NEWLINE_BATCH = "src/test/resources/sample_win_batch.hl7";

    // # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
    // NOTE VALIDATION IS INTENTIONALLY TURNED OFF BECAUSE WE ARE CREATING RESOURCES THAT ARE NOT STANDARD
    // private static final ConverterOptions OPTIONS = new Builder().withValidateResource().withPrettyPrint().build(
    private static final ConverterOptions OPTIONS = new Builder().withPrettyPrint().build();
    // # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
    // private static final Logger LOGGER = LoggerFactory.getLogger(FHIRConverterTest.class);


    private static final String CONF_PROP_HOME = "hl7converter.config.home";

    @TempDir
    static File folder;
  
    static String originalConfigHome;
  
    @BeforeAll
    public static void saveConfigHomeProperty() {
      originalConfigHome = System.getProperty(CONF_PROP_HOME);
    }
  
    @AfterEach
    public void reset() {
      System.clearProperty(CONF_PROP_HOME);
      ConverterConfiguration.reset();
      ResourceReader.reset();
    }
  
    @AfterAll
    public static void reloadPreviousConfigurations() {
      if (originalConfigHome != null)
        System.setProperty(CONF_PROP_HOME, originalConfigHome);
      else
        System.clearProperty(CONF_PROP_HOME);
    }

    @Test
    public void testHistRpt() throws IOException {

        // Set up the config file
        File configFile = new File(folder, "config.properties");
        Properties prop = new Properties();
        prop.put("base.path.resource", "src/main/resources");
        prop.put("supported.hl7.messages", "*");
        // prop.put("supported.hl7.messages", "ADT_A01, ADT_A08, ADT_A34, ADT_A40, MDM_T02, MDM_T06, ORM_O01, OMP_O09, ORU_R01, PPR_PC1, RDE_O11, RDE_O25");
        prop.put("default.zoneid", "+08:00");
        prop.put("additional.resources.location", "src/test/resources/additional_resources");
        prop.store(new FileOutputStream(configFile), null);
        System.setProperty(CONF_PROP_HOME, configFile.getParent());

        ClassLoader loader = Test.class.getClassLoader();
        System.out.println(loader.getResource("custom_packages/2.6"));
        System.out.println(loader.getResource("io/github/linuxforhealth/hl7/expression/Hl7ExpressionTest.class"));
        System.out.println(loader.getResource("HIST_RPT.class"));
        System.out.println(loader.getResource("message/HIST_RPT.class"));
        System.out.println(loader.getResource("com/ibm/whpa/hl7/custom/message/HIST_RPT.class"));

        String hl7message = 
                "MSH|^~\\&|WHI BULK|WHI|WHI||20210709162149||HIST^RPT|20210709162149|P|2.6|||ER|AL\n"
                +"PID|1|100008^^^FAC|||^Clinton||197005150000|M|||KEENE ST^^COLUMBIA^MO^65201^ US\n"
                +"PV1|||||||||||||||||||3.2191\n"
                +"OBR|||1206141230440|X71010^Xray chest 1 vw|||201206141232||||||||||||||||||F|||||||123456789^TEST^INTERP^^^|||123456789^TEST^TRANSCRIPT^^^\n"
                +"OBX|1|TX|||MEASUREMENTS:~ 2D ECHO~ LV Diastolic Diameter Base LX     3.9 cm                3.6-5.4~ Surjya Das MD~ (Electronically Signed)||||||F\n"
                ;


        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS);
        System.out.println(json);
        FHIRContext context = new FHIRContext();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Check for the expected resources
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        List<Resource> diagnosticReportResource = ResourceUtils.getResourceList(e, ResourceType.DiagnosticReport);
        assertThat(diagnosticReportResource).hasSize(1);

        List<Resource> serviceRequestResource = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequestResource).hasSize(1);

        List<Resource> practitionerResource = ResourceUtils.getResourceList(e, ResourceType.Practitioner);
        assertThat(practitionerResource).hasSize(1);

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(5);

    }


    @Test
    public void testHistPat() throws IOException {

        // Set up the config file
        File configFile = new File(folder, "config.properties");
        Properties prop = new Properties();
        prop.put("base.path.resource", "src/main/resources");
        prop.put("supported.hl7.messages", "*");
        // prop.put("supported.hl7.messages", "ADT_A01, ADT_A08, ADT_A34, ADT_A40, MDM_T02, MDM_T06, ORM_O01, OMP_O09, ORU_R01, PPR_PC1, RDE_O11, RDE_O25");
        prop.put("default.zoneid", "+08:00");
        prop.put("additional.resources.location", "src/test/resources/additional_resources");
        prop.store(new FileOutputStream(configFile), null);
        System.setProperty(CONF_PROP_HOME, configFile.getParent());

        ClassLoader loader = Test.class.getClassLoader();
        System.out.println(loader.getResource("custom_packages/2.6"));
        System.out.println(loader.getResource("io/github/linuxforhealth/hl7/expression/Hl7ExpressionTest.class"));
        System.out.println(loader.getResource("HIST_PAT.class"));
        System.out.println(loader.getResource("message/HIST_PAT.class"));
        System.out.println(loader.getResource("com/ibm/whpa/hl7/custom/message/HIST_PAT.class"));

        String hl7message = 
            "MSH|^~\\&|WHI BULK|WHI|WHI||20211005105125||HIST^PAT|1a3952f1-38fe-4d55-95c6-ce58ebfc7f10|P|2.6\n"
            + "PID|1|100009^^^FAC^MR|100009^^^FAC^MR||Doo^Scooby||195001010000|M|||311 N Keene Street^^COLUMBIA^MO^65201^ US||5734421788|||U\n"
            + "PRB|1|20211005|10281^LYMPHOID LEUKEMIA NEC^ICD9||||201208061011||201208061011|||||||201208061011\n"
            + "PRB|2|20211005|11334^ABNORMALITIES OF HAIR^ICD9||||201208071000||201208071000|||||||201208071000\n"
            + "AL1|50|DA|penicillin|MO||20210629\n"
            + "AL1|50|MA|cat dander|SV|hives\\R\\ difficult breathing|20210629\n"
            ;
        // Get 1 patient, 2 conditions, 2 allergies
        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS);
        System.out.println(json);
        FHIRContext context = new FHIRContext();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Check for the expected resources
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(2);

        List<Resource> allergyIntoleranceResource = ResourceUtils.getResourceList(e, ResourceType.AllergyIntolerance);
        assertThat(allergyIntoleranceResource).hasSize(2);

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(5);
    }


    @Test
    public void testHistEnc() throws IOException {

        // Set up the config file
        File configFile = new File(folder, "config.properties");
        Properties prop = new Properties();
        prop.put("base.path.resource", "src/main/resources");
        prop.put("supported.hl7.messages", "*");
        // prop.put("supported.hl7.messages", "ADT_A01, ADT_A08, ADT_A34, ADT_A40, MDM_T02, MDM_T06, ORM_O01, OMP_O09, ORU_R01, PPR_PC1, RDE_O11, RDE_O25");
        prop.put("default.zoneid", "+08:00");
        prop.put("additional.resources.location", "src/test/resources/additional_resources");
        prop.store(new FileOutputStream(configFile), null);
        System.setProperty(CONF_PROP_HOME, configFile.getParent());

        ClassLoader loader = Test.class.getClassLoader();
        System.out.println(loader.getResource("custom_packages/2.6"));
        System.out.println(loader.getResource("io/github/linuxforhealth/hl7/expression/Hl7ExpressionTest.class"));
        System.out.println(loader.getResource("HIST_PAT.class"));
        System.out.println(loader.getResource("message/HIST_PAT.class"));
        System.out.println(loader.getResource("com/ibm/whpa/hl7/custom/message/HIST_PAT.class"));

        String hl7message = 
            "MSH|^~\\&|WHI BULK|WHI|WHI||20210709142435||HIST^ENC|20210709142435|P|2.6\n"
            + "PID|1|100000^^^FAC|100000^^^FAC||Vickers^Tony||197910280000|M|||229 S Tyler St^^BEVERLY HILLS^FL^34465^ US\n"
            + "OBR|1|||X73600^XRay Ankle 2 views^INTERNAL||201206080800||||||||||123456789^TEST^ORDERING^^^||||||||||||||||||89^TEST^TECH^^^\n"
            + "OBR|2|||71550^MRI chest with contrast^INTERNAL||201206081027||||||||||123456789^TEST^ORDERING^^^||||||||||||||||||89^TEST^TECH^^^\n"
            + "RXE|1|20^Ibuprofen||||||||100|MG|||||2||||||||||||||||201704010000\n"
            + "RXE|2|10^acetaminophen||||||||200|MG|||||1||||||||||||||||201801010000\n"
            + "DG1|1||704.2^ABNORMALITIES OF HAIR|LYMPHOID LEUKEMIA NEC|202004101359\n"
            + "DG1|2||R03.0^Elevated blood-pressure reading, without diagnosis of hypertension|Elevated blood-pressure reading, without diagnosis of hypertension|202004101405\n"
            ;

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS);
        System.out.println(json);
        FHIRContext context = new FHIRContext();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Check for the expected resources
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> diagnosticReportResource = ResourceUtils.getResourceList(e, ResourceType.DiagnosticReport);
        assertThat(diagnosticReportResource).hasSize(2);

        List<Resource> serviceRequestResource = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequestResource).hasSize(2);

        List<Resource> practitionerResource = ResourceUtils.getResourceList(e, ResourceType.Practitioner);
        assertThat(practitionerResource).hasSize(2);

        List<Resource> medicationRequestResource = ResourceUtils.getResourceList(e, ResourceType.MedicationRequest);
        assertThat(medicationRequestResource).hasSize(2);

        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(2);

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(11);
    }


    @Test
    public void testHistLab() throws IOException {

        // Set up the config file
        File configFile = new File(folder, "config.properties");
        Properties prop = new Properties();
        prop.put("base.path.resource", "src/main/resources");
        prop.put("supported.hl7.messages", "*");
        // prop.put("supported.hl7.messages", "ADT_A01, ADT_A08, ADT_A34, ADT_A40, MDM_T02, MDM_T06, ORM_O01, OMP_O09, ORU_R01, PPR_PC1, RDE_O11, RDE_O25");
        prop.put("default.zoneid", "+08:00");
        prop.put("additional.resources.location", "src/test/resources/additional_resources");
        prop.store(new FileOutputStream(configFile), null);
        System.setProperty(CONF_PROP_HOME, configFile.getParent());

        ClassLoader loader = Test.class.getClassLoader();
        System.out.println(loader.getResource("custom_packages/2.6"));
        System.out.println(loader.getResource("io/github/linuxforhealth/hl7/expression/Hl7ExpressionTest.class"));
        System.out.println(loader.getResource("HIST_LAB.class"));
        System.out.println(loader.getResource("message/HIST_LAB.class"));
        System.out.println(loader.getResource("com/ibm/whpa/hl7/custom/message/HIST_LAB.class"));

        String hl7message = 
            "MSH|^~\\&|WHI BULK|WHI|WHI||20211005172734||HIST^LAB|4bb9d61c-337d-441c-bfd6-015b9721cdc8|P|2.6\n"
            + "PID|1|100014^^^FAC^MR|||Sullivan^April||198302090000|F|||123^^COLUMBIA^MO^65201^ US\n"
            + "PV1|||||||||||||||||||9.3416\n"
            + "OBR||||LAB^LAB RESULT\n"
            + "OBX|1|NM|Glu^Glucose Level^LABCORP|1|90.53|mg/dL||||||||201208221628\n"
            + "OBX|2|NM|Glu^Glucose Level^LABCORP|1|90.47|mg/dL||||||||201208221629\n"
            + "SPM||||OTH|\n"
            ;

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS);
        System.out.println(json);
        FHIRContext context = new FHIRContext();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Check for the expected resources
        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1);

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1);

        List<Resource> observationResource = ResourceUtils.getResourceList(e, ResourceType.Observation);
        assertThat(observationResource).hasSize(2);
        // For each observation resource, ensure it has category of Laboratory
        for (int observationIndex = 0; observationIndex < observationResource.size(); observationIndex++) {
          Observation obs = (Observation) observationResource.get(observationIndex);
          // Because there is an SPM record, there should be a category.
          assertThat(obs.hasCategory()).isTrue();
          assertThat(obs.getCategory()).hasSize(1);
          DatatypeUtils.checkCommonCodeableConceptAssertions(obs.getCategoryFirstRep(), "laboratory", "Laboratory",
                  "http://terminology.hl7.org/CodeSystem/observation-category", null);
        }

        List<Resource> diagnosticReportResource = ResourceUtils.getResourceList(e, ResourceType.DiagnosticReport);
        assertThat(diagnosticReportResource).hasSize(1);

        List<Resource> serviceRequestResource = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceRequestResource).hasSize(1);

        // Confirm that no extra resources are created
        assertThat(e.size()).isEqualTo(6);
    }
}
