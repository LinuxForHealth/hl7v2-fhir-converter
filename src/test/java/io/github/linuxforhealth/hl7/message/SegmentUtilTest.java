/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Type;
import io.github.linuxforhealth.hl7.data.Hl7DataHandlerUtil;
import io.github.linuxforhealth.hl7.message.util.SegmentExtractorUtil;
import io.github.linuxforhealth.hl7.message.util.SegmentGroup;
import io.github.linuxforhealth.hl7.parsing.HL7DataExtractor;
import io.github.linuxforhealth.hl7.parsing.HL7HapiParser;
import io.github.linuxforhealth.hl7.parsing.result.ParsingResult;

class SegmentUtilTest {
    private static final String NORMAL_LV_CHAMBER_SIZE_WITH_MILD_CONCENTRIC_LVH = "NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH";

    private static final ArrayList<String> ORDER_GROUP_LIST = Lists.newArrayList("PROBLEM", "ORDER", "ORDER_DETAIL",
            "ORDER_OBSERVATION");
    private static final ArrayList<String> PROBLEM_GROUP_LIST = Lists.newArrayList("PROBLEM", "PROBLEM_OBSERVATION");

    private static final String ECHOCARDIOGRAPHIC_REPORT = "ECHOCARDIOGRAPHIC REPORT";

    private String messageSingleOrderGroup = "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson||19600614|M||C|99 Oakland #106^^qwerty^OH^44889||^^^^^626^5641111|^^^^^626^5647654|||||343132266|||N\r"
            + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"
            + "NTE|1|P|Problem Comments\r" + "VAR|varid1|200603150610\r"
            + "ORC|NW|1000^OE|9999999^RX|||E|^Q6H^D10^^^R\r"
            + "OBR|1|CD150920001336|CD150920001336|||20150930000000|20150930164100|||||||||25055^MARCUSON^PATRICIA^L|||||||||F|||5755^DUNN^CHAD^B~25055^MARCUSON^PATRICIA^L|||WEAKNESS|DAS, SURJYA P||SHIELDS, SHARON A|||||||||\r"
            + "OBX|1|TX|||ECHOCARDIOGRAPHIC REPORT||||||F|||20150930164100|||\r"
            + "OBX|2|TX|||NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH||||||F|||20150930164100|||\r";

    private String messageSingleProblemGroup = "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson||19600614|M||C|99 Oakland #106^^qwerty^OH^44889||^^^^^626^5641111|^^^^^626^5647654|||||343132266|||N\r"
            + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"
            + "NTE|1|P|Problem Comments\r" + "VAR|varid1|200603150610\r"
            + "OBX|1|TX|||ECHOCARDIOGRAPHIC REPORT||||||F|||20150930164100|||\r"
            + "OBX|2|TX|||NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH||||||F|||20150930164100|||\r";

    private String messageRepeat = "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson||19600614|M||C|99 Oakland #106^^qwerty^OH^44889||^^^^^626^5641111|^^^^^626^5647654|||||343132266|||N\r"
            + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"
            + "ORC|NW|1000^OE|9999999^RX|||E|^Q6H^D10^^^R\r"
            + "OBR|1|CD150920001336|CD150920001336|||20150930000000|20150930164100|||||||||25055^MARCUSON^PATRICIA^L|||||||||F|||5755^DUNN^CHAD^B~25055^MARCUSON^PATRICIA^L|||WEAKNESS|DAS, SURJYA P||SHIELDS, SHARON A|||||||||\r"
            + "OBX|1|TX|||ECHOCARDIOGRAPHIC REPORT||||||F|||20150930164100|||\r"
            + "NTE|1|P|Problem Comments11\r" + "VAR|varid1|200603150610\r"
            + "OBX|2|TX|||NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH||||||F|||20150930164100|||\r"
            + "NTE|2|P|Problem Comments22\r" + "VAR|varid1|200603150610\r"
            + "ORC|NW|1001^OE|9999999^RX|||E|^Q6He^D10^^^R\r"
            + "OBR|1|CD150920001337|CD150920001336|||20150930000000|20150930164100|||||||||25055^MARCUSON^PATRICIA^L|||||||||F|||5755^DUNN^CHAD^B~25055^MARCUSON^PATRICIA^L|||WEAKNESS|DAS, SURJYA P||SHIELDS, SHARON A|||||||||\r"
            + "OBX|1|TX|||ECHOCARDIOGRAPHIC REPORT2||||||F|||20150930164100|||\r"
            + "OBX|2|TX|||NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH 3||||||F|||20150930164100|||";

