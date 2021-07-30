/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.expression.condition;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import io.github.linuxforhealth.api.EvaluationResult;
import io.github.linuxforhealth.core.expression.SimpleEvaluationResult;

public class SimpleBiConditionTest {

  // EQUALS on strings tests
  @Test
  public void simple_EQUALS_condition_tests() {
    reflexive_comparison_tests_expected_TRUE("abc", "EQUALS", "abc");
    reflexive_comparison_tests_expected_FALSE("abc", "EQUALS", "ac");
  }

  // EQUALS on integers tests
  @Test
  public void simple_EQUALS_on_integers_condition_tests() {
    reflexive_comparison_tests_expected_TRUE(33, "EQUALS", 33);
    reflexive_comparison_tests_expected_FALSE(29, "EQUALS", 30);
  }

  // EQUALS_INTEGER tests
  @Test
  public void simple_EQUALS_INTEGER_condition_tests() {
    reflexive_comparison_tests_expected_TRUE(33, "EQUALS_INTEGER", 33);
    reflexive_comparison_tests_expected_FALSE(29, "EQUALS_INTEGER", 30);
  }

  // NOT_EQUALS tests
  @Test
  public void simple_NOT_EQUALS_condition_tests() {
    reflexive_comparison_tests_expected_TRUE("abc", "NOT_EQUALS", "ac");
    reflexive_comparison_tests_expected_FALSE("abc", "NOT_EQUALS", "abc");
  }

  // NOT_EQUALS on integers tests
  @Test
  public void simple_NOT_EQUALS_on_integers_condition_tests() {
    reflexive_comparison_tests_expected_TRUE(33, "NOT_EQUALS", 30);
    reflexive_comparison_tests_expected_FALSE(29, "NOT_EQUALS", 29);
  }

  // NOT_EQUALS_INTEGER tests
  @Test
  public void simple_NOT_EQUALS_INTEGER_condition_tests() {
    reflexive_comparison_tests_expected_TRUE(30, "NOT_EQUALS_INTEGER", 33);
    reflexive_comparison_tests_expected_FALSE(29, "NOT_EQUALS_INTEGER", 29);
  }

  // CONTAINS tests
  @Test
  public void simple_CONTAINS_condition_tests() {
    directional_comparison_tests_expected_TRUE("wxyz", "CONTAINS", "xyz");
    directional_comparison_tests_expected_TRUE("wxyz", "CONTAINS", "wxyz"); // same expected true
    directional_comparison_tests_expected_FALSE("xyz", "CONTAINS", "wxyz");
  }

  // NOT_CONTAINS tests
  @Test
  public void simple_NOT_CONTAINS_condition_tests() {
    directional_comparison_tests_expected_TRUE("wxy", "NOT_CONTAINS", "wxyz");
    directional_comparison_tests_expected_FALSE("wxyz", "NOT_CONTAINS", "xyz");
    directional_comparison_tests_expected_FALSE("wxyz", "NOT_CONTAINS", "wxyz"); // same expected false
  }

  // ENDS_WITH tests
  @Test
  public void simple_ENDS_WITH_condition_tests() {
    directional_comparison_tests_expected_TRUE("wxyz", "ENDS_WITH", "xyz");
    directional_comparison_tests_expected_TRUE("wxyz", "ENDS_WITH", "wxyz"); // same expected true
    directional_comparison_tests_expected_FALSE("wxyz", "ENDS_WITH", "w");
  }

  // NOT_ENDS_WITH tests
  @Test
  public void simple_NOT_ENDS_WITH_condition_tests() {
    directional_comparison_tests_expected_TRUE("wxyz", "NOT_ENDS_WITH", "wxy");
    directional_comparison_tests_expected_FALSE("wxyz", "NOT_ENDS_WITH", "xyz");
    directional_comparison_tests_expected_FALSE("wxyz", "NOT_ENDS_WITH", "wxyz"); // same expected false
  }

  // STARTS_WITH tests
  @Test
  public void simple_STARTS_WITH_condition_tests() {
    directional_comparison_tests_expected_TRUE("abcde", "STARTS_WITH", "abcd");
    directional_comparison_tests_expected_TRUE("abcde", "STARTS_WITH", "abcde"); // same expected true
    directional_comparison_tests_expected_FALSE("abcde", "STARTS_WITH", "bc");
  }

