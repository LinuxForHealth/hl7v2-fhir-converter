/*
 * (C) Copyright IBM Corp. 2020. 2021
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
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Observation.ObservationReferenceRangeComponent;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

import io.github.linuxforhealth.api.ResourceModel;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.resource.ResourceReader;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

import io.github.linuxforhealth.hl7.segments.util.DatatypeUtils;

public class DifferentObservationValueTest {
        private static FHIRContext context = new FHIRContext();
        private static HL7MessageEngine engine = new HL7MessageEngine(context);
        private static final ConverterOptions OPTIONS = new Builder().withValidateResource().withPrettyPrint().build();

        private String baseMessage = "MSH|^~\\&|hl7Integration|hl7Integration|||||ADT^A01|||2.6|\r"
                        + "EVN|A01|20130617154644\r"
                        + "PID|1|465 306 5961|000010016^^^MR~000010017^^^MR~000010018^^^MR|407623|Wood^Patrick^^Sr^MR||19700101|female|||High Street^^Oxford^^Ox1 4DP~George St^^Oxford^^Ox1 5AP|||||||\r"
                        + "NK1|1|Wood^John^^^MR|Father||999-9999\r" + "NK1|2|Jones^Georgie^^^MSS|MOTHER||999-9999\r"
                        + "PV1|1||Location||||||||||||||||261938_6_201306171546|||||||||||||||||||||||||20130617134644|||||||||\r";

        private ResourceModel rsm = ResourceReader.getInstance().generateResourceModel("resource/Observation");

        HL7FHIRResourceTemplateAttributes attributes = new HL7FHIRResourceTemplateAttributes.Builder()
                        .withResourceName("Observation").withResourceModel(rsm).withSegment("OBX")
                        .withIsReferenced(false).withRepeats(true).build();

        HL7FHIRResourceTemplate observation = new HL7FHIRResourceTemplate(attributes);
        private HL7MessageModel message = new HL7MessageModel("ADT", Lists.newArrayList(observation));

        @Test
        public void testObservationNmResult() throws IOException {
                String hl7message = baseMessage + "OBX|1|NM|0135–4^TotalProtein||7.3|gm/dl|5.9-8.4|||R|F";
                String json = message.convert(hl7message, engine);

                IBaseResource bundleResource = context.getParser().parseResource(json);
                assertThat(bundleResource).isNotNull();
                Bundle b = (Bundle) bundleResource;
                List<BundleEntryComponent> e = b.getEntry();
                List<Resource> obsResource = e.stream()
                                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
                assertThat(obsResource).hasSize(1);
                Observation obs = (Observation) obsResource.get(0);
                assertThat(obs.getValueQuantity()).isNotNull();
                Quantity q = obs.getValueQuantity();
                assertThat(q.getUnit()).isEqualTo("gm/dl");
                assertThat(q.getValue().floatValue()).isEqualTo(7.3f);
                assertThat(obs.hasReferenceRange()).isTrue();
                assertThat(obs.getReferenceRange()).hasSize(1);
                ObservationReferenceRangeComponent range = obs.getReferenceRangeFirstRep();
                assertThat(range).isNotNull();
                assertThat(range.hasHigh()).isTrue();
                assertThat(range.hasLow()).isTrue();
                Quantity high = range.getHigh();
                assertThat(high.getUnit()).isEqualTo("gm/dl");
                assertThat(high.getValue().floatValue()).isEqualTo(8.4f);
                Quantity low = range.getLow();
                assertThat(low.getValue().floatValue()).isEqualTo(5.9f);
                assertThat(low.getUnit()).isEqualTo("gm/dl");
        }

        @Test
        public void testObservationTxResult() throws IOException {
                String hl7message = baseMessage
                                + "OBX|1|TX|^Type of protein feed^L||Fourth Line: HYPERDYNAMIC LV SYSTOLIC FUNCTION, VISUAL EF 80%||||||F||||Alex||";
                String json = message.convert(hl7message, engine);

                IBaseResource bundleResource = context.getParser().parseResource(json);
                assertThat(bundleResource).isNotNull();
                Bundle b = (Bundle) bundleResource;
                List<BundleEntryComponent> e = b.getEntry();
                List<Resource> obsResource = e.stream()
                                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
                assertThat(obsResource).hasSize(1);
                Observation obs = (Observation) obsResource.get(0);
                assertThat(obs.getValueStringType()).isNotNull();
                StringType q = obs.getValueStringType();
                assertThat(q.asStringValue())
                                .isEqualTo("Fourth Line: HYPERDYNAMIC LV SYSTOLIC FUNCTION, VISUAL EF 80%");
        }

        @Test
        public void testObservationTxMultiplePartsResult() throws IOException {
                String hl7message = baseMessage
                                + "OBX|1|TX|^Type of protein feed^L||HYPERDYNAMIC LV SYSTOLIC FUNCTION, VISUAL EF 80%~Fifth line, as part of a repeated field||||||F||";
                String json = message.convert(hl7message, engine);
                IBaseResource bundleResource = context.getParser().parseResource(json);
                assertThat(bundleResource).isNotNull();
                Bundle b = (Bundle) bundleResource;
                List<BundleEntryComponent> e = b.getEntry();
                List<Resource> obsResource = e.stream()
                                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
                assertThat(obsResource).hasSize(1);
                Observation obs = (Observation) obsResource.get(0);
                assertThat(obs.getValueStringType()).isNotNull();
                StringType q = obs.getValueStringType();
                assertThat(q.asStringValue()).isEqualTo(
                                "HYPERDYNAMIC LV SYSTOLIC FUNCTION, VISUAL EF 80%. Fifth line, as part of a repeated field.");
        }

        // NOTE that even though we are testing for it CE is not part of HL7 V2.6
        @Test
        public void testObservationCeResultUnknownSystem() throws IOException {
                String hl7message = baseMessage
                                + "OBX|1|CE|93000&CMP^LIN^CPT4|11|1305^No significant change was found^MEIECG";
                String json = message.convert(hl7message, engine);

                IBaseResource bundleResource = context.getParser().parseResource(json);
                assertThat(bundleResource).isNotNull();
                Bundle b = (Bundle) bundleResource;
                List<BundleEntryComponent> e = b.getEntry();
                List<Resource> obsResource = e.stream()
                                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
                assertThat(obsResource).hasSize(1);
                Observation obs = (Observation) obsResource.get(0);
                assertThat(obs.getValueCodeableConcept()).isNotNull();
                assertThat(obs.getStatus()).isNotNull();
                CodeableConcept cc = obs.getValueCodeableConcept();
                assertThat(cc.getCoding()).isNotNull();
                assertThat(cc.getCoding().get(0)).isNotNull();
                assertThat(cc.getCoding().get(0).getSystem()).isNull();
                assertThat(cc.getCoding().get(0).getCode()).isEqualTo("1305");
                assertThat(cc.getText()).isEqualTo("No significant change was found");
        }

        @Test
        public void testObservationCeResultKnownSystem() throws IOException {
                String hl7message = baseMessage
                                + "OBX|1|CE|93000&CMP^LIN^CPT4|11|1305^No significant change was found^LN";
                String json = message.convert(hl7message, engine);

                IBaseResource bundleResource = context.getParser().parseResource(json);
                assertThat(bundleResource).isNotNull();
                Bundle b = (Bundle) bundleResource;
                List<BundleEntryComponent> e = b.getEntry();
                List<Resource> obsResource = e.stream()
                                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
                assertThat(obsResource).hasSize(1);
                Observation obs = (Observation) obsResource.get(0);
                assertThat(obs.getValueCodeableConcept()).isNotNull();
                assertThat(obs.getStatus()).isNotNull();
                CodeableConcept cc = obs.getValueCodeableConcept();
                assertThat(cc.getCoding()).isNotNull();
                assertThat(cc.getCoding().get(0)).isNotNull();
                assertThat(cc.getCoding().get(0).getSystem()).isEqualTo("http://loinc.org");
                assertThat(cc.getCoding().get(0).getCode()).isEqualTo("1305");
                assertThat(cc.getText()).isEqualTo("No significant change was found");
        }

        @Test
        public void testObservationStNullResult() throws IOException {
                String hl7message = baseMessage
                                + "OBX|1|ST|14151-5^HCO3 BldCo-sCnc^LN|TEST|||||||F|||20210311122016|||||20210311122153||||";
                String json = message.convert(hl7message, engine);

                IBaseResource bundleResource = context.getParser().parseResource(json);
                assertThat(bundleResource).isNotNull();
                Bundle b = (Bundle) bundleResource;
                List<BundleEntryComponent> e = b.getEntry();
                List<Resource> obsResource = e.stream()
                                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
                assertThat(obsResource).hasSize(1);
                Observation obs = (Observation) obsResource.get(0);
                assertThat(obs.getValueStringType()).isNotNull();
                StringType q = obs.getValueStringType();
                assertThat(q.asStringValue()).isNull();

                // Check the coding  (OBX.3)
                assertThat(obs.hasCode()).isTrue();
                DatatypeUtils.checkCommonCodeableConceptAssertions(obs.getCode(), "14151-5", "HCO3 BldCo-sCnc", "http://loinc.org",
                                "HCO3 BldCo-sCnc");

                // Check the effective Date Time  (OBX 14)
                assertThat(obs.hasEffective()).isTrue();
                assertThat(obs.hasEffectiveDateTimeType()).isTrue();
                assertThat(obs.getEffectiveDateTimeType().asStringValue()).isEqualTo("2021-03-11T12:20:16+08:00");

        }

        // Tests most fields of OBX
        @Test
        public void extendedObservationCWEtest() throws IOException {
                String hl7message = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|ADT^A01^ADT_A01|||2.6||||||||2.6\r"
                                + "OBX|1|CWE|DQW^Some text 1^SNM3|100|DQW^Other text 2^SNM3|mm^Text 3^SNM3|56-98|IND|25|ST|F|20210322153839|LKJ|20210320153850|N56|1111^ClinicianLastName^ClinicianFirstName^^^^Title|Manual^Text the 4th^SNM3|Device_1234567^mySystem|20210322153925|Observation Site^Text 5^SNM3|INST^Instance Identifier System||Radiology^Radiological Services|467 Albany Hospital^^Albany^NY|Cardiology^ContactLastName^Jane^Q^^Dr.^MD\r";

                String json = message.convert(hl7message, engine);

                IBaseResource bundleResource = context.getParser().parseResource(json);
                assertThat(bundleResource).isNotNull();
                Bundle b = (Bundle) bundleResource;
                List<BundleEntryComponent> e = b.getEntry();
                List<Resource> obsResource = e.stream()
                                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
                assertThat(obsResource).hasSize(1);
                Observation obs = (Observation) obsResource.get(0);
                assertThat(obs.hasValueCodeableConcept()).isTrue();

                // Check the coding  (OBX.3)
                assertThat(obs.hasCode()).isTrue();
                DatatypeUtils.checkCommonCodeableConceptAssertions(obs.getCode(), "DQW", "Some text 1",
                                "http://terminology.hl7.org/CodeSystem/SNM3", "Some text 1");

                // Check the value  (OBX.5)
                assertThat(obs.hasValueCodeableConcept()).isTrue();
                DatatypeUtils.checkCommonCodeableConceptAssertions(obs.getValueCodeableConcept(), "DQW", "Other text 2",
                                "http://terminology.hl7.org/CodeSystem/SNM3", "Other text 2");

                // OBX.6 is ignored because the record can only have one valueX and this one is valueCodeableConcept. See test test_observation_NM_result.
                assertThat(obs.hasReferenceRange()).isTrue();
                assertThat(obs.getReferenceRange()).hasSize(1);
                ObservationReferenceRangeComponent range = obs.getReferenceRangeFirstRep();
                assertThat(range).isNotNull();
                assertThat(range.hasHigh()).isTrue();
                assertThat(range.hasLow()).isTrue();
                Quantity high = range.getHigh();
                assertThat(high.getUnit()).isEqualTo("mm");
                assertThat(high.getValue().floatValue()).isEqualTo(98.0f);
                Quantity low = range.getLow();
                assertThat(low.getValue().floatValue()).isEqualTo(56.0f);
                assertThat(low.getUnit()).isEqualTo("mm");

                // Check interpretation (OBX.8)
                assertThat(obs.hasInterpretation()).isTrue();
                assertThat(obs.getInterpretation()).hasSize(1);
                DatatypeUtils.checkCommonCodeableConceptAssertions(obs.getInterpretationFirstRep(), "IND", "Indeterminate",
                                "http://terminology.hl7.org/CodeSystem/v2-0078", "IND");

                // Check the effective Date Time  (OBX.14)
                assertThat(obs.hasEffective()).isTrue();
                assertThat(obs.hasEffectiveDateTimeType()).isTrue();
                assertThat(obs.getEffectiveDateTimeType().asStringValue()).isEqualTo("2021-03-20T15:38:50+08:00");

                // Check performer  (OBX.16 Practictioner + OBX.23/OBX.24/OBX.25 Organization)
                assertThat(obs.hasPerformer()).isTrue();
                assertThat(obs.getPerformer()).hasSize(2); // Practioner and Organization
                // Get Practitioner and see that it is populated with OBX.16 information
                assertThat(obs.getPerformer().get(0).hasReference()).isTrue();
                List<Resource> practitionerResource = e.stream()
                                .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
                                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
                assertThat(practitionerResource).hasSize(1);
                Practitioner doctor = getResourcePractitioner(practitionerResource.get(0));
                assertThat(doctor.getName().get(0).getFamily()).isEqualTo("ClinicianLastName");
                // Get Organization and see that it is populated with OBX.23/OBX.24/OBX.25 information
                assertThat(obs.getPerformer().get(1).hasReference()).isTrue();
                List<Resource> organizationResource = e.stream()
                                .filter(v -> ResourceType.Organization == v.getResource().getResourceType())
                                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
                assertThat(organizationResource).hasSize(1);
                Organization org = getResourceOrganization(organizationResource.get(0));
                assertThat(org.getName()).isEqualTo("Radiology"); // from OBX.23
                assertThat(org.getAddress().get(0).getLine().get(0).getValueAsString())
                                .isEqualTo("467 Albany Hospital"); // from OBX.24
                assertThat(org.getAddress().get(0).getCity()).isEqualTo("Albany"); // from OBX.24
                assertThat(org.getAddress().get(0).getState()).isEqualTo("NY"); // from OBX.24
                assertThat(org.getContact().get(0).getName().getFamily()).isEqualTo("ContactLastName"); // from OBX.25
                assertThat(org.getContact().get(0).getName().getGiven().get(0).getValueAsString()).isEqualTo("Jane"); // from OBX.25
                assertThat(org.getContact().get(0).getName().getText()).isEqualTo("Dr. Jane Q ContactLastName"); // from OBX.25
                assertThat(org.getContact().get(0).hasPurpose()).isTrue(); // purpose added because of OBX.25
                DatatypeUtils.checkCommonCodeableConceptAssertions(org.getContact().get(0).getPurpose(), "ADMIN", "Administrative",
                                "http://terminology.hl7.org/CodeSystem/contactentity-type",
                                "Organization Medical Director");

                // Check method  (OBX.17)
                assertThat(obs.hasMethod()).isTrue();
                DatatypeUtils.checkCommonCodeableConceptAssertions(obs.getMethod(), "Manual", "Text the 4th",
                                "http://terminology.hl7.org/CodeSystem/SNM3", "Text the 4th");

                // Check device  (OBX.18)
                assertThat(obs.hasDevice()).isTrue();
                assertThat(obs.getDevice().hasReference()).isTrue();
                List<Resource> deviceResource = e.stream()
                                .filter(v -> ResourceType.Device == v.getResource().getResourceType())
                                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
                assertThat(deviceResource).hasSize(1);
                Device device = getResourceDevice(deviceResource.get(0));
                assertThat(device.getIdentifier().get(0).getValue()).isEqualTo("Device_1234567");
                assertThat(device.getIdentifier().get(0).getSystem()).isEqualTo("urn:id:mySystem");

                // Check bodySite  (OBX.20)
                assertThat(obs.hasBodySite()).isTrue();
                DatatypeUtils.checkCommonCodeableConceptAssertions(obs.getBodySite(), "Observation Site", "Text 5",
                                "http://terminology.hl7.org/CodeSystem/SNM3", "Text 5");

                // Check identifier  (OBX.21)
                assertThat(obs.hasIdentifier()).isTrue();
                assertThat(obs.getIdentifier()).hasSize(2);
                assertThat(obs.getIdentifier().get(1).getValue()).isEqualTo("INST");
                assertThat(obs.getIdentifier().get(1).getSystem()).isEqualTo("urn:id:Instance_Identifier_System");

                // OBX.23/OBX.24/OBX.25 went into Performer: Organization.  Checked above.

                // Check for ABSENCE of category (because no SPM)  Presence of category tested in extendedObservationUnusualRangesAndOtherTest
                assertThat(obs.hasCategory()).isFalse();
        }

        // A companion test to extendedObservationCWEtest that looks for edge cases
        @Test
        public void extendedObservationUnusualRangesAndOtherTest() throws IOException {
                String ORU_r01 = "MSH|^~\\&|NIST Test Lab APP|NIST Lab Facility||NIST EHR Facility|20150926140551||ORU^R01|NIST-LOI_5.0_1.1-NG|T|2.5.1|||AL|AL|||||\r"
                                + "PID|1||||DOE^JANE||||||||||||\r"
                                + "ORC|NW|ORD448811^NIST EHR|R-511^NIST Lab Filler||||||20120628070100|||5742200012^Radon^Nicholas^^^^^^NPI^L^^^NPI\r"
                                + "OBR|1|ORD448811^NIST EHR|R-511^NIST Lab Filler|1000^Hepatitis A B C Panel^99USL|||20120628070100|||||||||5742200012^Radon^Nicholas^^^^^^NPI^L^^^NPI\r"
                                + "OBX|1|CWE|22314-9^Hepatitis A virus IgM Ab [Presence] in Serum^LN^HAVM^Hepatitis A IgM antibodies (IgM anti-HAV)^L^2.52||260385009^Negative (qualifier value)^SCT^NEG^NEGATIVE^L^201509USEd^^Negative (qualifier value)||Negative|N|||F|||20150925|||||201509261400\r"
                                + "OBX|2|NM|22316-4^Hepatitis B virus core Ab [Units/volume] in Serum^LN^HBcAbQ^Hepatitis B core antibodies (anti-HBVc) Quant^L^2.52||0.70|[IU]/mL^international unit per milliliter^UCUM^IU/ml^^L^1.9|<0.50 IU/mL|H|||F|||20150925|||||201509261400\r"
                                + "SPM|1|SpecimenID||BLOOD^Blood^^87612001^BLOOD^SCT^^||||Cord Art^Blood, Cord Arterial^^^^^^^|||P||||||201110060535|201110060821||Y||||||1\r";

                HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
                String json = ftv.convert(ORU_r01, OPTIONS);

                FHIRContext context = new FHIRContext();
                IBaseResource bundleResource = context.getParser().parseResource(json);
                assertThat(bundleResource).isNotNull();
                Bundle b = (Bundle) bundleResource;
                List<BundleEntryComponent> e = b.getEntry();
                List<Resource> obsResource = e.stream()
                                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
                assertThat(obsResource).hasSize(2);
                Observation obs = (Observation) obsResource.get(0);
                assertThat(obs.hasReferenceRange()).isTrue();
                assertThat(obs.getReferenceRange()).hasSize(1);
                ObservationReferenceRangeComponent range = obs.getReferenceRangeFirstRep();
                assertThat(range).isNotNull();
                assertThat(range.hasHigh()).isFalse();
                assertThat(range.hasLow()).isFalse();
                assertThat(range.hasText()).isTrue();
                assertThat(range.getText()).isEqualTo("Negative");

                obs = (Observation) obsResource.get(1);
                assertThat(obs.hasValueQuantity()).isTrue();
                Quantity q = obs.getValueQuantity();
                assertThat(q.hasUnit()).isTrue();
                assertThat(q.getUnit()).isEqualTo("[IU]/mL");
                assertThat(q.hasValue()).isTrue();
                assertThat(q.getValue().floatValue()).isEqualTo(0.7f);
                assertThat(q.hasSystem()).isTrue();
                assertThat(q.getSystem()).isEqualTo("http://unitsofmeasure.org");

                assertThat(obs.hasReferenceRange()).isTrue();
                assertThat(obs.getReferenceRange()).hasSize(1);
                range = obs.getReferenceRangeFirstRep();
                assertThat(range).isNotNull();
                assertThat(range.hasHigh()).isTrue();
                assertThat(range.hasLow()).isFalse();
                Quantity high = range.getHigh();
                assertThat(high.getUnit()).isEqualTo("[IU]/mL");
                assertThat(high.getValue().floatValue()).isEqualTo(0.5f);
                assertThat(range.hasText()).isTrue();
                assertThat(range.getText()).isEqualTo("<0.50 IU/mL");

                // Because there is an SPM record, there should be a category.  (Absence of SPM and category checkedin extendedObservationCWEtest)
                assertThat(obs.hasCategory()).isTrue();
                assertThat(obs.getCategory()).hasSize(1);
                DatatypeUtils.checkCommonCodeableConceptAssertions(obs.getCategoryFirstRep(), "laboratory", "Laboratory",
                                "http://terminology.hl7.org/CodeSystem/observation-category", null);

        }

        private static Practitioner getResourcePractitioner(Resource resource) {
                String s = context.getParser().encodeResourceToString(resource);
                Class<? extends IBaseResource> klass = Practitioner.class;
                return (Practitioner) context.getParser().parseResource(klass, s);
        }

        private static Organization getResourceOrganization(Resource resource) {
                String s = context.getParser().encodeResourceToString(resource);
                Class<? extends IBaseResource> klass = Organization.class;
                return (Organization) context.getParser().parseResource(klass, s);
        }

        private static Device getResourceDevice(Resource resource) {
                String s = context.getParser().encodeResourceToString(resource);
                Class<? extends IBaseResource> klass = Device.class;
                return (Device) context.getParser().parseResource(klass, s);
        }

}
