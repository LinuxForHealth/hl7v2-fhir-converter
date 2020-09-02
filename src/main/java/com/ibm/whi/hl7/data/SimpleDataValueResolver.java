package com.ibm.whi.hl7.data;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ca.uhn.hl7v2.model.primitive.DT;
import ca.uhn.hl7v2.model.primitive.TSComponentOne;

public class SimpleDataValueResolver {
  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDataValueResolver.class);


  public static final DataEvaluator<Object, LocalDate> LOCAL_DATE = (Object value) -> {
    LOGGER.info("parsing value to Localdate {}  ", value);
    if (value != null) {
      LOGGER.info("parsing value to Localdate {} type {} ", value, value.getClass());
    }
    LocalDate date = null;
    if (value instanceof ca.uhn.hl7v2.model.primitive.DT) {
      date = Hl7DataHandlerUtil.toLocalDate((ca.uhn.hl7v2.model.primitive.DT) value);
    } else if (value instanceof TSComponentOne) {
      date = Hl7DataHandlerUtil.toLocalDate((TSComponentOne) value);
    } else if (value instanceof String) {
      date = LocalDate.parse((String) value, DateTimeFormatter.BASIC_ISO_DATE);
    }
    return date;
  };

  public static final DataEvaluator<Object, LocalDateTime> LOCAL_DATE_TIME = (Object value) -> {

    LocalDateTime date = null;
    if (value instanceof DT) {
      date = Hl7DataHandlerUtil.toLocalDateTime((DT) value);
    } else if (value instanceof TSComponentOne) {
      date = Hl7DataHandlerUtil.toLocalDateTime((TSComponentOne) value);
    } else if (value instanceof String) {
      date = LocalDateTime.parse((String) value, DateTimeFormatter.ISO_INSTANT);
    }


    return date;
  };

  public static final DataEvaluator<Object, String> STRING = (Object value) -> {
    return Hl7DataHandlerUtil.getStringValue(value);


  };


  public static final DataEvaluator<Object, URI> URI_VAL = (Object value) -> {

    try {
      String val = Hl7DataHandlerUtil.getStringValue(value);
      if (val != null) {
        return URI.create(val);
      } else {
        return null;
      }
    } catch (IllegalArgumentException e) {
      LOGGER.warn("Value not valid URI, value: {}", value, e);
      return null;
    }

  };

  public static final DataEvaluator<Object, String> ADMINISTRATIVE_GENDER = (Object value) -> {

    String val = Hl7DataHandlerUtil.getStringValue(value);
    if (null == val) {
      return AdministrativeGender.UNKNOWN.toCode();
    }
        String code;
    if ("F".equalsIgnoreCase(val) || ("Female").equalsIgnoreCase(val)) {
          code = "female";
    } else if ("M".equalsIgnoreCase(val) || "male".equalsIgnoreCase(val)) {
          code = "male";
    } else if ("O".equalsIgnoreCase(val) || "other".equalsIgnoreCase(val)) {
          code = "other";
        } else {
          code = "unknown";
        }

        return AdministrativeGender.fromCode(code).toCode();

  };
  
  public static final DataEvaluator<Object, String> OBSERVATION_STATUS = (Object value) -> {
    String val = Hl7DataHandlerUtil.getStringValue(value);
    if (null == val) {
          return ObservationStatus.UNKNOWN.toCode();
        }
        ObservationStatus status;
    switch (StringUtils.upperCase(val, Locale.ENGLISH)) {
          case "C":
            status = ObservationStatus.CORRECTED;
            break;
          case "D":
            status = ObservationStatus.CANCELLED;
            break;
          case "F":
            status = ObservationStatus.FINAL;
            break;
          case "I":
            status = ObservationStatus.REGISTERED;
            break;
          case "P":
            status = ObservationStatus.PRELIMINARY;
            break;
          case "W":
            status = ObservationStatus.ENTEREDINERROR;
            break;
          default:
            status = ObservationStatus.UNKNOWN;

        }

        return status.toCode();

  };
  



  

  public static final DataEvaluator<Object, Boolean> BOOLEAN = (Object value) -> {
    String val = Hl7DataHandlerUtil.getStringValue(value);
    if (null == val) {
      return false;
    }
    return BooleanUtils.toBoolean(val);

  };

  public static final DataEvaluator<Object, Integer> INTEGER = (Object value) -> {
    String val = Hl7DataHandlerUtil.getStringValue(value);
    if (null == val) {
      return null;
    }
    if (NumberUtils.isCreatable(val)) {
      return NumberUtils.createInteger(val);
    } else {
      LOGGER.warn("Value {} for INTEGER is not a valid number so returning null.", value);
      return null;
    }
    
  };

  public static final DataEvaluator<Object, Float> FLOAT = (Object value) -> {
    String val = Hl7DataHandlerUtil.getStringValue(value);
    if (null == val) {
      return null;
    }
    if (NumberUtils.isCreatable(val)) {
      return NumberUtils.createFloat(val);
      } else {
      LOGGER.warn("Value {} for DECIMAL is not a valid number so returning null.", value);
        return null;
      }

  };


  public static final DataEvaluator<Object, Instant> INSTANT = (Object value) -> {
    LocalDateTime date = null;
    if (value instanceof DT) {
      date = Hl7DataHandlerUtil.toLocalDateTime((DT) value);
    } else if (value instanceof TSComponentOne) {
      date = Hl7DataHandlerUtil.toLocalDateTime((TSComponentOne) value);
    } else if (value instanceof String) {
      date = LocalDateTime.parse((String) value, DateTimeFormatter.ISO_INSTANT);
    }
    if (date != null) {
      return date.toInstant(ZoneOffset.UTC);
    } else {
      return null;
    }

  };

  private SimpleDataValueResolver() {}



  private static Object getUUID(String value) {
    if (value != null) {
      try {
        return UUID.fromString(value);
      } catch (IllegalArgumentException e) {
        LOGGER.warn("Value not valid UUID, value: {}", value, e);
        return null;
      }
    } else {
      LOGGER.info("Value for  UUID is null, value: {}", value);
      return null;
    }
  }





}
