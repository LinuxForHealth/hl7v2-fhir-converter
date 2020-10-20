/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.linuxforhealth.core.exception;

/**
 * 
 * This exception is thrown if a failure is encountered during data extraction or evaluation.
 *
 * @author pbhallam
 */
public class DataExtractionException extends RuntimeException {

  public DataExtractionException(String message, Exception e) {
    super(message, e);
  }


}
