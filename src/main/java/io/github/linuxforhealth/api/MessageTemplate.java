/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.api;

import org.hl7.fhir.r4.model.Bundle;

import java.util.List;

/**
 * Message template defines how to convert a particular type of input information to FHIR resources.
 * Example, for HL7, the message template can be defined per HL7 message type, linuxforhealthch
 * would define list of resources to be generated.
 * 
 * @param <T> - Input type
 *
 * @author pbhallam
 */
public interface  MessageTemplate<T> {



  /**
   * Takes input and converts it to FHIR bundle resource
   *
   * @param data - Input
   * @param engine - {@link MessageEngine}
   * @return Bundle - {@link Bundle}
   *
   */
  Bundle convert(T data, MessageEngine engine);

  /**
   * Name of the message
   * 
   * @return String
   */
  String getMessageName();

  /**
   * List of templates for FHIR resources that needs to be generated
   * 
   * @return {@link List} of {@link FHIRResourceTemplate}
   */
  List<FHIRResourceTemplate> getResources();


}
