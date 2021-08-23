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
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Property;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

public class HL7ConditionFHIRConversionTest {

    private static final ConverterOptions OPTIONS = new Builder().withValidateResource().withPrettyPrint().build();

    // Tests the DG1 segment (encounter) with all support message types.
    @ParameterizedTest
    @ValueSource(strings = { "MSH|^~\\&|||||||ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6||||||\r",
        //     "MSH|^~\\&|||||||ADT^A03|64322|P|2.4|123|456|ER|AL|USA|ASCII|en|2.4||||||\r",
        //     "MSH|^~\\&|||||||ADT^A04|64322|P|2.4|123|456|ER|AL|USA|ASCII|en|2.4||||||\r",
        //     "MSH|^~\\&|||||||ADT^A08|64322|P|2.4|123|456|ER|AL|USA|ASCII|en|2.4||||||\r",
        //     "MSH|^~\\&|||||||ADT^A28^ADT^A28|64322|P|2.4|123|456|ER|AL|USA|ASCII|en|2.4||||||\r",
        //     "MSH|^~\\&|||||||ADT^A31|64322|P|2.4|123|456|ER|AL|USA|ASCII|en|2.4||||||\r",
        //     "MSH|^~\\&|||||||ORM^O01|64322|P|2.4|123|456|ER|AL|USA|ASCII|en|2.4||||||\r" 
        })
    public void validate_encounter(String msh) {

        String hl7message = msh
                + "PID|||555444222111^^^MPI&GenHosp&L^MR||||19600614|M||C|99 Oakland #106^^qwerty^OH^44889||^^^^^626^5641111|^^^^^626^5647654|||||343132266|||N\r"
                + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
                + "DG1|1|ICD10|B45678|Broken Arm|20210322154449|A|E123|R45|Y|J76|C|15|1458.98||1|123^DOE^JOHN^A^|C|Y|20210322154326|V45|S1234|Parent Diagnosis|Value345|Group567|DiagnosisG45|Y\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS);
        System.out.println(json);
        assertThat(json).isNotBlank();

        FHIRContext context = new FHIRContext(true, false);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(1);

        // Get condition Resource
        Resource condition = conditionResource.get(0);

        // Get condition identifiers
        Property identifierProperty = condition.getNamedProperty("identifier");
        List<Base> identifierList = identifierProperty.getValues();

        // Verify we have 3 identifiers
        assertThat(identifierList).hasSize(3);

        // The 3rd identifier value should be from DG1.20
        String thirdIdentiferValue = ResourceUtils.getValueAsString(identifierList.get(2), "value");
        assertThat(thirdIdentiferValue).isEqualTo("V45");

        // Verify encounter reference exists
        Base encounter = ResourceUtils.getValue(condition, "encounter");
        assertThat(ResourceUtils.getValueAsString(encounter, "reference").substring(0, 10)).isEqualTo("Encounter/");

        // Verify asserter reference to Practitioner exists
        Base asserter = ResourceUtils.getValue(condition, "asserter");
        assertThat(ResourceUtils.getValueAsString(asserter, "reference").substring(0, 13)).isEqualTo("Practitioner/");

        // Verify recorded date is set correctly.
        assertThat(ResourceUtils.getValueAsString(condition, "recordedDate"))
                .isEqualTo("DateTimeType[2021-03-22T15:43:26+08:00]");

        // Verify onset data time is set correctly.
        assertThat(ResourceUtils.getValueAsString(condition, "onsetDateTime"))
                .isEqualTo("DateTimeType[2021-03-22T15:44:49+08:00]");

        // Verify code is set correctly.
        Base code = ResourceUtils.getValue(condition, "code");
        Base coding = ResourceUtils.getValue(code, "coding");
        assertThat(ResourceUtils.getValueAsString(coding, "code")).isEqualTo("B45678");

        // Verify subject reference to Patient exists
        Base subject = ResourceUtils.getValue(condition, "subject");
        assertThat(ResourceUtils.getValueAsString(subject, "reference").substring(0, 8)).isEqualTo("Patient/");

        // Verify category text is set correctly.
        Base category = ResourceUtils.getValue(condition, "category");
        assertThat(ResourceUtils.getValueAsString(category, "text")).isEqualTo("encounter-diagnosis");

        // Verify category coding fields are set correctly.
        Base catCoding = ResourceUtils.getValue(category, "coding");
        assertThat(ResourceUtils.getValueAsString(catCoding, "system"))
                .isEqualTo("UriType[http://terminology.hl7.org/CodeSystem/condition-category]");
        assertThat(ResourceUtils.getValueAsString(catCoding, "code")).isEqualTo("encounter-diagnosis");
        assertThat(ResourceUtils.getValueAsString(catCoding, "display")).isEqualTo("Encounter Diagnosis");

        // Find the practitioner resource from the FHIR bundle.
        List<Resource> practitionerResource = e.stream()
                .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(practitionerResource).hasSize(1);

        // Get practitioner Resource
        Resource practitioner = practitionerResource.get(0);

        Base name = ResourceUtils.getValue(practitioner, "name");
        assertThat(ResourceUtils.getValueAsString(name, "text")).isEqualTo("JOHN A DOE");
        assertThat(ResourceUtils.getValueAsString(name, "family")).isEqualTo("DOE");
        assertThat(ResourceUtils.getValueAsString(name, "given")).isEqualTo("JOHN");

    }

