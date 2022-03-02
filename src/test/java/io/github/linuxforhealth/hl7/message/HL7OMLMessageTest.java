/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;

public class HL7OMLMessageTest {
    private static FHIRContext context = new FHIRContext();
    private static final ConverterOptions OPTIONS_PRETTYPRINT = new Builder()
            .withBundleType(BundleType.COLLECTION)
            .withValidateResource()
            .withPrettyPrint()
            .build();
    private HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();

    @Test
    public void testOMLO21WithPatientAndOrderWithObservationRequestWithDG1() throws IOException {
        String hl7message = "MSH|^~\\&||Test System|||20210917110100||OML^O21^OML_O21|||2.6\r"
                + "PID|1||7659afb9-0dfc-d744-1f40-5b9314807108^^^^MR||Feeney^Sam^^^^^L|||M||||||||\r"
                + "ORC|NW|8125550e-04db-11ec-a9a8-086d41d421ca^^ID^UUID||||||||||\r"
                + "OBR|1|8125550e-04db-11ec-a9a8-086d41d421ca^^ID^UUID||58410-2^CBC panel - Blood by Automated count^LN||||||||||||\r"
                + "DG1|1||A013^Paratyphoid fever C^I10C|||A|||||||||1\r";

        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); //from PID

        List<Resource> serviceResource = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceResource).hasSize(1); //from ORC

        List<Resource> diagnosticresource = ResourceUtils.getResourceList(e, ResourceType.DiagnosticReport);
        assertThat(diagnosticresource).hasSize(1); //from OBR

        List<Resource> conditionresource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(conditionresource).hasSize(1); //from DG1

        DiagnosticReport diag = ResourceUtils.getResourceDiagnosticReport(diagnosticresource.get(0), context);
        Reference patRef = diag.getSubject();
        assertThat(patRef.isEmpty()).isFalse();
        ServiceRequest request = ResourceUtils.getResourceServiceRequest(serviceResource.get(0), context);
        Reference patientRef = request.getSubject();
        assertThat(patientRef.isEmpty()).isFalse();

        // Confirm that there are no extra resources
        assertThat(e.size()).isEqualTo(4);

    }

    @Test
    public void testOMLO21WithPatientWithPatientVisitAndMinimumOrder() throws IOException {
        String hl7message = "MSH|^~\\&||Test System|||20210917110100||OML^O21^OML_O21|||2.6\r"
                + "PID|1||7659afb9-0dfc-d744-1f40-5b9314807108^^^^MR||Feeney^Sam^^^^^L|||M||||||||\r"
                + "PV1|1|O|||||||||||||||||2462201|||||||||||||||||||||||||20180520230000\r"
                + "ORC|NW|8125550e-04db-11ec-a9a8-086d41d421ca^^ID^UUID||||||||||\r";

        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); //from PID

        List<Resource> encounterResource = ResourceUtils.getResourceList(e, ResourceType.Encounter);
        assertThat(encounterResource).hasSize(1); //from PV1

        // TODO: When function is added to create ServiceRequest from ORC then also check for ServiceRequest
        // List<Resource> serviceResource = e.stream()
        //         .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
        //         .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        // assertThat(serviceResource).hasSize(1);

        // Confirm that there are no extra resources
        assertThat(e.size()).isEqualTo(2); //TODO: When ServiceRequest is added then this line should check for 3.

    }

    @Test
    public void testOMLO21WithPatientAndOrderWithPriorOrder() throws IOException {
        String hl7message = "MSH|^~\\&||Test System|||20210917110100||OML^O21^OML_O21|||2.6\r"
                + "PID|1||7659afb9-0dfc-d744-1f40-5b9314807108^^^^MR||Feeney^Sam^^^^^L|||M||||||||\r"
                + "ORC|NW|8125550e-04db-11ec-a9a8-086d41d421ca^^ID^UUID|||||||\r"
                + "OBR|1|8125550e-04db-11ec-a9a8-086d41d421ca^^ID^UUID||58410-2^CBC panel - Blood by Automated count^LN||||||||||||\r"
                + "DG1|1||A013^Paratyphoid fever C^I10C|||A|||||||||1\r"
                + "ORC|NW|8125550e-04db-11ec-a9a8-086d41d421cb^^ID^UUID|||||||\r"
                + "OBR|2|8125550e-04db-11ec-a9a8-086d41d421cb^^ID^UUID||57698-3^Lipid panel with direct LDL - Serum or Plasma^LN||||||||||||\r"
                + "DG1|1||A001^Cholera due to Vibrio cholerae 01, biovar eltor^I10C|||A|||||||||1\r";

        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); //from PID

        List<Resource> diagnosisResource = ResourceUtils.getResourceList(e, ResourceType.Condition);
        assertThat(diagnosisResource).hasSize(2); //from DG1

        List<Resource> serviceResource = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceResource).hasSize(2); //from ORC

        List<Resource> diagnosticresource = ResourceUtils.getResourceList(e, ResourceType.DiagnosticReport);
        assertThat(diagnosticresource).hasSize(2); //from OBR

        DiagnosticReport diag = ResourceUtils.getResourceDiagnosticReport(diagnosticresource.get(0), context);
        Reference ref = diag.getSubject();
        assertThat(ref.isEmpty()).isFalse();

        // Confirm that there are no extra resources
        assertThat(e.size()).isEqualTo(7);

    }

    @Test
    public void testOMLO21WithSpecimen() throws IOException {
        String hl7message = "MSH|^~\\&||Test System|||20210917110100||OML^O21^OML_O21|||2.6\r"
                + "PID|1||7659afb9-0dfc-d744-1f40-5b9314807108^^^^MR||Feeney^Sam^^^^^L|||M||||||||\r"
                + "ORC|NW|8125550e-04db-11ec-a9a8-086d41d421ca^^ID^UUID||||||||||\r"
                + "OBR|1|8125550e-04db-11ec-a9a8-086d41d421ca^^ID^UUID||58410-2^CBC panel - Blood by Automated count^LN||||||||||||\r"
                + "SPM|1|SpecimenID||BLD|||||||P||||||201410060535|201410060821||Y||||||1\r";

        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); //from PID

        List<Resource> specimenResource = ResourceUtils.getResourceList(e, ResourceType.Specimen);
        assertThat(specimenResource).hasSize(1); //from SPM

        List<Resource> serviceResource = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceResource).hasSize(1); //from ORC

        List<Resource> diagnosticresource = ResourceUtils.getResourceList(e, ResourceType.DiagnosticReport);
        assertThat(diagnosticresource).hasSize(1); //from OBR

        // Verify the specimen reference
        DiagnosticReport diag = ResourceUtils.getResourceDiagnosticReport(diagnosticresource.get(0), context);
        List<Reference> spmRef = diag.getSpecimen();
        assertThat(spmRef.isEmpty()).isFalse();
        assertThat(spmRef).hasSize(1); //from SPM
        assertThat(spmRef.get(0).isEmpty()).isFalse();

        // Confirm that there are no extra resources
        assertThat(e.size()).isEqualTo(4);
    }

    @Test
    public void testOMLO21WithAllergy() throws IOException {
        String hl7message = "MSH|^~\\&||Test System|||20210917110100||OML^O21^OML_O21|||2.6\r"
                + "PID|1||7659afb9-0dfc-d744-1f40-5b9314807108^^^^MR||Feeney^Sam^^^^^L|||M||||||||\r"
                + "AL1|1|DRUG|00000741^OXYCODONE||HYPOTENSION\r"
                + "AL1|2|DRUG|00001433^TRAMADOL||SEIZURES~VOMITING\r"
                + "ORC|NW|8125550e-04db-11ec-a9a8-086d41d421ca^^ID^UUID||||||||||\r"
                + "OBR|1|8125550e-04db-11ec-a9a8-086d41d421ca^^ID^UUID||58410-2^CBC panel - Blood by Automated count^LN||||||||||||\r"
                + "AL1|1|DA|1605^acetaminophen^L|MO|Muscle Pain~hair loss\r";

        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); //from PID

        List<Resource> allergyResource = ResourceUtils.getResourceList(e, ResourceType.AllergyIntolerance);
        assertThat(allergyResource).hasSize(3); //from AL1

        List<Resource> serviceResource = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceResource).hasSize(1); //from ORC

        List<Resource> diagnosticresource = ResourceUtils.getResourceList(e, ResourceType.DiagnosticReport);
        assertThat(diagnosticresource).hasSize(1); //from OBR

        // Confirm that there are no extra resources
        assertThat(e.size()).isEqualTo(6);
    }

    @Test
    @Disabled
    // This test is not currently working.  Support is yet to be added to ensure that a ServiceRequest is created when there is no OBR
    // Note that even though the HL7 message definition does not require PID, it is necessary so that the resulting FHIR ServiceRequests pass FHIR validation.
    public void testOMLO21MinimumMessageWithOrderWithoutObservation() throws IOException {
        String hl7message = "MSH|^~\\&||Test System|||20210917110100||OML^O21^OML_O21|||2.6\r"
                + "PID|1||7659afb9-0dfc-d744-1f40-5b9314807108^^^^MR||Feeney^Sam^^^^^L|||M||||||||\r"
                + "ORC|NW|8125550e-04db-11ec-a9a8-086d41d421ca^^ID^UUID||||||||||\r";

        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); // from PID

        List<Resource> serviceResource = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceResource).hasSize(1); //from ORC

        // Confirm that there are no extra resources
        assertThat(e.size()).isEqualTo(2);
    }

    @Test
    @Disabled
    // This test is not currently working.  Support is yet to be added to ensure that multiple orders are created when there are no Observation groups.
    // In general, support is yet to be added for multiple ORDER groups.  Currently subsequent ORCs are assumed to be part of the ORDER_PRIOR group.
    // Note that even though the HL7 message definition does not require PID, it is necessary so that the resulting FHIR ServiceRequests pass FHIR validation.
    public void testOMLO21MultipleOrdersWithoutObservations() throws IOException {
        String hl7message = "MSH|^~\\&||Test System|||20210917110100||OML^O21^OML_O21|||2.6\r"
                + "PID|1||7659afb9-0dfc-d744-1f40-5b9314807108^^^^MR||Feeney^Sam^^^^^L|||M||||||||\r"
                + "ORC|NW|8125550e-04db-11ec-a9a8-086d41d421ca^^ID^UUID||||||||||\r"
                + "ORC|NW|8125550e-04db-11ec-a9a8-086d41d421ca^^ID^UUID||||||||||\r";

        String json = ftv.convert(hl7message, OPTIONS_PRETTYPRINT);
        assertThat(json).isNotBlank();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> patientResource = ResourceUtils.getResourceList(e, ResourceType.Patient);
        assertThat(patientResource).hasSize(1); //from PID

        List<Resource> serviceResource = ResourceUtils.getResourceList(e, ResourceType.ServiceRequest);
        assertThat(serviceResource).hasSize(2); //from ORC

        // Confirm that there are no extra resources
        assertThat(e.size()).isEqualTo(3);
    }

}
