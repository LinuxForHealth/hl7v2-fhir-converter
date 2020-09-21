/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.core.data.JexlEngineUtil;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.message.InputData;
import com.ibm.whi.hl7.data.Hl7RelatedGeneralUtils;
import com.ibm.whi.hl7.exception.DataExtractionException;
import com.ibm.whi.hl7.parsing.HL7DataExtractor;
import com.ibm.whi.hl7.parsing.result.ParsingResult;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.Type;

public class HL7MessageData implements InputData {
  private HL7DataExtractor hde;

  private static final Logger LOGGER = LoggerFactory.getLogger(HL7MessageData.class);
  protected static final Pattern HL7_SPEC_SPLITTER = Pattern.compile(".");
  private static final JexlEngineUtil JEXL =
      new JexlEngineUtil("GeneralUtils", Hl7RelatedGeneralUtils.class);

  public HL7MessageData(HL7DataExtractor hde) {
    Preconditions.checkArgument(hde != null, "Hl7DataExtractor cannot be null.");
    this.hde = hde;
  }


  @Override
  public GenericResult extractMultipleValuesForSpec(List<String> hl7specs,
      Map<String, GenericResult> contextValues) {
    if (hl7specs.isEmpty()) {
      return null;
    }
    ParsingResult<?> fetchedValue = null;
    for (String hl7specValue : hl7specs) {

      fetchedValue = valuesFromHl7Message(hl7specValue, ImmutableMap.copyOf(contextValues));
      // break the loop and return
      if (fetchedValue != null && !fetchedValue.isEmpty()) {
        return new GenericResult(fetchedValue.getValues());
      }
    }
    return null;


  }

  @Override
  public GenericResult extractSingleValueForSpec(List<String> hl7specs,
      Map<String, GenericResult> contextValues) {
    if (hl7specs.isEmpty()) {
      return null;
    }
    ParsingResult<?> fetchedValue = null;
    for (String hl7specValue : hl7specs) {

      fetchedValue = valuesFromHl7Message(hl7specValue, ImmutableMap.copyOf(contextValues));
      // break the loop and return
      if (fetchedValue != null && !fetchedValue.isEmpty()) {
        return new GenericResult(fetchedValue.getValue());
      }
    }
    return null;


  }






  private ParsingResult<?> valuesFromHl7Message(String hl7specs,
      ImmutableMap<String, GenericResult> varables) {
    if(StringUtils.isBlank(hl7specs)) {
      return null;
    }
    ParsingResult<?> res = null;

    String[] tokens = StringUtils.split(hl7specs, HL7_SPEC_SPLITTER.pattern());
    int subcomponent = -1;
    if (tokens.length == 3) {
      subcomponent = NumberUtils.toInt(tokens[2]);
    }
    
    
    GenericResult valuefromVariables = varables.get(tokens[0]);


    Object obj = null;
    if (valuefromVariables != null) {
      obj = valuefromVariables.getValue();
    }

    try {
      if (obj instanceof Segment) {
        int field = NumberUtils.toInt(tokens[1]);
        res = hde.getTypes((Segment) obj, field);


      } else if (obj instanceof Type) {

        int component = NumberUtils.toInt(tokens[1]);
        if (subcomponent != -1) {
        res = hde.getComponent((Type) obj, component, subcomponent);
        } else {
          res = hde.getComponent((Type) obj, component);
        }

      } else if (tokens.length == 2) {
        res = hde.get(tokens[0], tokens[1]);

      } else {

        res = hde.getAllStructures(tokens[0]);

      }
    } catch (DataExtractionException e) {
      LOGGER.error("cannot extract value for variable {} ", hl7specs, e);
    }

    return res;

  }





  public HL7DataExtractor getHL7DataParser() {
    return hde;
  }


  @Override
  public GenericResult evaluateJexlExpression(String expression,
      Map<String, GenericResult> contextValues) {
    Preconditions.checkArgument(StringUtils.isNotBlank(expression), "jexlExp cannot be blank");
    Preconditions.checkArgument(contextValues != null, "context cannot be null");
    String trimedJexlExp = StringUtils.trim(expression);
    Map<String, Object> localContext = new HashMap<>();
    Map<String, GenericResult> resolvedVariables = new HashMap<>(contextValues);
    resolvedVariables.forEach((key, value) -> localContext.put(key, value.getValue()));
    Object obj = JEXL.evaluate(trimedJexlExp, localContext);
    if (obj != null) {
      return new GenericResult(obj);
    } else {
      return null;
    }
  }

}