    // Tests the DG1 segment (encounter) with a full Entity Identifier(EI) for the
    // asserter
    @Test
    public void validate_encounter_with_EI_identifiers() {

        String hl7message = "MSH|^~\\&|||||||ADT^A01^ADT_A01|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6||||||\r"
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^||||||||||||||||||Born in USA|Y||USA||||\r"
                + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
                + "DG1|1|ICD10|B45678|Broken Arm|20210322154449|A|E123|R45|Y|J76|C|15|1458.98||1|123^DOE^JOHN^A^|C|Y|20210322154326|one^https://terminology.hl7.org/CodeSystem/two^three^https://terminology.hl7.org/CodeSystem/four|S1234|Parent Diagnosis|Value345|Group567|DiagnosisG45|Y\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS);
        System.out.println(json);
        assertThat(json).isNotBlank();

        FHIRContext context = new FHIRContext(true, false);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(1);

        // Get condition Resource
        Resource condition = conditionResource.get(0);

        // Get condition identifiers
        Property identifierProperty = condition.getNamedProperty("identifier");
        List<Base> identifierList = identifierProperty.getValues();

        // Verify we have 4 identifiers
        assertThat(identifierList).hasSize(4);

        // 2nd identifier
        Base identifierTwo = identifierList.get(2);
        // value = DG1.20.1
        assertThat(ResourceUtils.getValueAsString(identifierTwo, "value")).isEqualTo("one");
        // system = DG1.20.2
        assertThat(ResourceUtils.getValueAsString(identifierTwo, "system"))
                .isEqualTo("UriType[https://terminology.hl7.org/CodeSystem/two]");

        // 3rd identifier.
        Base identifierThree = identifierList.get(3);
        // value = DG1.20.3
        assertThat(ResourceUtils.getValueAsString(identifierThree, "value")).isEqualTo("three");
        // system = DG1.20.4
        assertThat(ResourceUtils.getValueAsString(identifierThree, "system"))
                .isEqualTo("UriType[https://terminology.hl7.org/CodeSystem/four]");

    }

