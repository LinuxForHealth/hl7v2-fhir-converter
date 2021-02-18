/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.terminology;

import java.util.Locale;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

public class CodingSystem {
  private String id;
  private String description;
  private String url;
  private String oid;

  @JsonCreator
  /**
   * 
   * @param id
   * @param display
   * @param url
   * @param OID
   */
  public CodingSystem(@JsonProperty("id") String id,
      @JsonProperty("description") String description, @JsonProperty("url") String url,
      @JsonProperty("oid") String oid) {
    Preconditions.checkArgument(StringUtils.isNotBlank(id), "id cannot be null or blank");
    Preconditions.checkArgument(StringUtils.isNotBlank(url), "url cannot be null or blank");
    Preconditions.checkArgument(
        StringUtils.isBlank(oid)
            || (StringUtils.isNotBlank(oid) && StringUtils.startsWith(oid, "urn:oid:")),
        oid + " OID can be null or blank, but if its not blank it  should start with  urn:oid:");

    this.id = id;
    this.description = description;
    this.url = url;

    this.oid = oid;

  }



  public String getId() {
    return StringUtils.upperCase(id, Locale.ENGLISH);
  }


  public String getDescription() {
    return description;
  }

  @JsonGetter("url")
  public String getUrl() {
    return url;
  }

  @JsonGetter("oid")
  public String getOid() {
    return oid;
  }

}
