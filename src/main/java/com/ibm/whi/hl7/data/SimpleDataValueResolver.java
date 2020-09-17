/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.data;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.UUID;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Observation.ObservationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.whi.hl7.data.date.DateUtil;

public class SimpleDataValueResolver {
  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDataValueResolver.class);

  private SimpleDataValueResolver() {}
  public static final ValueExtractor<Object, String> DATE = (Object value) -> {

      String val = Hl7DataHandlerUtil.getStringValue(value);
    if (val != null) {
      return DateUtil.formatToDate(val);
    }
    return null;
  };

  public static final ValueExtractor<Object, String> DATE_TIME = (Object value) -> {

    String val = Hl7DataHandlerUtil.getStringValue(value);
    if (val != null) {
      return DateUtil.formatToDateTime(val);
    }
    return null;
  };

  public static final ValueExtractor<Object, String> STRING = (Object value) -> {
    return Hl7DataHandlerUtil.getStringValue(value);


  };

  public static final ValueExtractor<Object, String> INSTANT = (Object value) -> {

    String val = Hl7DataHandlerUtil.getStringValue(value);
    if (val != null) {
      return DateUtil.formatToZonedDateTime(val);
    }
    return null;
  };




  public static final ValueExtractor<Object, URI> URI_VAL = (Object value) -> {

    try {
      String val = Hl7DataHandlerUtil.getStringValue(value);
      if (val != null && isValidUUID(val)) {
        return new URI("urn", "uuid", val);
      } else {
        return null;
      }

    } catch (IllegalArgumentException | URISyntaxException e) {
      LOGGER.warn("Value not valid URI, value: {}", value, e);
      return null;
    }

  };

  public static final ValueExtractor<Object, String> ADMINISTRATIVE_GENDER_FHIR =
      (Object value) -> {

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
  
  public static final ValueExtractor<Object, String> OBSERVATION_STATUS_FHIR = (Object value) -> {
    String val = Hl7DataHandlerUtil.getStringValue(value);
    if (null == val) {
          return ObservationStatus.UNKNOWN.toCode();
        }
        ObservationStatus status = getObservationStatus(val);

        return status.toCode();

  };

  
  

  public static final ValueExtractor<Object, Boolean> BOOLEAN = (Object value) -> {
    String val = Hl7DataHandlerUtil.getStringValue(value);
    if (null == val) {
      return false;
    }
    return BooleanUtils.toBoolean(val);

  };

  public static final ValueExtractor<Object, Integer> INTEGER = (Object value) -> {
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

  public static final ValueExtractor<Object, Float> FLOAT = (Object value) -> {
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







  public static final ValueExtractor<Object, UUID> UUID_VAL = (Object value) -> {
    String val = Hl7DataHandlerUtil.getStringValue(value);
    return getUUID(val);

  };

  public static final ValueExtractor<Object, Object> OBJECT = (Object value) -> {
    return value;

  };



  private static UUID getUUID(String value) {
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

  private static boolean isValidUUID(String val) {
    try {
      UUID.fromString(val);
      return true;
    } catch (IllegalArgumentException e) {
      LOGGER.warn("Not a valid UUID ", e);
      return false;
    }

  }

  private static ObservationStatus getObservationStatus(String val) {
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
    return status;
  }








}