    // Tests the PRB segment (problem) with all supported message types.
    @ParameterizedTest
    @ValueSource(strings = { 
        "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r",
        // "MSH|^~\\&|||||20040629164652|1|PPR^PC2|331|P|2.3.1||\r",
        // "MSH|^~\\&|||||20040629164652|1|PPR^PC3|331|P|2.3.1||\r",
    })
    public void validate_problem() {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^||||||||||||||||||Born in USA|Y||USA||||\r"
                + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
                + "PRB|AD|20170110074000|K80.00^Cholelithiasis^I10|53956|E1|1|20100907175347|20150907175347|20180310074000||||confirmed^Confirmed^http://terminology.hl7.org/CodeSystem/condition-ver-status|resolved|20180310074000|20170102074000|textual representation of the time when the problem began|1^primary|ME^Medium|0.4|marginal|good|marginal|marginal|highly sensitive|some prb detail|\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS);
        System.out.println(json);
        assertThat(json).isNotBlank();

        FHIRContext context = new FHIRContext(true, false);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(1);

        // Get the condition Resource
        Resource condition = conditionResource.get(0);

        // Verify recorded date is set correctly.
        assertThat(ResourceUtils.getValueAsString(condition, "recordedDate"))
                .isEqualTo("DateTimeType[2017-01-10T07:40:00+08:00]");

        // Verify abatement date is set correctly.
        assertThat(ResourceUtils.getValueAsString(condition, "abatementDateTime"))
                .isEqualTo("DateTimeType[2018-03-10T07:40:00+08:00]");

        // Verify verification status text is set correctly.
        Base verificationStatus = ResourceUtils.getValue(condition, "verificationStatus");
        assertThat(ResourceUtils.getValueAsString(verificationStatus, "text")).isEqualTo("Confirmed");

        // Verify verification status coding code is set correctly
        Base coding = ResourceUtils.getValue(verificationStatus, "coding");
        assertThat(ResourceUtils.getValueAsString(coding, "code")).isEqualTo("confirmed");

        // Verify onset string is set correctly.
        assertThat(ResourceUtils.getValueAsString(condition, "onsetString"))
                .isEqualTo("textual representation of the time when the problem began");

        // Verify encounter reference exists
        Base encounter = ResourceUtils.getValue(condition, "encounter");
        assertThat(ResourceUtils.getValueAsString(encounter, "reference").substring(0, 10)).isEqualTo("Encounter/");

        // Verify subject reference to Patient exists
        Base subject = ResourceUtils.getValue(condition, "subject");
        assertThat(ResourceUtils.getValueAsString(subject, "reference").substring(0, 8)).isEqualTo("Patient/");

        // Verify category text is set correctly.
        Base category = ResourceUtils.getValue(condition, "category");
        assertThat(ResourceUtils.getValueAsString(category, "text")).isEqualTo("problem-list-item");

        // Verify category coding fields are set correctly.
        Base catCoding = ResourceUtils.getValue(category, "coding");
        assertThat(ResourceUtils.getValueAsString(catCoding, "system"))
                .isEqualTo("UriType[http://terminology.hl7.org/CodeSystem/condition-category]");
        assertThat(ResourceUtils.getValueAsString(catCoding, "display")).isEqualTo("Problem List Item");
        assertThat(ResourceUtils.getValueAsString(catCoding, "code")).isEqualTo("problem-list-item");

        // Verify extension is set correctly.
        Base extension = ResourceUtils.getValue(condition, "extension");
        assertThat(ResourceUtils.getValueAsString(extension, "url"))
                .isEqualTo("UriType[http://hl7.org/fhir/StructureDefinition/condition-assertedDate]");
        assertThat(ResourceUtils.getValueAsString(extension, "valueDateTime"))
                .isEqualTo("DateTimeType[2010-09-07T17:53:47+08:00]");

        // Verify severity code is set correctly.
        Base severity = ResourceUtils.getValue(condition, "severity");
        Base sevCoding = ResourceUtils.getValue(severity, "coding");
        assertThat(ResourceUtils.getValueAsString(sevCoding, "code"))
                .isEqualTo("some prb detail");

        // Verify code text is set correctly.
        Base code = ResourceUtils.getValue(condition, "code");
        assertThat(ResourceUtils.getValueAsString(code, "text")).isEqualTo("Cholelithiasis");

        // Verify code coding fields are set correctly.
        Base codeCoding = ResourceUtils.getValue(code, "coding");
        assertThat(ResourceUtils.getValueAsString(codeCoding, "system"))
                .isEqualTo("UriType[http://hl7.org/fhir/sid/icd-10]");
        assertThat(ResourceUtils.getValueAsString(codeCoding, "code")).isEqualTo("K80.00");
        
         // Verify stage summary code is set correctly.
         Base stage = ResourceUtils.getValue(condition, "stage");
         Base summary = ResourceUtils.getValue(stage, "summary");
         Base sumCoding = ResourceUtils.getValue(summary, "coding");
         assertThat(ResourceUtils.getValueAsString(sumCoding, "code"))
                 .isEqualTo("resolved");

    }

