/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.linuxforhealth.fhir;

import org.apache.commons.lang3.EnumUtils;
import org.hl7.fhir.r4.model.Resource;

public enum FHIRResourceMapper {

  PATIENT(org.hl7.fhir.r4.model.Patient.class), //
  ENCOUNTER(org.hl7.fhir.r4.model.Encounter.class), //
  OBSERVATION(org.hl7.fhir.r4.model.Observation.class), //
  ALLERGYINTOLERANCE(org.hl7.fhir.r4.model.AllergyIntolerance.class), //
  CONDITION(org.hl7.fhir.r4.model.Condition.class), //
  PRACTITIONER(org.hl7.fhir.r4.model.Practitioner.class);

  private Class<? extends Resource> klass;

  FHIRResourceMapper(Class<? extends Resource> klass) {
    this.klass = klass;

  }


  public Class<? extends Resource> getFHIRClass() {
    return klass;
  }


  public static Class<? extends Resource> getResourceClass(String name) {
    FHIRResourceMapper fm = EnumUtils.getEnumIgnoreCase(FHIRResourceMapper.class, name);
    if (fm == null) {
      throw new IllegalStateException(
          "Resource type not mapped in FHIRResourceMapper , resource name" + name);
    }
    return fm.klass;
  }


}
