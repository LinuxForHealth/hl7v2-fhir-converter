/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.segments;

import io.github.linuxforhealth.hl7.segments.util.DatatypeUtils;
import io.github.linuxforhealth.hl7.segments.util.PatientUtils;
import io.github.linuxforhealth.hl7.segments.util.ResourceUtils;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class Hl7DocumentReferenceFHIRConversionTest {

    @Test
   public void doc_ref_master_identifier_test(){
        // Test masterIdentifier uses the value(12.1) and uses "urn:id:extID" for system ID when 12.2 is empty
        String documentReference =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1|OP|TEXT|20180117144200|5566^PAPLast^PAPFirst^J^^MD|20180117144200|201801180346||<PHYSID>|<PHYSID>|MODL|<MESSAGEID>||4466^TRANSCLast^TRANSCFirst^J|<MESSAGEID>||PA||AV\n" +
                        "ORC|NW|PON001^LE|FON001^OE|PGN001|SC|D|1||20170825010500|MS|MS||||20170825010500|\n" +
                        "OBR|1||CD_000000^IE|2244^General Order|||20170825010500||||||Relevant Clinical Information|||||||002|||||F|||550600^Tsadok550600^Janetary~660600^Merrit660600^Darren^F~770600^Das770600^Surjya^P~880600^Winter880600^Oscar^||||770600&Das770600&Surjya&P^^^6N^1234^A|\n";
        DocumentReference report = ResourceUtils.getDocumentReference(documentReference);

        assertThat(report.hasMasterIdentifier()).isTrue();
        Identifier masterID =  report.getMasterIdentifier();
        assertThat(masterID.getValue()).isEqualTo("<MESSAGEID>");
        assertThat(masterID.getSystem()).isEqualTo("urn:id:extID");

        // Test masterIdentifier uses the backup value(12.3) and pulls systemID from 12.2
        documentReference =
                "MSH|^~\\&|HL7Soup|Instance1|MCM|Instance2|200911021022|Security|MDM^T02^MDM_T02|64322|P|2.6|123|456|ER|AL|USA|ASCII|en|2.6|56789^NID^UID|MCM||||\n" +
                        "PID|1||000054321^^^MRN|||||||||||||M|CAT|||||N\n" +
                        "TXA|1|OP|TEXT|20180117144200|5566^PAPLast^PAPFirst^J^^MD|20180117144200|201801180346||<PHYSID>|<PHYSID>|MODL|^SYSTEM^BACKUPID||4466^TRANSCLast^TRANSCFirst^J|<MESSAGEID>||PA||AV\n" +
                        "ORC|NW|PON001^LE|FON001^OE|PGN001|SC|D|1||20170825010500|MS|MS||||20170825010500|\n" +
                        "OBR|1||CD_000000^IE|2244^General Order|||20170825010500||||||Relevant Clinical Information|||||||002|||||F|||550600^Tsadok550600^Janetary~660600^Merrit660600^Darren^F~770600^Das770600^Surjya^P~880600^Winter880600^Oscar^||||770600&Das770600&Surjya&P^^^6N^1234^A|\n";
        report = ResourceUtils.getDocumentReference(documentReference);

        assertThat(report.hasMasterIdentifier()).isTrue();
        masterID =  report.getMasterIdentifier();
        assertThat(masterID.getValue()).isEqualTo("BACKUPID");
        assertThat(masterID.getSystem()).isEqualTo("urn:id:SYSTEM");
    }
}