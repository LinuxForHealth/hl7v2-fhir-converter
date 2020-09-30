/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.data;

import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.UnsupportedTemporalTypeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringTokenizer;
import org.hl7.fhir.r4.model.codesystems.EncounterStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.whi.hl7.data.date.DateUtil;


public class Hl7RelatedGeneralUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(Hl7RelatedGeneralUtils.class);

  private Hl7RelatedGeneralUtils() {}

  public static String extractLow(Object var) {
    String val = Hl7DataHandlerUtil.getStringValue(var);
    if (StringUtils.isNotBlank(val)) {
      StringTokenizer stk = new StringTokenizer(val, "-");
      if (stk.hasNext())
        return stk.next();
    }
    return null;
  }


  public static String extractHigh(Object var) {
    String val = Hl7DataHandlerUtil.getStringValue(var);
    if (StringUtils.isNotBlank(val)) {
      String[] values = val.split("-");
      if (values.length == 2) {
        return values[1];
      }
    }
    return null;
  }

  public static String getEncounterStatus(Object var1, Object var2, Object var3) {
    LOGGER.info("Generating encounter status from var1{}, var2 {}, var3 {}", var1, var2, var3);
    EncounterStatus status = EncounterStatus.UNKNOWN;
    if (var1 != null) {
      status = EncounterStatus.FINISHED;
    } else if (var2 != null) {
      status = EncounterStatus.ARRIVED;
    } else if (var3 != null) {
      status = EncounterStatus.CANCELLED;
    }
    return status.toCode();
  }



  public static String generateName(Object prefix, Object given, Object family, Object suffix) {
    LOGGER.info("Generating name from  from prefix {}, given {}, family {} ,suffix {}", prefix,
        given, family, suffix);
    StringBuilder sb = new StringBuilder();
    String valprefix = Hl7DataHandlerUtil.getStringValue(prefix);
    String valgiven = Hl7DataHandlerUtil.getStringValue(given);
    String valfamily = Hl7DataHandlerUtil.getStringValue(family);
    String valsuffix = Hl7DataHandlerUtil.getStringValue(suffix);

    if (valprefix != null) {

      sb.append(valprefix).append(" ");
    }
    if (valgiven != null) {
      sb.append(valgiven).append(" ");
    }
    if (valfamily != null) {
      sb.append(valfamily).append(" ");
    }
    if (valsuffix != null) {
      sb.append(valsuffix).append(" ");
    }
    String name = sb.toString();
    if (StringUtils.isNotBlank(name)) {
      return name.trim();
    } else {
      return null;
    }

  }

  /**
   * Returns difference between time2 - time1 in minutes
   * 
   * @param start DateTime
   * @param end DateTime
   * @return Minutes in Long
   */

  public static Long diffDateMin(Object start, Object end) {
    LOGGER.info("Generating time diff in min  from var1{}, var2 {}", start, end);
    try {
      Temporal date1 = DateUtil.getTemporal(Hl7DataHandlerUtil.getStringValue(start));
      Temporal date2 = DateUtil.getTemporal(Hl7DataHandlerUtil.getStringValue(end));
      if (date1 != null && date2 != null) {

        return ChronoUnit.MINUTES.between(date1, date2);

      }
    } catch (UnsupportedTemporalTypeException e) {
      LOGGER.error("Cannot evaluate time difference for {} {} ", start, end, e);
      return null;
    }
    return null;
  }

  public static String split(Object input, String delimitter, int index) {
    String stringRepVal = Hl7DataHandlerUtil.getStringValue(input);
    if (StringUtils.isNotBlank(stringRepVal)) {
      StringTokenizer stk = new StringTokenizer(stringRepVal, delimitter);
     if(stk.getTokenList().size()> index) {
      return stk.getTokenList().get(index);
     }

    }
    return null;
  }


}
