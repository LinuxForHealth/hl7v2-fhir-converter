package com.ibm.whi.hl7.data;

import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ca.uhn.hl7v2.model.Composite;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Primitive;
import ca.uhn.hl7v2.model.Structure;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Variable;


public class Hl7DataHandlerUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(Hl7DataHandlerUtil.class);

  private Hl7DataHandlerUtil() {}


  public static String getStringValue(Object obj) {
    if (obj == null) {
      return null;
    }
    LOGGER.info("Extracting string value for {} type {}", obj, obj.getClass());

    Object local = obj;
    if (local instanceof Collection) {
      List list = ((List) local);
      if (!list.isEmpty()) {
        local = list.get(0);
      } else {
        return null;
      }
    }
    String returnvalue;


    if (local instanceof Variable) {
      returnvalue = convertVariesDataTypeToString(local);
    } else if (local instanceof Composite) {
      Composite com = (Composite) local;

      try {
        returnvalue = com.getComponent(0).toString();
      } catch (DataTypeException e) {
        LOGGER.error("Failure when extracting string value for {}", local, e);
        returnvalue = null;
      }
    } else if (local instanceof Primitive) {
      Primitive prem = (Primitive) local;
      returnvalue = prem.getValue();
    } else {
      returnvalue = local.toString();
    }

    return returnvalue;

  }


  private static String convertVariesDataTypeToString(Object obj) {
    if (obj instanceof Variable) {
      Variable v = (Variable) obj;
      return getStringValue(v.getData());
    }
    return obj.toString();
  }


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
