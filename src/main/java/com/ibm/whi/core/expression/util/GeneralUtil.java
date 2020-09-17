/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.core.expression.util;

import org.apache.commons.lang3.StringUtils;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Type;


public class GeneralUtil {

  private GeneralUtil() {}

  public static String getDataType(Object data) {

    if (data instanceof Structure) {
      return ((Structure) data).getName();
    } else if (data instanceof Type) {
      return ((Type) data).getName();
    } else {
      return data.getClass().getSimpleName();
    }
  }

  public static boolean isVar(String name) {
    return StringUtils.isNotBlank(name) && name.startsWith("$") && name.length() > 1;
  }

  public static String getVarName(String name) {
    if (isVar(name)) {
      return StringUtils.removeStart(name, "$");
    }
    return name;
  }

}