    private String messageRepeatMultiplePRB = "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson||19600614|M||C|99 Oakland #106^^qwerty^OH^44889||^^^^^626^5641111|^^^^^626^5647654|||||343132266|||N\r"
            + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
            + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"
            + "ORC|NW|1000^OE|9999999^RX|||E|^Q6H^D10^^^R\r"
            + "OBR|1|CD150920001336|CD150920001336|||20150930000000|20150930164100|||||||||25055^MARCUSON^PATRICIA^L|||||||||F|||5755^DUNN^CHAD^B~25055^MARCUSON^PATRICIA^L|||WEAKNESS|DAS, SURJYA P||SHIELDS, SHARON A|||||||||\r"
            + "OBX|1|TX|||ECHOCARDIOGRAPHIC REPORT||||||F|||20150930164100|||\r"
            + "NTE|1|P|Problem Comments11\r" + "VAR|varid1|200603150610\r"
            + "OBX|2|TX|||NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH||||||F|||20150930164100|||\r"
            + "NTE|2|P|Problem Comments22\r" + "VAR|varid1|200603150610\r"
            + "ORC|NW|1001^OE|9999999^RX|||E|^Q6He^D10^^^R\r"
            + "OBR|1|CD150920001337|CD150920001336|||20150930000000|20150930164100|||||||||25055^MARCUSON^PATRICIA^L|||||||||F|||5755^DUNN^CHAD^B~25055^MARCUSON^PATRICIA^L|||WEAKNESS|DAS, SURJYA P||SHIELDS, SHARON A|||||||||\r"
            + "OBX|1|TX|||ECHOCARDIOGRAPHIC REPORT2||||||F|||20150930164100|||\r"
            + "OBX|2|TX|||NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH 3||||||F|||20150930164100|||\r"
            + "PRB|CE|20060315076|lung  new issue|53693||2||200603150625\r"
            + "ORC|NW|1009^OE|9999998^RX|||E|^Q6H^D10^^^R\r"
            + "OBR|1|CD150920001337|CD150920001337|||20150930000000|20150930164100|||||||||25055^MARCUSON^PATRICIA^L|||||||||F|||5755^DUNN^CHAD^B~25055^MARCUSON^PATRICIA^L|||WEAKNESS|DAS, SURJYA P||SHIELDS, SHARON A|||||||||\r"
            + "OBX|1|TX|||ECHOCARDIOGRAPHIC REPORT new group||||||F|||20150930164100|||\r";

    String hl7ADTmessage = "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|ADT^A08^ADT_A08|1|P^I|2.6||||||ASCII||\r"
            + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson||19600614|M||C|99 Oakland #106^^qwerty^OH^44889||^^^^^626^5641111|^^^^^626^5647654|||||343132266|||N\r"
            + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
            + "NTE|1|P|Problem Comments\r" + "VAR|varid1|200603150610\r"
            + "OBX|1|TX|||ECHOCARDIOGRAPHIC REPORT||||||F|||20150930164100|||\r"
            + "OBX|2|TX|||NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH||||||F|||20150930164100|||\r";

    // Tests for extracting segment from group
    @Test
    void test_single_order_group() throws HL7Exception {

        Message hl7message = getMessage(messageSingleOrderGroup);
        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
        List<SegmentGroup> segmentGroups = SegmentExtractorUtil.extractSegmentGroups(ORDER_GROUP_LIST,
                "OBX", Lists.newArrayList(), hl7DTE, Lists.newArrayList());
        assertThat(segmentGroups).isNotEmpty().hasSize(2);
        validateEachGroup(hl7DTE, segmentGroups.get(0), 1, 0, ECHOCARDIOGRAPHIC_REPORT);
        validateEachGroup(hl7DTE, segmentGroups.get(1), 1, 0,
                NORMAL_LV_CHAMBER_SIZE_WITH_MILD_CONCENTRIC_LVH);
    }