  // NOT_STARTS_WITH tests
  @Test
  public void simple_NOT_STARTS_WITH_condition_tests() {
    directional_comparison_tests_expected_TRUE("abcde", "NOT_STARTS_WITH", "bcde");
    directional_comparison_tests_expected_FALSE("abcde", "NOT_STARTS_WITH", "abc");
    directional_comparison_tests_expected_FALSE("abcde", "NOT_STARTS_WITH", "abcde"); // same expected false
  }

  // GREATER_THAN tests
  @Test
  public void simple_GREATER_THAN_condition_tests() {
    directional_comparison_tests_expected_TRUE(33, "GREATER_THAN", 6);
    directional_comparison_tests_expected_FALSE(6, "GREATER_THAN", 33);
  }

  // GREATER_THAN_INTEGER tests
  @Test
  public void simple_GREATER_THAN_INTEGER_condition_tests() {
    directional_comparison_tests_expected_TRUE(33, "GREATER_THAN_INTEGER", 6);
    directional_comparison_tests_expected_FALSE(6, "GREATER_THAN_INTEGER", 33);
  }

  // LESS_THAN tests
  @Test
  public void simple_LESS_THAN_condition_tests() {
    directional_comparison_tests_expected_TRUE(3, "LESS_THAN", 6);
    directional_comparison_tests_expected_FALSE(6, "LESS_THAN", 3);
  }

  // LESS_THAN_INTEGER tests
  @Test
  public void simple_LESS_THAN_INTEGER_condition_tests() {
    directional_comparison_tests_expected_TRUE(3, "LESS_THAN_INTEGER", 6);
    directional_comparison_tests_expected_FALSE(6, "LESS_THAN_INTEGER", 3);
  }

  // GREATER_THAN_OR_EQUAL_TO tests
  @Test
  public void simple_GREATER_THAN_OR_EQUAL_TO_condition_tests() {
    directional_comparison_tests_expected_TRUE(33, "GREATER_THAN_OR_EQUAL_TO", 6);
    directional_comparison_tests_expected_FALSE(6, "GREATER_THAN_OR_EQUAL_TO", 33);
    reflexive_comparison_tests_expected_TRUE(33, "GREATER_THAN_OR_EQUAL_TO", 33);
  }

  // GREATER_THAN_OR_EQUAL_TO_INTEGER tests
  @Test
  public void simple_GREATER_THAN_OR_EQUAL_TO_INTEGER_condition_tests() {
    directional_comparison_tests_expected_TRUE(33, "GREATER_THAN_OR_EQUAL_TO_INTEGER", 6);
    directional_comparison_tests_expected_FALSE(6, "GREATER_THAN_OR_EQUAL_TO_INTEGER", 33);
    reflexive_comparison_tests_expected_TRUE(33, "GREATER_THAN_OR_EQUAL_TO_INTEGER", 33);
  }

  // LESS_THAN_OR_EQUAL_TO tests
  @Test
  public void simple_LESS_THAN_OR_EQUAL_TO_condition_tests() {
    directional_comparison_tests_expected_TRUE(3, "LESS_THAN_OR_EQUAL_TO", 6);
    directional_comparison_tests_expected_FALSE(6, "LESS_THAN_OR_EQUAL_TO", 3);
    reflexive_comparison_tests_expected_TRUE(3, "LESS_THAN_OR_EQUAL_TO", 3);
  }

  // LESS_THAN_OR_EQUAL_TO_INTEGER tests
  @Test
  public void simple_LESS_THAN_OR_EQUAL_TO_INTEGER_condition_tests() {
    directional_comparison_tests_expected_TRUE(3, "LESS_THAN_OR_EQUAL_TO_INTEGER", 6);
    directional_comparison_tests_expected_FALSE(6, "LESS_THAN_OR_EQUAL_TO_INTEGER", 3);
    reflexive_comparison_tests_expected_TRUE(3, "LESS_THAN_OR_EQUAL_TO_INTEGER", 3);
  }

  // Directional comparisons are only true in one direction
  // The caller excpects all these comparisons to be TRUE
  private void directional_comparison_tests_expected_TRUE(Object value1, String comparison, Object value2) {
    // value1 as $var1 compares to value2 as constant should be true
    String condition = "$var1 " + comparison + " " + value2.toString();
    SimpleBiCondition simplecondition = (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult(value1));
    contextVariables.put("var2", new SimpleEvaluationResult(value2));
    assertThat(simplecondition.test(contextVariables)).isTrue();

    // forward: value1 as $var1 compares to value2 as $var2 should be true
    condition = "$var1 " + comparison + " $var2";
    simplecondition = (SimpleBiCondition) ConditionUtil.createCondition(condition);
    assertThat(simplecondition.test(contextVariables)).isTrue();

  }

