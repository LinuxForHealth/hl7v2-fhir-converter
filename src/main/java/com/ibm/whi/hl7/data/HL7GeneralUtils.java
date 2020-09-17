package com.ibm.whi.hl7.data;


import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.UnsupportedTemporalTypeException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.codesystems.EncounterStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.whi.hl7.data.date.DateUtil;

public class HL7GeneralUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(HL7GeneralUtils.class);

  private HL7GeneralUtils() {}

  public static String getEncounterStatus(String var1, String var2, String var3) {
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





  public static String generateName(String prefix, String given, String family, String suffix) {
    LOGGER.info("Generating name from  from prefix {}, given {}, family {} ,suffix {}", prefix,
        given, family, suffix);
    StringBuilder sb = new StringBuilder();
    if (prefix != null) {
      sb.append(prefix).append(" ");
    }
    if (given != null) {
      sb.append(given).append(" ");
    }
    if (family != null) {
      sb.append(family).append(" ");
    }
    if (suffix != null) {
      sb.append(suffix).append(" ");
    }
    String name = sb.toString();
    if (StringUtils.isNotBlank(name)) {
      return name.trim();
    } else {
      return null;
    }

  }





}
