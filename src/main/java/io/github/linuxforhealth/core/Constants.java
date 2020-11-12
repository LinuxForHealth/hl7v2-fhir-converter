/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core;

import java.io.File;
import java.nio.file.Paths;

import org.hl7.fhir.r4.model.Bundle.BundleType;

public class Constants {

  public static final String DEFAULT_HL7_RESOURCES = "hl7";
  public static final String DEFAULT_HL7_MESSAGE_FOLDER = Paths.get(DEFAULT_HL7_RESOURCES, "message").toString();
  public static final boolean DEFAULT_PRETTY_PRINT = false;
  public static final BundleType DEFAULT_BUNDLE_TYPE = BundleType.COLLECTION;
  public static final String GROUP_ID = "GROUP_ID";
  public static final String BASE_VALUE_NAME = "base";
  public static final String USE_GROUP = "useGroup";

  private Constants() {}
}
