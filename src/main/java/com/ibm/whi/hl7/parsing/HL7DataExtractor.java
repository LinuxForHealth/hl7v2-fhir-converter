/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.parsing;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.ibm.whi.hl7.exception.DataExtractionException;
import com.ibm.whi.hl7.parsing.result.Hl7ParsingStringResult;
import com.ibm.whi.hl7.parsing.result.Hl7ParsingStructureResult;
import com.ibm.whi.hl7.parsing.result.Hl7ParsingTypeResult;
import com.ibm.whi.hl7.parsing.result.ParsingResult;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Composite;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Primitive;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Unmodifiable;
import ca.uhn.hl7v2.model.Variable;
import ca.uhn.hl7v2.util.Terser;

public class HL7DataExtractor {
  private static final String REP_CANNOT_BE_NEGATIVE = "rep cannot be negative";

  private static final String HL7_STRING = "HL7 String {}  ";

  private static final String CANNOT_EXTRACT_VALUE_FROM_OF_TAG =
      "Cannot extract value from of tag ";

  private static final String CANNOT_EXTRACT_VALUE_FROM_OF_TAG_STRING =
      "Cannot extract value from of tag , string:";

  private static final String CANNOT_ADD_REPETITION_WITH_INDEX = "Cannot add repetition with index";

  private static final String CAN_T_GET_REPETITION = "Can't get repetition";

  private static final Logger LOGGER = LoggerFactory.getLogger(HL7DataExtractor.class);


  private final Message message;

  public HL7DataExtractor(Message message) {
    this.message = message;

  }

  public boolean doesSegmentExists(String spec) {
    LOGGER.debug("Checking if segment exists: {}", spec);
    try {
      Preconditions.checkArgument(StringUtils.isNotBlank(spec),
          "Not a valid string to extract from Message");
      Message unmodifiableMessage = Unmodifiable.unmodifiableMessage(message);
      Structure s = unmodifiableMessage.get(spec);
      return s != null;


    } catch (IllegalArgumentException | HL7Exception | ArrayIndexOutOfBoundsException e) {
      LOGGER.warn("Failure in checking if segment exsts: {}", spec, e);
      return false;
    }
  }

  public boolean doesSegmentExists(String spec, int rep) {
    LOGGER.debug("Checking if segment exists: {}", spec);
    try {
      Preconditions.checkArgument(StringUtils.isNotBlank(spec),
          "Not a valid string to extract from Terser");
      Preconditions.checkArgument(rep >= 0, "Segment rep cannot be less than 0");
      Message unmodifiableMessage = Unmodifiable.unmodifiableMessage(message);
      Structure s = unmodifiableMessage.get(spec, rep);
        return s != null;


    } catch (IllegalArgumentException | HL7Exception | ArrayIndexOutOfBoundsException e) {
      LOGGER.warn("Failure in checking if segment exsts: {}", spec, e);
      return false;
    }
  }



  public ParsingResult<Structure> getStructure(String spec, int rep) {
    try {
      ParsingResult<Structure> parsingResult = null;
      if (doesSegmentExists(spec, rep)) {
      Preconditions.checkArgument(StringUtils.isNotBlank(spec),
          "Not a valid string to extract from Hl7");
      Preconditions.checkArgument(rep >= 0, REP_CANNOT_BE_NEGATIVE);
        LOGGER.debug("fetching values for spec {} rep {}", spec, rep);

        parsingResult = new Hl7ParsingStructureResult(message.get(spec, rep));
      } else {
        parsingResult = new Hl7ParsingStructureResult(new ArrayList<>());
      }
      return parsingResult;
    } catch (HL7Exception | IllegalArgumentException e) {
      if (e.getMessage().contains(CAN_T_GET_REPETITION)
          || e.getMessage().contains(CANNOT_ADD_REPETITION_WITH_INDEX)) {
        LOGGER.warn(CANNOT_EXTRACT_VALUE_FROM_OF_TAG_STRING + spec + " rep:" + rep, e);
        return new Hl7ParsingStructureResult(new ArrayList<>());
      } else {
        throw new DataExtractionException(CANNOT_EXTRACT_VALUE_FROM_OF_TAG, e);
      }

    } catch (ArrayIndexOutOfBoundsException are) {
      LOGGER.error(HL7_STRING, spec, are);
      return new Hl7ParsingStructureResult(new ArrayList<>());
    }
  }



