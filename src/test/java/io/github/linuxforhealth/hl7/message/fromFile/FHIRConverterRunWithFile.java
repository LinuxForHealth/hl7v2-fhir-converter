package io.github.linuxforhealth.hl7.message.fromFile;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.ConverterOptions;
import io.github.linuxforhealth.hl7.ConverterOptions.Builder;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;

public class FHIRConverterRunWithFile {
    //private static final ConverterOptions OPTIONS = new Builder().withValidateResource().withPrettyPrint().build();

    public static void main(String[] args) throws IOException {
        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        ConverterOptions options = new Builder()
                .withBundleType(BundleType.COLLECTION)
                .withValidateResource()
                .withPrettyPrint()
                .build();

        String filename = System.getProperty("hl7.filename");
        if (filename == null) {
        	//filename = "../hl7v2-fhir-converter/src/test/resources/ADT-multiline.hl7";
        	filename = "../hl7v2-fhir-converter/src/test/resources/ORU-multiline-short.hl7";
        	//filename = "../hl7v2-fhir-converter/src/test/resources/ORU-multiline.hl7";
        }
        System.out.println("Converting file: " + filename);
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String everything = "";
        try {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
            everything = sb.toString();
        } finally {
            br.close();
        }
        String json = ftv.convert(everything, options);
        //String json = ftv.convert(new File(filename), options);        
        System.out.println("----------------");
        System.out.println(json);
        System.out.println("----------------");
        
        verifyResult(json, BundleType.COLLECTION, true);
    }

    private static void verifyResult(String json, BundleType expectedBundleType,
            boolean messageHeaderExpected) {
        FHIRContext context = new FHIRContext();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        Bundle b = (Bundle) bundleResource;
        if (b.getType() != expectedBundleType) {
            System.out.println("Bundle type not expected:" + b.getType());
        }
        b.getId();
        b.getMeta().getLastUpdated();

        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> patientResource = e.stream()
                .filter(v -> ResourceType.Patient == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        //        assertThat(patientResource).hasSize(1);
        //
        List<Resource> encounterResource = e.stream()
                .filter(v -> ResourceType.Encounter == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        //        assertThat(encounterResource).hasSize(1);
        List<Resource> obsResource = e.stream()
                .filter(v -> ResourceType.Observation == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        //        assertThat(obsResource).hasSize(1);
        List<Resource> pracResource = e.stream()
                .filter(v -> ResourceType.Practitioner == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        //        assertThat(pracResource).hasSize(4);
        //
        List<Resource> allergyResources = e.stream()
                .filter(v -> ResourceType.AllergyIntolerance == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        //        assertThat(allergyResources).hasSize(2);
        //
        if (messageHeaderExpected) {
            List<Resource> messageHeader = e.stream()
                    .filter(v -> ResourceType.MessageHeader == v.getResource().getResourceType())
                    .map(BundleEntryComponent::getResource).collect(Collectors.toList());
            //            assertThat(messageHeader).hasSize(1);
        }
    }

}
