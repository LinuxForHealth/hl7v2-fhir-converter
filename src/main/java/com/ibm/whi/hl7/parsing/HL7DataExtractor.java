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
import com.ibm.whi.hl7.exception.NoMoreRepititionException;
import com.ibm.whi.hl7.parsing.result.Hl7ParsingStringResult;
import com.ibm.whi.hl7.parsing.result.Hl7ParsingStructureResult;
import com.ibm.whi.hl7.parsing.result.Hl7ParsingTypeResult;
import com.ibm.whi.hl7.parsing.result.ParsingResult;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Composite;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Variable;
import ca.uhn.hl7v2.util.Terser;

public class HL7DataExtractor {
  private static final String REP_CANNOT_BE_NEGATIVE = "rep cannot be negative";

  private static final String HL7_STRING = "HL7 String {}  ";

  private static final String CANNOT_EXTRACT_VALUE_FROM_OF_TAG_FROM_TERSER =
      "Cannot extract value from of tag from Terser";

  private static final String CANNOT_EXTRACT_VALUE_FROM_OF_TAG_FROM_TERSER_STRING =
      "Cannot extract value from of tag from Terser, string:";

  private static final String CANNOT_ADD_REPETITION_WITH_INDEX = "Cannot add repetition with index";

  private static final String CAN_T_GET_REPETITION = "Can't get repetition";

  private static final Logger LOGGER = LoggerFactory.getLogger(HL7DataExtractor.class);

  private final Message message;

  public HL7DataExtractor(Message message) {
    this.message = message;
  }

  public boolean doesSegmentExists(String spec) {
    LOGGER.info("Checking if segment exsts: {}", spec);
    try {
      Preconditions.checkArgument(StringUtils.isNotBlank(spec),
          "Not a valid string to extract from Message");
      Structure s = message.get(spec);
      return s != null;


    } catch (IllegalArgumentException | HL7Exception | ArrayIndexOutOfBoundsException e) {
      LOGGER.warn("Failure in checking if segment exsts: {}", spec, e);
      return false;
    }
  }

  public boolean doesSegmentExists(String spec, int rep) {
    LOGGER.info("Checking if segment exsts: {}", spec);
    try {
      Preconditions.checkArgument(StringUtils.isNotBlank(spec),
          "Not a valid string to extract from Terser");
      Preconditions.checkArgument(rep >= 0, "Segment rep cannot be less than 0");

      Structure s = message.get(spec, rep);
        return s != null;


    } catch (IllegalArgumentException | HL7Exception | ArrayIndexOutOfBoundsException e) {
      LOGGER.warn("Failure in checking if segment exsts: {}", spec, e);
      return false;
    }
  }


  public ParsingResult<Structure> getStructure(String spec, int rep) {
    try {

      Preconditions.checkArgument(StringUtils.isNotBlank(spec),
          "Not a valid string to extract from Hl7");
      Preconditions.checkArgument(rep >= 0, REP_CANNOT_BE_NEGATIVE);
      LOGGER.info("fetching values for spec {} rep {}", spec, rep);

      return new Hl7ParsingStructureResult(message.get(spec, rep));

    } catch (HL7Exception | IllegalArgumentException e) {
      if (e.getMessage().contains(CAN_T_GET_REPETITION)
          || e.getMessage().contains(CANNOT_ADD_REPETITION_WITH_INDEX)) {
        throw new NoMoreRepititionException(
            CANNOT_EXTRACT_VALUE_FROM_OF_TAG_FROM_TERSER_STRING + spec, e);
      } else {
        throw new DataExtractionException(CANNOT_EXTRACT_VALUE_FROM_OF_TAG_FROM_TERSER, e);
      }

    } catch (ArrayIndexOutOfBoundsException are) {
      LOGGER.error(HL7_STRING, spec, are);
      return null;

    }
  }



