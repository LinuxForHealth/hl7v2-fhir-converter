/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringTokenizer;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;


/**
 * Represents HL7 Segment.
 * 
 *
 * @author pbhallam
 */

public class HL7Segment {

  private static final String SEGMENT_CANNOT_BE_NULL_OR_EMPTY = "Segment cannot be null or empty";
  private static final String DEFAULT_GROUP = null;
  private List<String> group;
  private String segment;
  private boolean fromGroup;
  

  public HL7Segment(String group, String segment) {
    Preconditions.checkArgument(StringUtils.isNotEmpty(segment), SEGMENT_CANNOT_BE_NULL_OR_EMPTY);
    this.segment = segment;
    this.group = new ArrayList<>();
    if (StringUtils.isNotBlank(group)) {
      this.group.add(group);
    }
  }

  public HL7Segment(List<String> group, String segment) {
    Preconditions.checkArgument(StringUtils.isNotEmpty(segment), SEGMENT_CANNOT_BE_NULL_OR_EMPTY);
    this.segment = segment;
    this.group = new ArrayList<>();
    if (group != null) {
      this.group.addAll(group);

    }
  }

  public HL7Segment(String segment, boolean fromGroup) {
    Preconditions.checkArgument(StringUtils.isNotEmpty(segment), SEGMENT_CANNOT_BE_NULL_OR_EMPTY);
    this.segment = segment;
    this.group = new ArrayList<>();
    this.fromGroup = fromGroup;
  }

  public HL7Segment(String segment) {
    this(DEFAULT_GROUP, segment);

  }

  public String getSegment() {
    return segment;
  }



  public List<String> getGroup() {
    return ImmutableList.copyOf(group);
  }



  public static HL7Segment parse(String rawSegment) {
    if (StringUtils.startsWith(rawSegment, ".")) {
      String segment = StringUtils.removeStart(rawSegment, ".");
      return new HL7Segment(segment, true);
    } else {
    StringTokenizer stk = new StringTokenizer(rawSegment, ".");
    String segment = null;
    List<String> group = new ArrayList<>();
    List<String> tokens = stk.getTokenList();

    if (tokens.size() > 1) {
      group.addAll(tokens.subList(0, tokens.size() - 1));
      segment = tokens.get(tokens.size() - 1);
    } else if (tokens.size() == 1) {
      segment = tokens.get(0);
    } else {
      throw new IllegalArgumentException("rawSegment cannot be parsed:" + rawSegment);
    }



    return new HL7Segment(group, segment);
    }

  }

  public boolean isFromGroup() {
    return fromGroup;
  }





}
