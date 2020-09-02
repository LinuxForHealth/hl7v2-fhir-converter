package com.ibm.whi.hl7.data;


import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.codesystems.EncounterStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ca.uhn.hl7v2.model.Type;
import ca.uhn.hl7v2.model.Variable;

public class GeneralUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(GeneralUtils.class);

  private GeneralUtils() {}

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



  public static long diffDateMin(LocalDateTime time1, LocalDateTime time2) {
    LOGGER.info("Generating time diff in min  from var1{}, var2 {}", time1, time2);
    if (time1 != null && time2 != null) {
      return ChronoUnit.MINUTES.between(time1, time2);
    } else {
      return 0;
    }
  }


  public static Period generatePeriod(LocalDateTime start, LocalDateTime end) {
    LOGGER.info("Generating period datatype  from start{}, end {}", start, end);
    if (start != null && end != null) {
      Period p = new Period();
      Date d = Date.from(start.toInstant(ZoneOffset.UTC));
      p.setStart(d);
      Date e = Date.from(end.toInstant(ZoneOffset.UTC));
      p.setStart(e);
      return p;


    } else {
      return null;
    }
  }

  public static String generateName(String prefix, String given, String family, String suffix) {
    LOGGER.info("Generating name from  from prefix {}, given {}, family {} ,suffix {}", prefix,
        given, family, suffix);
    return String.join(" ", prefix, given, family, suffix);
  }


  public static Object getObservationValue(Object observation, Object observationType,
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


  public static Object getPerformer(Object practitioner, Object device, Object organization) {
    return null;
  }

}
