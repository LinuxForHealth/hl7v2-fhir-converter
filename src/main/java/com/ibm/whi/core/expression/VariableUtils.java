/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.core.expression;

import org.apache.commons.lang3.StringUtils;

public class VariableUtils {

  private VariableUtils() {}

  public static boolean isVar(String hl7spec) {
    return StringUtils.isNotBlank(hl7spec) && hl7spec.startsWith("$") && hl7spec.length() > 1;
  }


  public static String getVarName(String name) {
    if (isVar(name)) {
      return StringUtils.removeStart(name, "$");
    }
    return name;
  }





}
