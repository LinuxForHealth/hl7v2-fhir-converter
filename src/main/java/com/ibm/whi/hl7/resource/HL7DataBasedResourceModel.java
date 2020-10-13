/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.api.EvaluationResult;
import com.ibm.whi.api.Expression;
import com.ibm.whi.api.InputDataExtractor;
import com.ibm.whi.api.ResourceModel;
import com.ibm.whi.api.ResourceValue;
import com.ibm.whi.core.Constants;
import com.ibm.whi.core.resource.ResourceResult;
import com.ibm.whi.core.resource.SimpleResourceValue;
import com.ibm.whi.hl7.exception.RequiredConstraintFailureException;
import com.ibm.whi.hl7.expression.Hl7Expression;
import com.ibm.whi.hl7.expression.JELXExpression;
import com.ibm.whi.hl7.expression.ReferenceExpression;
import com.ibm.whi.hl7.expression.ResourceExpression;
import com.ibm.whi.hl7.expression.SimpleExpression;
import com.ibm.whi.hl7.expression.ValueExtractionGeneralExpression;
import com.ibm.whi.hl7.resource.deserializer.HL7DataBasedResourceDeserializer;


@JsonDeserialize(using = HL7DataBasedResourceDeserializer.class)
public class HL7DataBasedResourceModel implements ResourceModel {

  private static final String EVALUATING = "Evaluating {} {}";


  private static final Logger LOGGER = LoggerFactory.getLogger(HL7DataBasedResourceModel.class);


  private Map<String, Expression> expressions;
  private String spec;

  private String name;
  private String group;

  public HL7DataBasedResourceModel(String name, Map<String, Expression> expressions,
      String hl7spec) {
    this(name, expressions, hl7spec, null);


  }

  public HL7DataBasedResourceModel(String name, Map<String, Expression> expressions, String hl7spec,
      String group) {
    this.expressions = new HashMap<>();
    this.expressions.putAll(expressions);
    this.spec = hl7spec;

    this.name = name;
    this.group = group;

  }



  public HL7DataBasedResourceModel(String name, Map<String, Expression> expressions) {
    this(name, expressions, null);
  }

  public Map<String, Expression> getExpressions() {
    return expressions;
  }



  @Override
  public ResourceResult evaluate(InputDataExtractor dataSource, Map<String, EvaluationResult> variables,
      EvaluationResult baseValue) {
    ResourceResult resources = null;

    try {

      LOGGER.info("Started Evaluating resource {}", this.name);
      Map<String, EvaluationResult> localContext = new HashMap<>(variables);


      Map<String, Expression> expressionMap = this.getExpressions();


      Map<String, Expression> defaultExp =
          expressionMap.entrySet().stream().filter(e -> (e.getValue() instanceof SimpleExpression))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


      Map<String, Expression> resourceExp = expressionMap.entrySet().stream()
          .filter(e -> (e.getValue() instanceof ResourceExpression))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      Map<String, Expression> refResourceExp = expressionMap.entrySet().stream()
          .filter(e -> (e.getValue() instanceof ReferenceExpression))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


      Map<String, Expression> hl7Exps =
          expressionMap.entrySet().stream().filter(e -> (e.getValue() instanceof Hl7Expression))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));



      Map<String, Expression> valueExtractionExp = expressionMap.entrySet().stream()
          .filter(e -> (e.getValue() instanceof ValueExtractionGeneralExpression))
          .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


      Map<String, Expression> jexlExp =
          expressionMap.entrySet().stream().filter(e -> (e.getValue() instanceof JELXExpression))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      // initialize the map and list to collect values
      List<ResourceValue> additionalResolveValues = new ArrayList<>();
      Map<String, Object> resolveValues = new HashMap<>();

      evaluateResourceExpression(dataSource, localContext, resourceExp, additionalResolveValues,
          resolveValues, baseValue);

      evaluateReferenceExpression(dataSource, localContext, refResourceExp, additionalResolveValues,
          resolveValues, baseValue);


      executeExpression(dataSource, localContext, resolveValues, hl7Exps, baseValue);
      executeExpression(dataSource, localContext, resolveValues, valueExtractionExp, baseValue);

      executeExpression(dataSource, localContext, resolveValues, defaultExp, baseValue);

      executeExpression(dataSource, localContext, resolveValues, jexlExp, baseValue);


