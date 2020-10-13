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
import java.util.Map.Entry;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import ca.uhn.hl7v2.model.Structure;

public class SegmentGroup {
  private String groupId;
  private List<Structure> segments;
  private Map<String, List<Structure>> additionalSegments;


  public SegmentGroup(Structure segment) {
    this(Lists.newArrayList(segment), new HashMap<>());
  }


  public SegmentGroup(List<Structure> segment, Map<String, List<Structure>> additionalSegments,
      String groupId) {
    Preconditions.checkArgument(segment != null && !segment.isEmpty(),
        "Segment cannot be null or empty");
    Preconditions.checkArgument(additionalSegments != null, "additionalSegments cannot be null");
    this.segments = new ArrayList<>(segment);
    this.additionalSegments = new HashMap<>();
    this.additionalSegments.putAll(additionalSegments);

    this.groupId = groupId;

  }


  public SegmentGroup(List<Structure> segments, Map<String, List<Structure>> additionalSegments) {
    this(segments, additionalSegments, null);
  }


  public List<Structure> getSegments() {
    return ImmutableList.copyOf(segments);
  }


  public Structure getSegment() {
    return segments.get(0);
  }


  public Map<String, List<Structure>> getAdditionalSegments() {
    return ImmutableMap.copyOf(additionalSegments);
  }


  public Map<String, Structure> getAdditionalSegmentsSingleInstance() {
    Map<String, Structure> additionalSegmentsSingleInstance = new HashMap<>();
    if (!this.additionalSegments.isEmpty()) {
      for (Entry<String, List<Structure>> e : additionalSegments.entrySet()) {
        if (e.getValue() != null && !e.getValue().isEmpty()) {
          additionalSegmentsSingleInstance.put(e.getKey(), e.getValue().get(0));
        }
      }
    }
    return additionalSegmentsSingleInstance;
  }


  public String getGroupId() {
    return groupId;
  }


}
