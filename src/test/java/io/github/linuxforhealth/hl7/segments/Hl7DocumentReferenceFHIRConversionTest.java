/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import io.github.linuxforhealth.hl7.segments.util.DatatypeUtils;
import io.github.linuxforhealth.hl7.segments.util.PatientUtils;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class Hl7DocumentReferenceFHIRConversionTest {

    @Test
    public void doc_ref_has_all_fields_in_yaml(){
        //every field covered in the yaml should be listed here
        String documentReference =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1|OP|TEXT|20180117144200|5566^PAPLast^PAPFirst^J^^MD|20180117144200|201801180346||<PHYSID>|<PHYSID>|MODL|<MESSAGEID>||4466^TRANSCLast^TRANSCFirst^J|<MESSAGEID>|This segment is for description|PA|R|AV|||||\n" +                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
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
        assertThat(report.hasRelatesTo()).isTrue();
        assertThat(report.hasSecurityLabel()).isTrue();
        assertThat(report.hasStatus()).isTrue();
        assertThat(report.hasSubject()).isTrue();
        assertThat(report.hasType()).isTrue();
    }

    @Test
    public void doc_ref_authenticator_and_author_test(){
        String documentReference =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1||TEXT||||||<PHYSID>|||||||||||||<PHYSID>||\n" +
                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n";
        DocumentReference report = ResourceUtils.getDocumentReference(documentReference);

        assertThat(report.hasAuthor()).isTrue();
        assertThat(report.hasAuthor()).isTrue();

        Reference author = report.getAuthorFirstRep();
        Reference Authenticator = report.getAuthenticator();

        //assertThat(author.getReference()).isEqualTo("Practitioner/2cb31d81-2618-4b27-bb84-0529f53f1fa9");
        Identifier masterID =  report.getMasterIdentifier();

        // Test masterIdentifier uses the backup value(12.3) and pulls systemID from 12.2
        documentReference =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1||TEXT|||||||||^SYSTEM^BACKUPID||||||||||||\n" +
                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n";
        report = ResourceUtils.getDocumentReference(documentReference);

        assertThat(report.hasMasterIdentifier()).isTrue();
        masterID =  report.getMasterIdentifier();
        assertThat(masterID.getValue()).isEqualTo("BACKUPID");
        assertThat(masterID.getSystem()).isEqualTo("urn:id:SYSTEM");
    }

    @Test
    public void doc_ref_master_identifier_test(){
        // Test masterIdentifier uses the value(12.1) and uses "urn:id:extID" for system ID when 12.2 is empty
        String documentReference =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1||TEXT|||||||||<MESSAGEID>|||||PA|R|AV|||||\n" +
                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n";
        DocumentReference report = ResourceUtils.getDocumentReference(documentReference);

        assertThat(report.hasMasterIdentifier()).isTrue();
        Identifier masterID =  report.getMasterIdentifier();
        assertThat(masterID.getValue()).isEqualTo("<MESSAGEID>");
        assertThat(masterID.getSystem()).isEqualTo("urn:id:extID");

        // Test masterIdentifier uses the backup value(12.3) and pulls systemID from 12.2
        documentReference =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1||TEXT|||||||||^SYSTEM^BACKUPID|||||PA|R|AV|||||\n" +
                        "ORC|NW|||PGN001|SC|D|1|||MS|MS|||||\n" +
                        "OBR|1||||||20170825010500|||||||||||||002|||||F||||||||\n";
        report = ResourceUtils.getDocumentReference(documentReference);

        assertThat(report.hasMasterIdentifier()).isTrue();
        masterID =  report.getMasterIdentifier();
        assertThat(masterID.getValue()).isEqualTo("BACKUPID");
        assertThat(masterID.getSystem()).isEqualTo("urn:id:SYSTEM");
    }
}