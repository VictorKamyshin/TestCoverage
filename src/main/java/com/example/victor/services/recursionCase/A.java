package com.example.victor.services.recursionCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class A {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    public A(B b){
        LOG.debug("A constructor with parameters called");
    }
}