  public ParsingResult<Structure> getAllStructures(String spec) {
    try {

      Preconditions.checkArgument(StringUtils.isNotBlank(spec),
          "Not a valid string to extract from Hl7");
      LOGGER.info("fetching values for spec {}, ", spec);
      List<Structure> segments = new ArrayList<>();
      Structure[] strs = message.getAll(spec);

      segments.addAll(Lists.newArrayList(strs));
      return new Hl7ParsingStructureResult(segments);

    } catch (HL7Exception | IllegalArgumentException e) {
      if (e.getMessage().contains(CAN_T_GET_REPETITION)
          || e.getMessage().contains(CANNOT_ADD_REPETITION_WITH_INDEX)) {
        throw new NoMoreRepititionException(
            CANNOT_EXTRACT_VALUE_FROM_OF_TAG_FROM_TERSER_STRING + spec, e);
      } else {
        throw new DataExtractionException(CANNOT_EXTRACT_VALUE_FROM_OF_TAG_FROM_TERSER, e);
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
      LOGGER.info("fetching values for Segment {} field {} rep {}, ", segment, field, rep);

      return new Hl7ParsingTypeResult(segment.getField(field, rep));

    } catch (HL7Exception | IllegalArgumentException e) {
      if (e.getMessage().contains(CAN_T_GET_REPETITION)
          || e.getMessage().contains(CANNOT_ADD_REPETITION_WITH_INDEX)) {
        throw new NoMoreRepititionException(
            CANNOT_EXTRACT_VALUE_FROM_OF_TAG_FROM_TERSER_STRING + segment, e);
      } else {
        throw new DataExtractionException(CANNOT_EXTRACT_VALUE_FROM_OF_TAG_FROM_TERSER, e);
      }

    } catch (ArrayIndexOutOfBoundsException are) {
      LOGGER.error(HL7_STRING, segment, are);
      return null;

    }
  }

  public ParsingResult<Type> getTypes(Segment segment, int field) {
    try {
      Preconditions.checkArgument(segment != null, "segment cannot be null");
      Preconditions.checkArgument(field >= 1, "field cannot be negative");

      LOGGER.info("fetching values for Segment {} field {}  ", segment, field);

      List<Type> types = new ArrayList<>();

      Type[] fields = segment.getField(field);
      types.addAll(Lists.newArrayList(fields));

      return new Hl7ParsingTypeResult(types);

    } catch (HL7Exception | IllegalArgumentException e) {
      if (e.getMessage().contains(CAN_T_GET_REPETITION)
          || e.getMessage().contains(CANNOT_ADD_REPETITION_WITH_INDEX)) {
        throw new NoMoreRepititionException(
            CANNOT_EXTRACT_VALUE_FROM_OF_TAG_FROM_TERSER_STRING + segment, e);
      } else {
        throw new DataExtractionException(CANNOT_EXTRACT_VALUE_FROM_OF_TAG_FROM_TERSER, e);
      }

    } catch (ArrayIndexOutOfBoundsException are) {
      LOGGER.error(HL7_STRING, segment, are);
      return new Hl7ParsingTypeResult(new ArrayList<>());

    }
  }



  public ParsingResult<Type> getComponent(Type inputType, int component) {
    try {
      Preconditions.checkArgument(inputType != null, "type!=null");
      Type type = inputType;
      if (inputType instanceof Variable) {
        type = ((Variable) inputType).getData();
      }
      if (type instanceof Composite) {
        return new Hl7ParsingTypeResult(((Composite) type).getComponent(component - 1));
      }

      return new Hl7ParsingTypeResult(Terser.getPrimitive(type, component - 1, 1));


    } catch (IllegalArgumentException | DataTypeException e) {
      if (e.getMessage().contains(CAN_T_GET_REPETITION)
          || e.getMessage().contains(CANNOT_ADD_REPETITION_WITH_INDEX)) {
        throw new NoMoreRepititionException(
            CANNOT_EXTRACT_VALUE_FROM_OF_TAG_FROM_TERSER_STRING + component, e);
      } else {
        throw new DataExtractionException(CANNOT_EXTRACT_VALUE_FROM_OF_TAG_FROM_TERSER, e);
      }

    } catch (ArrayIndexOutOfBoundsException are) {
      LOGGER.error(HL7_STRING, component, are);
      return null;

    }
  }

  public ParsingResult<Type> getComponent(Type inputType, int component, int subComponent) {
    try {
      Preconditions.checkArgument(inputType != null, "inputType!=null");
      Type type = inputType;
      if (inputType instanceof Variable) {
        type = ((Variable) inputType).getData();
      }
      return new Hl7ParsingTypeResult(Terser.getPrimitive(type, component, subComponent));


    } catch (IllegalArgumentException e) {
      if (e.getMessage().contains(CAN_T_GET_REPETITION)
          || e.getMessage().contains(CANNOT_ADD_REPETITION_WITH_INDEX)) {
        throw new NoMoreRepititionException(
            CANNOT_EXTRACT_VALUE_FROM_OF_TAG_FROM_TERSER_STRING + component, e);
      } else {
        throw new DataExtractionException(CANNOT_EXTRACT_VALUE_FROM_OF_TAG_FROM_TERSER, e);
      }

    } catch (ArrayIndexOutOfBoundsException are) {
      LOGGER.error(HL7_STRING, component, are);
      return null;

    }
  }



  private Terser getTerser() {
    return new Terser(message);
  }

  public static String getMessageType(Message message) {

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
      return null;

    }
  }
}
