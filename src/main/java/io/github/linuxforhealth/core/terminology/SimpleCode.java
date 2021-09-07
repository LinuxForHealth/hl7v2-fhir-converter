/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.terminology;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class SimpleCode {


  private String system;
  private String code;
  private String display;
  private String version;

  /**
   * Returns simple representation of Code.
   * 
   * @param code
   * @param system
   * @param display
   * 
   */

  public SimpleCode(String code, String system, String display) {
    this.code = code;
    this.system = system;
    this.display = display;
    this.version = null;
  }

  public SimpleCode(String code, String system, String display, String version) {
    this.code = code;
    this.system = system;
    this.display = display;
    this.version = version;
  }

  public String getSystem() {
    return system;
  }

  public String getCode() {
    return code;
  }

  public String getDisplay() {
    return display;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public String toString() {
    return ReflectionToStringBuilder.toString(this);
  }


}
