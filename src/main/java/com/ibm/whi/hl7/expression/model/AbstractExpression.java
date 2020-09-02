package com.ibm.whi.hl7.expression.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.matcher.StringMatcherFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.hl7.data.DataEvaluator;
import com.ibm.whi.hl7.data.SimpleDataTypeMapper;
import com.ibm.whi.hl7.exception.DataExtractionException;
import com.ibm.whi.hl7.expression.Expression;
import com.ibm.whi.hl7.expression.GenericResult;
import com.ibm.whi.hl7.expression.Variable;
import com.ibm.whi.hl7.parsing.Hl7DataExtractor;
import com.ibm.whi.hl7.parsing.result.ParsingResult;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Type;

public abstract class AbstractExpression implements Expression {
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExpression.class);

  protected static final Pattern HL7_SPEC_SPLITTER = Pattern.compile(".");
  private final String type;
  private final Object defaultValue;
  private final boolean required;
  private final String hl7spec;
  private List<Variable> variables;

  public AbstractExpression(String type, Object defaultValue, boolean required, String hl7spec,
      Map<String, String> rawvariables) {
    if (type == null) {
      this.type = "String";
    } else {
      this.type = type;
    }
    this.defaultValue = defaultValue;
    this.required = required;
    this.hl7spec = hl7spec;


    initVariables(rawvariables);

  }

  private void initVariables(Map<String, String> rawvariables) {
    this.variables = new ArrayList<>();
    if (rawvariables != null) {
      for (Entry<String, String> e : rawvariables.entrySet()) {
        StringTokenizer stk = new StringTokenizer(e.getValue(), " ").setIgnoreEmptyTokens(true);
        if (stk.getTokenList().size() == 2) {
          String vartype = stk.nextToken();

          String specs = stk.nextToken();

          this.variables.add(new Variable(e.getKey(), getTokens(specs), vartype));
        } else {


          this.variables.add(new Variable(e.getKey(), getTokens(e.getValue())));
        }
      }

    }
  }

  public String getType() {
    return type;
  }

  public Object getDefaultValue() {
    return defaultValue;
  }

  public boolean isRequired() {
    return required;
  }

  public List<String> getHl7specs() {


    return getTokens(this.hl7spec);
  }

  public List<Variable> getVariables() {
    return variables;
  }



  protected static Map<String, GenericResult> resolveVariables(List<Variable> variables,
      ImmutableMap<String, ?> executables, ImmutableMap<String, GenericResult> varables) {

    Map<String, GenericResult> localVariables = new HashMap<>();

    for (Variable var : variables) {
      try {
        GenericResult value = getValueVariable(var.getSpec(), executables, varables);
        Object resolvedValue = value;
        if (value != null && !var.getType().equalsIgnoreCase(Variable.OBJECT_TYPE)) {

          DataEvaluator<Object, ?> resolver = SimpleDataTypeMapper.getValueResolver(var.getType());
          if (resolver != null) {
            resolvedValue = resolver.apply(value.getValue());
            LOGGER.info("Evaluating variable type {} , spec {} resolved value {} ", var.getType(),
                var.getSpec(), resolvedValue);
          }

          localVariables.put(var.getName(), new GenericResult(resolvedValue));
        } else if (value != null) {

          localVariables.put(var.getName(), new GenericResult(value.getValue()));
        } else {
          // enclose null in GenericParsingResult
          localVariables.put(var.getName(), new GenericResult(null));
      }
      } catch (DataExtractionException e) {
        LOGGER.error("cannot extract value for variable {} ", var.getName(), e);
      }
    }
    return localVariables;
  }



  private static List<String> getTokens(String value) {
    if (StringUtils.isNoneBlank(value)) {
      StringTokenizer st = new StringTokenizer(value, "|").setIgnoreEmptyTokens(true)
          .setTrimmerMatcher(StringMatcherFactory.INSTANCE.spaceMatcher());
      return st.getTokenList();
    }


    return new ArrayList<>();
  }



  protected static GenericResult getValuesFromSpecs(List<String> hl7specs,
      ImmutableMap<String, ?> executables, ImmutableMap<String, GenericResult> variables) {
    if (hl7specs.isEmpty()) {
      return null;
    }
    ParsingResult<?> fetchedValue = null;
    for (String hl7specValue : hl7specs) {

      fetchedValue = valuesFromHl7Message(hl7specValue, executables, variables);
      // break the loop and return
      if (fetchedValue != null && !fetchedValue.isEmpty()) {
        return new GenericResult(fetchedValue.getValues());
      }
    }
    return null;


  }

  protected static GenericResult getValueFromSpecs(List<String> hl7specs,
      ImmutableMap<String, ?> executables, ImmutableMap<String, GenericResult> variables) {
    if (hl7specs.isEmpty()) {
      return null;
    }
    ParsingResult<?> fetchedValue = null;
    for (String hl7specValue : hl7specs) {

      fetchedValue = valuesFromHl7Message(hl7specValue, executables, variables);
      // break the loop and return
      if (fetchedValue != null && !fetchedValue.isEmpty()) {
        return new GenericResult(fetchedValue.getValue());
      }
    }
    return null;


  }



  private static GenericResult getValueVariable(List<String> variableOptions,
      ImmutableMap<String, ?> executables, ImmutableMap<String, GenericResult> variables) {
    if (variableOptions.isEmpty()) {
      return null;
    }
    GenericResult fetchedValue = null;
    for (String varName : variableOptions) {

      if (isVar(varName)) {

        fetchedValue = getVariableValueFromVariableContextMap(varName, variables);
      } else {

        ParsingResult<?> p = valuesFromHl7Message(varName, executables, variables);
        if (p != null) {
          fetchedValue = new GenericResult(p.getValue());
        }
      }
      // break the loop and return
      if (fetchedValue != null) {
        return fetchedValue;
      }
    }
    return fetchedValue;


  }



  private static ParsingResult<?> valuesFromHl7Message(String hl7specs,
      ImmutableMap<String, ?> executables, ImmutableMap<String, GenericResult> varables) {
    ParsingResult<?> res = null;

    String[] tokens = StringUtils.split(hl7specs, HL7_SPEC_SPLITTER.pattern());
    GenericResult valuefromVariables = varables.get(tokens[0]);

    Hl7DataExtractor hde = (Hl7DataExtractor) executables.get("hde");
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
        res = hde.getComponent((Type) obj, component);

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



  protected static GenericResult getVariableValueFromVariableContextMap(String varName,
      ImmutableMap<String, GenericResult> varables) {
    if (StringUtils.isNotBlank(varName)) {
      GenericResult fetchedValue;
      fetchedValue = varables.get(varName.replace("$", ""));
      return fetchedValue;
    } else {
      return null;
    }
  }



  protected static boolean isVar(String hl7spec) {
    return StringUtils.isNotBlank(hl7spec) && hl7spec.startsWith("$") && hl7spec.length() > 1;
  }

  protected static String getDataType(Object data) {

    if (data instanceof Structure) {
      return ((Structure) data).getName();
    } else if (data instanceof Type) {
      return ((Type) data).getName();
    } else {
      return data.getClass().getCanonicalName();
    }
  }


}
