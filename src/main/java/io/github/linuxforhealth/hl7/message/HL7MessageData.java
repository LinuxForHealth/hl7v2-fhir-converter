/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.Type;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.api.Specification;
import io.github.linuxforhealth.core.data.JexlEngineUtil;
import io.github.linuxforhealth.core.expression.EmptyEvaluationResult;
import io.github.linuxforhealth.core.expression.EvaluationResultFactory;
import io.github.linuxforhealth.core.expression.SimpleEvaluationResult;
import io.github.linuxforhealth.hl7.data.Hl7RelatedGeneralUtils;
import io.github.linuxforhealth.hl7.exception.DataExtractionException;
import io.github.linuxforhealth.hl7.expression.specification.HL7Specification;
import io.github.linuxforhealth.hl7.parsing.HL7DataExtractor;
import io.github.linuxforhealth.hl7.parsing.result.ParsingResult;

public class HL7MessageData implements InputDataExtractor {
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
  public EvaluationResult extractMultipleValuesForSpec(Specification spec,
      Map<String, EvaluationResult> contextValues) {
    HL7Specification hl7spec = (HL7Specification) spec;
    EvaluationResult valuefromVariables;
    if (StringUtils.isNotBlank(hl7spec.getSegment())) {
      valuefromVariables = contextValues.get(hl7spec.getSegment());
    } else if (StringUtils.isNotBlank(hl7spec.getField())) {
      valuefromVariables = contextValues.get(hl7spec.getField());
    } else {
      valuefromVariables = null;
    }

    Object obj = null;
    if (valuefromVariables != null) {
      obj = valuefromVariables.getValue();
    }
    EvaluationResult res = null;
    try {
      if (obj instanceof Segment) {
        res = extractSpecValuesFromSegment(obj, hl7spec);

      } else if (obj instanceof Type) {
        res = extractSpecValuesFromField(obj, hl7spec);
      } else if (obj == null) {
        res = extractSpecValues(hl7spec);

      }
    } catch (DataExtractionException e) {
      LOGGER.error("cannot extract value for variable {} ", hl7spec, e);
    }

    return res;

  }





  private EvaluationResult extractSpecValues(HL7Specification hl7spec) {
    if (StringUtils.isNotBlank(hl7spec.getSegment())) {
      ParsingResult<?> res;
      if (StringUtils.isNotBlank(hl7spec.getField())) {
        res = hde.get(hl7spec.getSegment(), hl7spec.getField());
      } else {
        res = hde.getAllStructures(hl7spec.getSegment());
      }

      if (res != null) {
        return EvaluationResultFactory.getEvaluationResult(res.getValue());
    }
    }
    return new EmptyEvaluationResult();
  }


  private EvaluationResult extractSpecValuesFromSegment(Object obj, HL7Specification hl7spec) {
    if (StringUtils.isNotBlank(hl7spec.getField()) && NumberUtils.isCreatable(hl7spec.getField())) {
      int field = NumberUtils.toInt(hl7spec.getField());
      ParsingResult<?> res = hde.getTypes((Segment) obj, field);
      if (res != null && !res.isEmpty()) {
        return new SimpleEvaluationResult<>(res.getValues());
      } else {
        return null;
      }

    } else {
      return new SimpleEvaluationResult<>(obj);
    }
  }


  private EvaluationResult extractSpecValuesFromField(Object obj, HL7Specification hl7spec) {

    if (hl7spec.getComponent() >= 0) {
      ParsingResult<?> res;
      if (hl7spec.getSubComponent() >= 0) {
        res = hde.getComponent((Type) obj, hl7spec.getComponent(), hl7spec.getSubComponent());
      } else {
        res = hde.getComponent((Type) obj, hl7spec.getComponent());
      }
      
      if(res!=null && !res.isEmpty()) {
        return new SimpleEvaluationResult<>(res.getValues());
      } else {
        return null;
      }
    } else {
      return new SimpleEvaluationResult<>(obj);
    }



  }


  public HL7DataExtractor getHL7DataParser() {
    return hde;
  }


  @Override
  public EvaluationResult evaluateJexlExpression(String expression,
      Map<String, EvaluationResult> contextValues) {
    Preconditions.checkArgument(StringUtils.isNotBlank(expression), "jexlExp cannot be blank");
    Preconditions.checkArgument(contextValues != null, "context cannot be null");
    String trimedJexlExp = StringUtils.trim(expression);
    Map<String, Object> localContext = new HashMap<>();
    Map<String, EvaluationResult> resolvedVariables = new HashMap<>(contextValues);
    resolvedVariables.forEach((key, value) -> localContext.put(key, value.getValue()));
    Object obj = JEXL.evaluate(trimedJexlExp, localContext);
    if (obj != null) {
      return new SimpleEvaluationResult<>(obj);
    } else {
      return new EmptyEvaluationResult();
    }
  }


  @Override
  public String getName() {
    return this.hde.getMessageType();
  }


  @Override
  public String getId() {
    return this.hde.getMessageId();
  }



  @Override
  public EvaluationResult extractValueForSpec(Specification spec,
      Map<String, EvaluationResult> contextValues) {
    EvaluationResult fetchedValue = this.extractMultipleValuesForSpec(spec, contextValues);
    if (fetchedValue != null && !fetchedValue.isEmpty()) {
      return new SimpleEvaluationResult<>(getSingleValue(fetchedValue.getValue()));
    } else {
      return new EmptyEvaluationResult();
    }

  }

  private static Object getSingleValue(Object object) {
    if (object instanceof List) {
      List value = (List) object;
      if (value.isEmpty()) {
        return null;
      } else {
        return value.get(0);
      }

    }
    return object;
  }






}
