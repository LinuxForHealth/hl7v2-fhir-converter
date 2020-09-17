/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.parsing.result;

import java.util.List;

/**
 * Represents the result of parsing HL7 message.
 * 
 *
 * @author {user}
 */

public interface ParsingResult<T> {

  /**
   * Returns single value of type T. If the parsing returned multiple values, then this method would
   * return the first occurrence of that value
   * 
   * @return
   */

  T getValue();

  /**
   * Returns all values of type T .
   * 
   * @return
   */

  List<T> getValues();


  /**
   * Returns if this object has no value
   * 
   * @return
   */
  boolean isEmpty();

}
