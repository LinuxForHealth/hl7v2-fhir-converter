package com.ibm.whi.core.expression.condition;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import com.ibm.whi.core.expression.GenericResult;

public class SimpleBiConditionTest {

  @Test
  public void simple_condition_is_evaluated_true() {
    String condition = "$var1 EQUALS abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, GenericResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new GenericResult("abc"));
    assertThat(simplecondition.test(contextVariables)).isTrue();
  }


  @Test
  public void simple_condition_with_two_variables_is_evaluated_true() {
    String condition = "$var1 EQUALS $var2";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, GenericResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new GenericResult("abc"));
    contextVariables.put("var2", new GenericResult("abc"));
    assertThat(simplecondition.test(contextVariables)).isTrue();
  }

  @Test
  public void simple_condition_with_two_variables_is_evaluated_false_when_condition_is_not_satisfied() {
    String condition = "$var1 EQUALS $var2";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, GenericResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new GenericResult("abc"));
    contextVariables.put("var2", new GenericResult("xyz"));
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }



  @Test
  public void simple_condition_is_evaluated_false() {
    String condition = "$var1 EQUALS abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, GenericResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new GenericResult("abcdf"));
    assertThat(simplecondition.test(contextVariables)).isFalse();
  }


  @Test
  public void simple_condition_is_evaluated_false_when_var_not_found() {
    String condition = "$var1 EQUALS abc";
    SimpleBiCondition simplecondition =
        (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, GenericResult> contextVariables = new HashMap<>();

    assertThat(simplecondition.test(contextVariables)).isFalse();
  }
}
