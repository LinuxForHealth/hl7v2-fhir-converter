/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Base64;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.github.linuxforhealth.fhir.FHIRContext;
import io.github.linuxforhealth.hl7.HL7ToFHIRConverter;
import io.github.linuxforhealth.hl7.segments.util.PatientUtils;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;
import io.github.linuxforhealth.hl7.segments.util.DatatypeUtils;


public class Hl7DocumentReferenceFHIRConversionTest {

    private static FHIRContext context = new FHIRContext();

    // Note: All tests for MDM_T02 and MDM_T06 are the same. Use parameters.
    @ParameterizedTest
    @ValueSource(strings = { 
        "MDM^T02", "MDM^T06"
    }) 
    public void doc_ref_has_all_fields_in_yaml(String segment) {
        //every field covered in the yaml should be listed here
        String documentReference = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|" + segment + "^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n"
                + "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n"
                + "TXA|1|OP|TEXT|20180117144200|5566^PAPLast^PAPFirst^J^^MD|20180117144200|201801180346||<PHYSID>|<PHYSID>|MODL|<MESSAGEID>|4466^TRANSCLast^TRANSCFirst^J||<MESSAGEID>|This segment is for description|PA|R|AV|||||\n";
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

    @ParameterizedTest
    @ValueSource(strings = { 
        "MDM^T02", "MDM^T06"
    }) 
    public void doc_ref_authenticator_and_author_test(String segment) {
        String documentReferenceMessage = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|" + segment + "^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n"
                + "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n"
                + "TXA|1||TEXT||||201801180346||<PHYSID1>^DOE^JANE^J^^MD||||||||||AV|||<PHYSID2>^DOE^JOHN^K^^MD||\n";
        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(documentReferenceMessage, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle bundle = (Bundle) bundleResource;
        List<Bundle.BundleEntryComponent> e = bundle.getEntry();

        List<Resource> documentReferenceList = e.stream()
                .filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
                .map(Bundle.BundleEntryComponent::getResource).collect(Collectors.toList());
        String s = context.getParser().encodeResourceToString(documentReferenceList.get(0));
        Class<? extends IBaseResource> klass = DocumentReference.class;

        DocumentReference documentReference = (DocumentReference) context.getParser().parseResource(klass, s);
        String requesterRefAuthor = documentReference.getAuthor().get(0).getReference();
        String requesterRefAuthenticator = documentReference.getAuthenticator().getReference();

        Practitioner practAuthor = ResourceUtils.getSpecificPractitionerFromBundle(bundle, requesterRefAuthor);
        Practitioner practAuthenticator = ResourceUtils.getSpecificPractitionerFromBundle(bundle,
                requesterRefAuthenticator);

        assertThat(documentReference.hasAuthor()).isTrue();
        assertThat(documentReference.hasAuthenticator()).isTrue();
        assertThat(documentReference.getAuthor()).hasSize(1);
        assertThat(documentReference.getAuthorFirstRep().getDisplay()).isEqualTo("MD JANE J DOE");
        assertThat(documentReference.getAuthenticator().getDisplay()).isEqualTo("MD JOHN K DOE");
        assertThat(practAuthor.getIdentifierFirstRep().getValue()).isEqualTo("<PHYSID1>"); // Value passed to Author is used as Identifier value in practitioner
        assertThat(practAuthenticator.getIdentifierFirstRep().getValue()).isEqualTo("<PHYSID2>"); // Value passed to Authenticator is used as Identifier value in practitioner
    }

    @ParameterizedTest
    @ValueSource(strings = { 
        "MDM^T02", "MDM^T06"
    }) 
    public void doc_ref_content_test(String segment) {
        String documentReferenceMessage = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|" + segment + "^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n"
                + "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n"
                + "TXA|1||TEXT||||201801180346||<PHYSID1>||||||||||AV|||<PHYSID2>||\n"
                // Next three lines create an attachment
                + "OBX|1|TX|||ECHOCARDIOGRAPHIC REPORT||||||F|||202101010000|||\n"
                + "OBX|2|TX|||NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH||||||F|||202101010000|||\n"
                + "OBX|3|TX|||HYPERDYNAMIC LV SYSTOLIC FUNCTION, VISUAL EF 80%||||||F|||202101010000|||\n";

        DocumentReference report = ResourceUtils.getDocumentReference(documentReferenceMessage);
        DocumentReference.DocumentReferenceContentComponent content = report.getContentFirstRep();
        assertThat(content.getAttachment().getContentType()).isEqualTo("text/plain"); // Future TXA.3, currently always defaults to text/plain
        assertThat(content.getAttachment().getCreationElement().toString()).containsPattern("2018-01-18T03:46:00"); // TXA.7 date
        assertThat(content.getAttachment().hasData()).isTrue();
        String decodedData = new String(Base64.getDecoder().decode(content.getAttachment().getDataElement().getValueAsString()));
        assertThat(decodedData).isEqualTo("ECHOCARDIOGRAPHIC REPORT\nNORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH\nHYPERDYNAMIC LV SYSTOLIC FUNCTION, VISUAL EF 80%\n");

        // TODO: Determine if we need to look at anything other than OBX.2 when it is TX
        // Leave this test in place as a reminder
        documentReferenceMessage = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|" + segment + "^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n"
                + "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n"
                + "TXA|1||||||201801180346||<PHYSID1>||||||||||AV|||<PHYSID2>||\n"
                // TODO: find better code for this test which is to see that OBX.2 is the fallback.
                + "OBX|1|SN|||||||||F";
        report = ResourceUtils.getDocumentReference(documentReferenceMessage);
        content = report.getContentFirstRep();
        assertThat(content.getAttachment().getContentType()).isEqualTo("text/plain"); // Future OBX.2 is the backup for content type, but currently always defaults to text/plain
        assertThat(content.getAttachment().getCreationElement().toString()).containsPattern("2018-01-18T03:46:00"); // TXA.7 date
        assertThat(content.getAttachment().hasData()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = { 
        "MDM^T02", "MDM^T06"
    }) 
    public void doc_ref_context_test(String segment) {
        String documentReferenceMessage = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|" + segment + "^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n"
                // OBR.24 converts to DocumentReference.practiceSetting
                + "OBR|1||||||20170825010500|||||||||||||002||||CUS|F||||||||\n"
                + "TXA|1||TEXT|20180117144200|5566^PAPLast^PAPFirst^J^^MD||201801180346||<PHYSID1>||||||||||AV|||<PHYSID2>||\n";
        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(documentReferenceMessage, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();

        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle bundle = (Bundle) bundleResource;
        List<Bundle.BundleEntryComponent> e = bundle.getEntry();

        List<Resource> documentReferenceList = e.stream()
                .filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
                .map(Bundle.BundleEntryComponent::getResource).collect(Collectors.toList());
        String s = context.getParser().encodeResourceToString(documentReferenceList.get(0));
        Class<? extends IBaseResource> klass = DocumentReference.class;

        DocumentReference documentReference = (DocumentReference) context.getParser().parseResource(klass, s);
        String requesterRef = documentReference.getContext().getRelatedFirstRep().getReference();

        Practitioner practBundle = ResourceUtils.getSpecificPractitionerFromBundle(bundle, requesterRef);

        DocumentReference.DocumentReferenceContextComponent context = documentReference.getContext();
        assertThat(context.getPeriod().getStartElement().toString()).containsPattern("2018-01-17T14:42:00");
        assertThat(context.hasRelated()).isTrue();
        assertThat(context.hasPracticeSetting()).isTrue();  // OBR.24
        DatatypeUtils.checkCommonCodeableConceptAssertions(context.getPracticeSetting(), "CUS", "Cardiac Ultrasound", "http://terminology.hl7.org/CodeSystem/v2-0074", "CUS");

        assertThat(practBundle.getIdentifierFirstRep().getValue()).isEqualTo("5566");
        // Value passed to Context(TXA.5) is used as Identifier value in practitioner
        assertThat(practBundle.hasName()).isTrue();
        assertThat(practBundle.getNameFirstRep().getTextElement().getValue()).isEqualTo("MD PAPFirst J PAPLast");

    }

    @ParameterizedTest
    @ValueSource(strings = { 
        "MDM^T02", "MDM^T06"
    }) 
    public void doc_ref_date_test(String segment) {
        String documentReferenceMessage = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|" + segment + "^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n"
                + "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n"
                + "TXA|1||TEXT|||20180117144200|201801180346||<PHYSID1>||||||||||AV|||<PHYSID2>||\n";
        DocumentReference report = ResourceUtils.getDocumentReference(documentReferenceMessage);

        InstantType date = report.getDateElement();
        assertThat(date.toString()).containsPattern("2018-01-17T14:42"); // TXA.6
    }

    @ParameterizedTest
    @ValueSource(strings = { 
        "MDM^T02", "MDM^T06"
    }) 
    public void doc_ref_description_test(String segment) {
        String documentReferenceMessage = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|" + segment + "^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n"
                + "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n"
                + "TXA|1||TEXT||||201801180346||<PHYSID1>|||||||This segment is for description|||AV|||<PHYSID2>||\n";
        DocumentReference report = ResourceUtils.getDocumentReference(documentReferenceMessage);

        String description = report.getDescription();
        assertThat(description).isEqualTo("This segment is for description"); // TXA.16
    }

    @ParameterizedTest
    @ValueSource(strings = { 
        "MDM^T02", "MDM^T06"
    }) 
    public void doc_ref_doc_status_test(String segment) {
        String documentReferenceMessage = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|" + segment + "^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n"
                + "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n"
                + "TXA|1||TEXT|||20180117144200|201801180346||<PHYSID1>||||||||PA||AV|||<PHYSID2>||\n";
        DocumentReference report = ResourceUtils.getDocumentReference(documentReferenceMessage);
        DocumentReference.ReferredDocumentStatus docStatus = report.getDocStatus();
        assertThat(docStatus.toCode()).isEqualTo("preliminary"); // TXA.17
        assertThat(docStatus.getDefinition())
                .contains("This is a preliminary composition or document (also known as initial or interim).");
        assertThat(docStatus.getSystem()).isEqualTo("http://hl7.org/fhir/composition-status");

        // Check OBX.11 as fallback
        documentReferenceMessage = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|" + segment + "^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n"
                + "OBR|1|||555|||20170825010500|||||||||||||002|||||F||||||||\n"
                + "TXA|1||TEXT|||20180117144200|201801180346||<PHYSID1>||||||||||AV|||<PHYSID2>||\n"
                + "OBX|1|SN|||||||||F";
        report = ResourceUtils.getDocumentReference(documentReferenceMessage);
        docStatus = report.getDocStatus();
        assertThat(docStatus.toCode()).isEqualTo("final"); // OBX.11
        assertThat(docStatus.getDefinition()).contains(
                "This version of the composition is complete and verified by an appropriate person and no further work is planned.");
        assertThat(docStatus.getSystem()).isEqualTo("http://hl7.org/fhir/composition-status");

        // Check a value that is not mapped results is no docStatus
        documentReferenceMessage = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|" + segment + "^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "ORC|NW|||PGN001|SC|D|1|||MS|MS\n"
                + "OBR|1|||555|||20170825010500|||||||||||||002|||||F\n"
                + "TXA|1||TEXT|||20180117144200|201801180346||<PHYSID1>||||||||||AV|||<PHYSID2>\n"
                + "OBX|1|SN|||||||||X";
        report = ResourceUtils.getDocumentReference(documentReferenceMessage);
        assertThat(report.getDocStatus()).isNull(); // OBX-11 'X' is not mapped
    }

    @ParameterizedTest
    @ValueSource(strings = { 
        "MDM^T02", "MDM^T06"
    }) 
    @Disabled
    // TODO: TXA-13 is not yet mapped in DocumentReference.yml
    public void doc_ref_relates_to_test(String segment) {
        String documentReferenceMessage = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|" + segment + "^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n"
                + "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n"
                + "TXA|1||TEXT|||20180117144200|201801180346||<PHYSID1>||||4466^TRANSCLast^TRANSCFirst^J||||||AV|||<PHYSID2>||\n";
        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(documentReferenceMessage, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();
        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle bundle = (Bundle) bundleResource;
        List<Bundle.BundleEntryComponent> e = bundle.getEntry();

        List<Resource> documentReferenceList = e.stream()
                .filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
                .map(Bundle.BundleEntryComponent::getResource).collect(Collectors.toList());
        String s = context.getParser().encodeResourceToString(documentReferenceList.get(0));
        Class<? extends IBaseResource> klass = DocumentReference.class;

        DocumentReference documentReference = (DocumentReference) context.getParser().parseResource(klass, s);
        String requesterRef = documentReference.getContext().getRelatedFirstRep().getReference();
        Practitioner practBundle = ResourceUtils.getSpecificPractitionerFromBundle(bundle, requesterRef);

        DocumentReference.DocumentReferenceRelatesToComponent relatesTo = documentReference.getRelatesToFirstRep();
        assertThat(relatesTo.getCode().toString()).isEqualTo("appends");
        Identifier practitionerIdentifier = practBundle.getIdentifierFirstRep();
        assertThat(practitionerIdentifier.getValue()).isEqualTo("4466"); // TXA-13.1
        assertThat(practitionerIdentifier.getSystem()).isEqualTo("TRANSCLast"); // TXA-13.2
    }

    @ParameterizedTest
    @ValueSource(strings = { 
        "MDM^T02", "MDM^T06"
    }) 
    public void doc_ref_security_label_test(String segment) {
        String documentReferenceMessage = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|" + segment + "^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n"
                + "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n"
                + "TXA|1||TEXT||||201801180346||<PHYSID1>||||||||PA|R|AV|||<PHYSID2>||\n";
        DocumentReference report = ResourceUtils.getDocumentReference(documentReferenceMessage);

        CodeableConcept securityLabel = report.getSecurityLabelFirstRep();
        assertThat(securityLabel.getCodingFirstRep().getCode()).isEqualTo("R"); // TXA.18
        assertThat(securityLabel.getCodingFirstRep().getDisplay()).isEqualTo("Restricted");
        assertThat(securityLabel.getCodingFirstRep().getSystem())
                .isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0272");
    }

    @ParameterizedTest
    @ValueSource(strings = { 
        "MDM^T02", "MDM^T06"
    }) 
    public void doc_ref_status_test(String segment) {
        // Check TXA.19
        // TXA.19 value maps to status; OBR.25 is ignored
        String documentReferenceMessage = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|" + segment + "^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n"
                + "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n"
                + "TXA|1||TEXT||||201801180346||<PHYSID1>||||||||PA||OB|||<PHYSID2>||\n";
        DocumentReference report = ResourceUtils.getDocumentReference(documentReferenceMessage);
        org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus status = report.getStatus();
        assertThat(status.toCode()).isEqualTo("superseded"); // TXA.19
        assertThat(status.getSystem()).isEqualTo("http://hl7.org/fhir/document-reference-status");

        // Check OBR.25 as fallback
        // TXA.19 value is empty so OBR.25 is used
        documentReferenceMessage = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|" + segment + "^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "ORC|NW|||PGN001|SC|D|1|||MS|MS\n"
                + "OBR|1||||||20170825010500|||||||||||||002|||||X\n"
                + "TXA|1||TEXT||||201801180346||<PHYSID1>||||||||PA|||||<PHYSID2>\n";
        report = ResourceUtils.getDocumentReference(documentReferenceMessage);
        status = report.getStatus();
        assertThat(status.toCode()).isEqualTo("entered-in-error"); // OBR.25
        assertThat(status.getSystem()).isEqualTo("http://hl7.org/fhir/document-reference-status");

        // Check default value case
        // TXA.19 and OBR.25 values are empty so should fallback to "current"
        documentReferenceMessage = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|" + segment + "^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n"
                + "OBR|1||||||20170825010500|||||||||||||002\n"
                + "TXA|1||TEXT||||201801180346||<PHYSID1>||||||||PA\n";
        report = ResourceUtils.getDocumentReference(documentReferenceMessage);
        status = report.getStatus();
        assertThat(status.toCode()).isEqualTo("current"); // default value
        assertThat(status.getSystem()).isEqualTo("http://hl7.org/fhir/document-reference-status");
    }

    @ParameterizedTest
    @ValueSource(strings = { 
        "MDM^T02", "MDM^T06"
    }) 
    public void doc_ref_subject_test(String segment) {
        String documentReferenceMessage = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|" + segment + "^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" + "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n"
                + "TXA|1||TEXT||||201801180346||<PHYSID1>||||||||||AV|||<PHYSID2>||\n";
        HL7ToFHIRConverter ftv = new HL7ToFHIRConverter();
        String json = ftv.convert(documentReferenceMessage, PatientUtils.OPTIONS);
        assertThat(json).isNotBlank();

        IBaseResource bundleResource = context.getParser().parseResource(json);
        assertThat(bundleResource).isNotNull();
        Bundle bundle = (Bundle) bundleResource;
        List<Bundle.BundleEntryComponent> e = bundle.getEntry();

        List<Resource> documentReferenceList = e.stream()
                .filter(v -> ResourceType.DocumentReference == v.getResource().getResourceType())
                .map(Bundle.BundleEntryComponent::getResource).collect(Collectors.toList());
        String s = context.getParser().encodeResourceToString(documentReferenceList.get(0));
        Class<? extends IBaseResource> klass = DocumentReference.class;

        DocumentReference documentReference = (DocumentReference) context.getParser().parseResource(klass, s);

        assertThat(documentReference.hasSubject()).isTrue();
        assertThat(documentReference.getSubject().getReference().startsWith("Patient"));
    }

    @ParameterizedTest
    @ValueSource(strings = { 
        "MDM^T02", "MDM^T06"
    }) 
    public void doc_ref_master_identifier_test(String segment) {
        // Test masterIdentifier uses the value(12.1) but does not require a system if 12.2 is empty
        String documentReference = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|" + segment + "^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n"
                + "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n"
                + "TXA|1||TEXT||||201801180346|||||<MESSAGEID>|||||PA|R|AV|||||\n";
        DocumentReference report = ResourceUtils.getDocumentReference(documentReference);
        assertThat(report.hasMasterIdentifier()).isTrue();
        Identifier masterID = report.getMasterIdentifier();
        assertThat(masterID.getValue()).isEqualTo("<MESSAGEID>");
        assertThat(masterID.getSystem()).isNull();

        // Test masterIdentifier uses the value(12.1) and pulls systemID from 12.2
        documentReference = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|" + segment + "^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n"
                + "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n"
                + "TXA|1||TEXT||||201801180346|||||<MESSAGEID>^SYSTEM|||||PA|R|AV|||||\n";
        report = ResourceUtils.getDocumentReference(documentReference);
        assertThat(report.hasMasterIdentifier()).isTrue();
        masterID = report.getMasterIdentifier();
        assertThat(masterID.getValue()).isEqualTo("<MESSAGEID>");
        assertThat(masterID.getSystem()).isEqualTo("urn:id:SYSTEM");

        // Test masterIdentifier uses the backup value(12.3) and does not have a system
        documentReference = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|" + segment + "^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n"
                + "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n"
                + "TXA|1||TEXT||||201801180346|||||^SYSTEM^<BACKUPID>|||||PA|R|AV|||||\n";
        report = ResourceUtils.getDocumentReference(documentReference);
        assertThat(report.hasMasterIdentifier()).isTrue();
        masterID = report.getMasterIdentifier();
        assertThat(masterID.getValue()).isEqualTo("<BACKUPID>");
        assertThat(masterID.getSystem()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { 
        "MDM^T02", "MDM^T06"
    }) 
    public void doc_ref_type_test(String segment) {
        String documentReferenceMessage = "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|" + segment + "^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n"
                + "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n"
                + "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n"
                + "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n"
                + "TXA|1|OP|TEXT||||201801180346||<PHYSID1>||||||||PA||AV|||<PHYSID2>||\n";
        DocumentReference report = ResourceUtils.getDocumentReference(documentReferenceMessage);

        CodeableConcept type = report.getType();
        assertThat(type.getCodingFirstRep().getCode()).isEqualTo("OP"); // TXA.2
        assertThat(type.getCodingFirstRep().getDisplay()).isEqualTo("Operative report");
        assertThat(type.getCodingFirstRep().getSystem()).isEqualTo("http://terminology.hl7.org/CodeSystem/v2-0270");
    }
}