    @Test
    void test_single_problem_group_wrong_group_list_provided() throws HL7Exception {

        Message hl7message = getMessage(messageSingleProblemGroup);
        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
        List<SegmentGroup> segmentGroups = SegmentExtractorUtil.extractSegmentGroups(ORDER_GROUP_LIST,
                "OBX", Lists.newArrayList(), hl7DTE, Lists.newArrayList());
        assertThat(segmentGroups).isEmpty();

    }

    @Test
    void test_single_problem_group() throws HL7Exception {

        Message hl7message = getMessage(messageSingleProblemGroup);
        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
        List<SegmentGroup> segmentGroups = SegmentExtractorUtil.extractSegmentGroups(PROBLEM_GROUP_LIST,
                "OBX", Lists.newArrayList(), hl7DTE, Lists.newArrayList());
        assertThat(segmentGroups).isNotEmpty().hasSize(2);
        validateEachGroup(hl7DTE, segmentGroups.get(0), 1, 0, ECHOCARDIOGRAPHIC_REPORT);
        validateEachGroup(hl7DTE, segmentGroups.get(1), 1, 0,
                NORMAL_LV_CHAMBER_SIZE_WITH_MILD_CONCENTRIC_LVH);
    }

    @Test
    void test_parent_repeat() throws HL7Exception {
        Message hl7message = getMessage(messageRepeat);
        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
        List<SegmentGroup> segmentGroups = SegmentExtractorUtil.extractSegmentGroups(ORDER_GROUP_LIST,
                "OBX", Lists.newArrayList(), hl7DTE, Lists.newArrayList());
        assertThat(segmentGroups).isNotEmpty().hasSize(4);

        validateEachGroup(hl7DTE, segmentGroups.get(0), 1, 0, ECHOCARDIOGRAPHIC_REPORT);
        validateEachGroup(hl7DTE, segmentGroups.get(1), 1, 0,
                NORMAL_LV_CHAMBER_SIZE_WITH_MILD_CONCENTRIC_LVH);
        validateEachGroup(hl7DTE, segmentGroups.get(2), 1, 0, "ECHOCARDIOGRAPHIC REPORT2");
        validateEachGroup(hl7DTE, segmentGroups.get(3), 1, 0,
                "NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH 3");
    }

    @Test
    void test_parent_repeat_additional_segment_under_parent() throws HL7Exception {
        Message hl7message = getMessage(messageRepeat);
        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
        List<SegmentGroup> segmentGroups = SegmentExtractorUtil.extractSegmentGroups(ORDER_GROUP_LIST,
                "OBX", Lists.newArrayList(new HL7Segment("PID")), hl7DTE, Lists.newArrayList());
        assertThat(segmentGroups).isNotEmpty().hasSize(4);

        validateEachGroupAdditionalSegment(hl7DTE, segmentGroups.get(0), 1, 1, ECHOCARDIOGRAPHIC_REPORT,
                "PID");
        validateEachGroupAdditionalSegment(hl7DTE, segmentGroups.get(1), 1, 1,
                NORMAL_LV_CHAMBER_SIZE_WITH_MILD_CONCENTRIC_LVH, "PID");
        validateEachGroupAdditionalSegment(hl7DTE, segmentGroups.get(2), 1, 1,
                "ECHOCARDIOGRAPHIC REPORT2", "PID");
        validateEachGroupAdditionalSegment(hl7DTE, segmentGroups.get(3), 1, 1,
                "NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH 3", "PID");

    }

    @Test
    void test_parent_repeat_additional_segment_under_group() throws HL7Exception {
        Message hl7message = getMessage(messageRepeat);
        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
        List<SegmentGroup> segmentGroups = SegmentExtractorUtil.extractSegmentGroups(ORDER_GROUP_LIST,
                "OBX", Lists.newArrayList(new HL7Segment(ORDER_GROUP_LIST, "NTE", true)), hl7DTE,
                Lists.newArrayList());
        assertThat(segmentGroups).isNotEmpty().hasSize(4);

        validateEachGroupAdditionalSegment(hl7DTE, segmentGroups.get(0), 1, 1, ECHOCARDIOGRAPHIC_REPORT,
                "NTE");
        validateEachGroupAdditionalSegment(hl7DTE, segmentGroups.get(1), 1, 1,
                NORMAL_LV_CHAMBER_SIZE_WITH_MILD_CONCENTRIC_LVH, "NTE");
        validateEachGroup(hl7DTE, segmentGroups.get(2), 1, 0, "ECHOCARDIOGRAPHIC REPORT2");
        validateEachGroup(hl7DTE, segmentGroups.get(3), 1, 0,
                "NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH 3");

    }

