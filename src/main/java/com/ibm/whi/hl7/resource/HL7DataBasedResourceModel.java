package com.ibm.whi.hl7.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.core.expression.Expression;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.expression.Variable;
import com.ibm.whi.core.expression.util.GeneralUtil;
import com.ibm.whi.core.message.InputData;
import com.ibm.whi.core.resource.ResourceModel;
import com.ibm.whi.hl7.expression.Hl7Expression;
import com.ibm.whi.hl7.expression.JELXExpression;
import com.ibm.whi.hl7.expression.ReferenceExpression;
import com.ibm.whi.hl7.expression.SimpleExpression;
import com.ibm.whi.hl7.expression.ValueExtractionGeneralExpression;
import com.ibm.whi.hl7.resource.deserializer.HL7DataBasedResourceDeserializer;


@JsonDeserialize(using = HL7DataBasedResourceDeserializer.class)
public class HL7DataBasedResourceModel implements ResourceModel {

  // public static final String BASE_VALUE_NAME = "baseValue";
  private static final Logger LOGGER = LoggerFactory.getLogger(HL7DataBasedResourceModel.class);


  private Map<String, Expression> expressions;
  private String spec;
  private int order;
  private String name;

  public HL7DataBasedResourceModel(String name, Map<String, Expression> expressions,
      String hl7spec) {
    this(name, expressions, hl7spec, 0);


  }

  public HL7DataBasedResourceModel(String name, Map<String, Expression> expressions, String hl7spec,
      int order) {
    this.expressions = new HashMap<>();
    this.expressions.putAll(expressions);
    this.spec = hl7spec;
    this.order = order;
    this.name = name;

  }



  public HL7DataBasedResourceModel(String name, Map<String, Expression> expressions) {
    this(name, expressions, null);
  }

  public Map<String, Expression> getExpressions() {
    return expressions;
  }

  @Override
  public List<?> evaluateMultiple(InputData dataSource, Map<String, GenericResult> contextValues,
      List<GenericResult> baseVariables, List<Variable> variables) {

    List<Object> resolvedvalues = new ArrayList<>();
    for (GenericResult baseVar : baseVariables) {
      Map<String, GenericResult> localContext =
          resolveLocalVariables(dataSource, baseVar, variables, ImmutableMap.copyOf(contextValues));
      Object obj = evaluateSingle(dataSource, localContext, baseVar);
      if (obj != null) {
        resolvedvalues.add(obj);
      }
    }
    if (resolvedvalues.isEmpty()) {
      return null;
    }
    return resolvedvalues;

  }

  @Override
  public Object evaluateSingle(InputData dataSource, Map<String, GenericResult> variables,
      GenericResult baseVariable) {

    LOGGER.info("Started Evaluating resource {}", this.name);
    Map<String, GenericResult> localContext = new HashMap<>(variables);
    if (baseVariable != null && !baseVariable.isEmpty()) {
      localContext.put(baseVariable.getKlassName(), baseVariable);
    }
    Map<String, Object> resolveValues = new HashMap<>();
    Map<String, Expression> expressionMap = this.getExpressions();


    Map<String, Expression> defaultExp =
        expressionMap.entrySet().stream().filter(e -> (e.getValue() instanceof SimpleExpression))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


    Map<String, Expression> refExp =
        expressionMap.entrySet().stream().filter(e -> (e.getValue() instanceof ReferenceExpression))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


    Map<String, Expression> hl7Exps =
        expressionMap.entrySet().stream().filter(e -> (e.getValue() instanceof Hl7Expression))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    Map<String, Expression> resourceRef = expressionMap.entrySet().stream()
        .filter(e -> (e.getValue() instanceof ValueExtractionGeneralExpression))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


    Map<String, Expression> valueref =
        expressionMap.entrySet().stream().filter(e -> (e.getValue() instanceof JELXExpression))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));



    for (Entry<String, Expression> entry : refExp.entrySet()) {

      ReferenceExpression exp = (ReferenceExpression) entry.getValue();
      LOGGER.info("----Evaluating {} {}", exp.getType(), entry.getKey());
      LOGGER.info("----Extracted reference resource  {} {} reference {}", exp.getType(),
          entry.getKey(), exp.getReference());
      if (exp.getData() != null) {

        GenericResult obj = exp.evaluate(dataSource, ImmutableMap.copyOf(localContext));
        LOGGER.info("----Extracted object from reference resource  {} {} reference {}  value {}",
            exp.getType(), entry.getKey(), exp.getReference(), obj);
        if (obj != null) {

          resolveValues.put(entry.getKey(), obj.getValue());
        }
      }
    }

    executeExpression(dataSource, localContext, resolveValues, hl7Exps);
    executeExpression(dataSource, localContext, resolveValues, resourceRef);

    executeExpression(dataSource, localContext, resolveValues, defaultExp);

    executeExpression(dataSource, localContext, resolveValues, valueref);


    resolveValues.values().removeIf(Objects::isNull);
    if (resolveValues.isEmpty()) {
      return null;
    } else {
      return resolveValues;
    }

  }

  private static void executeExpression(InputData dataSource,
      Map<String, GenericResult> localContext, Map<String, Object> resolveValues,
      Map<String, Expression> hl7Exps) {
    for (Entry<String, Expression> entry : hl7Exps.entrySet()) {
      Expression exp = entry.getValue();
      LOGGER.info("Evaluating {} {}", entry.getKey(), entry.getValue());
      GenericResult obj = exp.evaluate(dataSource, localContext);
      LOGGER.info("Evaluated {} {} value returned {} ", entry.getKey(), entry.getValue(), obj);

      if (obj != null) {

        resolveValues.put(entry.getKey(), obj.getValue());
      } else if (exp.getDefaultValue() != null) {
        resolveValues.put(entry.getKey(), exp.getDefaultValue());
      }
    }
  }

  public int getOrder() {
    return order;
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

  private static Map<String, GenericResult> resolveLocalVariables(InputData dataExtractor,
      Object hl7Value, List<Variable> variableNames,
      ImmutableMap<String, GenericResult> contextValues) {
    Map<String, GenericResult> localVariables = new HashMap<>(contextValues);

    if (hl7Value != null) {
      String type = GeneralUtil.getDataType(hl7Value);

      LOGGER.info(type);
      localVariables.put(type, new GenericResult(hl7Value));
    }

    localVariables
        .putAll(dataExtractor.resolveVariables(variableNames, ImmutableMap.copyOf(localVariables)));

    return ImmutableMap.copyOf(localVariables);
  }



}
