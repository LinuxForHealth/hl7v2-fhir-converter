/*
 * (C) Copyright IBM Corp. 2020, 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.expression.condition;

import java.util.function.BiPredicate;
import org.apache.commons.lang3.EnumUtils;

/**
 * This is rules predicate enum class that defines different types of predicates and type of input object it acts on
 * @author pbhallam@us.ibm.com
 *
 */
public enum ConditionPredicateEnum {

  GREATER_THAN_INTEGER(ConditionBiPredicates.GREATER_THAN, Integer.class, Integer.class), //
  EQUALS_INTEGER(ConditionBiPredicates.EQUAL_TO, Integer.class, Integer.class), //
  LESS_THAN_INTEGER(ConditionBiPredicates.LESS_THAN, Integer.class, Integer.class), //
  GREATER_THAN_OR_EQUAL_TO_INTEGER(ConditionBiPredicates.GREATER_THAN_OR_EQUAL_TO, Integer.class,
      Integer.class), //
  LESS_THAN_OR_EQUAL_TO_INTEGER(ConditionBiPredicates.LESS_THAN_OR_EQUAL_TO, Integer.class,
      Integer.class), //

  GREATER_THAN_FLOAT(ConditionBiPredicates.GREATER_THAN_FLOAT, Float.class, Float.class), //
  LESS_THAN_FLOAT(ConditionBiPredicates.LESS_THAN_FLOAT, Float.class, Float.class), //
  GREATER_THAN_OR_EQUAL_TO_FLOAT(ConditionBiPredicates.GREATER_THAN_OR_EQUAL_TO_FLOAT, Float.class,
      Float.class), //
  LESS_THAN_OR_EQUAL_TO_FLOAT(ConditionBiPredicates.LESS_THAN_OR_EQUAL_TO_FLOAT, Float.class,
      Float.class), //



  CONTAINS_STRING(ConditionBiPredicates.CONTAINS, String.class, String.class), //
  NOT_CONTAINS_STRING(ConditionBiPredicates.NOT_CONTAINS, String.class, String.class), //
  EQUALS_STRING(ConditionBiPredicates.EQUALS_IC, String.class, String.class), //
  NOT_EQUALS_STRING(ConditionBiPredicates.NOT_EQUALS, String.class, String.class), //
  STARTS_WITH_STRING(ConditionBiPredicates.STARTS_WITH, String.class, String.class), //
  NOT_STARTS_WITH_STRING(ConditionBiPredicates.NOT_STARTS_WITH, String.class, String.class), //
  ENDS_WITH_STRING(ConditionBiPredicates.ENDS_WITH, String.class, String.class), //
  NOT_ENDS_WITH_STRING(ConditionBiPredicates.NOT_ENDS_WITH, String.class, String.class); //

    private BiPredicate<?, ?> predicate;
    private Class<?> klassT;
    private Class<?> klassU;

    ConditionPredicateEnum(BiPredicate<?, ?> p, Class<?> klassT, Class<?> klassU) {
        this.predicate = p;
        this.klassT = klassT;
        this.klassU = klassU;
    }

  public BiPredicate getPredicate() {
        return this.predicate;

    }

    public Class<?> getKlassT() {
        return this.klassT;
    }

    public Class<?> getKlassU() {
        return this.klassU;
    }

  public static ConditionPredicateEnum getConditionPredicate(String conditionOperator,
      String klassSimpleName) {
    // Append the predicate if not already present      
    String enumName = conditionOperator.endsWith(klassSimpleName.toUpperCase()) ? conditionOperator : conditionOperator + "_" + klassSimpleName;
    return EnumUtils.getEnumIgnoreCase(ConditionPredicateEnum.class, enumName);

  }

}
