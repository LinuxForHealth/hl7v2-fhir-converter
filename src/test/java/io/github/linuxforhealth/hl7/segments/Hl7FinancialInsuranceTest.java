/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.hl7.segments.util.DatatypeUtils;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

/**
 * Tests for IN1, Coverage, and related segments
 */
class Hl7FinancialInsuranceTest {

    @Test
    void testBasicInsuranceCoverageFields() throws IOException {
        // Currently only tests limited items, other fields to be added

        String hl7message = "MSH|^~\\&|||||20151008111200||ADT^A01^ADT_A01|MSGID000001|T|2.6|||||||||\n"
                + "EVN||20210407191342||||||\n"
                + "PID|||MR1^^^XYZ^MR||DOE^JANE^|||F||||||||||||||||||||||\n"
                // Keep temporarily for reference
                // + "PID|0|1001|adt-a01-a08-patient-6^^^MRN|1008|DEMPSY^PAT^L|BEILY^MIRANDA^M|19251008|F|PAT|Caucasian|1221 CALIFORNIA CIRCLE^^MILPITAS^CA^95127|CA|6471112222|6473334444|ENGLISH|C|BTH|5451235|988775|D-12445889-Z|56877|N|Winnipeg, MB|N|16|CANADIAN|VET|CANADIAN|20170815080000|Y|N|AL|20170816060000|UMD|SPECIESCODE1|BREEDCODE1|STRAIN|NA|NNK1|1|Stanley^Jane^S^Jr^Mrs^BS^P|SPO^Text^CodeSystem|19 Raymond St^Route 3^Albany^NY^56321^USA^H|(002)912-8668^WPN^CP|(555)777-8888^WPN^PH|C^Text^SNM3|19980708||Nurse|C34|D345678|Ward|M|F|19790612153405|S|A0|USA|EN|F|F|N|N|COC|Flick|GERMAN|N|EMERGANCY|Jane Flick|1-888-777-9999|20 Golden Drive^Rochester^NY|Flick|O|2131-1|NO|111224444|Denver^CO\n"
                + "PV1||I||||||||||||||||||||||||||||||||||||||||||\n"
                // More than needed for now, but will use as more fields are implemented
                // Values in use:  
                // IN1.3 to Organization Identifier Value
                // IN1.4 to Organization Name
                // IN1.8 to Coverage.class.value
                // IN1.9.1 to Coverage.class.name
                + "IN1|1|GLOBAL|7776664|Blue Cross Blue Shield|456 Blue Cross Lane|Maria Kirk|1-222-555-6666|UA34567|Blue|987123|IBM|20210101145034|20211231145045|Verbal|DELUXE|Jim Stanley|NON|19780429145224|19 Raymond St|M|CO|3|Y|20210101145300|N|20210102145320|N|Certificate here|20210322145350|Dr Disney|S|GOOD|200|12|B6543|H789456|1000.00|5000.00|17|210.00|520.00|1|M|123 IBM way|True|NONE|B|NO|J321456|M|20210322145605|London|YES\n";

        List<BundleEntryComponent> e = ResourceUtils.createFHIRBundleFromHL7MessageReturnEntryList(hl7message);

        List<Resource> encounters = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounters).hasSize(1); // From PV1

        List<Resource> patients = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patients).hasSize(1); // From PID
        Patient patient = (Patient) patients.get(0);
        String patientId = patient.getId();

        List<Resource> organizations = ResourceUtils.getResourceList(e, ResourceType.Organization);
        assertThat(organizations).hasSize(1); // From Payor created by IN1
        Organization organization = (Organization) organizations.get(0);
        String organizationId = organization.getId();
        assertThat(organization.getName()).isEqualTo("Blue Cross Blue Shield"); // IN1.4
        assertThat(organization.getIdentifier()).hasSize(1);
        assertThat(organization.getIdentifierFirstRep().getValue()).isEqualTo("7776664"); // IN1.3

        List<Resource> coverages = ResourceUtils.getResourceList(e, ResourceType.Coverage);
        assertThat(coverages).hasSize(1); // From IN1 segment
        Coverage coverage = (Coverage) coverages.get(0);
        // Confirm Beneficiary references to Patient, and Payor references to Organization
        assertThat(coverage.getBeneficiary().getReference()).isEqualTo(patientId);
        assertThat(coverage.getPayorFirstRep().getReference()).isEqualTo(organizationId);

        // Only one class expected.  (getClass_ is correct name for method)
        assertThat(coverage.getClass_()).hasSize(1);
        assertThat(coverage.getClass_FirstRep().getName()).isEqualTo("Blue"); // IN1.9.1
        assertThat(coverage.getClass_FirstRep().getValue()).isEqualTo("UA34567"); // IN1.8
        DatatypeUtils.checkCommonCodeableConceptVersionedAssertions(coverage.getClass_FirstRep().getType(), "group",
                "Group", "http://terminology.hl7.org/CodeSystem/coverage-class", null, null);

        // Confirm there are no unaccounted for resources
        assertThat(e).hasSize(4);
    }

}