    // Tests a particular PRB segment that wasn't working properly recently with
    // regards to condition category.
    @Test
    public void validate_naughty_problem() {

        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID|1||12345678^^^^MR|ALTID|Mouse^Mickey^J^III^^^||||||||||||||||||Born in USA|Y||USA||||\r"
                + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
                + "PRB|AD|20210101000000|G47.31^Primary central sleep apnea^ICD-10-CM|28827016|||20210101000000|20210101000000|20210101000000||||confirmed^Confirmed^http://terminology.hl7.org/CodeSystem/condition-ver-status||20210101000000|20210101000000\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS);
        System.out.println(json);
        assertThat(json).isNotBlank();

        FHIRContext context = new FHIRContext(true, false);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(1);

        // Get the condition Resource
        Resource condition = conditionResource.get(0);

        // Verify category text is set correctly.
        Base category = ResourceUtils.getValue(condition, "category");
        assertThat(ResourceUtils.getValueAsString(category, "text")).isEqualTo("problem-list-item");

        // Verify category coding fields are set correctly.
        Base catCoding = ResourceUtils.getValue(category, "coding");
        assertThat(ResourceUtils.getValueAsString(catCoding, "system"))
                .isEqualTo("UriType[http://terminology.hl7.org/CodeSystem/condition-category]");
        assertThat(ResourceUtils.getValueAsString(catCoding, "display")).isEqualTo("Problem List Item");
        assertThat(ResourceUtils.getValueAsString(catCoding, "code")).isEqualTo("problem-list-item");

    }

    // Tests multiple DG1 segments to verify we get multiple conditions with
    // references to the encounter.
    @Test
    public void validate_encounter_multiple_reasonReferences() {

        String hl7message = "MSH|^~\\&||||||S1|ADT^A01^ADT_A01||T|2.6|||||||||\r"
                + "EVN|A04|20151008111200|20171013152901|O|OID1006|20171013153621|EVN1009\r"
                + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I|6N^1234^A^GENHOS||||0100^NONAME^JOHN|0148^NONAME^JOHN||SUR|||||||0148^NONAME^JOHN|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
                + "PV2|||||||||||||||||||||||||||||||RR|Y|2|Y|Y|N|N|\r"
                + "DG1|1|D1|V72.83^Other specified pre-operative examination^ICD-9^^^|Other specified pre-operative examination|20151008111200|A\r"
                + "DG1|2|D2|R00.0^Tachycardia, unspecified^ICD-10^^^|Tachycardia, unspecified|20150725201300|A\r"
                + "DG1|3|D3|R06.02^Shortness of breath^ICD-10^^^|Shortness of breath||A\r"
                + "DG1|7|D8|J45.909^Unspecified asthma, uncomplicated^ICD-10^^^|Unspecified asthma, uncomplicated||A";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS);
        System.out.println(json);
        assertThat(json).isNotBlank();

        FHIRContext context = new FHIRContext(true, false);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(4);

    }

    // Tests multiple PRB segments to verify we get multiple conditions with
    // references to the encounter.
    @Test
    public void validate_problem_multiple_reasonReferences() {

        String hl7message = "MSH|^~\\&||||||S1|ADT^A01^ADT_A01||T|2.6|||||||||\r"
                + "EVN|A04|20151008111200|20171013152901|O|OID1006|20171013153621|EVN1009\r"
                + "PID|||1234||DOE^JANE^|||F||||||||||||||||||||||\r"
                + "PV1||I|6N^1234^A^GENHOS||||0100^NONAME^JOHN|0148^NONAME^JOHN||SUR|||||||0148^NONAME^JOHN|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
                + "PV2|||||||||||||||||||||||||||||||RR|Y|2|Y|Y|N|N|\r"
                + "DG1|1|D1|V72.83^Other specified pre-operative examination^ICD-9^^^|Other specified pre-operative examination|20151008111200|A\r"
                + "DG1|2|D2|R00.0^Tachycardia, unspecified^ICD-10^^^|Tachycardia, unspecified|20150725201300|A\r"
                + "DG1|3|D3|R06.02^Shortness of breath^ICD-10^^^|Shortness of breath||A\r"
                + "DG1|7|D8|J45.909^Unspecified asthma, uncomplicated^ICD-10^^^|Unspecified asthma, uncomplicated||A";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS);
        System.out.println(json);
        assertThat(json).isNotBlank();

        FHIRContext context = new FHIRContext(true, false);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionResource).hasSize(4);

    }

}
