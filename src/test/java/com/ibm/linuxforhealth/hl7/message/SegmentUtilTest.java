/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.linuxforhealth.hl7.message;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import com.google.common.collect.Lists;
import com.ibm.linuxforhealth.hl7.data.Hl7DataHandlerUtil;
import com.ibm.linuxforhealth.hl7.message.util.SegmentExtractorUtil;
import com.ibm.linuxforhealth.hl7.message.util.SegmentGroup;
import com.ibm.linuxforhealth.hl7.parsing.HL7DataExtractor;
import com.ibm.linuxforhealth.hl7.parsing.HL7HapiParser;
import com.ibm.linuxforhealth.hl7.parsing.result.ParsingResult;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.Type;

public class SegmentUtilTest {
  private static final String NORMAL_LV_CHAMBER_SIZE_WITH_MILD_CONCENTRIC_LVH =
      "NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH";
  private static final ArrayList<String> ORDER_GROUP_LIST = Lists.newArrayList("PROBLEM", "ORDER", "ORDER_DETAIL", "ORDER_OBSERVATION");
  private static final ArrayList<String> PROBLEM_GROUP_LIST =
      Lists.newArrayList("PROBLEM", "PROBLEM_OBSERVATION");

  private static final String ECHOCARDIOGRAPHIC_REPORT = "ECHOCARDIOGRAPHIC REPORT";

  private String messageSingleOrderGroup =
      "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\r"
          + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson||19600614|M||C|99 Oakland #106^^qwerty^OH^44889||^^^^^626^5641111|^^^^^626^5647654|||||343132266|||N\r"
          + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
          + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"
          + "NTE|1|P|Problem Comments\r" + "VAR|varid1|200603150610\r"
          + "ORC|NW|1000^OE|9999999^RX|||E|^Q6H^D10^^^R\r"
          + "OBR|1|CD150920001336|CD150920001336|||20150930000000|20150930164100|||||||||25055^MARCUSON^PATRICIA^L|||||||||F|||5755^DUNN^CHAD^B~25055^MARCUSON^PATRICIA^L|||WEAKNESS|DAS, SURJYA P||SHIELDS, SHARON A|||||||||\r"
          + "OBX|1|TX|||ECHOCARDIOGRAPHIC REPORT||||||F|||20150930164100|||\r"
          + "OBX|2|TX|||NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH||||||F|||20150930164100|||\r";

  private String messageSingleProblemGroup =
      "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\r"
          + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson||19600614|M||C|99 Oakland #106^^qwerty^OH^44889||^^^^^626^5641111|^^^^^626^5647654|||||343132266|||N\r"
          + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
          + "PRB|AD|200603150625|aortic stenosis|53692||2||200603150625\r"
          + "NTE|1|P|Problem Comments\r" + "VAR|varid1|200603150610\r"
          + "OBX|1|TX|||ECHOCARDIOGRAPHIC REPORT||||||F|||20150930164100|||\r"
          + "OBX|2|TX|||NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH||||||F|||20150930164100|||\r";

  private String messageRepeat =
      "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\r"
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

  private String messageRepeatMultiplePRB =
      "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|PPR^PC1^PPR_PC1|1|P^I|2.6||||||ASCII||\r"
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



  String hl7ADTmessage =
      "MSH|^~\\&|SendTest1|Sendfac1|Receiveapp1|Receivefac1|200603081747|security|ADT^A08^ADT_A08|1|P^I|2.6||||||ASCII||\r"
          + "PID|||555444222111^^^MPI&GenHosp&L^MR||james^anderson||19600614|M||C|99 Oakland #106^^qwerty^OH^44889||^^^^^626^5641111|^^^^^626^5647654|||||343132266|||N\r"
          + "PV1||I|6N^1234^A^GENHOS||||0100^ANDERSON^CARL|0148^ADDISON^JAMES||SUR|||||||0148^ANDERSON^CARL|S|1400|A|||||||||||||||||||SF|K||||199501102300\r"
          + "NTE|1|P|Problem Comments\r" + "VAR|varid1|200603150610\r"
          + "OBX|1|TX|||ECHOCARDIOGRAPHIC REPORT||||||F|||20150930164100|||\r"
          + "OBX|2|TX|||NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH||||||F|||20150930164100|||\r";
  // Tests for extracting segment from group
  @Test
  public void test_single_order_group() throws HL7Exception {


    Message hl7message = getMessage(messageSingleOrderGroup);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
    List<SegmentGroup> segmentGroups = SegmentExtractorUtil.extractSegmentGroups(
        ORDER_GROUP_LIST, "OBX",
        new ArrayList<>(), hl7DTE);
    assertThat(segmentGroups).isNotEmpty();
    assertThat(segmentGroups).hasSize(2);
    validateEachGroup(hl7DTE, segmentGroups.get(0), 1, 0, ECHOCARDIOGRAPHIC_REPORT);
    validateEachGroup(hl7DTE, segmentGroups.get(1), 1, 0,
        NORMAL_LV_CHAMBER_SIZE_WITH_MILD_CONCENTRIC_LVH);
  }

