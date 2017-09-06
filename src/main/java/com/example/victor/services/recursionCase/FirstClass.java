package com.example.victor.services.recursionCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FirstClass {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    public FirstClass(SecondClass secondClass){
        LOG.debug("A constructor with parameters called");
    }
}