    @Test
    void test_parent_repeat_additional_segment_under_group_with_group_name()
            throws HL7Exception {
        Message hl7message = getMessage(messageRepeatMultiplePRB);
        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
        List<SegmentGroup> segmentGroups = SegmentExtractorUtil.extractSegmentGroups(ORDER_GROUP_LIST,
                "OBX", Lists.newArrayList(new HL7Segment(ORDER_GROUP_LIST, "NTE", true)), hl7DTE,
                Lists.newArrayList("PROBLEM"));
        assertThat(segmentGroups).isNotEmpty().hasSize(5);

        validateEachGroupAdditionalSegment(hl7DTE, segmentGroups.get(0), 1, 1, ECHOCARDIOGRAPHIC_REPORT,
                "NTE");
        validateEachGroupAdditionalSegment(hl7DTE, segmentGroups.get(1), 1, 1,
                NORMAL_LV_CHAMBER_SIZE_WITH_MILD_CONCENTRIC_LVH, "NTE");
        validateEachGroup(hl7DTE, segmentGroups.get(2), 1, 0, "ECHOCARDIOGRAPHIC REPORT2");
        validateEachGroup(hl7DTE, segmentGroups.get(3), 1, 0,
                "NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH 3");

        validateEachGroup(hl7DTE, segmentGroups.get(4), 1, 0, "ECHOCARDIOGRAPHIC REPORT new group");
        List<String> sameGroup = Lists.newArrayList(segmentGroups.get(0).getGroupId(),
                segmentGroups.get(1).getGroupId(),
                segmentGroups.get(2).getGroupId(), segmentGroups.get(3).getGroupId());
        assertThat(sameGroup).containsOnly(segmentGroups.get(0).getGroupId());
        assertThat(segmentGroups.get(4).getGroupId()).isNotEqualTo(segmentGroups.get(0).getGroupId());
    }

