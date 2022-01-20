/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.data.date;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.linuxforhealth.core.config.ConverterConfiguration;

public class DateUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(DateUtil.class);

    private DateUtil() {
    }

    public static String formatToDate(String input) {
        DateTimeFormatter format = null;
        for (Entry<Pattern, DateTimeFormatter> pattern : DateFormats.getDatePatternsInstance()
                .entrySet()) {
            if (pattern.getKey().matcher(input).matches()) {
                format = pattern.getValue();
                break;
            }
        }
        try {
            LocalDate ldt = LocalDate.parse(input, DateFormats.getFormatterInstance());
            return ldt.format(format);
        } catch (DateTimeParseException e) {
            LOGGER.warn("Input value cannot be formatted to LocalDate.");
            LOGGER.debug("Input value cannot be formatted to LocalDate {}", input, e);
            return null;
        }
    }

    public static String formatToDateTimeWithZone(String input) {
        String returnValue = getLocalDate(input);
        if (returnValue == null) {
            returnValue = getZonedDate(input);
        }
        if (returnValue == null) {
            returnValue = getLocalDateTimeWithDefaultZone(input);
        }
        return returnValue;
    }

    private static String getLocalDateTimeWithDefaultZone(String input) {
        String returnValue;

        try {
            LocalDateTime ldt = LocalDateTime.parse(input, DateFormats.getFormatterInstance());
            ZoneId zone = ConverterConfiguration.getInstance().getZoneId();
            if (zone != null) {
                returnValue = ldt.atZone(zone).format(DateFormats.FHIR_ZONE_DATE_TIME_FORMAT);
                return returnValue;
            } else {
                LOGGER.warn("No default zone set, cannot convert LocalDateTime to ZonedDateTime");
                LOGGER.debug("No default zone set, cannot convert LocalDateTime to ZonedDateTime, input {} ", input);
                return null;
            }
        } catch (DateTimeParseException e) {
            LOGGER.warn("Date parsing failure for value.");
            LOGGER.debug("Date parsing exception for value {}", input, e);
            return null;
        }
    }

    private static String getLocalDate(String input) {
        DateTimeFormatter format = null;
        for (Entry<Pattern, DateTimeFormatter> pattern : DateFormats
                .getDatePatternsWithoutTimeInstance().entrySet()) {
            if (pattern.getKey().matcher(input).matches()) {
                format = pattern.getValue();
                break;
            }
        }
        if (format != null) {
            try {
                LocalDate ldt = LocalDate.parse(input, DateFormats.getFormatterInstance());
                return ldt.atStartOfDay().format(format);
            } catch (DateTimeParseException e) {
                LOGGER.warn("Input value cannot be formatted to LocalDate");
                LOGGER.debug("Input value cannot be formatted to LocalDate {} ", input, e);
                return null;
            }
        }
        return null;
    }

    private static String getZonedDate(String input) {
        DateTimeFormatter format = null;
        for (Entry<Pattern, DateTimeFormatter> pattern : DateFormats.getDatePatternsWithZoneInstance()
                .entrySet()) {
            if (pattern.getKey().matcher(input).matches()) {
                format = pattern.getValue();
                break;
            }
        }
        if (format != null) {
            try {
                ZonedDateTime zdt = ZonedDateTime.parse(input, DateFormats.getFormatterInstance());
                return zdt.format(format);
            } catch (DateTimeParseException e) {
                LOGGER.warn("Input value cannot be parsed to ZonedDateTime");
                LOGGER.debug("Input value cannot be parsed to ZonedDateTime {} ", input, e);
                return null;
            }
        }
        return null;
    }

    public static Temporal getTemporal(String dateString) {
        Map<String, DateTimeParseException> warnings = new HashMap<>();
        if (dateString == null) {
            return null;
        }
        Temporal temporal = null;
        try {
            temporal = Instant.parse(dateString);
            LOGGER.debug("Date parsed for instant {}", dateString);
        } catch (DateTimeParseException e) {
            warnings.put("Input value cannot be parsed to  instant " + dateString, e);
        }
        if (temporal == null) {
            try {
                temporal = ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_ZONED_DATE_TIME);
                LOGGER.debug("Date parsed for ZonedDateTime {}", dateString);
            } catch (DateTimeParseException e) {
                warnings.put("Input value cannot be parsed to ZonedDateTime " + dateString, e);
            }
        }
        if (temporal == null) {
            try {
                temporal = LocalDateTime.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                LOGGER.debug("Date parsed for LocalDateTime {}", dateString);
            } catch (DateTimeParseException e) {
                warnings.put("Input value cannot be parsed to LocalDateTime " + dateString, e);
            }
        }
        if (temporal == null) {
            try {
                temporal = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE);
                LOGGER.debug("Date parsed for LocalDate {}", dateString);
            } catch (DateTimeParseException e) {
                warnings.put("Input value cannot be parsed to LocalDate " + dateString, e);
            }
        }
        if (temporal == null && !warnings.isEmpty()) {
            LOGGER.warn("Attempt to parse input value to temporal failed.");
            LOGGER.debug("Attempt to parse input value {} to temporal failed ", dateString);
            for (Entry<String, DateTimeParseException> entry : warnings.entrySet()) {
                LOGGER.debug("{} reason {}", entry.getKey(), entry.getValue().toString());
            }
        }
        return temporal;
    }

    public static String formatToZonedDateTime(String input) {
        String zoned = getZonedDate(input);
        if (zoned == null) {
            zoned = formatToDateTimeWithZone(input);
        }
        return zoned;
    }
}