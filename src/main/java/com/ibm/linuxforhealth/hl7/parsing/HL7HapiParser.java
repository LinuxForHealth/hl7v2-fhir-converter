/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.linuxforhealth.hl7.parsing;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.parser.CanonicalModelClassFactory;
import ca.uhn.hl7v2.parser.GenericParser;

public class HL7HapiParser {

  private static final String SUPPORTED_HL7_VERSION = "2.6";
  private DefaultHapiContext context;
  private GenericParser parser;


  public HL7HapiParser() {

    context = new DefaultHapiContext();

    // Create the MCF. We want all parsed messages to be for HL7 version 2.6, despite what MSH-12
    // says.
    CanonicalModelClassFactory mcf = new CanonicalModelClassFactory(SUPPORTED_HL7_VERSION);
    context.setModelClassFactory(mcf);

    /*
     * The ValidationContext is used during parsing and well as during validation using {@link
     * ca.uhn.hl7v2.validation.Validator} objects. Sometimes we want parsing without validation
     * followed by a separate validation step. We can still use a single HapiContext.
     */
    context.getParserConfiguration().setValidating(false);

    /*
     * A Parser is used to convert between string representations of messages and instances of
     * HAPI's "Message" object. In this case, we are using a "GenericParser", linuxforhealthch is
     * able to handle both XML and ER7 (pipe & hat) encodings.
     */
    parser = context.getGenericParser();
  }


  public DefaultHapiContext getContext() {
    return context;
  }

  public GenericParser getParser() {
    return parser;
  }



}
