package com.ibm.whi.core.expression.util;

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
      return data.getClass().getCanonicalName();
    }
  }

}
