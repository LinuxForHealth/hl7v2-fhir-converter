/*
 * (c) Copyright Te Whatu Ora, Health New Zealand, 2023
 *
 * SPDX-License-Identifier: Apache-2.0
 * 
 */
package io.github.linuxforhealth.hl7.message;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.GenericPrimitive;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.Varies;

import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.api.ResourceCondition;
import io.github.linuxforhealth.hl7.expression.specification.HL7Specification;
import io.github.linuxforhealth.hl7.expression.specification.SpecificationParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
  * Allows us to conditionally apply a ResourceTemplate depending upon particular values being present in the HL7 message.
  * 
  *   <expression>  EQUALS | NOT_EQUALS | IN | NOT_IN | IS_NULL | NOT_NULL   value | [ ... ]
  *
  *   eg:  ZSC.2.1.3 NOT_NULL
  *        ZCR.2(1).1 IS_NULL
  *        ZCP.3.1 IN [A3, A4, H1, H3, FA, DA]
  *        ZCP.3.1 NOT_IN [A2, F3, DA]
  *        ZFD.2 EQUALS A4
  *        ZSG NOT_EQUALS H2
  *        
  *
  *  Note:  Condition cannot be much more complex, as there are no context variables available at evaluation time.
  */
public class HL7FHIRResourceCondition implements ResourceCondition {
  public enum Operator {
    EQUALS, NOT_EQUALS, IN, NOT_IN, IS_NULL, NOT_NULL
  }

  public HL7Specification fieldSpec;       //  expression for the field to be checked
  public Operator op;                      //  Which comparison?
  public ArrayList<String> values = new ArrayList<String>();

  private static final Logger LOGGER = LoggerFactory.getLogger(HL7FHIRResourceCondition.class);

  public HL7FHIRResourceCondition(String condition) {
    Preconditions.checkArgument(condition != null && ! condition.isEmpty() && ! condition.isBlank(),
        "HL7FHIRResourceCondition (if present) cannot be null, empty or blank");

    //  Chop the condition up into pieces - using a non-trivial regexp
    String regexp="([A-Z][A-Z0-9]+(?:\\.\\d+)?(?:\\(\\d\\))?(?:\\.\\d+)?(?:\\.\\d+)?)\\s+(IN|NOT_IN|EQUALS|NOT_EQUALS|IS_NULL|NOT_NULL)(?:\\s+(?:(\\w+)|\\[([\\s\\w,]+)\\])?)?";
    Pattern pattern = Pattern.compile(regexp);
    Matcher matcher = pattern.matcher(condition);

    // Make sure the expression matched
    if(! matcher.matches())
      throw new IllegalArgumentException(String.format("Can't parse %s - regexp 'fieldSpec EQUALS|NOT_EQUALS|IN|NOT_IN values' doesn't match", condition));

    // populate our fields
    this.fieldSpec = (HL7Specification) SpecificationParser.parse(matcher.group(1), false, false);
    op = Operator.valueOf(matcher.group(2));

    // group(3) and group(4) are both null for IS_NULL and NOT_NULL
    if(matcher.group(3) == null && matcher.group(4) == null) {

      // Only on IS_NULL and NOT_NULL
      if(! op.equals(Operator.IS_NULL) && ! op.equals(Operator.NOT_NULL))
        throw new IllegalArgumentException(String.format("Can't parse %s - non-value only applies to IS_NULL and NOT_NULL", condition));

      // ... and we're done
      return;
    }


    // group(3) is a single value and only for EQUALS and NOT_EQUALS
    if(matcher.group(3) != null) {

       // Only on EQUALS and NOT_EQUALS
      if(! op.equals(Operator.EQUALS) && ! op.equals(Operator.NOT_EQUALS))
        throw new IllegalArgumentException(String.format("Can't parse %s - Single value only applies to EQUALS and NOT_EQUALS", condition));

        values.add(matcher.group(3));

      // ...and we're done
      return;
    }

    // We're working with multiple values in group(4)
    if(matcher.group(4) == null) {
      throw new IllegalArgumentException(String.format("Can't parse %s - Multiple values not found", condition));
    }

    // Only on IN and NOT_IN
    if(! op.equals(Operator.IN) && ! op.equals(Operator.NOT_IN))
      throw new IllegalArgumentException(String.format("Can't parse %s - Multiple values only applies to IN and NOT_IN", condition));

    // group(4) is a single string with multiple values
    String toks[] = matcher.group(4).split("\\s*,\\s*");

    // Make sure we got at least one token
    if(toks.length == 0)
      throw new IllegalArgumentException(String.format("Can't parse %s - no values for IN | NOT_IN found", condition));

    for(int ndx=0; ndx<toks.length; ndx++) {
      values.add(toks[ndx]);
    }
  }

