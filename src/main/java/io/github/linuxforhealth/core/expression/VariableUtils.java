/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.expression;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringTokenizer;

public class VariableUtils {

  private VariableUtils() {}

  public static boolean isVar(String hl7spec) {
    return StringUtils.isNotBlank(hl7spec) && hl7spec.startsWith("$") && hl7spec.length() > 1;
  }


  public static String getVarName(String name) {
    String varName = name;
    if (isVar(name) && StringUtils.contains(name, ".")) {
      StringTokenizer stk = new StringTokenizer(name, ".");
      varName = StringUtils.removeStart(stk.nextToken(), "$");
    } else if (isVar(name)) {
      varName = StringUtils.removeStart(name, "$");
    }

    if (StringUtils.endsWith(varName, "?")) {
      varName = StringUtils.removeEnd(varName, "?");
    }
    return varName;
  }

  public static boolean isFuzzyMatch(String value) {
    return StringUtils.isNotBlank(value) && value.endsWith("?");
  }




}
