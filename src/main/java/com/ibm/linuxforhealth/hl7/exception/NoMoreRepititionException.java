/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.linuxforhealth.hl7.exception;

public class NoMoreRepititionException extends RuntimeException {

  public NoMoreRepititionException(String message, Throwable e) {
    super(message, e);
  }


}
