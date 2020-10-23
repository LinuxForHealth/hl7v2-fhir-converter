/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.fhir;

import java.util.Map;
import org.apache.commons.lang3.ClassUtils;
import org.hl7.fhir.r4.model.Resource;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.linuxforhealth.core.Constants;
import io.github.linuxforhealth.core.ObjectMapperUtil;
import io.github.linuxforhealth.hl7.resource.ResourceReader;

public class FHIRResourceMapper {

  private static FHIRResourceMapper fhirResourceMapper;

  private Map<String, String> resourceMapping;
  private FHIRResourceMapper() {
    String resource =
        ResourceReader.getInstance()
            .getResource(Constants.RESOURCE_MAPPING_PATH);
    try {
      resourceMapping = ObjectMapperUtil.getYAMLInstance().readValue(resource, Map.class);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(
          "Cannot read resource mapping file fhir/resourcemapping.yml ", e);
    }
  }




  public static Class<? extends Resource> getResourceClass(String name) {
    if (fhirResourceMapper == null) {
      fhirResourceMapper = new FHIRResourceMapper();
    }
    String resourceName = fhirResourceMapper.resourceMapping.get(name);

    if (resourceName != null) {
      try {
        return (Class<? extends Resource>) ClassUtils.getClass(resourceName);
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException(
            "Resource type not mapped in FHIRResourceMapper , resource name" + name, e);
      }
    } else {

      throw new IllegalStateException(
          "Resource type not mapped in FHIRResourceMapper , resource name" + name);
    }

  }


}