      resolveValues.values().removeIf(Objects::isNull);
      if (!resolveValues.isEmpty()) {
        String groupId = getGroupId(localContext);
        resources =
            new ResourceResult(new SimpleResourceValue(resolveValues, this.name),
                additionalResolveValues, groupId);

      }


    } catch (RequiredConstraintFailureException e) {
      LOGGER.error("RequiredConstraintFailureException during  resource {} evaluation", this.name,
          e);
      return null;

    } catch (IllegalArgumentException | IllegalStateException e) {
      LOGGER.error("Exception during  resource {} evaluation", this.name, e);
      return null;

    }
    return resources;
  }

  private static String getGroupId(Map<String, EvaluationResult> localContext) {
    EvaluationResult result = localContext.get(Constants.GROUP_ID);
    if (result != null && result.getValue() instanceof String) {
      return result.getValue();
    }
    return null;
  }

  private static void evaluateReferenceExpression(InputDataExtractor dataSource,
      Map<String, EvaluationResult> localContext, Map<String, Expression> refResourceExp,
      List<ResourceValue> additionalResolveValues, Map<String, Object> resolveValues,
      EvaluationResult baseValue) {
    for (Entry<String, Expression> entry : refResourceExp.entrySet()) {

      ReferenceExpression exp = (ReferenceExpression) entry.getValue();
      LOGGER.debug(EVALUATING, exp.getType(), entry.getKey());
      LOGGER.debug("Extracting reference resource  {} {} reference {}", exp.getType(),
          entry.getKey(), exp.getReference());
      if (exp.getData() != null) {

        EvaluationResult obj =
            exp.evaluate(dataSource, ImmutableMap.copyOf(localContext), baseValue);
        LOGGER.debug("Extracted object from reference resource  {} {} reference {}  value {}",
            exp.getType(), entry.getKey(), exp.getReference(), obj);
        if (obj != null && !obj.isEmpty()) {
          resolveValues.put(getKeyName(entry.getKey()), obj.getValue());
          if (obj.getAdditionalResources() != null && !obj.getAdditionalResources().isEmpty()) {
            additionalResolveValues.addAll(obj.getAdditionalResources());
          }
        }
      }
    }

  }

  private static void evaluateResourceExpression(InputDataExtractor dataSource,
      Map<String, EvaluationResult> localContext, Map<String, Expression> resourceExp,
      List<ResourceValue> additionalResolveValues, Map<String, Object> resolveValues,
      EvaluationResult baseValue) {
    for (Entry<String, Expression> entry : resourceExp.entrySet()) {

      ResourceExpression exp = (ResourceExpression) entry.getValue();
      LOGGER.debug(EVALUATING, exp.getType(), entry.getKey());
      LOGGER.debug("Extracted resource  {} {} reference {}", exp.getType(),
          entry.getKey(), exp.getResourceName());
      if (exp.getData() != null) {

        EvaluationResult obj =
            exp.evaluate(dataSource, ImmutableMap.copyOf(localContext), baseValue);
        LOGGER.debug("Extracted object from reference resource  {} {} reference {}  value {}",
            exp.getType(), entry.getKey(), exp.getResourceName(), obj);
        if (obj != null && !obj.isEmpty()) {
          resolveValues.put(getKeyName(entry.getKey()), obj.getValue());
          if (obj.getAdditionalResources() != null && !obj.getAdditionalResources().isEmpty()) {
            additionalResolveValues.addAll(obj.getAdditionalResources());
          }
        }
      }
    }
  }

  private static String getKeyName(String key) {
    return StringUtils.split(key, "_")[0];
  }

  private static void executeExpression(InputDataExtractor dataSource,
      Map<String, EvaluationResult> localContext, Map<String, Object> resolveValues,
      Map<String, Expression> hl7Exps, EvaluationResult baseValue) {
    for (Entry<String, Expression> entry : hl7Exps.entrySet()) {
      Expression exp = entry.getValue();
      LOGGER.debug(EVALUATING, entry.getKey(), entry.getValue());
      EvaluationResult obj = exp.evaluate(dataSource, localContext, baseValue);
      LOGGER.debug("Evaluated {} {} value returned {} ", entry.getKey(), entry.getValue(), obj);

      if (obj != null && !obj.isEmpty()) {

        resolveValues.put(getKeyName(entry.getKey()), obj.getValue());
      } else if (exp.getDefaultValue() != null) {
        resolveValues.put(entry.getKey(), exp.getDefaultValue());
      }
    }
  }


  public String getSpec() {
    return spec;
  }


  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return this.name;
  }


}
