/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.segments.util.PatientUtils;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class Hl7DocumentReferenceFHIRConversionTest {

    private static FHIRContext context = new FHIRContext();

    @Test
    public void doc_ref_has_all_fields_in_yaml(){
        //every field covered in the yaml should be listed here
        String documentReference =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1|OP|TEXT|20180117144200|5566^PAPLast^PAPFirst^J^^MD|20180117144200|201801180346||<PHYSID>|<PHYSID>|MODL|<MESSAGEID>|4466^TRANSCLast^TRANSCFirst^J||<MESSAGEID>|This segment is for description|PA|R|AV|||||\n" +                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n";
        DocumentReference report = ResourceUtils.getDocumentReference(documentReference);

        assertThat(report.hasAuthenticator()).isTrue();
        assertThat(report.hasAuthor()).isTrue();
        assertThat(report.hasContent()).isTrue();
        assertThat(report.hasContext()).isTrue();
        assertThat(report.hasDate()).isTrue();
        assertThat(report.hasDescription()).isTrue();
        assertThat(report.hasDocStatus()).isTrue();
        assertThat(report.hasId()).isTrue();
        assertThat(report.hasIdentifier()).isTrue();
        assertThat(report.hasMasterIdentifier()).isTrue();
        assertThat(report.hasSecurityLabel()).isTrue();
        assertThat(report.hasStatus()).isTrue();
        assertThat(report.hasSubject()).isTrue();
        assertThat(report.hasType()).isTrue();
    }

    @Test
    public void doc_ref_authenticator_and_author_test(){
        String documentReferenceMessage =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1||TEXT||||201801180346||<PHYSID1>||||||||||AV|||<PHYSID2>||\n" +
                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n";
        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(documentReferenceMessage, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle bundle = (Bundle) bundleResource;
        List<Bundle.BundleEntryComponent> e = bundle.getEntry();

        List<Resource> documentReferenceList =
                e.stream().filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
                        .map(Bundle.BundleEntryComponent::getResource).collect(Collectors.toList());
        String s = context.getParser().encodeResourceToString(documentReferenceList.get(0));
        Class<? extends IBaseResource> klass = DocumentReference.class;

        DocumentReference documentReference =  (DocumentReference) context.getParser().parseResource(klass, s);
        String requesterRefAuthor = documentReference.getAuthor().get(0).getReference();
        String requesterRefAuthenticator = documentReference.getAuthenticator().getReference();

        Practitioner practAuthor = ResourceUtils.getSpecificPractitionerFromBundle(bundle, requesterRefAuthor);
        Practitioner practAuthenticator = ResourceUtils.getSpecificPractitionerFromBundle(bundle, requesterRefAuthenticator);

        assertThat(documentReference.hasAuthor()).isTrue();
        assertThat(documentReference.hasAuthenticator()).isTrue();
        assertThat(documentReference.getAuthor()).hasSize(1);
        assertThat(practAuthor.getIdentifierFirstRep().getValue()).isEqualTo("<PHYSID1>"); //Value passed to Author is used as Identifier value in practitioner
        assertThat(practAuthenticator.getIdentifierFirstRep().getValue()).isEqualTo("<PHYSID2>"); //Value passed to Authenticator is used as Identifier value in practitioner

    }

    @Test
    public void doc_ref_content_test(){
        String documentReferenceMessage =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1||TEXT||||201801180346||<PHYSID1>||||||||||AV|||<PHYSID2>||\n" +
                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n";
        DocumentReference report = ResourceUtils.getDocumentReference(documentReferenceMessage);

        DocumentReference.DocumentReferenceContentComponent content = report.getContentFirstRep();
        assertThat(content.getAttachment().getContentType()).isEqualTo("TEXT"); //TXA.3
        assertThat(content.getAttachment().getCreationElement().toString()).containsPattern("2018-01-18T03:46:00"); //TXA.7 date

        documentReferenceMessage =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1||||||201801180346||<PHYSID1>||||||||||AV|||<PHYSID2>||\n" +
                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n" +
                        "OBX|1|SN|||||||||F";
        report = ResourceUtils.getDocumentReference(documentReferenceMessage);

        content = report.getContentFirstRep();
        assertThat(content.getAttachment().getContentType()).isEqualTo("SN"); //OBX.2 is the backup for content type
        assertThat(content.getAttachment().getCreationElement().toString()).containsPattern("2018-01-18T03:46:00"); //TXA.7 date

    }

    @Test
    public void doc_ref_context_test(){
        String documentReferenceMessage =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1||TEXT|20180117144200|5566^PAPLast^PAPFirst^J^^MD||201801180346||<PHYSID1>||||||||||AV|||<PHYSID2>||\n" +
                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n";
        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(documentReferenceMessage, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();

        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle bundle = (Bundle) bundleResource;
        List<Bundle.BundleEntryComponent> e = bundle.getEntry();

        List<Resource> documentReferenceList =
                e.stream().filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
                        .map(Bundle.BundleEntryComponent::getResource).collect(Collectors.toList());
        String s = context.getParser().encodeResourceToString(documentReferenceList.get(0));
        Class<? extends IBaseResource> klass = DocumentReference.class;

        DocumentReference documentReference =  (DocumentReference) context.getParser().parseResource(klass, s);
        String requesterRef = documentReference.getContext().getRelatedFirstRep().getReference();

        Practitioner practBundle = ResourceUtils.getSpecificPractitionerFromBundle(bundle, requesterRef);

        DocumentReference.DocumentReferenceContextComponent context =documentReference.getContext();
        assertThat(context.getPeriod().getStartElement().toString()).containsPattern("2018-01-17T14:42:00");
        assertThat(context.hasRelated()).isTrue();
        assertThat(practBundle.getIdentifierFirstRep().getValue()).isEqualTo("5566"); //Value passed to Context(TXA.5) is used as Identifier value in practitioner
    }

    @Test
    public void doc_ref_date_test(){
        String documentReferenceMessage =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1||TEXT|||20180117144200|201801180346||<PHYSID1>||||||||||AV|||<PHYSID2>||\n" +
                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n";
        DocumentReference report = ResourceUtils.getDocumentReference(documentReferenceMessage);

        InstantType date = report.getDateElement();
        assertThat(date.toString()).containsPattern("2018-01-17T14:42"); //TXA.6
    }

    @Test
    public void doc_ref_description_test(){
        String documentReferenceMessage =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1||TEXT||||201801180346||<PHYSID1>|||||||This segment is for description|||AV|||<PHYSID2>||\n" +
                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n";
        DocumentReference report = ResourceUtils.getDocumentReference(documentReferenceMessage);

        String  description = report.getDescription();
        assertThat(description).isEqualTo("This segment is for description"); //TXA.16

    }

    @Test
    public void doc_ref_doc_status_test(){
        String documentReferenceMessage =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1||TEXT|||20180117144200|201801180346||<PHYSID1>||||||||PA||AV|||<PHYSID2>||\n" +
                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n";
        DocumentReference report = ResourceUtils.getDocumentReference(documentReferenceMessage);

        DocumentReference.ReferredDocumentStatus docStatus = report.getDocStatus();
        assertThat(docStatus.toCode()).isEqualTo(DocumentReference.ReferredDocumentStatus.PRELIMINARY.toCode()); //TXA.17
        assertThat(docStatus.getDefinition()).isEqualTo(DocumentReference.ReferredDocumentStatus.PRELIMINARY.getDefinition()); //TXA.17
        assertThat(docStatus.getSystem()).isEqualTo(DocumentReference.ReferredDocumentStatus.PRELIMINARY.getSystem()); //TXA.17

        documentReferenceMessage = //pulls from the back-up OBX.11
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1||TEXT|||20180117144200|201801180346||<PHYSID1>||||||||||AV|||<PHYSID2>||\n" +
                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n" +
                        "OBX|1|SN|||||||||F";

        report = ResourceUtils.getDocumentReference(documentReferenceMessage);

        docStatus = report.getDocStatus();
        assertThat(docStatus.toCode()).isEqualTo(DocumentReference.ReferredDocumentStatus.FINAL.toCode()); //OBX.11
        assertThat(docStatus.getDefinition()).isEqualTo(DocumentReference.ReferredDocumentStatus.FINAL.getDefinition()); //OBX.11
        assertThat(docStatus.getSystem()).isEqualTo(DocumentReference.ReferredDocumentStatus.FINAL.getSystem()); //OBX.11
    }

    @Test@Disabled
    public void doc_ref_relates_to_test(){
        String documentReferenceMessage =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1||TEXT|||20180117144200|201801180346||<PHYSID1>||||4466^TRANSCLast^TRANSCFirst^J||||||AV|||<PHYSID2>||\n" +
                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n";
        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(documentReferenceMessage, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle bundle = (Bundle) bundleResource;
        List<Bundle.BundleEntryComponent> e = bundle.getEntry();

        List<Resource> documentReferenceList =
                e.stream().filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
                        .map(Bundle.BundleEntryComponent::getResource).collect(Collectors.toList());
        String s = context.getParser().encodeResourceToString(documentReferenceList.get(0));
        Class<? extends IBaseResource> klass = DocumentReference.class;

        DocumentReference documentReference =  (DocumentReference) context.getParser().parseResource(klass, s);
        String requesterRef = documentReference.getContext().getRelatedFirstRep().getReference();
        Practitioner practBundle = ResourceUtils.getSpecificPractitionerFromBundle(bundle, requesterRef);

        DocumentReference.DocumentReferenceRelatesToComponent relatesTo = documentReference.getRelatesToFirstRep();
        assertThat(relatesTo.getCode().toString()).isEqualTo("APPENDS");
        assertThat(practBundle.getIdentifierFirstRep().getValue()).isEqualTo("4466"); //Value passed to RelatesTo is used as Identifier value in practitioner

    }

    @Test
    public void doc_ref_security_label_test(){
        String documentReferenceMessage =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1||TEXT||||201801180346||<PHYSID1>||||||||PA|R|AV|||<PHYSID2>||\n" +
                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n";
        DocumentReference report = ResourceUtils.getDocumentReference(documentReferenceMessage);

        CodeableConcept securityLabel = report.getSecurityLabelFirstRep();
        assertThat(securityLabel.getCodingFirstRep().getCode()).isEqualTo("R"); //TXA.18
        assertThat(securityLabel.getCodingFirstRep().getDisplay()).isEqualTo("Restricted"); //TXA.18
        assertThat(securityLabel.getCodingFirstRep().getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0272"); //TXA.18

    }

    @Test
    public void doc_ref_status_test(){
        String documentReferenceMessage =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1||TEXT||||201801180346||<PHYSID1>||||||||PA||AV|||<PHYSID2>||\n" +
                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n";
        DocumentReference report = ResourceUtils.getDocumentReference(documentReferenceMessage);

        org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus status = report.getStatus();
        assertThat(status.toCode()).isEqualTo("current"); //TXA.19
        assertThat(status.getSystem()).isEqualTo(Enumerations.DocumentReferenceStatus.CURRENT.getSystem());

        documentReferenceMessage =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1||TEXT||||201801180346||<PHYSID1>||||||||PA||AV|||<PHYSID2>||\n" +
                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n";
        report = ResourceUtils.getDocumentReference(documentReferenceMessage);

        status = report.getStatus();
        assertThat(status.toCode()).isEqualTo("current"); //OBR.25
        assertThat(status.getSystem()).isEqualTo(Enumerations.DocumentReferenceStatus.CURRENT.getSystem());

    }

    @Test
    public void doc_ref_subject_test() {
        String documentReferenceMessage =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1||TEXT||||201801180346||<PHYSID1>||||||||||AV|||<PHYSID2>||\n" +
                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n";
        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(documentReferenceMessage, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();

        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle bundle = (Bundle) bundleResource;
        List<Bundle.BundleEntryComponent> e = bundle.getEntry();

        List<Resource> documentReferenceList =
                e.stream().filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
                        .map(Bundle.BundleEntryComponent::getResource).collect(Collectors.toList());
        String s = context.getParser().encodeResourceToString(documentReferenceList.get(0));
        Class<? extends IBaseResource> klass = DocumentReference.class;

        DocumentReference documentReference = (DocumentReference) context.getParser().parseResource(klass, s);

        assertThat(documentReference.hasSubject()).isTrue();
        assertThat(documentReference.getSubject().getReference().startsWith("Patient"));
    }

    @Test
    public void doc_ref_master_identifier_test(){
        // Test masterIdentifier uses the value(12.1) and uses "urn:id:extID" for system ID when 12.2 is empty
        String documentReference =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1||TEXT||||201801180346|||||<MESSAGEID>|||||PA|R|AV|||||\n" +
                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n";
        DocumentReference report = ResourceUtils.getDocumentReference(documentReference);

        assertThat(report.hasMasterIdentifier()).isTrue();
        Identifier masterID =  report.getMasterIdentifier();
        assertThat(masterID.getValue()).isEqualTo("<MESSAGEID>");
        assertThat(masterID.getSystem()).isEqualTo("urn:id:extID");

        // Test masterIdentifier uses the backup value(12.1) and pulls systemID from 12.2
        documentReference =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1||TEXT||||201801180346|||||<MESSAGEID>^SYSTEM|||||PA|R|AV|||||\n" +
                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n";
        report = ResourceUtils.getDocumentReference(documentReference);

        assertThat(report.hasMasterIdentifier()).isTrue();
        masterID =  report.getMasterIdentifier();
        assertThat(masterID.getValue()).isEqualTo("<MESSAGEID>");
        assertThat(masterID.getSystem()).isEqualTo("urn:id:SYSTEM");
    }

    @Test
    public void doc_ref_type_test(){
        String documentReferenceMessage =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1|OP|TEXT||||201801180346||<PHYSID1>||||||||PA||AV|||<PHYSID2>||\n" +
                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n";
        DocumentReference report = ResourceUtils.getDocumentReference(documentReferenceMessage);

        CodeableConcept type = report.getType();
        assertThat(type.getCodingFirstRep().getCode()).isEqualTo("OP"); //TXA.2
        assertThat(type.getCodingFirstRep().getDisplay()).isEqualTo("Operative report"); //TXA.2
        assertThat(type.getCodingFirstRep().getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0270"); //TXA.2

    }
}