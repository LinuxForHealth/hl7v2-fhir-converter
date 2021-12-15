/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Device;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.MedicationAdministration;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.MessageHeader;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Procedure;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.ServiceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

public class ResourceUtils {

    public static FHIRContext context = new FHIRContext();
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceUtils.class);
    private static final ConverterOptions OPTIONS = new Builder().withValidateResource().withPrettyPrint().build();

    public static List<BundleEntryComponent> createFHIRBundleFromHL7MessageReturnEntryList(String inputSegment) {
        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(inputSegment, OPTIONS);
        System.out.print(json);
        assertThat(json).isNotBlank();
        LOGGER.debug("FHIR json result:\n" + json);
        FHIRContext context = new FHIRContext();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();
        return e;
    }

    // Helper method that gets the first (and usually only) value of the property out of a FHIR Base object.
    public static Base getValue(Base obj, String name) {
        Base value = obj.getNamedProperty(name).getValues().get(0);
        return value;
    }

    // Helper method that gets the first (and usually only) value of the property out of a FHIR Base object and returns it as string.
    public static String getValueAsString(Base obj, String name) {
        String value = obj.getNamedProperty(name).getValues().get(0).toString();
        return value;
    }

    public static AllergyIntolerance getAllergyResource(String inputSegment) {
        List<BundleEntryComponent> resource = createFHIRBundleFromHL7MessageReturnEntryList(inputSegment);

        List<Resource> allergy = resource.stream()
                .filter(v -> ResourceType.AllergyIntolerance == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        String s = context.getParser().encodeResourceToString(allergy.get(0));
        Class<? extends IBaseResource> klass = AllergyIntolerance.class;
        return (AllergyIntolerance) context.getParser().parseResource(klass, s);
    }

    public static Condition getCondition(String inputSegment) {
        List<BundleEntryComponent> resource = createFHIRBundleFromHL7MessageReturnEntryList(inputSegment);

        List<Resource> condition = resource.stream()
                .filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        String s = context.getParser().encodeResourceToString(condition.get(0));
        Class<? extends IBaseResource> klass = Condition.class;

        return (Condition) context.getParser().parseResource(klass, s);
    }

    public static DiagnosticReport getDiagnosticReport(String inputSegment) {
        List<BundleEntryComponent> resource = createFHIRBundleFromHL7MessageReturnEntryList(inputSegment);

        List<Resource> diagnosticReport = resource.stream()
                .filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        String s = context.getParser().encodeResourceToString(diagnosticReport.get(0));
        Class<? extends IBaseResource> klass = DiagnosticReport.class;

        return (DiagnosticReport) context.getParser().parseResource(klass, s);
    }

    public static DocumentReference getDocumentReference(String inputSegment) {
        List<BundleEntryComponent> resource = createFHIRBundleFromHL7MessageReturnEntryList(inputSegment);

        List<Resource> documentReference = resource.stream()
                .filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        String s = context.getParser().encodeResourceToString(documentReference.get(0));
        Class<? extends IBaseResource> klass = DocumentReference.class;

        return (DocumentReference) context.getParser().parseResource(klass, s);
    }

    public static Encounter getEncounter(String inputSegment) {
        List<BundleEntryComponent> resource = createFHIRBundleFromHL7MessageReturnEntryList(inputSegment);

        List<Resource> encounter = resource.stream()
                .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        String s = context.getParser().encodeResourceToString(encounter.get(0));
        Class<? extends IBaseResource> klass = Encounter.class;

        return (Encounter) context.getParser().parseResource(klass, s);
    }

    public static Immunization getImmunization(String inputSegment) {
        List<BundleEntryComponent> resource = createFHIRBundleFromHL7MessageReturnEntryList(inputSegment);

        List<Resource> immunization = resource.stream()
                .filter(v -> ResourceType.Immunization == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        String s = context.getParser().encodeResourceToString(immunization.get(0));
        Class<? extends IBaseResource> klass = Immunization.class;

        return (Immunization) context.getParser().parseResource(klass, s);
    }

    public static Observation getObservation(String inputSegment) {
        List<BundleEntryComponent> resource = createFHIRBundleFromHL7MessageReturnEntryList(inputSegment);

        List<Resource> observation = resource.stream()
                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        String s = context.getParser().encodeResourceToString(observation.get(0));
        Class<? extends IBaseResource> klass = Observation.class;

        return (Observation) context.getParser().parseResource(klass, s);
    }

    public static Procedure getProcedure(String inputSegment) {
        List<BundleEntryComponent> resource = createFHIRBundleFromHL7MessageReturnEntryList(inputSegment);

        List<Resource> procedure = resource.stream()
                .filter(v -> ResourceType.Procedure == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        String s = context.getParser().encodeResourceToString(procedure.get(0));
        Class<? extends IBaseResource> klass = Procedure.class;

        return (Procedure) context.getParser().parseResource(klass, s);
    }

    public static ServiceRequest getServiceRequest(String inputSegment) {
        List<BundleEntryComponent> resource = createFHIRBundleFromHL7MessageReturnEntryList(inputSegment);

        List<Resource> serviceRequest = resource.stream()
                .filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        String s = context.getParser().encodeResourceToString(serviceRequest.get(0));
        Class<? extends IBaseResource> klass = ServiceRequest.class;

        return (ServiceRequest) context.getParser().parseResource(klass, s);
    }

    public static Patient getPatient(String inputSegment) {
        List<BundleEntryComponent> resource = createFHIRBundleFromHL7MessageReturnEntryList(inputSegment);

        List<Resource> patient = resource.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(patient).hasSize(1);

        String s = context.getParser().encodeResourceToString(patient.get(0));
        Class<? extends IBaseResource> klass = ServiceRequest.class;

        return (Patient) context.getParser().parseResource(klass, s);
    }

    public static MedicationRequest getMedicationRequest(String inputSegment) {
        List<BundleEntryComponent> resource = createFHIRBundleFromHL7MessageReturnEntryList(inputSegment);

        List<Resource> medicationRequest = resource.stream()
                .filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        String s = context.getParser().encodeResourceToString(medicationRequest.get(0));
        Class<? extends IBaseResource> klass = MedicationRequest.class;

        return (MedicationRequest) context.getParser().parseResource(klass, s);
    }

    public static MedicationAdministration getMedicationAdministration(String inputSegment) {
        List<BundleEntryComponent> resource = createFHIRBundleFromHL7MessageReturnEntryList(inputSegment);

        List<Resource> medicationAdministration = resource.stream()
                .filter(v -> ResourceType.MedicationAdministration == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());

        String s = context.getParser().encodeResourceToString(medicationAdministration.get(0));
        Class<? extends IBaseResource> klass = MedicationAdministration.class;

        return (MedicationAdministration) context.getParser().parseResource(klass, s);
    }

    // Given a bundle and practioner reference, returns the matching referenced Practitioner from the bundle
    // Asserts that there is at least one practitioner in the bundle and exactly one match for the reference
    public static Practitioner getSpecificPractitionerFromBundle(Bundle bundle, String practitionerRef) {
        return getSpecificPractitionerFromBundleEntriesList(bundle.getEntry(), practitionerRef);
    }

    // Given a list of entries from a bundle and practioner reference, returns the matching referenced Practitioner from the bundle
    // Asserts that there is at least one practitioner in the bundle and exactly one match for the reference
    public static Practitioner getSpecificPractitionerFromBundleEntriesList(List<BundleEntryComponent> entries,
            String practitionerRef) {
        // Find the practitioner resources from the FHIR bundle.
        List<Resource> practitioners = entries.stream()
                .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(practitioners.size()).isPositive(); // Confirm there is at least one practitioner
        // Find all practitioners with matching Id's in the list of practitioners.
        List<Resource> matchingPractitioners = new ArrayList<Resource>();
        for (int i = 0; i < practitioners.size(); i++) {
            if (practitioners.get(i).getId().equalsIgnoreCase(practitionerRef)) {
                matchingPractitioners.add(practitioners.get(i));
            }
        }
        assertThat(matchingPractitioners).hasSize(1); // Count must be exactly 1.  
        Practitioner pract = (Practitioner) matchingPractitioners.get(0);
        return pract;
    }

    public static ServiceRequest getResourceServiceRequest(Resource resource, FHIRContext context) {
        String s = context.getParser().encodeResourceToString(resource);
        Class<? extends IBaseResource> klass = ServiceRequest.class;
        return (ServiceRequest) context.getParser().parseResource(klass, s);
    }

    public static DiagnosticReport getResourceDiagnosticReport(Resource resource, FHIRContext context) {
        String s = context.getParser().encodeResourceToString(resource);
        Class<? extends IBaseResource> klass = DiagnosticReport.class;
        return (DiagnosticReport) context.getParser().parseResource(klass, s);
    }

    public static DocumentReference getResourceDocumentReference(Resource resource, FHIRContext context) {
        String s = context.getParser().encodeResourceToString(resource);
        Class<? extends IBaseResource> klass = DocumentReference.class;
        return (DocumentReference) context.getParser().parseResource(klass, s);
    }

    public static MedicationRequest getResourceMedicationRequest(Resource resource, FHIRContext context) {
        String s = context.getParser().encodeResourceToString(resource);
        Class<? extends IBaseResource> klass = MedicationRequest.class;
        return (MedicationRequest) context.getParser().parseResource(klass, s);
    }

    public static Patient getResourcePatient(Resource resource, FHIRContext context) {
        String s = context.getParser().encodeResourceToString(resource);
        Class<? extends IBaseResource> klass = Patient.class;
        return (Patient) context.getParser().parseResource(klass, s);
    }

    public static Immunization getResourceImmunization(Resource resource, FHIRContext context) {
        String s = context.getParser().encodeResourceToString(resource);
        Class<? extends IBaseResource> klass = Immunization.class;
        return (Immunization) context.getParser().parseResource(klass, s);
    }

    public static Encounter getResourceEncounter(Resource resource, FHIRContext context) {
        String s = context.getParser().encodeResourceToString(resource);
        Class<? extends IBaseResource> klass = Encounter.class;
        return (Encounter) context.getParser().parseResource(klass, s);
    }

    public static Practitioner getResourcePractitioner(Resource resource, FHIRContext context) {
        String s = context.getParser().encodeResourceToString(resource);
        Class<? extends IBaseResource> klass = Practitioner.class;
        return (Practitioner) context.getParser().parseResource(klass, s);
    }

    public static Organization getResourceOrganization(Resource resource, FHIRContext context) {
        String s = context.getParser().encodeResourceToString(resource);
        Class<? extends IBaseResource> klass = Organization.class;
        return (Organization) context.getParser().parseResource(klass, s);
    }

    public static MessageHeader getResourceMessageHeader(Resource resource, FHIRContext context) {
        String s = context.getParser().encodeResourceToString(resource);
        Class<? extends IBaseResource> klass = MessageHeader.class;
        return (MessageHeader) context.getParser().parseResource(klass, s);
    }

    public static Condition getResourceCondition(Resource resource, FHIRContext context) {
        String s = context.getParser().encodeResourceToString(resource);
        Class<? extends IBaseResource> klass = Condition.class;
        return (Condition) context.getParser().parseResource(klass, s);
    }

    public static Observation getResourceObservation(Resource resource, FHIRContext context) {
        String s = context.getParser().encodeResourceToString(resource);
        Class<? extends IBaseResource> klass = Observation.class;
        return (Observation) context.getParser().parseResource(klass, s);
    }

    public static Device getResourceDevice(Resource resource, FHIRContext context) {
        String s = context.getParser().encodeResourceToString(resource);
        Class<? extends IBaseResource> klass = Device.class;
        return (Device) context.getParser().parseResource(klass, s);
    }

    public static List<Resource> getResourceList(List<BundleEntryComponent> e, ResourceType resourceType) {
        return e.stream()
                .filter(v -> resourceType == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    }
}
