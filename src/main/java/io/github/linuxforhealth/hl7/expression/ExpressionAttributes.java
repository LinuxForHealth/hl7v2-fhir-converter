/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.linuxforhealth.hl7.expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.matcher.StringMatcherFactory;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.linuxforhealth.api.Condition;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.api.Expression;
import io.github.linuxforhealth.api.Specification;
import io.github.linuxforhealth.api.Variable;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.expression.VariableUtils;
import io.github.linuxforhealth.core.expression.condition.ConditionUtil;
import io.github.linuxforhealth.hl7.expression.specification.SpecificationParser;
import io.github.linuxforhealth.hl7.expression.variable.VariableGenerator;

@JsonDeserialize(builder = ExpressionAttributes.Builder.class)
public class ExpressionAttributes {
  private static final String OBJECT_TYPE = Object.class.getSimpleName();


  // Basic properties of an expression
  private String type;
  private EvaluationResult defaultValue;
  private boolean isRequired;
  private List<Specification> specs;
  private List<Variable> variables;
  private Condition filter; // filter is applies to the specs, spec values that pass the condition
                            // are used for evaluating the expression.
  private Map<String, String> constants;


  // Specific expression properties
  // Property specific to JEXLExpression
  private String evaluate;

  // Property specific to ReferenceExpression
  private String referenceResource;

  // Property specific to ResourceExpression
  private String resourceToGenerate;

  // Property specific to ResourceExpression and ReferenceExpression
  private boolean isGenerateMultipleResource;



  // Property specific to SimpleExpression
  private String value;

  // Property specific to ValueExtractionGeneralExpression
  private ImmutablePair<String, String> fetch;
  private boolean useGroup;
  private Class<? extends Expression> expressionType;

  private ExpressionAttributes(Builder exBuilder) {
    if (exBuilder.type == null) {
      this.type = OBJECT_TYPE;
    } else {
      this.type = exBuilder.type;
    }
    if (exBuilder.defaultValue != null) {
      this.defaultValue = exBuilder.defaultValue;
    } else {
      this.defaultValue = null;
    }

    this.isRequired = exBuilder.isRequired;
    this.specs = getSpecList(exBuilder.rawSpecs, exBuilder.useGroup);
    if (StringUtils.isNotBlank(exBuilder.rawCondition)) {
      this.filter = ConditionUtil.createCondition(exBuilder.rawCondition);
    }


    this.constants = new HashMap<>();
    if (exBuilder.constants != null && !exBuilder.constants.isEmpty()) {
      this.constants.putAll(exBuilder.constants);
    }


    this.variables = new ArrayList<>();
    if (exBuilder.rawVariables != null) {
      for (Entry<String, String> e : exBuilder.rawVariables.entrySet()) {
        this.variables.add(VariableGenerator.parse(e.getKey(), e.getValue()));
      }

    }

    this.fetch = getPair(exBuilder.rawFetch);
    this.value = exBuilder.value;
    this.resourceToGenerate = exBuilder.resourceToGenerate;
    this.isGenerateMultipleResource = exBuilder.isGenerateMultipleResource;
    this.evaluate = exBuilder.evaluate;
    this.referenceResource = exBuilder.referenceResource;
    this.value = exBuilder.value;
    this.expressionType = exBuilder.expressionType;
    this.useGroup = exBuilder.useGroup;

    if (this.expressionType == null && CollectionUtils.isNotEmpty(this.specs)) {
      this.expressionType = Hl7Expression.class;
    }



  }

  public boolean isUseGroup() {
    return useGroup;
  }


  public String getType() {
    return type;
  }



  public EvaluationResult getDefaultValue() {
    return defaultValue;
  }



  public boolean isRequired() {
    return isRequired;
  }



  public List<Specification> getSpecs() {
    return specs;
  }



  public List<Variable> getVariables() {
    return variables;
  }



  public Condition getFilter() {
    return filter;
  }



  public Map<String, String> getConstants() {
    return constants;
  }

  public String getEvaluate() {
    return evaluate;
  }



  public String getReferenceResource() {
    return referenceResource;
  }



  public String getResourceToGenerate() {
    return resourceToGenerate;
  }



  public boolean isGenerateMultipleResource() {
    return isGenerateMultipleResource;
  }



  public String getValue() {
    return value;
  }



  public ImmutablePair<String, String> getFetch() {
    return fetch;
  }



  public Class<? extends Expression> getExpressionType() {
    return expressionType;
  }


