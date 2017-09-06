package com.example.victor.services.recursionCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecondClass {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    public SecondClass(FirstClass firstClass){
        LOG.debug("B constructor with parameters called");
    }
    public SecondClass(){
        LOG.debug("B constructor without parameters called");
    }

}
