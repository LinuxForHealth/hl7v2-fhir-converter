/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.data.date;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


public class DateFormats {

  private static final Pattern PATTERN8 =
      Pattern.compile("^\\d{4}\\d{2}\\d{2}(\\d{0,6})(\\.\\d{0,4})[\\+|\\-]\\d{4}$");
  private static final Pattern PATTERN7 =
      Pattern.compile("^\\d{4}\\d{2}\\d{2}(\\d{0,6})[\\+|\\-]\\d{4}$");
  private static final Pattern PATTERN6 =
      Pattern.compile("^\\d{4}\\d{2}\\d{2}(\\d{0,6})(\\.\\d{0,4})$");
  private static final Pattern PATTERN5 = Pattern.compile("^\\d{4}\\d{2}\\d{2}(\\d{0,6})$");
  private static final Pattern PATTERN4 = Pattern.compile("^\\d{4}\\d{2}\\d{2}\\d{2}$");
  private static final Pattern PATTERN3 = Pattern.compile("^\\d{4}\\d{2}\\d{2}$");
  private static final Pattern PATTERN2 = Pattern.compile("^\\d{4}\\d{2}$");
  private static final Pattern PATTERN1 = Pattern.compile("^\\d{4}$");
  
  private static final String YYYY_MM_DD = "yyyy-MM-dd";
  
  static DateTimeFormatter FHIR_ZONE_DATE_TIME_FORMAT =
      DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  private static final DateFormats dateFormats = new DateFormats();

  private DateTimeFormatter formatter;
  private Map<Pattern, DateTimeFormatter> datePatterns;
  private Map<Pattern, DateTimeFormatter> dateTimePatterns;
  private Map<Pattern, DateTimeFormatter> dateTimePatternsWithoutTime;
  private Map<Pattern, DateTimeFormatter> dateTimePatternsWithZone;
  private DateFormats() {
    this.formatter = getFormatter();
    this.datePatterns = getDatePatterns();
    this.dateTimePatterns = getDateTimePatterns();
    this.dateTimePatternsWithoutTime = getDateTimePatternsWithoutTime();
    this.dateTimePatternsWithZone = getDateTimeWithZonePatterns();
  }



  private static DateTimeFormatter getFormatter() {
    String patterns =
        "[yyyyMMddHHmmss.SZ][yyyyMMddHHmmss.SSZ][yyyyMMddHHmmss.SSSZ][yyyyMMddHHmmss.SSSSZ][yyyyMMddHHmmssZ][yyyyMMddHHmmZ][yyyyMMddHHZ][yyyyMMddZ][yyyyMMZ][yyyyZ][yyyyMMddHHmmss][yyyyMMddHHmm][yyyyMMddHH][yyyyMMdd][yyyyMM][yyyy]";
    return new DateTimeFormatterBuilder()

        .appendPattern(patterns).optionalStart()
        .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1).parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
        .optionalEnd()
        .toFormatter();



  }


  private static Map<Pattern, DateTimeFormatter> getDatePatterns() {
    Map<Pattern, DateTimeFormatter> patterns = new HashMap<>();
    patterns.put(PATTERN1, DateTimeFormatter.ofPattern("yyyy"));
    patterns.put(PATTERN2, DateTimeFormatter.ofPattern("yyyy-MM"));
    patterns.put(PATTERN3, DateTimeFormatter.ofPattern(YYYY_MM_DD));
    patterns.put(PATTERN5, DateTimeFormatter.ofPattern(YYYY_MM_DD));
    patterns.put(PATTERN6, DateTimeFormatter.ofPattern(YYYY_MM_DD));
    patterns.put(PATTERN7, DateTimeFormatter.ofPattern(YYYY_MM_DD));
    patterns.put(PATTERN8, DateTimeFormatter.ofPattern(YYYY_MM_DD));
    return patterns;

  }



  private static Map<Pattern, DateTimeFormatter> getDateTimePatterns() {
    Map<Pattern, DateTimeFormatter> patterns = new HashMap<>();
    patterns.put(PATTERN4, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    patterns.put(PATTERN5, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    patterns.put(PATTERN6, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

    return patterns;
  }


  private static Map<Pattern, DateTimeFormatter> getDateTimeWithZonePatterns() {
    Map<Pattern, DateTimeFormatter> patterns = new HashMap<>();

    patterns.put(PATTERN8, FHIR_ZONE_DATE_TIME_FORMAT);
    patterns.put(PATTERN7, FHIR_ZONE_DATE_TIME_FORMAT);
    return patterns;
  }


  private static Map<Pattern, DateTimeFormatter> getDateTimePatternsWithoutTime() {
    Map<Pattern, DateTimeFormatter> patterns = new HashMap<>();
    patterns.put(PATTERN1, DateTimeFormatter.ofPattern("yyyy"));
    patterns.put(PATTERN2, DateTimeFormatter.ofPattern("yyyy-MM"));
    patterns.put(PATTERN3, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    return patterns;
  }



  public static Map<Pattern, DateTimeFormatter> getDateTimePatternsInstance() {
    return dateFormats.dateTimePatterns;
  }


  public static Map<Pattern, DateTimeFormatter> getDatePatternsInstance() {
    return dateFormats.datePatterns;
  }

  public static Map<Pattern, DateTimeFormatter> getDatePatternsWithoutTimeInstance() {
    return dateFormats.dateTimePatternsWithoutTime;
  }


  public static Map<Pattern, DateTimeFormatter> getDatePatternsWithZoneInstance() {
    return dateFormats.dateTimePatternsWithZone;
  }



  public static DateTimeFormatter getFormatterInstance() {
    return dateFormats.formatter;
  }
}



