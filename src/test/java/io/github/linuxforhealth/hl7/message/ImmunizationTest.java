/*
 * (C) Copyright IBM Corp. 2020, 2021
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
import org.hl7.fhir.r4.model.Immunization;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import com.google.common.collect.Lists;
import io.github.linuxforhealth.api.ResourceModel;
import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.resource.ResourceReader;

public class ImmunizationTest {
    private static FHIRContext context = new FHIRContext(true, false);
    private static HL7MessageEngine engine = new HL7MessageEngine(context);

    @Test
    public void testImmunization() throws IOException {
        ResourceModel rsmPatient = ResourceReader.getInstance().generateResourceModel("resource/Patient");

        HL7FHIRResourceTemplateAttributes attributesPatient = new HL7FHIRResourceTemplateAttributes.Builder()
                .withResourceName("Patient")
                .withResourceModel(rsmPatient).withSegment("PID").withIsReferenced(true)
                .withRepeats(false).build();

        HL7FHIRResourceTemplate patient = new HL7FHIRResourceTemplate(attributesPatient);

        ResourceModel rsm = ResourceReader.getInstance().generateResourceModel("resource/Immunization");

        HL7FHIRResourceTemplateAttributes attributes = new HL7FHIRResourceTemplateAttributes.Builder()
                .withResourceName("Immunization").withResourceModel(rsm).withGroup("ORDER")
                .withSegment(".RXA").withAdditionalSegments(Lists.newArrayList(".OBSERVATION.OBX", ".ORC"))
                .withRepeats(true).build();

        HL7FHIRResourceTemplate immunization = new HL7FHIRResourceTemplate(attributes);
        HL7MessageModel message = new HL7MessageModel("VXU_V04", Lists.newArrayList(patient, immunization));
        String hl7VUXmessageRep = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.5.1|||NE|AL||||||RI543763\r"
                + "PID|1||12345^^^^MR||TestPatient^Jane^^^^^L||||||\r"
                + "ORC|RE||197027|||||||^Clerk^Myron||MD67895^Pediatric^MARY^^^^MD^^RIA|||||RI2050\r"
                + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|0.5|ML^^ISO+||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX|||CP|A\r"
                + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r"
                + "OBX|1|CE|64994-7^vaccine fund pgm elig cat^LN|1|V02^VFC eligible Medicaid/MedicaidManaged Care^HL70064||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r";

        String json = message.convert(hl7VUXmessageRep, engine);
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle b = (Bundle) bundleResource;
        List<BundleEntryComponent> e = b.getEntry();
        List<Resource> immu = e.stream().filter(v -> ResourceType.Immunization == v.getResource().getResourceType())
                .map(BundleEntryComponent::getResource).collect(Collectors.toList());
        assertThat(immu).hasSize(1);
        Immunization resource = getResource(immu.get(0));
        assertThat(resource).isNotNull();

        assertThat(resource.getStatus().getDisplay()).isEqualTo("completed");
        assertThat(resource.getIdentifier().get(0).getValue()).isEqualTo("48-CVX");
        assertThat(resource.getIdentifier().get(0).getSystem()).isEqualTo("urn:id:extID");
        assertThat(resource.getVaccineCode().getCoding().get(0).getSystem())
                .isEqualTo("http://hl7.org/fhir/sid/cvx");
        assertThat(resource.getVaccineCode().getCoding().get(0).getCode()).isEqualTo("48");
        assertThat(resource.getVaccineCode().getText()).isEqualTo("HIB PRP-T");
        assertThat(resource.getOccurrence().toString()).isEqualTo("DateTimeType[2013-05-31]");

        assertThat(resource.getReportOrigin().getCoding().get(0).getSystem()).isEqualTo("urn:id:NIP001");
        assertThat(resource.getReportOrigin().getCoding().get(0).getCode()).isEqualTo("00");
        assertThat(resource.getReportOrigin().getText()).isEqualTo("new immunization record");
        assertThat(resource.getManufacturer().isEmpty()).isFalse();

        assertThat(resource.getLotNumber()).isEqualTo("33k2a");
        assertThat(resource.getExpirationDate()).isEqualTo("2013-12-10");

        assertThat(resource.getPerformer().get(0).getFunction().getCoding().get(0).getCode())
                .isEqualTo("OP");
        assertThat(resource.getPerformer().get(0).getFunction().getText())
                .isEqualTo("Ordering Provider");
        assertThat(resource.getPerformer().get(0).getActor().isEmpty()).isFalse();

        assertThat(resource.getManufacturer().isEmpty()).isFalse();

    }

    private static Immunization getResource(Resource resource) {
        String s = context.getParser().encodeResourceToString(resource);
        Class<? extends IBaseResource> klass = Immunization.class;
        return (Immunization) context.getParser().parseResource(klass, s);
    }

}
