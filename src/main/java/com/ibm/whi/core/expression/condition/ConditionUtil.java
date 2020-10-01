/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.core.expression.condition;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringTokenizer;
import com.google.common.base.Preconditions;
import com.ibm.whi.api.Condition;

/**
 * Utility class to create different conditions from string value.
 * 
 *
 */
public class ConditionUtil {
  private ConditionUtil() {}


  public static Condition createCondition(String conditionString) {
    Preconditions.checkArgument(StringUtils.isNotBlank(conditionString),
        "conditionString cannot be blank or null.");
    StringTokenizer ors = new StringTokenizer(conditionString, "||");
    StringTokenizer ands = new StringTokenizer(conditionString, "&&");
    if (ors.getTokenList().size() > 1) {
      return getListOrConditions(ors);
    } else if (ands.getTokenList().size() > 1) {
      return getListAndConditions(ands);
    } else {
      return createSimpleCondition(conditionString);
    }


  }

  private static Condition createSimpleCondition(String conditionString) {
    StringTokenizer stk = new StringTokenizer(conditionString);
    if (stk.getTokenList().size() == 2) {
      String var1 = stk.nextToken();
      String var2 = stk.nextToken();

      if (var2.equalsIgnoreCase(CheckNotNull.NOT_NULL)) {
        return new CheckNotNull(var1);
      } else if (var2.equalsIgnoreCase(CheckNull.NULL)) {
        return new CheckNull(var1);
      } else {
        throw new IllegalArgumentException("Condition string incorrect format");
      }
    } else if (stk.getTokenList().size() == 3) {
      String var1 = stk.nextToken();
      String operator = stk.nextToken();
      String var2 = stk.nextToken();

      return new SimpleBiCondition(var1, var2, operator);
    } else {
      throw new IllegalArgumentException("Condition string incorrect format");
    }
  }

  private static CompountAndCondition getListAndConditions(StringTokenizer ands) {
    List<Condition> conditions = new ArrayList<>();
    for (String tok : ands.getTokenList()) {
      conditions.add(createSimpleCondition(tok));
    }
    return new CompountAndCondition(conditions);
  }

  private static CompountORCondition getListOrConditions(StringTokenizer ors) {
    List<Condition> conditions = new ArrayList<>();
    for (String tok : ors.getTokenList()) {
      conditions.add(createSimpleCondition(tok));
    }
    return new CompountORCondition(conditions);
  }

}