  public ParsingResult<Structure> getAllStructures(String spec) {
    try {
      ParsingResult<Structure> parsingResult = null;
      if (doesSegmentExists(spec)) {
      Preconditions.checkArgument(StringUtils.isNotBlank(spec),
          "Not a valid string to extract from Hl7");
        LOGGER.debug("fetching values for spec {}, ", spec);
      List<Structure> segments = new ArrayList<>();
        Structure[] strs = message.getAll(spec);

      segments.addAll(Lists.newArrayList(strs));
        parsingResult = new Hl7ParsingStructureResult(segments);
      } else {
        parsingResult = new Hl7ParsingStructureResult(new ArrayList<>());
      }
      return parsingResult;
    } catch (HL7Exception | IllegalArgumentException e) {
      if (e.getMessage().contains(CAN_T_GET_REPETITION)
          || e.getMessage().contains(CANNOT_ADD_REPETITION_WITH_INDEX)) {
        LOGGER.warn(CANNOT_EXTRACT_VALUE_FROM_OF_TAG_STRING + spec, e);
        return new Hl7ParsingStructureResult(new ArrayList<>());
      } else {
        throw new DataExtractionException(CANNOT_EXTRACT_VALUE_FROM_OF_TAG, e);
      }

    } catch (ArrayIndexOutOfBoundsException are) {
      LOGGER.error(HL7_STRING, spec, are);
      return new Hl7ParsingStructureResult(new ArrayList<>());

    }
  }



  public ParsingResult<Type> getType(Segment segment, int field, int rep) {
    try {

      Preconditions.checkArgument(segment != null, "segment cannot be null");
      Preconditions.checkArgument(field >= 1, "field cannot be negative");
      Preconditions.checkArgument(rep >= 0, REP_CANNOT_BE_NEGATIVE);
      LOGGER.debug("fetching values for Segment {} field {} rep {}, ", segment, field, rep);
      return new Hl7ParsingTypeResult(segment.getField(field, rep));

    } catch (HL7Exception | IllegalArgumentException e) {
      if (e.getMessage().contains(CAN_T_GET_REPETITION)
          || e.getMessage().contains(CANNOT_ADD_REPETITION_WITH_INDEX)) {
        LOGGER.warn(CANNOT_EXTRACT_VALUE_FROM_OF_TAG_STRING + segment, e);
        return new Hl7ParsingTypeResult(new ArrayList<>());
      } else {
        throw new DataExtractionException(CANNOT_EXTRACT_VALUE_FROM_OF_TAG, e);
      }

    } catch (ArrayIndexOutOfBoundsException are) {
      LOGGER.error(HL7_STRING, segment, are);
      return new Hl7ParsingTypeResult(new ArrayList<>());

    }
  }



  public ParsingResult<Type> getTypes(Segment segment, int field) {
    try {
      Preconditions.checkArgument(segment != null, "segment cannot be null");
      Preconditions.checkArgument(field >= 1, "field cannot be negative");

      LOGGER.debug("fetching values for Segment {} field {}  ", segment, field);

      List<Type> types = new ArrayList<>();

      Type[] fields = segment.getField(field);
      types.addAll(Lists.newArrayList(fields));

      return new Hl7ParsingTypeResult(types);

    } catch (HL7Exception | IllegalArgumentException e) {
      if (e.getMessage().contains(CAN_T_GET_REPETITION)
          || e.getMessage().contains(CANNOT_ADD_REPETITION_WITH_INDEX)) {
        LOGGER.warn(CANNOT_EXTRACT_VALUE_FROM_OF_TAG_STRING + segment, e);
        return new Hl7ParsingTypeResult(new ArrayList<>());
      } else {
        throw new DataExtractionException(CANNOT_EXTRACT_VALUE_FROM_OF_TAG, e);
      }

    } catch (ArrayIndexOutOfBoundsException are) {
      LOGGER.error(HL7_STRING, segment, are);
      return new Hl7ParsingTypeResult(new ArrayList<>());

    }
  }



