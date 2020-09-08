package com.ibm.whi.hl7.data;


import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import org.hl7.fhir.r4.model.codesystems.EncounterStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.whi.hl7.data.date.DateUtil;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Variable;

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




  public static long diffDateMin(Object time1, Object time2) {
    LOGGER.info("Generating time diff in min  from var1{}, var2 {}", time1, time2);
    Temporal date1 = DateUtil.getTemporal(Hl7DataHandlerUtil.getStringValue(time1));
    Temporal date2 = DateUtil.getTemporal(Hl7DataHandlerUtil.getStringValue(time2));


    if (date1 != null && date2 != null) {

      return ChronoUnit.MINUTES.between(date2, date1);

    } else {
      return 0;
    }
  }





  public static String generateName(String prefix, String given, String family, String suffix) {
    LOGGER.info("Generating name from  from prefix {}, given {}, family {} ,suffix {}", prefix,
        given, family, suffix);

    return String.join(" ", prefix, given, family, suffix);
  }


  public static String getObservationValue(Object observation, Object observationType,
      Object units) {
    LOGGER.info("Generating onservation value from observation {}, observationType {}, units  {}",
        observation, observationType, units);

    if (observation instanceof Variable) {
      Variable v = (Variable) observation;
      Type t = v.getData();
      LOGGER.info("Generating onservation value from observation {} type {}", t, t.getClass());
      return Hl7DataHandlerUtil.getStringValue(t);

    }

    return null;

  }


}