  private static List<Specification> getSpecList(String inputString, boolean useGroup) {
    final boolean extractMultiple;
    String hl7SpecExpression = inputString;
    if (StringUtils.endsWith(inputString, "*")) {
      hl7SpecExpression = StringUtils.removeEnd(inputString, "*");
      extractMultiple = true;
    } else {
      extractMultiple = false;
    }

    hl7SpecExpression = StringUtils.strip(hl7SpecExpression);
    List<Specification> specs = new ArrayList<>();
    if (StringUtils.isNotBlank(hl7SpecExpression)) {
      StringTokenizer st = new StringTokenizer(hl7SpecExpression, "|").setIgnoreEmptyTokens(true)
          .setTrimmerMatcher(StringMatcherFactory.INSTANCE.spaceMatcher());
      st.getTokenList()
          .forEach(s -> specs.add(SpecificationParser.parse(s, extractMultiple, useGroup)));
    }

    return specs;
  }

  private static ImmutablePair<String, String> getPair(String tok) {
    if (StringUtils.isNotBlank(tok)) {
      String[] token = tok.split(":");
      if (token.length == 2) {
        return new ImmutablePair<>(VariableUtils.getVarName(token[0]), token[1]);
      } else if (token.length == 1) {
        return new ImmutablePair<>(Constants.BASE_VALUE_NAME, token[0]);
      } else {

        throw new IllegalArgumentException(
            "fetch token not in correct format, expected format $varName:key, input" + tok);
      }
    } else {
      return null;
    }

  }



  public static class Builder {



    private String type;
    private EvaluationResult defaultValue;
    private boolean isRequired;
    private String rawSpecs;
    private String rawCondition;
    private Map<String, String> rawVariables;
    private Map<String, String> constants;
    private boolean useGroup;
    private Class<? extends Expression> expressionType;
    // Specific expression properties
    // Property specific to JEXLExpression
    private String evaluate;

    // Property specific to ReferenceExpression
    private String referenceResource;

    // Property specific to ResourceExpression
    private String resourceToGenerate;

    // Property specific to ResourceExpression and ReferenceExpression
    private boolean isGenerateMultipleResource;


    // Property specific to SimpleExpression
    private String value;

    // Property specific to ValueExtractionGeneralExpression
    private String rawFetch;



    public Builder() {}

    public Builder(String singleValue) {
      value = singleValue;
      this.expressionType = SimpleExpression.class;
    }

    public boolean isUseGroup() {
      return useGroup;
    }



    public Builder withType(String type) {
      this.type = type;
      return this;
    }

    public Builder withDefault(EvaluationResult defaultValue) {
      this.defaultValue = defaultValue;
      return this;
    }

    public Builder withRequired(boolean isRequired) {
      this.isRequired = isRequired;
      return this;
    }

    public Builder withSpecs(String rawSpecs) {
      this.rawSpecs = rawSpecs;
      return this;
    }

    public Builder withUseGroup(boolean useGroup) {
      this.useGroup = useGroup;
      return this;
    }

    public Builder withCondition(String rawCondition) {
      this.rawCondition = rawCondition;
      return this;
    }


    public Builder withVars(Map<String, String> rawVariables) {
      this.rawVariables = rawVariables;
      return this;
    }


    public Builder withConstants(Map<String, String> constants) {
      this.constants = constants;
      return this;
    }

    public Builder withEvaluate(String evaluate) {
      this.evaluate = evaluate;
      this.expressionType = JEXLExpression.class;
      return this;
    }

    public Builder withReference(String reference) {
      if (reference.endsWith("*")) {
        this.isGenerateMultipleResource = true;
        reference = StringUtils.removeEnd(reference, "*");
      }
      this.referenceResource = StringUtils.strip(reference);
      this.expressionType = ReferenceExpression.class;
      return this;
    }

    public Builder withResource(String resourceToGenerate) {
      if (resourceToGenerate.endsWith("*")) {
        this.isGenerateMultipleResource = true;
        resourceToGenerate = StringUtils.removeEnd(resourceToGenerate, "*");
      }
      this.resourceToGenerate = StringUtils.strip(resourceToGenerate);
      this.expressionType = ResourceExpression.class;

      return this;
    }

    public Builder withValue(String value) {
      this.value = value;
      this.expressionType = SimpleExpression.class;
      return this;
    }

    public Builder withFetch(String fetch) {
      this.rawFetch = fetch;
      this.expressionType = ValueExtractionGeneralExpression.class;
      return this;
    }


    public ExpressionAttributes build() {
      return new ExpressionAttributes(this);
    }

  }
}

