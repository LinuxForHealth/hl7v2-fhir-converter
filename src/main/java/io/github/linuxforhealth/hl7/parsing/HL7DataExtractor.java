/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import ca.uhn.hl7v2.model.v26.segment.MSH;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Composite;
import ca.uhn.hl7v2.model.Group;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.Primitive;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Unmodifiable;
import ca.uhn.hl7v2.model.Variable;
import ca.uhn.hl7v2.util.Terser;
import io.github.linuxforhealth.hl7.parsing.result.Hl7ParsingStringResult;
import io.github.linuxforhealth.hl7.parsing.result.Hl7ParsingStructureResult;
import io.github.linuxforhealth.hl7.parsing.result.Hl7ParsingTypeResult;
import io.github.linuxforhealth.hl7.parsing.result.ParsingResult;

public class HL7DataExtractor {
  private static final String CANNOT_EXTRACT_VALUE_FOR_REP_REASON = "Cannot extract value for {} rep {} reason {}";

  private static final String SEGMENT_CANNOT_BE_NULL_OR_EMPTY = "segment cannot be null or empty";

  private static final String REP_CANNOT_BE_NEGATIVE = "rep cannot be negative";



  private static final Logger LOGGER = LoggerFactory.getLogger(HL7DataExtractor.class);


  private final Message message;

  public HL7DataExtractor(Message message) {
    this.message = message;
  }


  private static Predicate<Structure> isEmpty() {
    return (Structure p) -> {
      try {
        return p == null || p.isEmpty();
      } catch (HL7Exception e) {
        LOGGER.debug("Error", e);
        return true;
      }
    };
  }



  public ParsingResult<Structure> getStructure(String group, int groupRep, String segment,
      int rep) {

    LOGGER.debug("Fetching segment: {} {} {} {}", group, groupRep, segment, rep);
    try {
      ParsingResult<Structure> parsingResult = null;

      Preconditions.checkArgument(StringUtils.isNotBlank(group), "group cannot be null or empty");
      Preconditions.checkArgument(StringUtils.isNotBlank(segment), SEGMENT_CANNOT_BE_NULL_OR_EMPTY);
      Preconditions.checkArgument(groupRep >= 0, "groupRep should be greater than or equal to 0");
      Preconditions.checkArgument(rep >= 0, "Segment rep cannot be less than 0");


      Structure groupStr = message.get(group, groupRep);
      if (groupStr instanceof Group) {
        Group gp = (Group) groupStr;
        Structure s = gp.get(segment, rep);
        if (s != null && !s.isEmpty()) {
          parsingResult = new Hl7ParsingStructureResult(s);
        } else {
          parsingResult = new Hl7ParsingStructureResult(new ArrayList<>());
        }

      } else {
        parsingResult = new Hl7ParsingStructureResult(new ArrayList<>());
      }


      return parsingResult;
    } catch (HL7Exception | IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
      String spec = group + " " + groupRep + " " + segment;
      LOGGER.debug("Cannot extract value for {} rep {} ", spec, rep, e);
      LOGGER.warn(CANNOT_EXTRACT_VALUE_FOR_REP_REASON, spec, rep, e.getMessage());
      return new Hl7ParsingStructureResult(new ArrayList<>());

    }
  }