  // Directional comparisons are only true in one direction
  // The caller excpects all these comparisons to be FALSE
  private void directional_comparison_tests_expected_FALSE(Object value1, String comparison, Object value2) {
    // value1 as $var1 compares to value2 as constant should be false
    String condition = "$var1 " + comparison + " " + value2.toString();
    SimpleBiCondition simplecondition = (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult(value1));
    contextVariables.put("var2", new SimpleEvaluationResult(value2));
    assertThat(simplecondition.test(contextVariables)).isFalse();

    // forward: value1 as $var1 compares to value2 as $var2 should be false
    condition = "$var1 " + comparison + " $var2";
    simplecondition = (SimpleBiCondition) ConditionUtil.createCondition(condition);
    assertThat(simplecondition.test(contextVariables)).isFalse();

    // missing variable: value1 as $var1 compares to missing $var3 should be false
    condition = "$var1 " + comparison + " $var3";
    simplecondition = (SimpleBiCondition) ConditionUtil.createCondition(condition);
    assertThat(simplecondition.test(contextVariables)).isFalse();

  }

  // Reflexive comparisons give the same result if the values are reversed in order
  // The caller excpects all these comparisons to be TRUE
  private void reflexive_comparison_tests_expected_TRUE(Object value1, String comparison, Object value2) {
    // value1 as $var1 compares to value2 as constant should be true
    String condition = "$var1 " + comparison + " " + value2.toString();
    SimpleBiCondition simplecondition = (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult(value1));
    contextVariables.put("var2", new SimpleEvaluationResult(value2));
    assertThat(simplecondition.test(contextVariables)).isTrue();

    // reverse: value2 as $var2 compares to value1 as constant should be true
    condition = "$var2 " + comparison + " " + value1.toString();
    simplecondition = (SimpleBiCondition) ConditionUtil.createCondition(condition);
    assertThat(simplecondition.test(contextVariables)).isTrue();

    // forward: value1 as $var1 compares to value2 as $var2 should be true
    condition = "$var1 " + comparison + " $var2";
    simplecondition = (SimpleBiCondition) ConditionUtil.createCondition(condition);
    assertThat(simplecondition.test(contextVariables)).isTrue();

    // reverse: value2 as $var2 compares to value1 as $var1 should be true
    condition = "$var2 " + comparison + " $var1";
    simplecondition = (SimpleBiCondition) ConditionUtil.createCondition(condition);
    assertThat(simplecondition.test(contextVariables)).isTrue();

  }

  // Reflexive comparisons give the same result if the values are reversed in order
  // The caller excpects all these comparisons to be FALSE
  private void reflexive_comparison_tests_expected_FALSE(Object value1, String comparison, Object value2) {
    // value1 as $var1 compares to value2 as constant should be false
    String condition = "$var1 " + comparison + " " + value2.toString();
    SimpleBiCondition simplecondition = (SimpleBiCondition) ConditionUtil.createCondition(condition);
    Map<String, EvaluationResult> contextVariables = new HashMap<>();
    contextVariables.put("var1", new SimpleEvaluationResult(value1));
    contextVariables.put("var2", new SimpleEvaluationResult(value2));
    assertThat(simplecondition.test(contextVariables)).isFalse();

    // reverse: value2 as $var2 compares to value1 as constant should be false
    condition = "$var2 " + comparison + " " + value1.toString();
    simplecondition = (SimpleBiCondition) ConditionUtil.createCondition(condition);
    assertThat(simplecondition.test(contextVariables)).isFalse();

    // forward: value1 as $var1 compares to value2 as $var2 should be false
    condition = "$var1 " + comparison + " $var2";
    simplecondition = (SimpleBiCondition) ConditionUtil.createCondition(condition);
    assertThat(simplecondition.test(contextVariables)).isFalse();

    // reverse: value2 as $var2 compares to value1 as $var1 should be false
    condition = "$var2 " + comparison + " $var1";
    simplecondition = (SimpleBiCondition) ConditionUtil.createCondition(condition);
    assertThat(simplecondition.test(contextVariables)).isFalse();

    // missing variable: value1 as $var1 compares to missing $var3 should be false
    condition = "$var1 " + comparison + " $var3";
    simplecondition = (SimpleBiCondition) ConditionUtil.createCondition(condition);
    assertThat(simplecondition.test(contextVariables)).isFalse();

  }

}
