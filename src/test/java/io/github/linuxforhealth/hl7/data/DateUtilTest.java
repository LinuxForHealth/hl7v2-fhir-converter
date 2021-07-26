/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.hl7.data;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import io.github.linuxforhealth.hl7.data.date.DateUtil;

public class DateUtilTest {

  @Test
  public void simple_date() {
    String ld = DateUtil.formatToDate("2008");
    assertThat(ld).isEqualTo("2008");

  }


  @Test
  public void simple_date_month() {
    String ld = DateUtil.formatToDate("200809");
    assertThat(ld).isEqualTo("2008-09");

  }

  @Test
  public void simple_date_month_day() {
    String ld = DateUtil.formatToDate("20080926");
    assertThat(ld).isEqualTo("2008-09-26");

  }


  @Test
  public void simple_date_month_day_hour() {
    String ld = DateUtil.formatToDate("2008092609");
    assertThat(ld).isEqualTo("2008-09-26");

  }

  @Test
  public void simple_date_month_day_zone() {
    String ld = DateUtil.formatToDate("200711040132-0400");
    assertThat(ld).isEqualTo("2007-11-04");

  }


  @Test
  public void simple_date_month_day_milliseconds_zone() {
    String ld = DateUtil.formatToDate("20071104013206.345-0400");
    assertThat(ld).isEqualTo("2007-11-04");

  }



  @Test
  public void simple_datetime() {
    String ld = DateUtil.formatToDateTimeWithZone("2008");
    assertThat(ld).isEqualTo("2008");

  }


  @Test
  public void simple_datetime_month() {
    String ld = DateUtil.formatToDateTimeWithZone("200809");
    assertThat(ld).isEqualTo("2008-09");

  }

  @Test
  public void simple_dateime_month_day() {
    String ld = DateUtil.formatToDateTimeWithZone("20080926");
    assertThat(ld).isEqualTo("2008-09-26");

  }


  @Test
  public void simple_datetime_month_day_hour() {
    String ld = DateUtil.formatToDateTimeWithZone("2008092609");
    assertThat(ld).isEqualTo("2008-09-26T09:00:00+08:00");

  }

  @Test
  public void simple_datetime_month_day_zone() {
    String ld = DateUtil.formatToDateTimeWithZone("200711040132-0400");
    assertThat(ld).isEqualTo("2007-11-04T01:32:00-04:00");

  }


  @Test
  public void simple_datetime_month_day_milliseconds_zone() {
    String ld = DateUtil.formatToDateTimeWithZone("20071104013206.345+0900");
    assertThat(ld).isEqualTo("2007-11-04T01:32:06.345+09:00");

  }



}
