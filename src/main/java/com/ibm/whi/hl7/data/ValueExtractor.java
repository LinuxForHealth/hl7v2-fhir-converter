package com.ibm.whi.hl7.data;

@FunctionalInterface
public interface ValueExtractor<T, R> {


  R apply(T t);

}
