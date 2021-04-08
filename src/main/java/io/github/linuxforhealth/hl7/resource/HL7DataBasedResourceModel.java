/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.resource;

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
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.Expression;
import io.github.linuxforhealth.api.InputDataExtractor;
import io.github.linuxforhealth.api.ResourceModel;
import io.github.linuxforhealth.api.ResourceValue;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.exception.DataExtractionException;
import io.github.linuxforhealth.core.exception.RequiredConstraintFailureException;
import io.github.linuxforhealth.core.resource.ResourceResult;
import io.github.linuxforhealth.core.resource.SimpleResourceValue;
import io.github.linuxforhealth.hl7.expression.Hl7Expression;
import io.github.linuxforhealth.hl7.expression.JEXLExpression;
import io.github.linuxforhealth.hl7.expression.ReferenceExpression;
import io.github.linuxforhealth.hl7.expression.ResourceExpression;
import io.github.linuxforhealth.hl7.expression.SimpleExpression;
import io.github.linuxforhealth.hl7.expression.ValueExtractionGeneralExpression;
import io.github.linuxforhealth.hl7.resource.deserializer.HL7DataBasedResourceDeserializer;


@JsonDeserialize(using = HL7DataBasedResourceDeserializer.class)
public class HL7DataBasedResourceModel implements ResourceModel {

  private static final String EVALUATING = "Evaluating {} {}";


  private static final Logger LOGGER = LoggerFactory.getLogger(HL7DataBasedResourceModel.class);


  private Map<String, Expression> expressions;
  private String spec;

  private String name;

  /**
   * 
   * @param name
   * @param expressions
   * @param hl7spec
   */

  public HL7DataBasedResourceModel(String name, Map<String, Expression> expressions,
      String hl7spec) {
    this.expressions = new HashMap<>();
    this.expressions.putAll(expressions);
    this.spec = hl7spec;

    this.name = name;


  }



  public HL7DataBasedResourceModel(String name, Map<String, Expression> expressions) {
    this(name, expressions, null);
  }

  @Override
  public Map<String, Expression> getExpressions() {
    return expressions;
  }



  @Override
  public ResourceResult evaluate(InputDataExtractor dataSource,
      Map<String, EvaluationResult> variables, EvaluationResult baseValue) {
    ResourceResult resources = null;

    try {


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
          expressionMap.entrySet().stream().filter(e -> (e.getValue() instanceof JEXLExpression))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

      // initialize the map and list to collect values
      List<ResourceValue> additionalResolveValues = new ArrayList<>();
      Map<String, Object> resolveValues = new HashMap<>();

      LOGGER.info("Started Evaluating resource expressions for {}", this.name);
      evaluateResourceExpression(dataSource, localContext, resourceExp, additionalResolveValues,
          resolveValues, baseValue);
      LOGGER.info("Started Evaluating reference resource expression for {}", this.name);
      evaluateReferenceExpression(dataSource, localContext, refResourceExp, additionalResolveValues,
          resolveValues, baseValue);

      LOGGER.info("Started Evaluating HL7 expression for {}", this.name);
      executeExpression(dataSource, localContext, resolveValues, hl7Exps, baseValue);

      LOGGER.info("Started Evaluating value extraction expression for {}", this.name);
      executeExpression(dataSource, localContext, resolveValues, valueExtractionExp, baseValue);

      LOGGER.info("Started Evaluating Simple expression for {}", this.name);
      executeExpression(dataSource, localContext, resolveValues, defaultExp, baseValue);

      LOGGER.info("Started Evaluating JEXL expressions for {}", this.name);
      executeExpression(dataSource, localContext, resolveValues, jexlExp, baseValue);


      resolveValues.values().removeIf(Objects::isNull);

      if (!resolveValues.isEmpty()) {
        String groupId = getGroupId(localContext);
        resources = new ResourceResult(new SimpleResourceValue(resolveValues, this.name),
            additionalResolveValues, groupId);

      }


    } catch (RequiredConstraintFailureException e) {
      LOGGER.warn("Resource Constraint condition not satisfied for  {} , exception {}", this.name,
          e.getMessage());
      LOGGER.debug("Resource Constraint condition not satisfied for  {} , exception {}", this.name,
          e);
      return null;

    } catch (IllegalArgumentException | IllegalStateException | DataExtractionException e) {
      LOGGER.error("Exception during  resource {} evaluation reason {}", this.name, e);
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


      EvaluationResult obj = exp.evaluate(dataSource, ImmutableMap.copyOf(localContext), baseValue);
      LOGGER.debug("Extracted object from reference resource  {} {} reference {}  value {}",
          exp.getType(), entry.getKey(), exp.getReference(), obj);
      if (obj != null && !obj.isEmpty()) {
        // Check if the key already exist in the HashMap, if found append, do not replace
        if(!resolveValues.containsKey(getKeyName(entry.getKey()))) {
          resolveValues.put(getKeyName(entry.getKey()), obj.getValue());
        } else {
          Object existing = resolveValues.get(getKeyName(entry.getKey()));
          if (existing instanceof List) {
            if (obj.getValue() instanceof List) {
              ((List<Object>)existing).addAll(obj.getValue());
            } else {
              ((List<Object>)existing).add(obj.getValue());
            }
          }
        }
        if (obj.getAdditionalResources() != null && !obj.getAdditionalResources().isEmpty()) {
          additionalResolveValues.addAll(obj.getAdditionalResources());
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
      LOGGER.debug("Extracted resource  {} {} reference {}", exp.getType(), entry.getKey(),
          exp.getResource());


      EvaluationResult obj = exp.evaluate(dataSource, ImmutableMap.copyOf(localContext), baseValue);
      LOGGER.debug("Extracted object from reference resource  {} {} reference {}  value {}",
          exp.getType(), entry.getKey(), exp.getResource(), obj);
      if (obj != null && !obj.isEmpty()) {
        // Check if the key already exist in the HashMap, if found append, do not replace
        if(!resolveValues.containsKey(getKeyName(entry.getKey()))) {
          resolveValues.put(getKeyName(entry.getKey()), obj.getValue());
        } else {
          Object existing = resolveValues.get(getKeyName(entry.getKey()));
          if (existing instanceof List) {
            if (obj.getValue() instanceof List) {
              ((List<Object>)existing).addAll(obj.getValue());
            } else {
              ((List<Object>)existing).add(obj.getValue());
            }
          }
        }
        if (obj.getAdditionalResources() != null && !obj.getAdditionalResources().isEmpty()) {
          additionalResolveValues.addAll(obj.getAdditionalResources());
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

        // Check if the key already exist in the HashMap, if found append, do not replace
        if(!resolveValues.containsKey(getKeyName(entry.getKey()))) {
          resolveValues.put(getKeyName(entry.getKey()), obj.getValue());
        } else {
          Object existing = resolveValues.get(getKeyName(entry.getKey()));
          if (existing instanceof List) {
            if (obj.getValue() instanceof List) {
              ((List<Object>)existing).addAll(obj.getValue());
            } else {
              ((List<Object>)existing).add(obj.getValue());
            }
          }
        }
      }
    }
  }


  public String getSpec() {
    return spec;
  }


  @Override
  public String getName() {
    return this.name;
  }


}