    @Test
    void test_repeating_primary_segment_with_repeating_parent_group()
            throws HL7Exception {
        String message = "MSH|^~\\\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|ORU^R01|MSGID000005|T|2.6\r"
                + "PID||45483|45483||SMITH^SUZIE^||20160813|M|||123 MAIN STREET^^SCHENECTADY^NY^12345||(123)456-7890|||||^^^T||||||||||||\r"
                + "OBR|1||986^IA PHIMS Stage^2.16.840.1.114222.4.3.3.5.1.2^ISO|112^Final Echocardiogram Report|||20151009173644|||||||||||||002|||||F|||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^||||3068^JOHN^Paul^J|\r"
                + "OBX|1|ST|TS-F-01-007^Endocrine Disorders 7^L||obs report||||||F\r"
                + "OBX|2|ST|TS-F-01-008^Endocrine Disorders 8^L||ECHOCARDIOGRAPHIC REPORT||||||F\r"
                + "OBR|1||98^IA PHIMS Stage^2.16.840.1.114222.4.3.3.5.1.2^ISO|113^Echocardiogram Report|||20151009173644|||||||||||||002|||||F|||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^||||3065^Mahoney^Paul^J|\r"
                + "OBX|1|CWE|625-4^Bacteria identified in Stool by Culture^LN^^^^2.33^^result1|1|27268008^Salmonella^SCT^^^^20090731^^Salmonella species|||A^A^HL70078^^^^2.5|||P|||20120301|||^^^^^^^^Bacterial Culture||201203140957||||||\r"
                + "OBX|2|ST|TS-F-01-002^Endocrine Disorders^L||ECHOCARDIOGRAPHIC REPORT Group 2||||||F\r";

        Message hl7message = getMessage(message);
        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
        List<String> orderGroupList = Lists.newArrayList("PATIENT_RESULT", "ORDER_OBSERVATION");
        List<String> observationGroupList = Lists.newArrayList("PATIENT_RESULT", "ORDER_OBSERVATION", "OBSERVATION");
        List<String> specimenGroupList = Lists.newArrayList("PATIENT_RESULT", "ORDER_OBSERVATION", "SPECIMEN");
        List<HL7Segment> additionalSegments = Lists.newArrayList(
                new HL7Segment(orderGroupList, "OBR", true),
                new HL7Segment(observationGroupList, "NTE", true),
                new HL7Segment(specimenGroupList, "SPM", true),
                new HL7Segment(orderGroupList, "MSH", false));
        List<SegmentGroup> segmentGroups = SegmentExtractorUtil.extractSegmentGroups(observationGroupList,
                "OBX", additionalSegments, hl7DTE,
                Lists.newArrayList("PATIENT_RESULT", "ORDER_OBSERVATION"));
        assertThat(segmentGroups).isNotEmpty().hasSize(4);

        List<String> firstGroupIds = Lists.newArrayList(segmentGroups.get(0).getGroupId(),
                segmentGroups.get(1).getGroupId());
        List<String> secondGroupIds = Lists.newArrayList(segmentGroups.get(2).getGroupId(),
                segmentGroups.get(3).getGroupId());
        // The first two OBX should have the same group ID
        assertThat(firstGroupIds).containsOnly(firstGroupIds.get(0));
        // The second two OBX should have the same group ID
        assertThat(secondGroupIds).containsOnly(secondGroupIds.get(0));
        // The parent should be the ORDER_OBSERVATION group
        assertThat(firstGroupIds.get(0)).contains("ORDER_OBSERVATION");
        assertThat(secondGroupIds.get(0)).contains("ORDER_OBSERVATION");
        // The first group of OBX should have a different ID than the second group of OBX.
        assertThat(firstGroupIds.get(0)).isNotEqualTo(secondGroupIds.get(0));
    }

    // Test for extracting segments outside of group

    @Test
    void test_get_segments() throws HL7Exception {

        Message hl7message = getMessage(hl7ADTmessage);
        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
        List<SegmentGroup> segmentGroups = SegmentExtractorUtil.extractSegmentNonGroups("OBX", new ArrayList<>(),
                hl7DTE);
        assertThat(segmentGroups).isNotEmpty().hasSize(1);
        assertThat(segmentGroups.get(0).getSegments()).hasSize(2);

        Segment s = (Segment) segmentGroups.get(0).getSegments().get(0);
        ParsingResult<Type> type = hl7DTE.getType(s, 5, 0);
        assertThat(Hl7DataHandlerUtil.getStringValue(type.getValue()))
                .isEqualTo(ECHOCARDIOGRAPHIC_REPORT);
        assertThat(segmentGroups.get(0).getAdditionalSegments()).isEmpty();

        s = (Segment) segmentGroups.get(0).getSegments().get(1);
        type = hl7DTE.getType(s, 5, 0);
        assertThat(Hl7DataHandlerUtil.getStringValue(type.getValue()))
                .isEqualTo(NORMAL_LV_CHAMBER_SIZE_WITH_MILD_CONCENTRIC_LVH);

    }

    @Test
    void test_single_segment() throws HL7Exception {

        Message hl7message = getMessage(hl7ADTmessage);
        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
        SegmentGroup segmentGroup = SegmentExtractorUtil.extractSegmentNonGroups("OBX", new ArrayList<>(), hl7DTE)
                .get(0);
        assertThat(segmentGroup).isNotNull();
        Segment s = (Segment) segmentGroup.getSegments().get(0);
        ParsingResult<Type> type = hl7DTE.getType(s, 5, 0);
        assertThat(Hl7DataHandlerUtil.getStringValue(type.getValue()))
                .isEqualTo(ECHOCARDIOGRAPHIC_REPORT);
        assertThat(segmentGroup.getAdditionalSegments()).isEmpty();

    }