  public ParsingResult<Type> getComponent(Type inputType, int component) {
    try {
      Preconditions.checkArgument(inputType != null, "type!=null");
      ParsingResult<Type> result;
      Type type = inputType;
      if (inputType instanceof Variable) {
        type = ((Variable) inputType).getData();
      }
      if (type instanceof Composite) {
        Type value = ((Composite) type).getComponent(component - 1);
        if (value != null && !value.isEmpty()) {
          result = new Hl7ParsingTypeResult(((Composite) type).getComponent(component - 1));
      } else {
          result = new Hl7ParsingTypeResult(new ArrayList<>());
      }
      } else {
        result = new Hl7ParsingTypeResult(type);
      }
      return result;

    } catch (HL7Exception | IllegalArgumentException e) {
      if (e.getMessage().contains(CAN_T_GET_REPETITION)
          || e.getMessage().contains(CANNOT_ADD_REPETITION_WITH_INDEX)) {
        LOGGER.warn(
            CANNOT_EXTRACT_VALUE_FROM_OF_TAG_STRING + component, e);
        return new Hl7ParsingTypeResult(new ArrayList<>());
      } else {
        throw new DataExtractionException(CANNOT_EXTRACT_VALUE_FROM_OF_TAG, e);
      }

    } catch (ArrayIndexOutOfBoundsException are) {
      LOGGER.error(HL7_STRING, component, are);
      return new Hl7ParsingTypeResult(new ArrayList<>());
    }
  }

  public ParsingResult<Type> getComponent(Type inputType, int component, int subComponent) {
    try {
      Preconditions.checkArgument(inputType != null, "inputType!=null");
      ParsingResult<Type> result;
      Type type = inputType;
      if (inputType instanceof Variable) {
        type = ((Variable) inputType).getData();
      }
      Primitive prim = Terser.getPrimitive(type, component, subComponent);
      if (prim != null && !prim.isEmpty()) {
        result = new Hl7ParsingTypeResult(prim);
      } else {
        result = new Hl7ParsingTypeResult(new ArrayList<>());
      }
      return result;
    } catch (IllegalArgumentException | HL7Exception e) {
      if (e.getMessage().contains(CAN_T_GET_REPETITION)
          || e.getMessage().contains(CANNOT_ADD_REPETITION_WITH_INDEX)) {
        LOGGER.warn(
            CANNOT_EXTRACT_VALUE_FROM_OF_TAG_STRING + component, e);
        return new Hl7ParsingTypeResult(new ArrayList<>());
      } else {
        throw new DataExtractionException(CANNOT_EXTRACT_VALUE_FROM_OF_TAG, e);
      }

    } catch (ArrayIndexOutOfBoundsException are) {
      LOGGER.error(HL7_STRING, component, are);
      return new Hl7ParsingTypeResult(new ArrayList<>());

    }
  }



  private Terser getTerser() {
    Message unmodifiableMessage = Unmodifiable.unmodifiableMessage(message);
    return new Terser(unmodifiableMessage);
  }

  public static String getMessageType(Message message) {
    return message.getName();
  }


  public String getMessageType() {
    return message.getName();
  }



  /**
   * 
   * @param segment
   * @param field
   * @return
   */
  public ParsingResult<String> get(String segment, String field) {
  
   Preconditions.checkArgument(StringUtils.isNotBlank(segment), "segment cannot be blank");
   Preconditions.checkArgument(StringUtils.isNotBlank(field), "field cannot be blank");

   try {
      return new Hl7ParsingStringResult(getTerser().get("/" + segment + "-" + field));

    } catch (HL7Exception | IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
      LOGGER.error(HL7_STRING, segment + "-" + field + e.getMessage());
      LOGGER.debug(HL7_STRING, segment + "-" + field, e);
      return new Hl7ParsingStringResult(null);

    }
  }

  public String getMessageId() {
    try {
      return getTerser().get("/MSH-10");
    } catch (HL7Exception e) {
      return null;
    }
  }
}
