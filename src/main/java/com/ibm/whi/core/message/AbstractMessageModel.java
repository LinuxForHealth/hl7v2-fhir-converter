package com.ibm.whi.core.message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class AbstractMessageModel<T> {

  private List<AbstractFHIRResource> resources;
  private String messageName;


  @JsonCreator
  public AbstractMessageModel(@JsonProperty("messageName") String messageName,
      @JsonProperty("resources") List<? extends AbstractFHIRResource> resources) {
    this.messageName = messageName;
    this.resources = new ArrayList<>();
    if (resources != null && !resources.isEmpty()) {
      this.resources.addAll(resources);
    }

  }

  /**
   * Takes a certain parsed input and converts it to FHIR bundle resource
   *
   * @param data
   * @return
   * @throws IOException
   */

  public abstract String convert(T data, MessageEngine engine) throws IOException;

  /**
   * Takes a certain raw data, parses the data and converts it to FHIR bundle resource
   *
   * @param data
   * @return
   * @throws IOException
   */
  public abstract String convert(String inputdata, MessageEngine engine) throws IOException;

  public String getMessageName() {
    return messageName;
  }

  public void setMessageName(String messageName) {
    this.messageName = messageName;
  }

  public List<? extends AbstractFHIRResource> getResources() {
    return resources;
  }


}
