/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.fhir;

import org.hl7.fhir.r4.model.Resource;
import io.github.linuxforhealth.api.IdResolver;

public class BasicFHIRResourceIdResolver implements IdResolver {

  @Override
  public String getId(Resource r) {
    return r.getId();
  }

}
