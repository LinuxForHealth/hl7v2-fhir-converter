package com.ibm.whi.fhir;

import org.apache.commons.lang3.EnumUtils;
import org.hl7.fhir.r4.model.Resource;

public enum FHIRResourceMapper {

  PATIENT(org.hl7.fhir.r4.model.Patient.class), //
  ENCOUNTER(org.hl7.fhir.r4.model.Encounter.class), //
  OBSERVATION(org.hl7.fhir.r4.model.Observation.class);

  private Class<? extends Resource> klass;

  FHIRResourceMapper(Class<? extends Resource> klass) {
    this.klass = klass;

  }


  public Class<? extends Resource> getFHIRClass() {
    return klass;
  }


  public static Class<? extends Resource> getResourceClass(String name) {
    FHIRResourceMapper fm = EnumUtils.getEnumIgnoreCase(FHIRResourceMapper.class, name);
    return fm.klass;
  }


}
