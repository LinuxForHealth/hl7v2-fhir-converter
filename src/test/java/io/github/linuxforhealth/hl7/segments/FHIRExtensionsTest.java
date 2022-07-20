/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.codesystems.V3ReligiousAffiliation;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;

import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.core.terminology.UrlLookup;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.segments.util.DatatypeUtils;
import io.github.linuxforhealth.hl7.segments.util.PatientUtils;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

class FHIRExtensionsTest {
    private static final String V3_RACE_SYSTEM = "http://terminology.hl7.org/CodeSystem/v3-Race";
    private HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();

    @Test
    void testExtensionMothersMaidenNameReligion() {

        String patientWithDataForExtensions = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Test for mother's maiden name and religion.  Using unique original CWE.2 text in religion to test that it is preserved
                + "PID|1||12345678^^^^MR||TestPatientLastName^Jane|TestMaidenName^Sue|||||||||||LUT^Luther Synod^|\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(ftv, patientWithDataForExtensions);
        assertThat(patient.hasExtension()).isTrue();
        Extension ext = patient.getExtensionByUrl(UrlLookup.getExtensionUrl("mothersMaidenName"));
        assertThat(ext).isNotNull();
        assertThat(ext.getValue()).hasToString("TestMaidenName");
        ext = patient.getExtensionByUrl(UrlLookup.getExtensionUrl("religion"));
        assertThat(ext).isNotNull();
        CodeableConcept cc = (CodeableConcept) ext.getValue();
        assertThat(cc.getText()).hasToString("Luther Synod");
    }

    @Test
    void testTextOnlyReligion() {

        String patientWithDataForExtensions = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Test text only religion
                + "PID|1||12345678^^^^MR||Jane^TestPatientLastName||||||||||||Methodist|\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(ftv, patientWithDataForExtensions);
        assertThat(patient.hasExtension()).isTrue();

        Extension ext = patient.getExtensionByUrl(UrlLookup.getExtensionUrl("religion"));
        assertThat(ext).isNotNull();
        CodeableConcept cc = (CodeableConcept) ext.getValue();
        assertThat(cc.hasCoding()).isTrue();
        assertThat(cc.hasText()).isFalse();
        Coding coding = cc.getCodingFirstRep();
        assertThat(coding).isNotNull();

        assertThat(coding.hasCode()).isFalse();
        String theSystem = V3ReligiousAffiliation._1029.getSystem();
        assertThat(coding.hasSystem()).isTrue();
        assertThat(coding.getSystem()).isEqualTo(theSystem);
        assertThat(coding.hasDisplay()).isTrue();
        assertThat(coding.getDisplay()).containsPattern("Invalid.*Methodist.*" + theSystem);
    }

    @Test
    void testExtensionTwoRaces() {

        String patientWithExtensionTwoRaces = "MSH|^~\\&|MIICEHRApplication|MIIC|MIIC|MIIC|201705130822||VXU^V04^VXU_V04|test1100|P|2.5.1|||AL|AL|||||Z22^CDCPHINVS|^^^^^MIIC^SR^^^MIIC|MIIC\n"
                // Test for two race variants
                + "PID|1||12345678^^^^MR||Jane^TestPatientLastName|||||2028-9^Asian^HL70005~2106-3^White^HL70005||||||||\n";

        Patient patient = PatientUtils.createPatientFromHl7Segment(ftv, patientWithExtensionTwoRaces);
        assertThat(patient.hasExtension()).isTrue();

        List<Extension> extensions = patient.getExtensionsByUrl(UrlLookup.getExtensionUrl("race"));
        assertThat(extensions).isNotNull();
        assertThat(extensions.size()).isEqualTo(2);
        assertThat(extensions.get(0).hasValue()).isTrue();
        assertThat(extensions.get(1).hasValue()).isTrue();
        // check for Asian & White.  Guess at order, switch if incorrect
        CodeableConcept ccAsian = (CodeableConcept) extensions.get(0).getValue();
        CodeableConcept ccWhite = (CodeableConcept) extensions.get(1).getValue();
        assertThat(ccAsian.hasText()).isTrue();
        // Switch if guess was not right. (If not reversed, then something is wrong)
        if (!ccAsian.getText().equalsIgnoreCase("Asian")) {
            ccAsian = (CodeableConcept) extensions.get(1).getValue();
            ccWhite = (CodeableConcept) extensions.get(0).getValue();
        }
        DatatypeUtils.checkCommonCodeableConceptAssertions(ccAsian, "2028-9", "Asian", V3_RACE_SYSTEM, "Asian");
        DatatypeUtils.checkCommonCodeableConceptAssertions(ccWhite, "2106-3", "White", V3_RACE_SYSTEM, "White");
    }

    // See CodeableConceptText.java for more tests on CodeableConcepts

    @Test
    // Verifies the meta extension process-timestamp is present and verifies if the property
    // TENANT is passed into converter option it will show up as a meta extension with the right value.
    void testProcessTimestampAndTenant() throws IOException {
        String hl7VUXmessageRep = "MSH|^~\\&|||||20140701041038||VXU^V04^VXU_V04|MSG.Valid_01|P|2.6|||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "ORC|||197027|||||||^Clerk^Myron|||||||RI2050\r"
                + "RXA|0|1|20130531||48^HIB PRP-T^CVX||||||^^^PlaceRXA11|||||||||||||||"
                + "OBX|1|CE|59784-9^Disease with presumed immunity^LN||\r";

        // TENANT is passed through the converter options
        ConverterOptions customOptionsWithTenant = new Builder().withValidateResource().withPrettyPrint()
                .withProperty("TENANT", "TenantId").build();
        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(ftv,
                hl7VUXmessageRep,
                customOptionsWithTenant);

        // Get the Meta from the first resource in the bundle 
        Meta m = e.get(0).getResource().getMeta();

        // Verify process-timestamp meta extension
        Extension process_timestamp = m.getExtensionByUrl("http://ibm.com/fhir/cdm/StructureDefinition/process-timestamp");
        assertThat(process_timestamp).isNotNull();
        String process_timestamp_class = process_timestamp.getValue().getClass().toString();
        assertThat(process_timestamp_class).contains("DateTimeType");

        Extension tenant_id = m.getExtensionByUrl("http://ibm.com/fhir/cdm/StructureDefinition/tenant-id");
        assertThat(tenant_id).isNotNull();
        String tenant = tenant_id.getValue().toString();
        assertThat(tenant).isEqualTo("TenantId");
        
    }

    @Test
    // Verified the tenant-id meta extension isn't present if we don't pass in TENANT to the converter options.
    void testNoTenant() throws IOException {
        String hl7VUXmessageRep = "MSH|^~\\&|||||20140701041038||VXU^V04^VXU_V04|MSG.Valid_01|P|2.6|||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "ORC|||197027|||||||^Clerk^Myron|||||||RI2050\r"
                + "RXA|0|1|20130531||48^HIB PRP-T^CVX||||||^^^PlaceRXA11|||||||||||||||"
                + "OBX|1|CE|59784-9^Disease with presumed immunity^LN||\r";

        // TENANT is passed through the options
        ConverterOptions customOptionsWithTenant = new Builder().withValidateResource().withPrettyPrint()
                .build();
        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(ftv,
                hl7VUXmessageRep,
                customOptionsWithTenant);

        // Get the Meta from the first resource in the bundle 
        Meta m = e.get(0).getResource().getMeta();

        Extension tenant_id = m.getExtensionByUrl("http://ibm.com/fhir/cdm/StructureDefinition/tenant-id");
        assertThat(tenant_id).isNull();
        
    }

}
