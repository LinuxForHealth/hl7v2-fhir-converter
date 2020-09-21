/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.core.data;

import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Type;


public class DataTypeUtil {

  private DataTypeUtil() {}


  public static String getDataType(Object data) {
    String dataType = null;
    if (data instanceof Structure) {
      dataType = ((Structure) data).getName();
    } else if (data instanceof Type) {
      dataType = ((Type) data).getName();
    } else if (data != null) {
      dataType = data.getClass().getSimpleName();
    } else {
      dataType = null;
    }
    return dataType;
  }


}
