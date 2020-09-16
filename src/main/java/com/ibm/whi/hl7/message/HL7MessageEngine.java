package com.ibm.whi.hl7.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.ibm.whi.core.expression.GenericResult;
import com.ibm.whi.core.message.AbstractFHIRResource;
import com.ibm.whi.core.message.InputData;
import com.ibm.whi.core.message.MessageEngine;
import com.ibm.whi.core.resource.ResourceModel;
import com.ibm.whi.core.resource.ResourceResult;
import com.ibm.whi.core.resource.ResourceValue;
import com.ibm.whi.fhir.FHIRContext;
import com.ibm.whi.fhir.FHIRResourceMapper;
import com.ibm.whi.hl7.resource.ObjectMapperUtil;
import ca.uhn.hl7v2.model.Structure;

public class HL7MessageEngine implements MessageEngine {
  private static final Logger LOGGER = LoggerFactory.getLogger(HL7MessageEngine.class);
  private static final ObjectMapper OBJ_MAPPER = ObjectMapperUtil.getJSONInstance();



  /**
   * Converts a HL7 message to a FHIR bundle with the list of resources specified
   * 
   * @param dataExtractor
   * @param resources
   * @param context
   * @return
   * @throws IOException
   */
  @Override
  public Bundle transform(InputData dataInput,
      Iterable<? extends AbstractFHIRResource> resources, Map<String, GenericResult> contextValues)
      throws IOException {
    Preconditions.checkArgument(dataInput != null, "dataInput cannot be null");
    Preconditions.checkArgument(contextValues != null, "contextValues cannot be null");
    Preconditions.checkArgument(resources != null, "resources cannot be null");

    HL7MessageData hl7DataInput = (HL7MessageData) dataInput;
    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.COLLECTION);

    for (AbstractFHIRResource res : resources) {
      HL7FHIRResource hres = (HL7FHIRResource) res;
      ResourceModel rs = res.getResource();
      List<Structure> segments =
          hl7DataInput.getHL7DataParser().getAllStructures(hres.getSegment()).getValues();
      if (!segments.isEmpty()) {

        generateFHIRResources(hl7DataInput, hres, rs, segments, contextValues, bundle);

      }



    }

    return bundle;



  }


  private static void generateFHIRResources(InputData dataExtractor, HL7FHIRResource res,
      ResourceModel rs, List<Structure> segments, Map<String, GenericResult> variables,
      Bundle bundle) {

    if (res.isRepeats()) {
      List<GenericResult> baseValues = new ArrayList<>();
      segments.removeIf(Objects::isNull);
      segments.forEach(d -> baseValues.add(new GenericResult(d)));

        Map<String, GenericResult> localVariables = new HashMap<>(variables);
        ResourceResult result=rs.evaluateMultiple(dataExtractor, ImmutableMap.copyOf(localVariables),
            baseValues, new ArrayList<>());

      if (result != null && result.getResources() != null && !result.getResources().isEmpty()) {
      List<ResourceValue> resourceObjects = result.getResources();
      addValues(bundle, resourceObjects);
      List<ResourceValue> additionalResourceObjects = result.getAdditionalResources();
      addValues(bundle, additionalResourceObjects);
      }

    } else {


      Map<String, GenericResult> localVariables = new HashMap<>(variables);
      localVariables.put(res.getSegment(), new GenericResult(segments.get(0)));

      ResourceResult evaluatedValue =
          rs.evaluateSingle(dataExtractor, ImmutableMap.copyOf(localVariables),
              new GenericResult(segments.get(0)));

      if (evaluatedValue != null && evaluatedValue.getResources() != null
          && !evaluatedValue.getResources().isEmpty()) {

        addEntry(res.getResourceName(), evaluatedValue.getResources().get(0), bundle);
        variables.put(res.getResourceName(), new GenericResult(evaluatedValue));
        List<ResourceValue> additionalResourceObjects = evaluatedValue.getAdditionalResources();
        addValues(bundle, additionalResourceObjects);
      }


    }
  }


  private static void addValues(Bundle bundle, List<ResourceValue> objects) {
    if (objects != null && !objects.isEmpty()) {
      objects.forEach(obj -> {
        addEntry(obj.getResourceClass(), obj, bundle);
      });

    }
  }



  private static void addEntry(String resourceClass, ResourceValue obj, Bundle bundle) {
    LOGGER.info("Converting resourceName {} to FHIR {}", resourceClass, obj);

    try {
      if (obj != null) {
        LOGGER.info("Converting resourceName {} to FHIR {}", resourceClass, obj.getResource());
        String json = OBJ_MAPPER.writeValueAsString(obj.getResource());
        if (json != null) {
          org.hl7.fhir.r4.model.Resource parsed = FHIRContext.getIParserInstance()
              .parseResource(FHIRResourceMapper.getResourceClass(resourceClass), json);
          bundle.addEntry().setResource(parsed);
        }
      }
    } catch (JsonProcessingException e) {
      LOGGER.error("Processing exception when Serialization", e);
    }


  }

}
