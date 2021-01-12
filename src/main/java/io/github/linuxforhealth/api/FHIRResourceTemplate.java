/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.api;

/**
 * Represents a template to generate a FHIR resource. <br>
 * Each template defines the following attributes:
 * <ul>
 * <li>Name</li>
 * <li>{@link ResourceModel}</li>
 * <li>If the resource is a list - multiple values needs to be generated</li>
 * <li>If the resource is referenced</li>
 * 
 * </ul>
 *
 * @author pbhallam
 */
public interface FHIRResourceTemplate {


  /**
   * Resource Name
   * 
   * @return String
   */
  String getResourceName();

  /**
   * ResourceModel
   * 
   * @return {@link ResourceModel}
   */
  ResourceModel getResource();

  /**
   * If multiple resources of this type needs to be generated
   * 
   * @return True/False
   */
  boolean isGenerateMultiple();

  /**
   * If this resource is referenced by other resources
   * 
   * @return True/False
   */
  boolean isReferenced();



}
