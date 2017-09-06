package com.example.victor.services.recursionCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class B {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());
    public B(A a){
        LOG.debug("B constructor with parameters called");
    }
    public B(){
        LOG.debug("B constructor without parameters called");
    }

}
