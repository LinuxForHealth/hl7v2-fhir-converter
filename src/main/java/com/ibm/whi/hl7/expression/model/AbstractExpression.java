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
import com.ibm.whi.hl7.data.DataEvaluator;
import com.ibm.whi.hl7.data.SimpleDataTypeMapper;
import com.ibm.whi.hl7.exception.DataExtractionException;
import com.ibm.whi.hl7.expression.Expression;
import com.ibm.whi.hl7.expression.Hl7SpecResult;
import com.ibm.whi.hl7.expression.Variable;
import com.ibm.whi.hl7.parsing.Hl7DataExtractor;
import ca.uhn.hl7v2.model.Segment;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Visitable;

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



  static Map<String, Object> resolveVariables(List<Variable> variables,
      Map<String, Object> context) {

    Map<String, Object> localContext = new HashMap<>();

    for (Variable var : variables) {
      try {
      Object value = getValueVariable(var.getSpec(), context);
        Object resolvedValue = value;
        if (value != null && !var.getType().equalsIgnoreCase(Variable.OBJECT_TYPE)) {

          DataEvaluator<Object, ?> resolver = SimpleDataTypeMapper.getValueResolver(var.getType());
          if (resolver != null) {
            resolvedValue = resolver.apply(value);
            LOGGER.info("Evaluating variable type {} , spec {} resolved value {} ", var.getType(),
                var.getSpec(), resolvedValue);
          }

          localContext.put(var.getName(), resolvedValue);
      } else {
        localContext.put(var.getName(), value);
      }
      } catch (DataExtractionException e) {
        LOGGER.error("cannot extract value for variable {} ", var.getName(), e);
      }
    }
    return localContext;
  }



  static List<String> getTokens(String value) {
    if (StringUtils.isNoneBlank(value)) {
      StringTokenizer st = new StringTokenizer(value, "|").setIgnoreEmptyTokens(true)
          .setTrimmerMatcher(StringMatcherFactory.INSTANCE.spaceMatcher());
      return st.getTokenList();
    }


    return new ArrayList<>();
  }



  static Hl7SpecResult getValuesFromSpecs(List<String> hl7specs, Map<String, Object> context) {
    if (hl7specs.isEmpty()) {
      return null;
    }
    Hl7SpecResult fetchedValue = null;
    for (String hl7specValue : hl7specs) {

      fetchedValue = valuesFromHl7Message(hl7specValue, context);
      // break the loop and return
      if (fetchedValue != null && fetchedValue.isNotEmpty()) {
        return fetchedValue;
      }
    }
    return fetchedValue;


  }

  static Object getValueFromSpecs(List<String> hl7specs, Map<String, Object> context) {
    if (hl7specs.isEmpty()) {
      return null;
    }
    Object fetchedValue = null;
    for (String hl7specValue : hl7specs) {

      fetchedValue = valueFromHl7Message(hl7specValue, context);
      // break the loop and return
      if (fetchedValue != null) {
        return fetchedValue;
      }
    }
    return fetchedValue;


  }



  static Object getValueVariable(List<String> variableOptions, Map<String, Object> context) {
    if (variableOptions.isEmpty()) {
      return null;
    }
    Object fetchedValue = null;
    for (String varName : variableOptions) {

      if (isVar(varName)) {

        fetchedValue = getVariableValue(varName, context);
      } else {
        fetchedValue = valueFromHl7Message(varName, context);
      }
      // break the loop and return
      if (fetchedValue != null) {
        return fetchedValue;
      }
    }
    return fetchedValue;


  }



  static Hl7SpecResult valuesFromHl7Message(String hl7specs, Map<String, Object> context) {
    Hl7SpecResult res = null;

    String[] tokens = StringUtils.split(hl7specs, HL7_SPEC_SPLITTER.pattern());
    Object obj = context.get(tokens[0]);
    Hl7DataExtractor hde = (Hl7DataExtractor) context.get("hde");


    try {
    if (obj instanceof Segment) {
      int field = NumberUtils.toInt(tokens[1]);
        List<Visitable> fetchedValues = new ArrayList<>();
      fetchedValues.addAll((List<Visitable>) hde.getTypes((Segment) obj, field));
        res = new Hl7SpecResult(fetchedValues);

    } else if (obj instanceof Type) {
        List<Visitable> fetchedValues = new ArrayList<>();
      int component = NumberUtils.toInt(tokens[1]);
      fetchedValues.add(hde.getComponent((Type) obj, component));
        res = new Hl7SpecResult(fetchedValues);
      } else if (tokens.length == 2) {
        String fetchedValue = hde.get(tokens[0], tokens[1]);
        res = new Hl7SpecResult(fetchedValue);

      } else {
        List<Visitable> fetchedValues = new ArrayList<>();
      fetchedValues.addAll(hde.getAllStructures(tokens[0]));
        res = new Hl7SpecResult(fetchedValues);
      }
    } catch (DataExtractionException e) {
      LOGGER.error("cannot extract value for variable {} ", hl7specs, e);
    }

    return res;

  }


  static Object valueFromHl7Message(String hl7specs, Map<String, Object> context) {
    Hl7SpecResult vals = valuesFromHl7Message(hl7specs, context);
    if (vals == null || vals.isEmpty()) {
      return null;
    } else {
      if (!vals.getHl7DatatypeValue().isEmpty()) {
        return vals.getHl7DatatypeValue().get(0);
      } else {
        return vals.getTextValue();
      }

    }

  }

  static Object getVariableValue(String varName, Map<String, Object> context) {
    if (StringUtils.isNotBlank(varName)) {
      Object fetchedValue;
      fetchedValue = context.get(varName.replace("$", ""));
      return fetchedValue;
    } else {
      return null;
    }
  }



  static boolean isVar(String hl7spec) {
    return StringUtils.isNotBlank(hl7spec) && hl7spec.startsWith("$") && hl7spec.length() > 1;
  }

  static String getDataType(Object data) {

    if (data instanceof Structure) {
      return ((Structure) data).getName();
    } else if (data instanceof Type) {
      return ((Type) data).getName();
    } else {
      return data.getClass().getCanonicalName();
    }
  }


}
