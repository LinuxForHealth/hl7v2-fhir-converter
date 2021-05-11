/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.data;

import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ca.uhn.hl7v2.model.Composite;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Primitive;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Variable;
import ca.uhn.hl7v2.model.primitive.ID;
import ca.uhn.hl7v2.model.primitive.IS;


public class Hl7DataHandlerUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(Hl7DataHandlerUtil.class);

  private Hl7DataHandlerUtil() {}

  public static String getStringValue(Object obj) {
    return getStringValue(obj, false);
  }

  public static String getStringValue(Object obj, boolean allComponents) {
	  return getStringValue(obj,allComponents,". ");
  }
  
  public static String getStringValue(Object obj, boolean allComponents, String separatorString) {
    if (obj == null) {
      return null;
    }
    LOGGER.debug("Extracting string value for {} type {}", obj, obj.getClass());

    Object local = obj;
    String returnValue;
    if (local instanceof Collection) {
      List<Object> list = ((List) local);
      if (list.size() == 1) {
        returnValue = toStringValue(list.get(0), allComponents);
      } else if (!list.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        list.forEach(e -> sb.append(toStringValue(e, allComponents)).append(separatorString));
        returnValue = StringUtils.strip(sb.toString());
      } else {
        returnValue = null;
      }

    } else {
      returnValue = toStringValue(local, allComponents);
    }

    return returnValue;
  }

  public static String getTableNumber(Object obj) {

    if (obj instanceof ID) {
      ID id = (ID) obj;
      return "v2-" + StringUtils.leftPad(String.valueOf(id.getTable()), 4, '0');
    } else if (obj instanceof IS) {
      IS id = (IS) obj;
      return "v2-" + StringUtils.leftPad(String.valueOf(id.getTable()), 4, '0');
    }
    return null;
  }


  private static String toStringValue(Object local, boolean allComponents) {

    if (local == null) {
      return null;
    }
    String returnvalue;
    if (local instanceof Variable) {
      returnvalue = convertVariesDataTypeToString(local, allComponents);
    } else if (local instanceof Composite) {
      Composite com = (Composite) local;
      if (allComponents) {
        returnvalue = getValueFromComposite(com);
      } else {
        try {
          returnvalue = com.getComponent(0).toString();
        } catch (DataTypeException e) {
          LOGGER.warn("Failure when extracting string value for {}", local, e);
          returnvalue = null;
        }
      }
    } else if (local instanceof Primitive) {
      Primitive prem = (Primitive) local;
      returnvalue = prem.getValue();
    } else {
      returnvalue = local.toString();
    }

    return returnvalue;
  }


  private static String convertVariesDataTypeToString(Object obj, boolean allComponents) {
    if (obj instanceof Variable) {
      Variable v = (Variable) obj;
      return getStringValue(v.getData(), allComponents);
    }
    return obj.toString();
  }

  private static String getValueFromComposite(Composite com) {

      Type[] types = com.getComponents();
      StringBuilder sb = new StringBuilder();
      for (Type t : types) {
      String text = t.toString();
      if (StringUtils.isNotBlank(text)) {
        sb.append(t.toString()).append(", ");
      }
      }
    return StringUtils.stripEnd(sb.toString(), ", ");

  }


}
