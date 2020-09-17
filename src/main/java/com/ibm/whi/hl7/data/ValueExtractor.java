/*
 * (C) Copyright IBM Corp. 2020
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package com.ibm.whi.hl7.data;

@FunctionalInterface
public interface ValueExtractor<T, R> {


  R apply(T t);

}
