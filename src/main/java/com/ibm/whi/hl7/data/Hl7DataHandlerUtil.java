package com.ibm.whi.hl7.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Composite;
import ca.uhn.hl7v2.model.DataTypeException;
import ca.uhn.hl7v2.model.Primitive;
import ca.uhn.hl7v2.model.primitive.DT;
import ca.uhn.hl7v2.model.primitive.TSComponentOne;


public class Hl7DataHandlerUtil {
  private static final String ERROR_PARSING_VALUE = "Error parsing value {}";
  private static final Logger LOGGER = LoggerFactory.getLogger(Hl7DataHandlerUtil.class);

  private Hl7DataHandlerUtil() {}

  public static LocalDate toLocalDate(DT dt) {
    try {
      if ((dt == null) || (dt.isEmpty())) {
        return null;
      }

      return LocalDate.of(dt.getYear(), dt.getMonth(), dt.getDay());

    } catch (HL7Exception e) {
      LOGGER.error(ERROR_PARSING_VALUE, dt, e);
      return null;
    }
  }

  public static LocalDate toLocalDate(TSComponentOne dtm) {
    try {
      if ((dtm == null) || (dtm.isEmpty())) {
        return null;
      }

      return LocalDate.of(dtm.getYear(), dtm.getMonth(), dtm.getDay());

    } catch (HL7Exception e) {
      LOGGER.error(ERROR_PARSING_VALUE, dtm, e);
      return null;
    }
  }

  public static LocalDateTime toLocalDateTime(DT dt) {
    try {
      if ((dt == null) || (dt.isEmpty())) {
        return null;
      }

      LocalDate ld = LocalDate.of(dt.getYear(), dt.getMonth(), dt.getDay());
      return ld.atStartOfDay();
    } catch (HL7Exception e) {
      LOGGER.error(ERROR_PARSING_VALUE, dt, e);
      return null;
    }
  }

  public static LocalDateTime toLocalDateTime(TSComponentOne dtm) {
    try {
      if ((dtm == null) || (dtm.isEmpty())) {
        return null;
      }

      return LocalDateTime.of(dtm.getYear(), dtm.getMonth(), dtm.getDay(), dtm.getHour(),
          dtm.getMinute(), dtm.getSecond());

    } catch (HL7Exception e) {
      LOGGER.error(ERROR_PARSING_VALUE, dtm, e);
      return null;
    }
  }



  public static String getStringValue(Object obj) {

    if (obj == null) {
      return null;
    }


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
    if (local instanceof Composite) {
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


}
