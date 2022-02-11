/*
 * (C) Copyright IBM Corp. 2021, 2022
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.data;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Duration;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

/**
 * Tests key changes in ZoneId input through context
 */
class InputTimeZoneIdTest {

    @Test
    void validateTimeZoneIdSettingViaOption() {

        // NOTE: This simple Condition (PRB) segment is used for testing time because it has both 
        // standard DateTime conversions (abatementDateTime, recordedDate) and value conversions (extension).
        String hl7message = "MSH|^~\\&|||||20040629164652|1|PPR^PC1|331|P|2.3.1||\r"
                + "PID||||||||||||||||||||||||||||||\r"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\r"
                // PRB.1 to PRB.4 required
                // PRB.2 to recordedDateTime (check time ZoneId) (Note winter date)
                // PRB.7 to Extension "condition-assertedDate" DateTime (check time ZoneId)
                // PRB.9 to abatementDateTime (check time ZoneId)
                // PRB.16 to onsetDateTime (check time ZoneId)
                + "PRB|AD|20020202020000|K80.00^Cholelithiasis^I10|53956|||20070707070000||20090909090000|||||||20160616160000\r";

        // FIRST test with city based ZoneId passed through options  
        ConverterOptions customOptionsWithTenant = new Builder().withValidateResource().withPrettyPrint()
                .withZoneIdText("America/Chicago").build();
        // "America/Chicago" will become -06:00 in winter (CST) -05:00 in spring/summer/fall (CDT).  This is expected. 
        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message,
                customOptionsWithTenant);
        // Find the condition from the FHIR bundle.
        List<Resource> conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1);
        Condition condition = (Condition) conditionResource.get(0);
        assert (condition.getRecordedDateElement().getValueAsString()).contains("2002-02-02T02:00:00-06:00"); // PRB.2 Chicago Central STANDARD Time
        assert (condition.getAbatementDateTimeType().getValueAsString()).contains("2009-09-09T09:00:00-05:00"); // PRB.9 Chicago Central DAYLIGHT Time
        assert (condition.getOnsetDateTimeType().getValueAsString()).contains("2016-06-16T16:00:00-05:00"); // PRB.16 Chicago CDT
        assert (condition.getExtensionByUrl("http://hl7.org/fhir/StructureDefinition/condition-assertedDate")
                .getValueAsPrimitive().getValueAsString()).contains("2007-07-07T07:00:00-05:00"); // PRB.7 Chicago CDT

        // SECOND test with fixed ZoneId passed through options      
        customOptionsWithTenant = new Builder().withValidateResource().withPrettyPrint().withZoneIdText("+03:00")
                .build();
        e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message,
                customOptionsWithTenant);
        // Find the condition from the FHIR bundle.
        conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1);
        condition = (Condition) conditionResource.get(0);
        assert (condition.getRecordedDateElement().getValueAsString()).contains("2002-02-02T02:00:00+03:00"); // PRB.2 fixed ZoneId
        assert (condition.getAbatementDateTimeType().getValueAsString()).contains("2009-09-09T09:00:00+03:00"); // PRB.9
        assert (condition.getOnsetDateTimeType().getValueAsString()).contains("2016-06-16T16:00:00+03:00"); // PRB.16
        assert (condition.getExtensionByUrl("http://hl7.org/fhir/StructureDefinition/condition-assertedDate")
                .getValueAsPrimitive().getValueAsString()).contains("2007-07-07T07:00:00+03:00"); // PRB.7    

        // THIRD test with no ZoneId passed through options, so it uses the config.properties ZoneId       
        customOptionsWithTenant = new Builder().withValidateResource().withPrettyPrint().build();
        e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message,
                customOptionsWithTenant);
        // Find the condition from the FHIR bundle.
        conditionResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionResource).hasSize(1);
        condition = (Condition) conditionResource.get(0);
        assert (condition.getRecordedDateElement().getValueAsString()).contains("2002-02-02T02:00:00+08:00"); // PRB.2 config.properties ZoneId
        assert (condition.getAbatementDateTimeType().getValueAsString()).contains("2009-09-09T09:00:00+08:00"); // PRB.9
        assert (condition.getOnsetDateTimeType().getValueAsString()).contains("2016-06-16T16:00:00+08:00"); // PRB.16
        assert (condition.getExtensionByUrl("http://hl7.org/fhir/StructureDefinition/condition-assertedDate")
                .getValueAsPrimitive().getValueAsString()).contains("2007-07-07T07:00:00+08:00"); // PRB.7    
    }

    @Test
    void testEncounterLengthWithZoneIdContext() {
        // This is the same as the test for PV1.44 and PV1.45 in Hl7EncounterFHIRConversionTest.testEncounterLength, but 
        // here we pass in ZoneIdText context to ensure that Hl7RelatedGeneralUtils.pv1DurationLength works correctly with an input ZoneId.

        // Both start PV1.44 and end PV1.45 must be present to use either value as part of length
        // When both are present, the calculation value is provided in "Minutes"
        String hl7message = "MSH|^~\\&|PROSOLV||||20151008111200||ADT^A01^ADT_A01|MSGID000001|T|2.6|||||||||\n"
                + "EVN|A04|20151008111200|||||\n"
                + "PID|||1234^^^^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                // PV1.44 present; PV1.45 present
                + "PV1|1|E||||||||||||||||||||||||||||||||||||||||||20161013154626|20161014154634|||||||\n";

        // Test with city based ZoneId passed through options  
        ConverterOptions customOptionsWithTenant = new Builder().withValidateResource().withPrettyPrint()
                .withZoneIdText("Europe/Paris").build();
        // "Europe/Paris" will become -01:00, but it we don't see because it is internal and relative to other date. 
        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message,
                customOptionsWithTenant);
        // Find the encounter from the FHIR bundle.
        List<Resource> encounters = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounters).hasSize(1);
        Encounter encounter = (Encounter) encounters.get(0);
        assertThat(encounter.hasLength()).isTrue();
        Duration encounterLength = encounter.getLength();
        assertThat(encounterLength.getValue()).isEqualTo((BigDecimal.valueOf(1440)));
        assertThat(encounterLength.getUnit()).isEqualTo("minutes");
        assertThat(encounterLength.getCode()).isEqualTo("min");
        assertThat(encounterLength.getSystem()).isEqualTo("http://unitsofmeasure.org");
    }

}
