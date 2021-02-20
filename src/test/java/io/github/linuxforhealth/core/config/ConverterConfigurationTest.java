/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import io.github.linuxforhealth.core.terminology.SystemUrlLookup;

public class ConverterConfigurationTest {

  @BeforeClass
  public static void setup() {
    System.setProperty("config.home", "src/test/resources");
    ConverterConfiguration.reset();
  }

  @AfterClass
  public static void reset() {
    System.clearProperty("config.home");
    ConverterConfiguration.reset();
    SystemUrlLookup.reinit();
  }

  @Test
  public void test_that_additional_conceptmap_values_are_loaded() throws IOException {
    SystemUrlLookup.reinit();
    String url = SystemUrlLookup.getSystemUrl("LN");
    assertThat(url).isEqualTo("http://loinc-additional.org");
  }

}