  public ParsingResult<Structure> getAllStructures(String group, int groupRep, String segment) {

    LOGGER.debug("Fetching segment: {} {} {} ", group, groupRep, segment);
    try {
      ParsingResult<Structure> parsingResult = null;

      Preconditions.checkArgument(StringUtils.isNotBlank(group), "group cannot be null or empty");
      Preconditions.checkArgument(StringUtils.isNotBlank(segment), SEGMENT_CANNOT_BE_NULL_OR_EMPTY);
      Preconditions.checkArgument(groupRep >= 0, "groupRep should be greater than or equal to 0");

      Structure groupStr = message.get(group, groupRep);
      if (groupStr instanceof Group) {
        Group gp = (Group) groupStr;
        Structure[] s = gp.getAll(segment);
        List<Structure> list = Lists.newArrayList(s);
        list.removeIf(isEmpty());
        parsingResult = new Hl7ParsingStructureResult(Lists.newArrayList(list));
      } else {
        parsingResult = new Hl7ParsingStructureResult(new ArrayList<>());
      }


      return parsingResult;
    } catch (HL7Exception | IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
      String spec = group + " " + groupRep + " " + segment;
      LOGGER.debug("Cannot extract value for {} ", spec, e);
      LOGGER.warn("Cannot extract value for {} reason {}", spec, e.getMessage());

      return new Hl7ParsingStructureResult(new ArrayList<>());

    }
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
      LOGGER.debug("Cannot extract value for {}  ", spec, e);
      LOGGER.warn("Cannot extract value for {} reason {}", spec, e.getMessage());

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
      LOGGER.debug("Cannot extract value for {} rep{}  ", spec, rep, e);
      LOGGER.warn(CANNOT_EXTRACT_VALUE_FOR_REP_REASON, spec, rep, e.getMessage());

      return false;

    }
  }


  public ParsingResult<Structure> getStructure(String structure, int rep) {
    try {
      ParsingResult<Structure> parsingResult = null;
      if (doesSegmentExists(structure, rep)) {
        Preconditions.checkArgument(StringUtils.isNotBlank(structure),
            "Not a valid string to extract from Hl7");
        Preconditions.checkArgument(rep >= 0, REP_CANNOT_BE_NEGATIVE);
        LOGGER.debug("fetching values for spec {} rep {}", structure, rep);

        parsingResult = new Hl7ParsingStructureResult(message.get(structure, rep));
      } else {
        parsingResult = new Hl7ParsingStructureResult(new ArrayList<>());
      }
      return parsingResult;
    } catch (HL7Exception | IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
      LOGGER.debug("Cannot extract value for {} rep {}  ", structure, rep, e);
      LOGGER.warn(CANNOT_EXTRACT_VALUE_FOR_REP_REASON, structure, rep, e.getMessage());

      return new Hl7ParsingStructureResult(new ArrayList<>());
    }
  }



  public ParsingResult<Structure> getAllStructures(String structure) {
    try {
      ParsingResult<Structure> parsingResult = null;
      if (doesSegmentExists(structure)) {
        Preconditions.checkArgument(StringUtils.isNotBlank(structure),
            "Not a valid string to extract from Hl7");
        LOGGER.debug("fetching values for spec {}, ", structure);
        Structure[] strs = message.getAll(structure);

        parsingResult = new Hl7ParsingStructureResult(Lists.newArrayList(strs));
      } else {
        parsingResult = new Hl7ParsingStructureResult(new ArrayList<>());
      }
      return parsingResult;
    } catch (HL7Exception | IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
      LOGGER.debug("Cannot extract value for {}   ", structure, e);
      LOGGER.warn("Cannot extract value for {}  reason {}", structure, e.getMessage());

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

    } catch (HL7Exception | IllegalArgumentException | ArrayIndexOutOfBoundsException e) {

      LOGGER.debug("Cannot extract value for {} rep {}  field {} ", segment, rep, field, e);
      LOGGER.warn("Cannot extract value for {} rep {} field {} reason {}", segment, rep, field,
          e.getMessage());

      return new Hl7ParsingTypeResult(new ArrayList<>());

    }
  }



  public ParsingResult<Type> getTypes(Segment segment, int field) {
    try {
      Preconditions.checkArgument(segment != null, "segment cannot be null");
      Preconditions.checkArgument(field >= 1, "field cannot be negative");

      LOGGER.debug("fetching values for Segment {} field {}  ", segment, field);
      Type[] fields = segment.getField(field);

      return new Hl7ParsingTypeResult(Lists.newArrayList(fields));

    } catch (HL7Exception | IllegalArgumentException | ArrayIndexOutOfBoundsException e) {

      LOGGER.debug("Cannot extract value for segment {} field {}  ", segment, field, e);
      LOGGER.warn("Cannot extract value for segment {} field {}, reason {}", segment, field,
          e.getMessage());

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


    } catch (IllegalArgumentException | HL7Exception | ArrayIndexOutOfBoundsException e) {
      LOGGER.debug("Cannot extract value for type {} component {}  ", inputType, component, e);
      LOGGER.warn("Cannot extract value for type {} component {}, reason {}", inputType, component,
          e.getMessage());

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
    } catch (IllegalArgumentException | HL7Exception | ArrayIndexOutOfBoundsException e) {
      LOGGER.debug("Cannot extract value for type {} component {} subComponent {}  ", inputType,
          component, subComponent, e);
      LOGGER.warn("Cannot extract value for type {} component {},subComponent {}, reason {}",
          inputType, component, subComponent, e.getMessage());

      return new Hl7ParsingTypeResult(new ArrayList<>());

    }
  }



  private Terser getTerser() {
    Message unmodifiableMessage = Unmodifiable.unmodifiableMessage(message);
    return new Terser(unmodifiableMessage);
  }

  public static String getMessageType(Message message) {
    try {
      MSH msh = (MSH) message.get("MSH");
      String theActualMessageType = msh.getMessageType().getMsg1_MessageCode().getValue() + "_" + msh.getMessageType().getMsg2_TriggerEvent().getValue();
      return theActualMessageType;
    } catch (HL7Exception e){
      // TODO Auto generated catch block
      e.printStackTrace();
    }
    return null;
  }


  public String getMessageType() {
  return getMessageType(message);
  }



  /**
   * 
   * @param segment
   * @param field
   * @return {@link ParsingResult}
   */
  public ParsingResult<String> get(String segment, String field) {

    Preconditions.checkArgument(StringUtils.isNotBlank(segment), "segment cannot be blank");
    Preconditions.checkArgument(StringUtils.isNotBlank(field), "field cannot be blank");

    try {
      return new Hl7ParsingStringResult(getTerser().get("/" + segment + "-" + field));

    } catch (HL7Exception | IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
      LOGGER.debug("Cannot extract value for Segment {} field {}   ", segment, field, e);
      LOGGER.warn("Cannot extract value for Segment {} field {}, reason {}", segment, field,
          e.getMessage());

      return new Hl7ParsingStringResult(null);


    }
  }

  public String getMessageId() {
    try {
      return getTerser().get("/MSH-10");
    } catch (HL7Exception | IllegalArgumentException e) {
      LOGGER.warn("Cannot extract message control id, reason {} ", e.getMessage());
      LOGGER.debug("Cannot extract message control id", e);
      return null;
    }
  }

  public ParsingResult<Structure> getAllStructures(Structure struct, String segment) {
    LOGGER.debug("Fetching segment: {} {}  ", struct, segment);
    try {
      ParsingResult<Structure> parsingResult = null;

      Preconditions.checkArgument(struct != null, "struct cannot be null ");
      Preconditions.checkArgument(StringUtils.isNotBlank(segment), SEGMENT_CANNOT_BE_NULL_OR_EMPTY);



      if (struct instanceof Group) {
        Group gp = (Group) struct;

        Structure[] s = gp.getAll(segment);
        List<Structure> list = Lists.newArrayList(s);
        list.removeIf(isEmpty());

        parsingResult = new Hl7ParsingStructureResult(list);
      } else {
        parsingResult = new Hl7ParsingStructureResult(new ArrayList<>());
      }
      return parsingResult;
    } catch (HL7Exception | IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
      LOGGER.debug("Cannot extract value for Structure {} Segment {}   ", struct, segment, e);
      LOGGER.warn("Cannot extract value for Structure {} Segment {}, reason {}", struct, segment,
          e.getMessage());

      return new Hl7ParsingStructureResult(new ArrayList<>());

    }
  }

}
