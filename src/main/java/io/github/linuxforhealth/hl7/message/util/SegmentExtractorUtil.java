/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import ca.uhn.hl7v2.model.Structure;
import io.github.linuxforhealth.hl7.message.HL7Segment;
import io.github.linuxforhealth.hl7.parsing.HL7DataExtractor;
import io.github.linuxforhealth.hl7.parsing.result.ParsingResult;

public class SegmentExtractorUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(HL7DataExtractor.class);

  private SegmentExtractorUtil() {}



  /**
   * Returns list of segments and additional segments without group constraint.
   *
   * @param segment
   * @param additionalSegments
   * @param dataExtractor
   * @return @return List of {@link SegmentGroup}
   */
  public static List<SegmentGroup> extractSegmentNonGroups(String segment,
      List<HL7Segment> additionalSegments, HL7DataExtractor dataExtractor) {
    LOGGER.debug("Extracting segment name {}", segment);

    List<SegmentGroup> returnValues = new ArrayList<>();

    List<Structure> segments = getStructures(segment, dataExtractor);

    if (segments != null && !segments.isEmpty()) {
      Map<String, List<Structure>> additionalSegmentValues =
          extractAdditionalSegmentValueNonGroup(additionalSegments, dataExtractor);
      returnValues.add(new SegmentGroup(segments, additionalSegmentValues));
    }



    return returnValues;
  }



  /**
   * Returns list of segments from all the repetitions of the group with a group name included in
   * groupId
   * 
   * @param primaryGroup
   * @param primarySegment
   * 
   * @param additionalSegments
   * @param dataExtractor
   * @param parentGroup
   * 
   * @return List of {@link SegmentGroup}
   */
  public static List<SegmentGroup> extractSegmentGroups(List<String> primaryGroup,
      String primarySegment, List<HL7Segment> additionalSegments, HL7DataExtractor dataExtractor,
      List<String> parentGroup) {
    Preconditions.checkArgument(primaryGroup != null,
        "Groups list for segment to be extracted cannot be null");
    Preconditions.checkArgument(StringUtils.isNotBlank(primarySegment),
        "primarySegment  cannot be null or blank.");
    LOGGER.debug("Extracting segment from group {} segment name {}", primaryGroup, primarySegment);

    if (parentGroup == null || parentGroup.isEmpty()) {
      List<Structure> parentSegments = getChildStructures(primaryGroup, dataExtractor);
      return generateSegmentGroup(primarySegment, primaryGroup, additionalSegments, dataExtractor,
          parentSegments, primaryGroup);
    } else { // if parentGroup is not null then fetch parent group.

      List<Structure> parentSegments = getChildStructures(parentGroup, dataExtractor);


      return generateSegmentGroup(primarySegment, primaryGroup, additionalSegments, dataExtractor,
          parentSegments, parentGroup);
    }
  }

  private static List<SegmentGroup> generateSegmentGroup(String primarySegment,
      List<String> primaryGroup, List<HL7Segment> additionalSegments,
      HL7DataExtractor dataExtractor, List<Structure> parentSegments,
      List<String> parentGroupUsedForParentSegment) {
    List<SegmentGroup> returnValues = new ArrayList<>();
    List<String> relativePrimaryGroups = null;

    relativePrimaryGroups = new ArrayList<>(primaryGroup);
    relativePrimaryGroups.removeAll(parentGroupUsedForParentSegment);

    for (Structure parent : parentSegments) {

      List<Structure> primarySegments =
          getChildStructures(parent, relativePrimaryGroups, primarySegment, dataExtractor);


      for (Structure primary : primarySegments) {
        // Extract additional structures
        Map<String, List<Structure>> additionalSegmentValues =
            extractAdditionalSegmentValue(primary, primaryGroup, additionalSegments, dataExtractor);


        String groupId = generateGroupId(parent, parentGroupUsedForParentSegment);
        if (primarySegments != null && !primarySegments.isEmpty()) {
          returnValues
              .add(new SegmentGroup(Lists.newArrayList(primary), additionalSegmentValues, groupId));
        }
      }



    }

    return returnValues;
  }



  private static List<Structure> getChildStructures(Structure s, List<String> groups,
      String segment, HL7DataExtractor dataExtractor) {

    if (StringUtils.isBlank(segment)) {
      return new ArrayList<>();
    } else if (groups == null || groups.isEmpty()) {
      return getStructures(s, segment, dataExtractor);
    } else {
      List<Structure> parents = getChildStructures(s, groups, dataExtractor);
      List<Structure> returnValues = new ArrayList<>();
      for (Structure parent : parents) {
        returnValues.addAll(getStructures(parent, segment, dataExtractor));
      }
      return returnValues;
    }

  }



  private static List<Structure> getChildStructures(List<String> parentGroup,
      HL7DataExtractor dataExtractor) {

    if (parentGroup.isEmpty()) {
      return new ArrayList<>();
    } else if (parentGroup.size() == 1) {
      return getStructures(parentGroup.get(0), dataExtractor);
    } else {
      String parent = parentGroup.get(0);
      List<String> subParents = parentGroup.subList(1, parentGroup.size());
      List<Structure> segments = getStructures(parent, dataExtractor);
      List<Structure> results = new ArrayList<>();
      for (Structure s : segments) {
        results.addAll(getChildStructures(s, subParents, dataExtractor));
      }

      return results;
    }



  }


  private static List<Structure> getChildStructures(Structure parentStruct,
      List<String> parentGroup, HL7DataExtractor dataExtractor) {

    if (parentGroup.isEmpty()) {
      return Lists.newArrayList(parentStruct);
    } else if (parentStruct == null) {
      return new ArrayList<>();
    } else if (parentGroup.size() == 1) {
      return getStructures(parentStruct, parentGroup.get(0), dataExtractor);
    } else {
      String parent = parentGroup.get(0);
      List<String> subParents = parentGroup.subList(1, parentGroup.size());
      List<Structure> segments = getStructures(parentStruct, parent, dataExtractor);
      List<Structure> result = new ArrayList<>();
      for (Structure s : segments) {
        result.addAll(getChildStructures(s, subParents, dataExtractor));
      }
      return result;
    }

  }



  private static Map<String, List<Structure>> extractAdditionalSegmentValue(Structure primaryStruct,
      List<String> primaryGroups, List<HL7Segment> additionalSegments,
      HL7DataExtractor dataExtractor) {
    Map<String, List<Structure>> additionalSegmentValues = new HashMap<>();
    for (HL7Segment seg : additionalSegments) {

      List<Structure> values =
          extractEachAdditionalSegment(primaryStruct, primaryGroups, seg, dataExtractor);
      if (values != null && !values.isEmpty()) {
        additionalSegmentValues.put(seg.getSegment(), values);
      }
    }
    return additionalSegmentValues;
  }

  private static List<Structure> extractEachAdditionalSegment(Structure primaryStruct,
      List<String> primaryGroups, HL7Segment seg, HL7DataExtractor dataExtractor) {

    List<Structure> values = null;
    List<String> groups = seg.getGroup();

    if (groups.isEmpty()) {
      values = getStructures(seg.getSegment(), dataExtractor);
    } else if (primaryGroups.isEmpty()) {
      // extract without parent
      List<Structure> parentSegments = getChildStructures(seg.getGroup(), dataExtractor);
      values = new ArrayList<>();
      for (Structure par : parentSegments) {
        values.addAll(getStructures(par, seg.getSegment(), dataExtractor));
      }
    } else if (CollectionUtils.containsAll(primaryGroups, groups)) {
      String commonParentGroup = getCommonParent(groups, primaryGroups);
      Structure commonParent = getParentGroup(primaryStruct, commonParentGroup);

      values = getStructures(commonParent, seg.getSegment(), dataExtractor);
    } else if (getCommonParent(groups, primaryGroups) != null) {

      String commonParentGroup = getCommonParent(groups, primaryGroups);

      Structure commonParent = getParentGroup(primaryStruct, commonParentGroup);
      List<String> relativeGroupsToCommonParent = new ArrayList<>(groups);
      relativeGroupsToCommonParent.removeAll(primaryGroups);

      values = getChildStructures(commonParent, relativeGroupsToCommonParent, seg.getSegment(),
          dataExtractor);

    } else  {
      List<Structure> parentSegments = getChildStructures(seg.getGroup(), dataExtractor);
      values = new ArrayList<>();
      for (Structure par : parentSegments) {
        values.addAll(getStructures(par, seg.getSegment(), dataExtractor));
      }
    }
    return values;


  }



  private static String getCommonParent(List<String> groups, List<String> primaryGroups) {
    List<String> common =
        groups.stream().filter(primaryGroups::contains).collect(Collectors.toList());
    if (!common.isEmpty()) {
      return common.get(common.size() - 1);
    }
    return null;
  }


  private static Map<String, List<Structure>> extractAdditionalSegmentValueNonGroup(
      List<HL7Segment> additionalSegments, HL7DataExtractor dataExtractor) {
    Map<String, List<Structure>> additionalSegmentValues = new HashMap<>();
    for (HL7Segment seg : additionalSegments) {
      List<Structure> values = null;
      if (seg.isFromGroup()) {
        throw new IllegalStateException(
            "Primary segment is not from a group, so additional segements cannot be from relative group.   ");

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



  // HL7 segments extraction
  private static List<Structure> getStructures(String seg, HL7DataExtractor dataExtractor) {
    ParsingResult<Structure> segments = dataExtractor.getAllStructures(seg);
    if (segments == null || segments.isEmpty()) {
      return new ArrayList<>();
    } else {
      return segments.getValues();
    }

  }

  private static List<Structure> getStructures(Structure parent, String segment,
      HL7DataExtractor dataExtractor) {
    ParsingResult<Structure> segments = dataExtractor.getAllStructures(parent, segment);
    if (segments == null || segments.isEmpty()) {
      return new ArrayList<>();
    } else {
      return segments.getValues();
    }

  }



  private static String generateGroupId(Structure struct, List<String> groups) {

    Structure parent = getParentGroup(struct, groups);


    if (parent != null) {
      return parent.getName() + "_" + parent.hashCode();

    } else {
      return null;
    }
  }

  private static Structure getParentGroup(Structure struct, List<String> groups) {
    if (groups == null || groups.isEmpty()) {
      return null;
    }

    List<String> reversedGroups = new ArrayList<>(groups);
    Collections.reverse(reversedGroups);

    Structure parent = struct;

    for (String eachSeg : reversedGroups) {
      parent = getParentGroup(parent, eachSeg);
    }

    if (parent != null) {
      return parent;

    } else {
      return null;
    }
  }

  private static Structure getParentGroup(Structure struct, String group) {
    boolean parentMatchFound = false;
    Structure parent = struct;
    boolean noMoreParent = false;
    while (!parentMatchFound && !noMoreParent) {

      if (parent != null && StringUtils.endsWith(parent.getName(), group)) {
        parentMatchFound = true;
      } else if (parent == null
          || parent.getName().equalsIgnoreCase(parent.getMessage().getName())) {
        noMoreParent = true;
      } else {
        parent = parent.getParent();
      }

    }

    if (parentMatchFound) {
      return parent;
    } else {
      return null;
    }

  }



}
