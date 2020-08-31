package com.ibm.whi.hl7.data;

@FunctionalInterface
public interface DataEvaluator<T, R> {


  R apply(T t);

}
