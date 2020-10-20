/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.data;

import java.util.List;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Type;


public class DataTypeUtil {

  private DataTypeUtil() {}


  public static String getDataType(Object data) {
    String dataType = null;
    Object value = data;
    if (data instanceof List && !((List) data).isEmpty()) {
      value = ((List) data).get(0);
    }

    if (value instanceof Structure) {
      dataType = ((Structure) value).getName();
    } else if (value instanceof Type) {
      dataType = ((Type) value).getName();
    } else if (value != null) {
      dataType = value.getClass().getSimpleName();
    } else {
      dataType = null;
    }
    return dataType;
  }


}
