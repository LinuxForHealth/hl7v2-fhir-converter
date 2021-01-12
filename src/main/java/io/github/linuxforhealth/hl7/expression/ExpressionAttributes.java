/*
 * (C) Copyright IBM Corp. 2021
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
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.matcher.StringMatcherFactory;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.github.linuxforhealth.api.Condition;
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
  private String defaultValue;
  private boolean isRequired;
  private List<Specification> specs;
  private List<Variable> variables;
  private Condition condition; // filter is applies to the specs, spec values that pass the
                               // condition
  // are used for evaluating the expression.
  private Map<String, String> constants;
  private String value;
  private String valueOf;
  private boolean useGroup;
  private ExpressionType expressionType;
  private String toString;

  // if valueof attribute ends with * then list of values will be generated
  private boolean generateMultiple;



  // Property specific to ValueExtractionGeneralExpression
  private ImmutablePair<String, String> fetch;

  private ExpressionAttributes(Builder exBuilder) {
    if (exBuilder.type == null) {
      this.type = OBJECT_TYPE;
    } else {
      this.type = exBuilder.type;
    }

    this.defaultValue = exBuilder.defaultValue;


    this.isRequired = exBuilder.isRequired;
    this.specs = getSpecList(exBuilder.rawSpecs, exBuilder.useGroup);
    if (StringUtils.isNotBlank(exBuilder.rawCondition)) {
      this.condition = ConditionUtil.createCondition(exBuilder.rawCondition);
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


    this.value = exBuilder.value;
    this.valueOf = exBuilder.valueOf;
    this.generateMultiple = exBuilder.generateList;

    this.value = exBuilder.value;
    this.expressionType = exBuilder.expressionType;
    this.useGroup = exBuilder.useGroup;

    if (this.expressionType == null && CollectionUtils.isNotEmpty(this.specs)) {
      this.expressionType = ExpressionType.HL7SPEC;
    }


  }

  public boolean isUseGroup() {
    return useGroup;
  }


  public String getType() {
    return type;
  }



  public String getDefaultValue() {
    return defaultValue;
  }



  public boolean isRequired() {
    return isRequired;
  }



  public List<Specification> getSpecs() {
    return ImmutableList.copyOf(specs);
  }



  public List<Variable> getVariables() {
    return ImmutableList.copyOf(variables);
  }



  public Condition getFilter() {
    return condition;
  }



  public Map<String, String> getConstants() {
    return ImmutableMap.copyOf(constants);
  }



  public boolean isGenerateMultiple() {
    return generateMultiple;
  }



  public String getValue() {
    return value;
  }



  public ImmutablePair<String, String> getFetch() {
    return fetch;
  }



  public ExpressionType getExpressionType() {
    return expressionType;
  }


  public String getValueOf() {
    return valueOf;
  }


  public static List<Specification> getSpecList(String inputString, boolean useGroup) {
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

  @Override
  public String toString() {
    if (this.toString == null) {
      this.toString = ReflectionToStringBuilder.toString(this, ToStringStyle.NO_CLASS_NAME_STYLE,
          false, false, true, null);
    }

    return this.toString;
  }


  public static class Builder {



    private String type;
    private String defaultValue;
    private boolean isRequired;
    private String rawSpecs;
    private String rawCondition;
    private Map<String, String> rawVariables;
    private Map<String, String> constants;
    private boolean useGroup;
    private ExpressionType expressionType;

    private String valueOf;
    private String value;
    private boolean generateList;

    public Builder() {}

    public Builder(String singleValue) {
      value = singleValue;
      this.expressionType = ExpressionType.SIMPLE;
    }

    public boolean isUseGroup() {
      return useGroup;
    }



    public Builder withType(String type) {
      this.type = type;
      return this;
    }

    public Builder withDefault(String defaultValue) {
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

    public Builder withValueOf(String valueOf) {

      this.valueOf = StringUtils.trim(valueOf);
      if (this.expressionType == null) {
        this.expressionType = ExpressionType.SIMPLE;
      }
      return this;

    }

    public Builder withExpressionType(String expressionType) {
      this.expressionType = EnumUtils.getEnumIgnoreCase(ExpressionType.class, expressionType);
      return this;
    }



    public Builder withValue(String value) {
      this.value = value;
      this.expressionType = ExpressionType.SIMPLE;
      return this;
    }

    public Builder withGenerateList(boolean generateList) {
      this.generateList = generateList;
      return this;
    }



    public ExpressionAttributes build() {
      return new ExpressionAttributes(this);
    }

  }
}