    @Test
    void test_single_segment_with_additional_segment() throws HL7Exception {

        Message hl7message = getMessage(hl7ADTmessage);
        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
        SegmentGroup segmentGroup = SegmentExtractorUtil
                .extractSegmentNonGroups("OBX", Lists.newArrayList(new HL7Segment("PV1")), hl7DTE).get(0);
        assertThat(segmentGroup).isNotNull();
        Segment s = (Segment) segmentGroup.getSegments().get(0);
        ParsingResult<Type> type = hl7DTE.getType(s, 5, 0);
        assertThat(Hl7DataHandlerUtil.getStringValue(type.getValue()))
                .isEqualTo(ECHOCARDIOGRAPHIC_REPORT);
        assertThat(segmentGroup.getAdditionalSegments()).hasSize(1);

        Segment pv1 = (Segment) segmentGroup.getAdditionalSegments().get("PV1").get(0);
        assertThat(pv1.isEmpty()).isFalse();
        assertThat(pv1.getName()).isEqualTo("PV1");

    }

    @Test
    void test_child_segment_with_additional_parent_segment() throws HL7Exception {
        String message = "MSH|^~\\\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|ORU^R01|MSGID000005|T|2.6\r"
                + "PID||45483|45483||SMITH^SUZIE^||20160813|M|||123 MAIN STREET^^SCHENECTADY^NY^12345||(123)456-7890|||||^^^T||||||||||||\r"
                + "OBR|1||986^IA PHIMS Stage^2.16.840.1.114222.4.3.3.5.1.2^ISO|112^Final Echocardiogram Report|||20151009173644|||||||||||||002|||||F|||2740^Tsadok^Janetary~2913^Merrit^Darren^F~3065^Mahoney^Paul^J~4723^Loh^Robert^L~9052^Winter^Oscar^||||3065^Mahoney^Paul^J|\r"
                + "OBX|1|TX|TS-F-01-002^Endocrine Disorders^L||obs report||||||F\r"
                + "OBX|2|TX|GA-F-01-024^Galactosemia^L||ECHOCARDIOGRAPHIC REPORT||||||F\r";

        List<String> ORDER_GROUP_LIST = Lists.newArrayList("PATIENT_RESULT", "ORDER_OBSERVATION", "OBSERVATION");
        Message hl7message = getMessage(message);
        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
        List<SegmentGroup> segmentGroups = SegmentExtractorUtil.extractSegmentGroups(ORDER_GROUP_LIST,
                "OBX",
                Lists.newArrayList(
                        new HL7Segment(Lists.newArrayList("PATIENT_RESULT", "ORDER_OBSERVATION"), "OBR", true)),
                hl7DTE,
                Lists.newArrayList("PATIENT_RESULT", "ORDER_OBSERVATION"));

        assertThat(segmentGroups).isNotNull().hasSize(2);

        Segment obr = (Segment) segmentGroups.get(0).getAdditionalSegments().get("OBR").get(0);
        assertThat(obr.isEmpty()).isFalse();
        assertThat(obr.getName()).isEqualTo("OBR");

    }

