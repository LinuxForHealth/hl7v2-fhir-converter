package io.github.linuxforhealth.utils;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemURI {

  private String id;
  private String display;
  private String url;
  private String OID;

  @JsonCreator
  /**
   * 
   * @param id
   * @param display
   * @param url
   * @param OID
   */
  public SystemURI(@JsonProperty("id") String id, @JsonProperty("title") String display,
      @JsonProperty("url") String url,
      @JsonProperty("identifier") List<Identifier> system) {
    this.id = id;
    this.display = display;
    this.url = url;
    if (system != null) {
      this.OID = system.get(0).getValue();
    }
  }



  public String getId() {
    return id;
  }

  @JsonGetter("title")
  public String getDisplay() {
    return display;
  }

  @JsonGetter("url")
  public String getUrl() {
    return url;
  }

  @JsonGetter("oid")
  public String getOID() {
    return OID;
  }

}