  @Test
  public void test_single_problem_group_wrong_group_list_provided() throws HL7Exception {


    Message hl7message = getMessage(messageSingleProblemGroup);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
    List<SegmentGroup> segmentGroups = SegmentExtractorUtil.extractSegmentGroups(ORDER_GROUP_LIST,
        "OBX", new ArrayList<>(), hl7DTE);
    assertThat(segmentGroups).isEmpty();

  }


  @Test
  public void test_single_problem_group() throws HL7Exception {


    Message hl7message = getMessage(messageSingleProblemGroup);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
    List<SegmentGroup> segmentGroups = SegmentExtractorUtil.extractSegmentGroups(PROBLEM_GROUP_LIST,
        "OBX", new ArrayList<>(), hl7DTE);
    assertThat(segmentGroups).isNotEmpty();
    assertThat(segmentGroups).hasSize(2);
    validateEachGroup(hl7DTE, segmentGroups.get(0), 1, 0, ECHOCARDIOGRAPHIC_REPORT);
    validateEachGroup(hl7DTE, segmentGroups.get(1), 1, 0,
        NORMAL_LV_CHAMBER_SIZE_WITH_MILD_CONCENTRIC_LVH);
  }




  @Test
  public void test_parent_repeat() throws HL7Exception {
    Message hl7message = getMessage(messageRepeat);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
    List<SegmentGroup> segmentGroups = SegmentExtractorUtil.extractSegmentGroups(
        ORDER_GROUP_LIST, "OBX",
        new ArrayList<>(), hl7DTE);
    assertThat(segmentGroups).isNotEmpty();
    assertThat(segmentGroups).hasSize(4);

    validateEachGroup(hl7DTE, segmentGroups.get(0), 1, 0, ECHOCARDIOGRAPHIC_REPORT);
    validateEachGroup(hl7DTE, segmentGroups.get(1), 1, 0,
        NORMAL_LV_CHAMBER_SIZE_WITH_MILD_CONCENTRIC_LVH);
    validateEachGroup(hl7DTE, segmentGroups.get(2), 1, 0, "ECHOCARDIOGRAPHIC REPORT2");
    validateEachGroup(hl7DTE, segmentGroups.get(3), 1, 0,
        "NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH 3");
  }



  @Test
  public void test_parent_repeat_additional_segment_under_parent() throws HL7Exception {
    Message hl7message = getMessage(messageRepeat);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
    List<SegmentGroup> segmentGroups = SegmentExtractorUtil.extractSegmentGroups(ORDER_GROUP_LIST,
        "OBX", Lists.newArrayList(new HL7Segment("PID")), hl7DTE);
    assertThat(segmentGroups).isNotEmpty();
    assertThat(segmentGroups).hasSize(4);

    validateEachGroupAdditionalSegment(hl7DTE, segmentGroups.get(0), 1, 1,
        ECHOCARDIOGRAPHIC_REPORT, "PID");
    validateEachGroupAdditionalSegment(hl7DTE, segmentGroups.get(1), 1, 1,
        NORMAL_LV_CHAMBER_SIZE_WITH_MILD_CONCENTRIC_LVH, "PID");
    validateEachGroupAdditionalSegment(hl7DTE, segmentGroups.get(2), 1, 1,
        "ECHOCARDIOGRAPHIC REPORT2", "PID");
    validateEachGroupAdditionalSegment(hl7DTE, segmentGroups.get(3), 1, 1,
        "NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH 3", "PID");

  }


  @Test
  public void test_parent_repeat_additional_segment_under_group() throws HL7Exception {
    Message hl7message = getMessage(messageRepeat);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
    List<SegmentGroup> segmentGroups = SegmentExtractorUtil.extractSegmentGroups(ORDER_GROUP_LIST,
        "OBX", Lists.newArrayList(new HL7Segment("NTE", true)), hl7DTE);
    assertThat(segmentGroups).isNotEmpty();
    assertThat(segmentGroups).hasSize(4);

    validateEachGroupAdditionalSegment(hl7DTE, segmentGroups.get(0), 1, 1, ECHOCARDIOGRAPHIC_REPORT,
        "NTE");
    validateEachGroupAdditionalSegment(hl7DTE, segmentGroups.get(1), 1, 1,
        NORMAL_LV_CHAMBER_SIZE_WITH_MILD_CONCENTRIC_LVH, "NTE");
    validateEachGroup(hl7DTE, segmentGroups.get(2), 1, 0, "ECHOCARDIOGRAPHIC REPORT2");
    validateEachGroup(hl7DTE, segmentGroups.get(3), 1, 0,
        "NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH 3");

  }


