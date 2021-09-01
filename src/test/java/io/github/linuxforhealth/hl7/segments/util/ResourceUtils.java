/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments.util;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceUtils {

  private static FHIRContext context = new FHIRContext();
  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceUtils.class);
  private static final ConverterOptions OPTIONS =
    new Builder().withValidateResource().withPrettyPrint().build();

  public static List<BundleEntryComponent> createHl7Segment(String inputSegment){
    HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
    String json = ftv.convert(inputSegment, OPTIONS);
    assertThat(json).isNotBlank();
    LOGGER.info("FHIR json result:\n" + json);
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
    List<BundleEntryComponent> resource = createHl7Segment(inputSegment);

    List<Resource> allergy =
            resource.stream().filter(v -> ResourceType.AllergyIntolerance == v.getResource().getResourceType())
                    .map(BundleEntryComponent::getResource).collect(Collectors.toList());

    String s = context.getParser().encodeResourceToString(allergy.get(0));
    Class<? extends IBaseResource> klass = AllergyIntolerance.class;
    return (AllergyIntolerance) context.getParser().parseResource(klass, s);
  }

  public static Condition getCondition(String inputSegment) {
    List<BundleEntryComponent> resource = createHl7Segment(inputSegment);

    List<Resource> condition =
            resource.stream().filter(v -> ResourceType.Condition == v.getResource().getResourceType())
                    .map(BundleEntryComponent::getResource).collect(Collectors.toList());

    String s = context.getParser().encodeResourceToString(condition.get(0));
    Class<? extends IBaseResource> klass = Condition.class;

    return (Condition) context.getParser().parseResource(klass, s);
  }

  public static DiagnosticReport getDiagnosticReport(String inputSegment) {
    List<BundleEntryComponent> resource = createHl7Segment(inputSegment);

    List<Resource> diagnosticReport =
            resource.stream().filter(v -> ResourceType.DiagnosticReport == v.getResource().getResourceType())
                    .map(BundleEntryComponent::getResource).collect(Collectors.toList());

    String s = context.getParser().encodeResourceToString(diagnosticReport.get(0));
    Class<? extends IBaseResource> klass = DiagnosticReport.class;

    return (DiagnosticReport) context.getParser().parseResource(klass, s);
  }

  public static DocumentReference getDocumentReference(String inputSegment) {
    List<BundleEntryComponent> resource = createHl7Segment(inputSegment);

    List<Resource> documentReference =
            resource.stream().filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
                    .map(BundleEntryComponent::getResource).collect(Collectors.toList());

    String s = context.getParser().encodeResourceToString(documentReference.get(0));
    Class<? extends IBaseResource> klass = DocumentReference.class;

    return (DocumentReference) context.getParser().parseResource(klass, s);
  }

  public static Encounter getEncounter(String inputSegment) {
    List<BundleEntryComponent> resource = createHl7Segment(inputSegment);

    List<Resource> encounter =
            resource.stream().filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
                    .map(BundleEntryComponent::getResource).collect(Collectors.toList());

    String s = context.getParser().encodeResourceToString(encounter.get(0));
    Class<? extends IBaseResource> klass = Encounter.class;

    return (Encounter) context.getParser().parseResource(klass, s);
  }

  public static Immunization getImmunization(String inputSegment) {
    List<BundleEntryComponent> resource = createHl7Segment(inputSegment);

    List<Resource> immunization =
            resource.stream().filter(v -> ResourceType.Immunization == v.getResource().getResourceType())
                    .map(BundleEntryComponent::getResource).collect(Collectors.toList());

    String s = context.getParser().encodeResourceToString(immunization.get(0));
    Class<? extends IBaseResource> klass = Immunization.class;

    return (Immunization) context.getParser().parseResource(klass, s);
  }

  public static Observation getObservation(String inputSegment) {
    List<BundleEntryComponent> resource = createHl7Segment(inputSegment);

    List<Resource> observation =
            resource.stream().filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                    .map(BundleEntryComponent::getResource).collect(Collectors.toList());

    String s = context.getParser().encodeResourceToString(observation.get(0));
    Class<? extends IBaseResource> klass = Observation.class;

    return (Observation) context.getParser().parseResource(klass, s);
  }

  public static Procedure getProcedure(String inputSegment) {
    List<BundleEntryComponent> resource = createHl7Segment(inputSegment);

    List<Resource> procedure =
            resource.stream().filter(v -> ResourceType.Procedure == v.getResource().getResourceType())
                    .map(BundleEntryComponent::getResource).collect(Collectors.toList());

    String s = context.getParser().encodeResourceToString(procedure.get(0));
    Class<? extends IBaseResource> klass = Procedure.class;

    return (Procedure) context.getParser().parseResource(klass, s);
  }

  public static ServiceRequest getServiceRequest(String inputSegment) {
    List<BundleEntryComponent> resource = createHl7Segment(inputSegment);

    List<Resource> serviceRequest =
            resource.stream().filter(v -> ResourceType.ServiceRequest == v.getResource().getResourceType())
                    .map(BundleEntryComponent::getResource).collect(Collectors.toList());

    String s = context.getParser().encodeResourceToString(serviceRequest.get(0));
    Class<? extends IBaseResource> klass = ServiceRequest.class;

    return (ServiceRequest) context.getParser().parseResource(klass, s);
  }

  public static Patient getPatient(String inputSegment) {
    List<BundleEntryComponent> resource = createHl7Segment(inputSegment);

    List<Resource> patient =
            resource.stream().filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                    .map(BundleEntryComponent::getResource).collect(Collectors.toList());
    assertThat(patient).hasSize(1);

    String s = context.getParser().encodeResourceToString(patient.get(0));
    Class<? extends IBaseResource> klass = ServiceRequest.class;

    return (Patient) context.getParser().parseResource(klass, s);
  }

  public static MedicationRequest getMedicationRequest(String inputSegment) {
    List<BundleEntryComponent> resource = createHl7Segment(inputSegment);

    List<Resource> medicationRequest =
            resource.stream().filter(v -> ResourceType.MedicationRequest == v.getResource().getResourceType())
                    .map(BundleEntryComponent::getResource).collect(Collectors.toList());

    String s = context.getParser().encodeResourceToString(medicationRequest.get(0));
    Class<? extends IBaseResource> klass = MedicationRequest.class;

    return (MedicationRequest) context.getParser().parseResource(klass, s);
  }

  public static MedicationAdministration getMedicationAdministration(String inputSegment) {
    List<BundleEntryComponent> resource = createHl7Segment(inputSegment);

    List<Resource> medicationAdministration =
            resource.stream().filter(v -> ResourceType.MedicationAdministration == v.getResource().getResourceType())
                    .map(BundleEntryComponent::getResource).collect(Collectors.toList());

    String s = context.getParser().encodeResourceToString(medicationAdministration.get(0));
    Class<? extends IBaseResource> klass = MedicationAdministration.class;

    return (MedicationAdministration) context.getParser().parseResource(klass, s);
  }
}