    @Test
    void test_VUX_no_rep() throws HL7Exception {
        String hl7VUXmessageNoRep = "MSH|^~\\&|ImmunizationGenerator^1.4|^2|||20121217134645||VXU^V04^VXU_V04|64443|P^|2.5.1^^^^^^^^^^^^|||ER||||\r"
                + "PID|||1234^^^^PI^||HASBRO^ANDY^JOHN^^^^L^|SLINKY^FUN^^^^^L^|20010606|M|||1564 MONROE^^BROOKLYN^HI^56808^^^^^^|||||||||||||||||||\r"
                + "PD1|||||||||||01|N||||A\r"
                + "NK1|1|HASBRO^ANDY^JOHN^^^^L^|SEL^SELF^HL70063^^^|1564 MONROE^^BROOKLYN^HI^13808^^^^^^|||||||||||||||||||||||||||||||||\r"
                + "IN1|1|G54321^Insurance plan^072|47055^^^NAIC^NIIP|||||||||20120101|20121231|||||||||||||||||||||||POL55555|\r"
                + "ORC|RE||2^DCS|||||||||||||||||||||||||R\r"
                + "RXA|0|1|20130124|20130124|10^Polio-Inject^CVX^90713^Polio-Inject^CPT|1.0|||00||||||12345||PMC\r"
                + "RXR|IM\r"
                + "OBX|1|CE|30945-0^Contraindication^LN||21^acute illness^NIP^^^|||||||F| \r";
        ArrayList<String> ORDER_GROUP_VUX = Lists.newArrayList("ORDER");
        ArrayList<String> OBSERVATION_GROUP_VUX = Lists.newArrayList("ORDER", "OBSERVATION");
        Message hl7message = getMessage(hl7VUXmessageNoRep);
        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
        List<SegmentGroup> segmentGroups = SegmentExtractorUtil.extractSegmentGroups(ORDER_GROUP_VUX,
                "RXA", Lists.newArrayList(new HL7Segment(OBSERVATION_GROUP_VUX, "OBX", true)), hl7DTE,
                ORDER_GROUP_VUX);
        SegmentGroup segmentGroup = segmentGroups.get(0);
        assertThat(segmentGroup).isNotNull();
        Segment s = (Segment) segmentGroup.getSegments().get(0);
        ParsingResult<Type> type = hl7DTE.getType(s, 4, 0);
        assertThat(Hl7DataHandlerUtil.getStringValue(type.getValue())).isEqualTo("20130124");
        assertThat(segmentGroup.getAdditionalSegments()).hasSize(1);
        Segment obx = (Segment) segmentGroup.getAdditionalSegments().get("OBX").get(0);

        ParsingResult<Type> obx3 = hl7DTE.getType(obx, 3, 0);
        assertThat(Hl7DataHandlerUtil.getStringValue(obx3.getValue())).isEqualTo("30945-0");

    }

    @Test
    void test_VUX_rep() throws HL7Exception {
        String hl7VUXmessageRep = "MSH|^~\\&|MYEHR2.5|RI88140101|KIDSNET_IFL|RIHEALTH|20130531||VXU^V04^VXU_V04|20130531RI881401010105|P|2.5.1|||NE|AL||||||RI543763\r"
                + "PID|1||432155^^^^MR||Patient^Johnny^New^^^^L|Smith^Sally|20130414|M||2106-3^White^HL70005|123 Any St^^Somewhere^WI^54000^^M\r"
                + "NK1|1|Patient^Sally|MTH^mother^HL70063|123 Any St^^Somewhere^WI^54000^^M|^PRN^PH^^^608^5551212|||||||||||19820517||||eng^English^ISO639\r"
                + "ORC|RE||197023|||||||^Clerk^Myron|||||||RI2050\r"
                + "RXA|0|1|20130415|20130415|31^Hep B Peds NOS^CVX|999|||01^historical record^NIP001||||||||\r"
                + "ORC|RE||197027|||||||^Clerk^Myron||MD67895^Pediatric^MARY^^^^MD^^RIA|||||RI2050\r"
                + "RXA|0|1|20130531|20130531|48^HIB PRP-T^CVX|0.5|ML^^ISO+||00^new immunization record^NIP001|^Sticker^Nurse|^^^RI2050||||33k2a|20131210|PMC^sanofi^MVX|||CP|A\r"
                + "RXR|C28161^IM^NCIT^IM^INTRAMUSCULAR^HL70162|RT^right thigh^HL70163\r"
                + "OBX|1|CE|64994-7^vaccine fund pgm elig cat^LN|1|V02^VFC eligible Medicaid/MedicaidManaged Care^HL70064||||||F|||20130531|||VXC40^per imm^CDCPHINVS\r"
                + "OBX|2|CE|30956-7^Vaccine Type^LN|2|48^HIB PRP-T^CVX||||||F|||20130531\r"
                + "OBX|3|TS|29768-9^VIS Publication Date^LN|2|19981216||||||F|||20130531\r"
                + "OBX|4|TS|29769-7^VIS Presentation Date^LN|2|20130531||||||F|||20130531\r";
        Message hl7message = getMessage(hl7VUXmessageRep);
        HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
        ArrayList<String> ORDER_GROUP_VUX = Lists.newArrayList("ORDER");
        ArrayList<String> OBSERVATION_GROUP_VUX = Lists.newArrayList("ORDER", "OBSERVATION");
        List<SegmentGroup> segmentGroups = SegmentExtractorUtil
                .extractSegmentGroups(ORDER_GROUP_VUX, "RXA",
                        Lists.newArrayList(new HL7Segment(OBSERVATION_GROUP_VUX, "OBX", true),
                                new HL7Segment(ORDER_GROUP_VUX, "RXR", true)),
                        hl7DTE, Lists.newArrayList("ORDER"));
        assertThat(segmentGroups).hasSize(2);
        SegmentGroup segmentGroup1 = segmentGroups.get(0);
        assertThat(segmentGroup1).isNotNull();
        Segment s = (Segment) segmentGroup1.getSegments().get(0);
        ParsingResult<Type> type = hl7DTE.getType(s, 4, 0);
        assertThat(Hl7DataHandlerUtil.getStringValue(type.getValue())).isEqualTo("20130415");
        assertThat(segmentGroup1.getAdditionalSegments()).isEmpty();

        SegmentGroup segmentGroup2 = segmentGroups.get(1);

        assertThat(segmentGroup2).isNotNull();
        Segment s2 = (Segment) segmentGroup2.getSegments().get(0);
        ParsingResult<Type> type2 = hl7DTE.getType(s2, 4, 0);
        assertThat(Hl7DataHandlerUtil.getStringValue(type2.getValue())).isEqualTo("20130531");
        assertThat(segmentGroup2.getAdditionalSegments()).hasSize(2);
        List<Structure> obxs = segmentGroup2.getAdditionalSegments().get("OBX");
        assertThat(obxs).hasSize(4);

        ParsingResult<Type> obx3 = hl7DTE.getType((Segment) obxs.get(0), 3, 0);
        assertThat(Hl7DataHandlerUtil.getStringValue(obx3.getValue())).isEqualTo("64994-7");

        Segment rxr = (Segment) segmentGroup2.getAdditionalSegments().get("RXR").get(0);

        ParsingResult<Type> rxr1 = hl7DTE.getType(rxr, 1, 0);
        assertThat(Hl7DataHandlerUtil.getStringValue(rxr1.getValue())).isEqualTo("C28161");

    }

