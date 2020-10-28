/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core;

import org.hl7.fhir.r4.model.Bundle.BundleType;

public class Constants {



  public static final boolean DEFAULT_PRETTY_PRINT = false;
  public static final BundleType DEFAULT_BUNDLE_TYPE = BundleType.COLLECTION;
  public static final String GROUP_ID = "GROUP_ID";
  public static final String BASE_VALUE_NAME = "BASE_VALUE";
  public static final String USE_GROUP = "useGroup";

  //
  public static final String CODING_SYSTEM_MAPPING_PATH = "codesystem/CodingSystemMapping.yml";
  public static final String V2_TO_FHIR_MAPPING_PATH = "codesystem/v2ToFhirMapping.yml";
  public static final String RESOURCE_MAPPING_PATH = "fhir/resourcemapping.yml";
  public static final String HL7V2_SYSTEM_PREFIX = "http://terminology.hl7.org/CodeSystem/v2-";

  public static final String HL7_BASE_PATH = "hl7/";
  public static final String FHIR_BASE_PATH = "fhir/";
  public static final String MESSAGE_BASE_PATH = "message/";

  private Constants() {}
}
