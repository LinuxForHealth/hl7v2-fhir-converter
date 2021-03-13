package io.github.linuxforhealth.api;

import org.hl7.fhir.r4.model.Resource;

/**
 * IdResolver helps resolves the Id for each resource
 * 
 * @author pbhallam@us.ibm.com
 *
 */
public interface IdResolver {

  String getId(Resource r);

}
