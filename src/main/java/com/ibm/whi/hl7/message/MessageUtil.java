package com.ibm.whi.hl7.message;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.Bundle;
import org.python.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.whi.fhir.FHIRContext;
import com.ibm.whi.fhir.FHIRResourceMapper;
import com.ibm.whi.hl7.parsing.Hl7DataExtractor;
import com.ibm.whi.hl7.resource.ResourceModel;
import ca.uhn.hl7v2.model.Structure;

public class MessageUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(MessageUtil.class);
  private static final ObjectMapper OBJ_MAPPER = new ObjectMapper();

  private MessageUtil() {}

  /**
   * Converts a HL7 message to a FHIR bundle with the list of resources specified
   * 
   * @param hl7DTE
   * @param resources
   * @param context
   * @return
   * @throws IOException
   */
  public static Bundle convertMessageToFHIRResource(Hl7DataExtractor hl7DTE,
      Iterable<FHIRResource> resources,
      Map<String, Object> context) throws IOException {

    Bundle bundle = new Bundle();
    bundle.setType(Bundle.BundleType.COLLECTION);

    for (FHIRResource res : resources) {

      ResourceModel rs = res.getResource();
      List<Structure> segments = hl7DTE.getAllStructures(res.getSegment());
      if (!segments.isEmpty()) {

        generateFhirResources(res, rs, segments, ImmutableMap.copyOf(context), bundle);

      }



    }

    return bundle;



  }


  private static void generateFhirResources(FHIRResource res, ResourceModel rs, List<Structure> segments,
      Map<String, Object> context, Bundle bundle) throws JsonProcessingException {

    if (res.isRepeates()) {

      for (Structure str : segments) {
        Map<String, Object> localcontext = new HashMap<>(context);
        localcontext.put(res.getSegment(), str);
        Object obj = rs.evaluate(localcontext);
        if (obj != null) {

            String json = OBJ_MAPPER.writeValueAsString(obj);
            addEntry(res.getResourceName(), json, bundle);

        }

      }
    } else {


      Map<String, Object> localcontext = new HashMap<>(context);
      localcontext.put(res.getSegment(), segments.get(0));
      Object evaluatedValue = rs.evaluate(localcontext);

      if (evaluatedValue != null) {
        String json = OBJ_MAPPER.writeValueAsString(evaluatedValue);
        addEntry(res.getResourceName(), json, bundle);
        context.put(res.getResourceName(), evaluatedValue);
      }

    }

  }


  private static void addEntry(String resourceName, String json, Bundle bundle) {
    LOGGER.info("Converting resourceName {} to FHIR {}", resourceName, json);
    if (json != null) {
      org.hl7.fhir.r4.model.Resource parsed = FHIRContext.getIParserInstance()
          .parseResource(FHIRResourceMapper.getResourceClass(resourceName), json);
      bundle.addEntry().setResource(parsed);
    }

  }
}