  @Test
  public void test_parent_repeat_additional_segment_under_group_with_group_name()
      throws HL7Exception {
    Message hl7message = getMessage(messageRepeatMultiplePRB);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
    List<SegmentGroup> segmentGroups = SegmentExtractorUtil.extractSegmentGroups(ORDER_GROUP_LIST,
        "OBX", Lists.newArrayList(new HL7Segment("NTE", true)), hl7DTE, "PROBLEM");
    assertThat(segmentGroups).isNotEmpty();
    assertThat(segmentGroups).hasSize(5);

    validateEachGroupAdditionalSegment(hl7DTE, segmentGroups.get(0), 1, 1, ECHOCARDIOGRAPHIC_REPORT,
        "NTE");
    validateEachGroupAdditionalSegment(hl7DTE, segmentGroups.get(1), 1, 1,
        NORMAL_LV_CHAMBER_SIZE_WITH_MILD_CONCENTRIC_LVH, "NTE");
    validateEachGroup(hl7DTE, segmentGroups.get(2), 1, 0, "ECHOCARDIOGRAPHIC REPORT2");
    validateEachGroup(hl7DTE, segmentGroups.get(3), 1, 0,
        "NORMAL LV CHAMBER SIZE WITH MILD CONCENTRIC LVH 3");

    validateEachGroup(hl7DTE, segmentGroups.get(4), 1, 0, "ECHOCARDIOGRAPHIC REPORT new group");
    List<String> sameGroup =
        Lists.newArrayList(segmentGroups.get(0).getGroupId(), segmentGroups.get(1).getGroupId(),
            segmentGroups.get(2).getGroupId(), segmentGroups.get(3).getGroupId());
    assertThat(sameGroup).containsOnly(segmentGroups.get(0).getGroupId());
    assertThat(segmentGroups.get(4).getGroupId()).isNotEqualTo(segmentGroups.get(0).getGroupId());
  }



  @Test
  public void test_parent_repeat_get_single_first_group() throws HL7Exception {

    Message hl7message = getMessage(messageRepeat);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
    SegmentGroup segmentGroups = SegmentExtractorUtil.extractSegmentGroup(
        ORDER_GROUP_LIST, "OBX",
        new ArrayList<>(), hl7DTE, "PROBLEM");

    validateEachGroup(hl7DTE, segmentGroups, 1, 0, ECHOCARDIOGRAPHIC_REPORT);

  }



  // Test for extracting segments outside of group

  @Test
  public void test_get_segments() throws HL7Exception {


    Message hl7message = getMessage(hl7ADTmessage);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
    List<SegmentGroup> segmentGroups =
        SegmentExtractorUtil.extractSegmentGroups("OBX", new ArrayList<>(), hl7DTE);
    assertThat(segmentGroups).isNotEmpty();
    assertThat(segmentGroups).hasSize(1);
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
  public void test_single_segment() throws HL7Exception {


    Message hl7message = getMessage(hl7ADTmessage);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
    SegmentGroup segmentGroup =
        SegmentExtractorUtil.extractSegmentGroup("OBX", new ArrayList<>(), hl7DTE);
    assertThat(segmentGroup).isNotNull();
    Segment s = (Segment) segmentGroup.getSegments().get(0);
    ParsingResult<Type> type = hl7DTE.getType(s, 5, 0);
    assertThat(Hl7DataHandlerUtil.getStringValue(type.getValue()))
        .isEqualTo(ECHOCARDIOGRAPHIC_REPORT);
    assertThat(segmentGroup.getAdditionalSegments()).isEmpty();



  }



  @Test
  public void test_single_segment_with_additional_segment() throws HL7Exception {


    Message hl7message = getMessage(hl7ADTmessage);
    HL7DataExtractor hl7DTE = new HL7DataExtractor(hl7message);
    SegmentGroup segmentGroup = SegmentExtractorUtil.extractSegmentGroup("OBX",
        Lists.newArrayList(new HL7Segment("PV1")), hl7DTE);
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


  private static void validateEachGroupAdditionalSegment(HL7DataExtractor hl7DTE,
      SegmentGroup segmentGroup, int noOfSegments, int noOfAddSegments, String matchValue,
      String additionalSegmentName)
      throws HL7Exception {
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
