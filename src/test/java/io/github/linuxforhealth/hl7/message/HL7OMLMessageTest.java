/*
 * (C) Copyright IBM Corp. 2020, 2021
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

import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HL7OMLMessageTest {
    private static FHIRContext context = new FHIRContext();
    private static final Logger LOGGER = LoggerFactory.getLogger(HL7OMLMessageTest.class);
    private static final ConverterOptions OPTIONS = new Builder().withValidateResource().build();
    private static final ConverterOptions OPTIONS_PRETTYPRINT = new Builder()
            .withBundleType(BundleType.COLLECTION)
            .withValidateResource()
            .withPrettyPrint()
            .build();

    @Test
    public void test_oml() throws IOException {
        String hl7message = "MSH|^~\\&||Test System|||20210917110100||OML^O21^OML_O21|d1577c69-dfbe-44ad-ba6d-3e05e953b2ea|T|2.5.1|||AL|AL|||||LOI_NG_PRU_PROFILE^^2.16.840.1.113883.9.87^ISO\r"
        + "PID|1||7659afb9-0dfc-d744-1f40-5b9314807108^^^^MR||Feeney^Sam^^^^^L||20110926000000+0530|M|||953 Schmitt Road^^Milford^MA^^^L|||||S\r"
        + "ORC|NW|8125550e-04db-11ec-a9a8-086d41d421ca^^ID^UUID|||||||20210824000000+0530|||9999997079^Hahn^Reginald^^^^^U^NPI^L^^^NPI\r"
        + "OBR|1|8125550e-04db-11ec-a9a8-086d41d421ca^^ID^UUID||58410-2^CBC panel - Blood by Automated count^LN||||||||||||9999997079^Hahn^Reginald^^^^^U^NPI^L^^^NPI\r"
        + "DG1|1||A013^Paratyphoid fever C^I10C|||A|||||||||1\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS);
        assertThat(json).isNotBlank();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> serviceResource = e.stream()
                .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(serviceResource).hasSize(1);

        List<Resource> physicianresource = e.stream()
                .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(physicianresource).hasSize(1);

        List<Resource> diagnosticresource = e.stream()
                .filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(diagnosticresource).hasSize(1);
        
        List<Resource> conditionresource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(conditionresource).hasSize(1);

        DiagnosticReport diag = ResourceUtils.getResourceDiagnosticReport(diagnosticresource.get(0), context);
        Reference patRef = diag.getSubject();
        assertThat(patRef.isEmpty()).isFalse();
        ServiceRequest request = ResourceUtils.getResourceServiceRequest(serviceResource.get(0), context);
        Reference patientRef = request.getSubject();
        assertThat(patientRef.isEmpty()).isFalse();
        
    }

    @Test
    public void test_omlo21_patient_encounter_present() throws IOException {
        String hl7message = "MSH|^~\\&||TestSystem|||20210917110100||OML^O21^OML_O21|d1577c69-dfbe-44ad-ba6d-3e05e953b2ea|T|2.5.1|||AL|AL|||||LOI_NG_PRU_PROFILE^^2.16.840.1.113883.9.87^ISO\r"
        + "PID|1||7659afb9-0dfc-d744-1f40-5b9314807108^^^^MR||Feeney^Sam^^^^^L||20110926000000+0530|M|||953 Schmitt Road^^Milford^MA^^^L|||||S\r"
        + "PV1|1|O|||||9905^Adams^John|9906^Yellow^William^F|9907^Blue^Oren^J||||||||9908^Green^Mircea^||2462201|||||||||||||||||||||||||20180520230000\r"
        + "ORC|NW|8125550e-04db-11ec-a9a8-086d41d421ca^^ID^UUID|||||||20210824000000+0530|||9999997079^Hahn^Reginald^^^^^U^NPI^L^^^NPI\r"
        + "OBR|1|8125550e-04db-11ec-a9a8-086d41d421ca^^ID^UUID||58410-2^CBC panel - Blood by Automated count^LN||||||||||||9999997079^Hahn^Reginald^^^^^U^NPI^L^^^NPI\r"
        + "DG1|1||A013^Paratyphoid fever C^I10C|||A|||||||||1\r";

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
    public void test_oml_multiple() throws IOException {
        String hl7message = "MSH|^~\\&||Test System|||20210917110100||OML^O21^OML_O21|d1577c69-dfbe-44ad-ba6d-3e05e953b2ea|T|2.5.1|||AL|AL|||||LOI_NG_PRU_PROFILE^^2.16.840.1.113883.9.87^ISO\r"
    + "PID|1||7659afb9-0dfc-d744-1f40-5b9314807108^^^^MR||Feeney^Sam^^^^^L||20110926000000+0530|M|||953 Schmitt Road^^Milford^MA^^^L|||||S\r"
    + "ORC|NW|8125550e-04db-11ec-a9a8-086d41d421ca^^ID^UUID|||||||20210824000000+0530|||9999997079^Hahn^Reginald^^^^^U^NPI^L^^^NPI\r"
    + "OBR|1|8125550e-04db-11ec-a9a8-086d41d421ca^^ID^UUID||58410-2^CBC panel - Blood by Automated count^LN||||||||||||9999997079^Hahn^Reginald^^^^^U^NPI^L^^^NPI\r"
    + "DG1|1||A013^Paratyphoid fever C^I10C|||A|||||||||1\r"
    + "ORC|NW|8125550e-04db-11ec-a9a8-086d41d421cb^^ID^UUID|||||||20210830000000+0530|||9999999979^Steuber^Ludivina^^^^^U^NPI^L^^^NPI\r"
    + "OBR|2|8125550e-04db-11ec-a9a8-086d41d421cb^^ID^UUID||57698-3^Lipid panel with direct LDL - Serum or Plasma^LN||||||||||||9999999979^Steuber^Ludivina^^^^^U^NPI^L^^^NPI\r"
    + "DG1|1||A001^Cholera due to Vibrio cholerae 01, biovar eltor^I10C|||A|||||||||1\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message);

        assertThat(json).isNotBlank();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        assertThat(b.getType()).isEqualTo(BundleType.COLLECTION);
        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patientResource).hasSize(1);

        List<Resource> diagnosisResource = e.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(diagnosisResource).hasSize(2);

        List<Resource> serviceResource = e.stream()
                .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(serviceResource).hasSize(2);

        List<Resource> physicianresource = e.stream()
                .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(physicianresource).hasSize(2);

        List<Resource> diagnosticresource = e.stream()
                .filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(diagnosticresource).hasSize(2);

        DiagnosticReport diag = ResourceUtils.getResourceDiagnosticReport(diagnosticresource.get(0), context);
        Reference ref = diag.getSubject();
        assertThat(ref.isEmpty()).isFalse();
    }

    @Test
    public void test_oml_spm() throws IOException {
        String hl7message = "MSH|^~\\&||Test System|||20210917110100||OML^O21^OML_O21|d1577c69-dfbe-44ad-ba6d-3e05e953b2ea|T|2.5.1|||AL|AL|||||LOI_NG_PRU_PROFILE^^2.16.840.1.113883.9.87^ISO\r"
        + "PID|1||7659afb9-0dfc-d744-1f40-5b9314807108^^^^MR||Feeney^Sam^^^^^L||20110926000000+0530|M|||953 Schmitt Road^^Milford^MA^^^L|||||S\r"
        + "ORC|NW|8125550e-04db-11ec-a9a8-086d41d421ca^^ID^UUID|||||||20210824000000+0530|||9999997079^Hahn^Reginald^^^^^U^NPI^L^^^NPI\r"
        + "OBR|1|8125550e-04db-11ec-a9a8-086d41d421ca^^ID^UUID||58410-2^CBC panel - Blood by Automated count^LN||||||||||||9999997079^Hahn^Reginald^^^^^U^NPI^L^^^NPI\r"
        + "DG1|1||A013^Paratyphoid fever C^I10C|||A|||||||||1\r"
        + "SPM|1|SpecimenID||BLD|||||||P||||||201410060535|201410060821||Y||||||1\r";

        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(hl7message, OPTIONS);
        assertThat(json).isNotBlank();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();

        List<Resource> diagnosticresource = e.stream()
                .filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(diagnosticresource).hasSize(1);

        DiagnosticReport diag = ResourceUtils.getResourceDiagnosticReport(diagnosticresource.get(0), context);
        List<Reference> spmRef = diag.getSpecimen();
        assertThat(spmRef.isEmpty()).isFalse();
        assertThat(spmRef).hasSize(1);
        assertThat(spmRef.get(0).isEmpty()).isFalse();
    }


}