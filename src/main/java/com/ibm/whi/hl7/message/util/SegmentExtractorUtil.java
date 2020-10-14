/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.message.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.whi.hl7.message.HL7Segment;
import com.ibm.whi.hl7.parsing.HL7DataExtractor;
import com.ibm.whi.hl7.parsing.result.ParsingResult;
import ca.uhn.hl7v2.model.Structure;

public class SegmentExtractorUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(HL7DataExtractor.class);

  private SegmentExtractorUtil() {}

  /**
   * Returns list of segments from all the repetitions of the group with a group name included in
   * groupId
   * 
   * @param groups
   * @param segment
   * @param additionalSegments
   * @param dataExtractor
   * @param groupName
   * @return List of {@link SegmentGroup}
   */
  public static List<SegmentGroup> extractSegmentGroups(List<String> groups, String segment,
      List<HL7Segment> additionalSegments, HL7DataExtractor dataExtractor, String groupName) {
    LOGGER.debug("Extracting segment from group {} segment name {}", groups, segment);
    List<Structure> values = getSegments(groups.get(0), dataExtractor);
    List<String> subGroups = groups.subList(1, groups.size());
    List<Structure> structs = getChildStructures(dataExtractor, values, subGroups);
    List<SegmentGroup> returnValues = new ArrayList<>();

    for (Structure s : structs) {

      List<Structure> segments = getSegments(s, segment, dataExtractor);

      if (segments != null && !segments.isEmpty()) {
        Map<String, List<Structure>> additionalSegmentValues =
            extractAdditionalSegmentValue(s, additionalSegments, dataExtractor);
        String groupId = generateGroupId(s, groupName);
        returnValues.add(new SegmentGroup(segments, additionalSegmentValues, groupId));
      }

    }

    return returnValues;
  }

  /**
   * Returns list of segments from all the repetitions of the group
   * 
   * @param groups
   * @param segment
   * @param additionalSegments
   * @param dataExtractor
   * @return @return List of {@link SegmentGroup}
   */
  public static List<SegmentGroup> extractSegmentGroups(List<String> groups, String segment,
      List<HL7Segment> additionalSegments, HL7DataExtractor dataExtractor) {
    return extractSegmentGroups(groups, segment, additionalSegments, dataExtractor, null);
  }


  /**
   * Returns single segment from the group
   * 
   * @param groups
   * @param segment
   * @param additionalSegments
   * @param dataExtractor
   * @return {@link SegmentGroup}
   */

  public static SegmentGroup extractSegmentGroup(List<String> groups, String segment,
      List<HL7Segment> additionalSegments, HL7DataExtractor dataExtractor) {
    return extractSegmentGroup(groups, segment, additionalSegments, dataExtractor, null);
  }

  /**
   * Returns single segment from the group with a group name included in groupId
   * 
   * @param groups
   * @param segment
   * @param additionalSegments
   * @param dataExtractor
   * @param groupName
   * @return {@link SegmentGroup}
   */
  public static SegmentGroup extractSegmentGroup(List<String> groups, String segment,
      List<HL7Segment> additionalSegments, HL7DataExtractor dataExtractor, String groupName) {
    LOGGER.debug("Extracting segment from group {} segment name {}", groups, segment);
    List<Structure> values = getSegments(groups.get(0), dataExtractor);

    List<String> subGroups = groups.subList(1, groups.size());
    SegmentGroup returnValue = null;
    if (values != null && !values.isEmpty()) {

      Structure struct = getFirstChildStructures(dataExtractor, values.get(0), subGroups);

      if (struct != null) {
        String groupId = generateGroupId(struct, groupName);
        List<Structure> segments = getSegments(struct, segment, dataExtractor);

        if (segments != null && !segments.isEmpty()) {
          Map<String, List<Structure>> additionalSegmentValues =
              extractAdditionalSegmentValue(struct, additionalSegments, dataExtractor);
          returnValue = new SegmentGroup(segments, additionalSegmentValues, groupId);
        }

      }
    }
    return returnValue;
  }

  private static Map<String, List<Structure>> extractAdditionalSegmentValue(Structure s,
      List<HL7Segment> additionalSegments, HL7DataExtractor dataExtractor) {
    Map<String, List<Structure>> additionalSegmentValues = new HashMap<>();
    for (HL7Segment seg : additionalSegments) {
      List<Structure> values = null;
      if (seg.isFromGroup()) {
        values = getSegments(s, seg.getSegment(), dataExtractor);

      } else {
        ParsingResult<Structure> result = dataExtractor.getAllStructures(seg.getSegment());

        values = result.getValues();
      }
      if (values != null && !values.isEmpty()) {
        additionalSegmentValues.put(seg.getSegment(), values);
      }
    }
    return additionalSegmentValues;
  }



  private static Map<String, List<Structure>> extractAdditionalSegmentValue(
      List<HL7Segment> additionalSegments, HL7DataExtractor dataExtractor) {
    Map<String, List<Structure>> additionalSegmentValues = new HashMap<>();
    for (HL7Segment seg : additionalSegments) {
      List<Structure> values = null;
      if (seg.isFromGroup()) {
        throw new IllegalStateException(
            "Primary segment is not from a group, so additional segements cannot be from relative group. ");

      } else if (!seg.getGroup().isEmpty()) {
        throw new IllegalStateException(
            "Additional segements cannot be from a group if primary segment is not defined to be from same group. ");

      } else {
        ParsingResult<Structure> result = dataExtractor.getAllStructures(seg.getSegment());
        values = result.getValues();
      }
      if (values != null) {
        additionalSegmentValues.put(seg.getSegment(), values);
      }
    }
    return additionalSegmentValues;
  }



  private static List<Structure> getChildStructures(HL7DataExtractor dataExtractor,
      List<Structure> values, List<String> subGroups) {

    if (subGroups.isEmpty()) {
      return values;
    }


    List<Structure> returnValues = new ArrayList<>();
    for (Structure s : values) {
      List<Structure> strucs = getSegments(s, subGroups.get(0), dataExtractor);

      if (strucs != null) {
        returnValues.addAll(
            getChildStructures(dataExtractor, strucs, subGroups.subList(1, subGroups.size())));
      } else {
        returnValues.addAll(strucs);

      }
    }
    return returnValues;
  }



  private static List<Structure> getSegments(String group, HL7DataExtractor dataExtractor) {
    ParsingResult<Structure> result = dataExtractor.getAllStructures(group);
    return result.getValues();

  }

  private static List<Structure> getSegments(Structure str, String group,
      HL7DataExtractor dataExtractor) {
    ParsingResult<Structure> result = dataExtractor.getAllStructures(str, group);
    return result.getValues();

  }

  /**
   * Returns list of segments from parent segment
   * 
   * @param segment
   * @param additionalSegments
   * @param dataExtractor
   * @return @return List of {@link SegmentGroup}
   */
  public static List<SegmentGroup> extractSegmentGroups(String segment,
      List<HL7Segment> additionalSegments, HL7DataExtractor dataExtractor) {
    LOGGER.debug("Extracting segment name {}", segment);

    List<SegmentGroup> returnValues = new ArrayList<>();

    List<Structure> segments = getSegments(segment, dataExtractor);

    if (segments != null && !segments.isEmpty()) {
      Map<String, List<Structure>> additionalSegmentValues =
          extractAdditionalSegmentValue(additionalSegments, dataExtractor);
      returnValues.add(new SegmentGroup(segments, additionalSegmentValues));
    }



    return returnValues;
  }

  /**
   * Returns a single segment from parent segment
   * 
   * @param segment
   * @param additionalSegments
   * @param dataExtractor
   * @return a {@link SegmentGroup}
   */
  public static SegmentGroup extractSegmentGroup(String segment,
      List<HL7Segment> additionalSegments, HL7DataExtractor dataExtractor) {
    LOGGER.debug("Extracting segment name {}", segment);

    SegmentGroup returnValue = null;

    List<Structure> segments = getSegments(segment, dataExtractor);

    if (segments != null && !segments.isEmpty()) {
      Map<String, List<Structure>> additionalSegmentValues =
          extractAdditionalSegmentValue(additionalSegments, dataExtractor);
      returnValue = new SegmentGroup(segments, additionalSegmentValues);
    }

    return returnValue;

  }




  private static String generateGroupId(Structure struct, String groupName) {
    if (groupName == null) {
      return null;
    }

    boolean parentMatchFound = false;
    Structure parent = struct;
    boolean noMoreParent = false;
    while (!parentMatchFound && !noMoreParent) {

      if (parent != null && StringUtils.endsWith(parent.getName(), groupName)) {
        parentMatchFound = true;
      } else if (parent == null
          || parent.getName().equalsIgnoreCase(parent.getMessage().getName())) {
        noMoreParent = true;
      } else {
        parent = parent.getParent();
      }

    }

    if (parent != null && parentMatchFound) {
      return parent.getName() + "_" + parent.hashCode();

    } else {
      return null;
    }
  }

  private static Structure getFirstChildStructures(HL7DataExtractor dataExtractor, Structure value,
      List<String> subGroups) {

    if (subGroups.isEmpty()) {
      return value;
    }

    Structure returnValue = null;
    List<Structure> strucs;

    strucs = getSegments(value, subGroups.get(0), dataExtractor);

    if (!subGroups.isEmpty()) {
      if (strucs != null && !strucs.isEmpty()) {
        returnValue = getFirstChildStructures(dataExtractor, strucs.get(0),
            subGroups.subList(1, subGroups.size()));
      }
    } else {
      returnValue = strucs.get(0);
    }


    return returnValue;

  }
}
