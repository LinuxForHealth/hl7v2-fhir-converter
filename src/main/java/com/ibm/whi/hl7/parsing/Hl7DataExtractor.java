package com.ibm.whi.hl7.parsing;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.python.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.Lists;
import com.ibm.whi.hl7.exception.DataExtractionException;
import com.ibm.whi.hl7.exception.NoMoreRepititionException;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Composite;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Visitable;
import ca.uhn.hl7v2.util.Terser;

public class Hl7DataExtractor {
  private static final String REP_CANNOT_BE_NEGATIVE = "rep cannot be negative";

  private static final String HL7_STRING = "HL7 String {}  ";

  private static final String CANNOT_EXTRACT_VALUE_FROM_OF_TAG_FROM_TERSER =
      "Cannot extract value from of tag from Terser";

  private static final String CANNOT_EXTRACT_VALUE_FROM_OF_TAG_FROM_TERSER_STRING =
      "Cannot extract value from of tag from Terser, string:";

  private static final String CANNOT_ADD_REPETITION_WITH_INDEX = "Cannot add repetition with index";

  private static final String CAN_T_GET_REPETITION = "Can't get repetition";

  private static final Logger LOGGER = LoggerFactory.getLogger(Hl7DataExtractor.class);

  private final Message message;

  public Hl7DataExtractor(Message message) {
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


  public Structure getStructure(String spec, int rep) {
    try {

      Preconditions.checkArgument(StringUtils.isNotBlank(spec),
          "Not a valid string to extract from Hl7");
      Preconditions.checkArgument(rep >= 0, REP_CANNOT_BE_NEGATIVE);
      LOGGER.info("fetching values for spec {} rep {}", spec, rep);

      return message.get(spec, rep);

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



  public List<Structure> getAllStructures(String spec) {
    try {

      Preconditions.checkArgument(StringUtils.isNotBlank(spec),
          "Not a valid string to extract from Hl7");
      LOGGER.info("fetching values for spec {}, ", spec);
      List<Structure> segments = new ArrayList<>();
      Structure[] strs = message.getAll(spec);

      segments.addAll(Lists.newArrayList(strs));
      return segments;

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
      return new ArrayList<>();

    }
  }



  public Type getType(Segment segment, int field, int rep) {
    try {
      Preconditions.checkArgument(segment != null, "segment cannot be null");
      Preconditions.checkArgument(field >= 1, "field cannot be negative");
      Preconditions.checkArgument(rep >= 0, REP_CANNOT_BE_NEGATIVE);
      LOGGER.info("fetching values for Segment {} field {} rep {}, ", segment, field, rep);

      return segment.getField(field, rep);

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

  public List<Visitable> getTypes(Segment segment, int field) {
    try {
      Preconditions.checkArgument(segment != null, "segment cannot be null");
      Preconditions.checkArgument(field >= 1, "field cannot be negative");

      LOGGER.info("fetching values for Segment {} field {}  ", segment, field);

      List<Visitable> types = new ArrayList<>();

      Type[] fields = segment.getField(field);
      types.addAll(Lists.newArrayList(fields));

      return types;

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
      return new ArrayList<>();

    }
  }



  public Type getComponent(Type type, int component) {
    try {
      Preconditions.checkArgument(type != null, "type!=null");
      if (type instanceof Composite) {
        return ((Composite) type).getComponent(component - 1);
      }

      return Terser.getPrimitive(type, component - 1, 1);


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

  public Type getComponent(Type type, int component, int subComponent) {
    try {
      Preconditions.checkArgument(type != null, "type!=null");
      return Terser.getPrimitive(type, component, subComponent);


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



  public Terser getTerser() {
    return new Terser(message);
  }

  public String getMessageType() {
    try {
      return new Terser(message).get("/MSH-9");
    } catch (HL7Exception e) {
      throw new IllegalArgumentException("cannot determine message type in message", e);
    }
  }

  //
  // /**
  // * Extracts the value of the specified input from the message and returns an object - which
  // could
  // * either be Structure (Segment/Group) or Type or a String value
  // *
  // * @param obj - if obj is null then uses the terser to extract the String specified by the spec
  // * @param spec - this field can be either represent Segment name or field type or spec for
  // * extracting value from Terser.
  // * @param rep -- validates that rep is non negative
  // * @return
  // */
  // public Object get(Structure obj, int field, int rep, String type) {
  //
  // Preconditions.checkArgument(rep >= 0, REP_CANNOT_BE_NEGATIVE);
  // Object returnObject = null;
  // try {
  // if (obj != null && obj instanceof Segment) {
  // List<Type> objects = this.getField((Segment) obj, NumberUtils.toInt(spec), rep);
  // if (!objects.isEmpty()) {
  // returnObject = objects.get(0);
  // }
  // } else if (obj != null && obj instanceof Type) {
  // returnObject = this.getComponent((Type) obj, NumberUtils.toInt(spec));
  // } else {
  // returnObject = getTerser().get(spec);
  // }
  //
  // return returnObject;
  // } catch (HL7Exception | IllegalArgumentException e) {
  // if (e.getMessage().contains(CAN_T_GET_REPETITION)
  // || e.getMessage().contains(CANNOT_ADD_REPETITION_WITH_INDEX)) {
  // throw new NoMoreRepititionException(
  // CANNOT_EXTRACT_VALUE_FROM_OF_TAG_FROM_TERSER_STRING + spec, e);
  // } else {
  // throw new DataExtractionException(CANNOT_EXTRACT_VALUE_FROM_OF_TAG_FROM_TERSER, e);
  // }
  //
  // } catch (ArrayIndexOutOfBoundsException are) {
  // LOGGER.error(HL7_STRING, spec, are);
  // return null;
  // // throw new DataExtractionException("Cannot extract value from of tag from Terser", are);
  // }
  // }
  //
  // /**
  // *
  // * @param obj
  // * @param spec
  // * @return
  // */
  // public List<?> getAllRepetitions(Object obj, String spec) {
  //
  // try {
  // if (obj instanceof Segment) {
  // return this.getField((Segment) obj, NumberUtils.toInt(spec), -1);
  // } else {
  // return this.getSegments(spec, -1);
  // }
  //
  //
  // } catch (IllegalArgumentException e) {
  // if (e.getMessage().contains(CAN_T_GET_REPETITION)
  // || e.getMessage().contains(CANNOT_ADD_REPETITION_WITH_INDEX)) {
  // throw new NoMoreRepititionException(
  // CANNOT_EXTRACT_VALUE_FROM_OF_TAG_FROM_TERSER_STRING + spec, e);
  // } else {
  // throw new DataExtractionException(CANNOT_EXTRACT_VALUE_FROM_OF_TAG_FROM_TERSER, e);
  // }
  //
  // } catch (ArrayIndexOutOfBoundsException are) {
  // LOGGER.error(HL7_STRING, spec, are);
  // return null;
  // // throw new DataExtractionException("Cannot extract value from of tag from Terser", are);
  // }
  // }
  //


}