  @Override
  public boolean isConditionSatisfied(InputDataExtractor ide, EvaluationResult context) {

    Preconditions.checkArgument(context != null && (context.getValue() instanceof Segment),
        String.format("HL7FHIRResourceCondition context must be of type Segment not %s", context.getClass().getName()));

    Segment seg = (Segment) context.getValue();
    String segName = seg.getName();

    // Have we been given the right context segment ?
    Preconditions.checkArgument(this.fieldSpec.getSegment() != null && this.fieldSpec.getSegment().equals(segName),
            String.format("HL7FHIRResourceCondition context Segment %s doesn't match %s", segName, fieldSpec.toString()));

    // We're going to extract a value from the context Segment for testing
    EvaluationResult evalResult;
    Object obj;

    try {
      evalResult = ide.extractValueForSpec(this.fieldSpec, Map.of(seg.getName(), context));

      // Empty result can't succeed
      if(evalResult.isEmpty()) {
        LOGGER.warn(String.format("extract field %s was empty", this.fieldSpec.toString()));
        return false;
      }

      // We actually just want the value (but it could be anything)
      obj = evalResult.getValue();

    } catch (Exception ex) {
      LOGGER.warn(String.format("extract field %s", this.fieldSpec.toString()));
      LOGGER.debug(String.format("extract field %s", this.fieldSpec.toString()), ex);
      return false;
    }


    // Grab the actual candidateValue from the returned object
    String candidateVal;
    try {
      if(obj instanceof Segment) {
        //  We got a whole Segment - grab the first field...
        candidateVal = ((Varies) ((Segment) obj).getField(1, 0)).getData().toString();        
      }
      else if(obj instanceof Varies) {
        candidateVal = ((Varies) obj).getData().toString();        
      }
      else if(obj instanceof GenericPrimitive) {
        candidateVal = ((GenericPrimitive) obj).getValue();
      }
      else {
        LOGGER.warn(String.format("extract segment %s unknown return type %s", this.fieldSpec.toString(), obj.getClass().getName()));
        return false;
      }

    } catch (HL7Exception ex) {
      LOGGER.warn(String.format("extract segment %s", this.fieldSpec.toString()));
      LOGGER.debug(String.format("extract segment %s", this.fieldSpec.toString()), ex);
      return false;
    }

    // Now do the comparison
    switch(this.op) {
      case EQUALS:
        return candidateVal.equals(this.values.get(0));

      case NOT_EQUALS:
        return ! candidateVal.equals(this.values.get(0));

      case IN:
        return this.values.contains(candidateVal);
        
      case NOT_IN:
        return ! this.values.contains(candidateVal);

      case NOT_NULL:
        return candidateVal != null;

      case IS_NULL:
        return candidateVal == null;

      default:
        LOGGER.error("Unknown ResourceCondition.Operator %s", this.op.toString());
        return false;
    }
  }

  public String toString() {
    return String.format("ResourceCondition: %s %s %s", this.fieldSpec.toString(), this.op.toString(), this.values.toString());
  }
}