    private static void validateEachGroupAdditionalSegment(HL7DataExtractor hl7DTE,
            SegmentGroup segmentGroup, int noOfSegments, int noOfAddSegments, String matchValue,
            String additionalSegmentName) throws HL7Exception {
        assertThat(segmentGroup.getSegments()).hasSize(noOfSegments);
        assertThat(segmentGroup.getSegments().get(0).isEmpty()).isFalse();
        Segment s = (Segment) segmentGroup.getSegments().get(0);
        ParsingResult<Type> type = hl7DTE.getType(s, 5, 0);
        assertThat(Hl7DataHandlerUtil.getStringValue(type.getValue())).isEqualTo(matchValue);
        assertThat(segmentGroup.getAdditionalSegments()).hasSize(noOfAddSegments);

        Segment pv1 = (Segment) segmentGroup.getAdditionalSegments().get(additionalSegmentName).get(0);
        assertThat(pv1.isEmpty()).isFalse();
        assertThat(pv1.getName()).isEqualTo(additionalSegmentName);
    }

    private static void validateEachGroup(HL7DataExtractor hl7DTE, SegmentGroup segmentGroup,
            int noOfSegments, int noOfAddSegments, String matchValue) throws HL7Exception {
        assertThat(segmentGroup.getSegments()).hasSize(noOfSegments);
        assertThat(segmentGroup.getSegments().get(0).isEmpty()).isFalse();
        Segment s = (Segment) segmentGroup.getSegments().get(0);
        ParsingResult<Type> type = hl7DTE.getType(s, 5, 0);
        assertThat(Hl7DataHandlerUtil.getStringValue(type.getValue())).isEqualTo(matchValue);
        assertThat(segmentGroup.getAdditionalSegments()).hasSize(noOfAddSegments);
    }

    private static Message getMessage(String message) {
        HL7HapiParser hparser = null;

        try {
            hparser = new HL7HapiParser();
            return hparser.getParser().parse(message);
        } catch (HL7Exception e) {
            throw new IllegalArgumentException(e);
        } finally {
            if (hparser != null) {
                try {
                    hparser.getContext().close();
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

    }

}
