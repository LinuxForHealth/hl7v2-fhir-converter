package io.github.linuxforhealth.utils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Identifier {

  private String sys;
  private String value;

  @JsonCreator
  /**
   * 
   * @param id
   * @param display
   * @param url
   * @param OID
   */
  public Identifier(@JsonProperty("system") String sys, @JsonProperty("value") String value) {
    this.sys = sys;
    this.value = value;

  }



  public String getSys() {
    return sys;
  }

  @JsonGetter("value")
  public String getValue() {
    return value;
  }



}
