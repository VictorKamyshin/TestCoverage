package com.example.victor.services;

import com.example.victor.utils.SomeClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotSoSimpleService {

    @Autowired
    private SimpleService ss;

    private SomeClass objectField;

    private int someInt;

    private double aDouble;

    public NotSoSimpleService(){ }

    public NotSoSimpleService(Integer i){
        this.someInt = i;
    }

    public NotSoSimpleService(Double d){
        this.aDouble = d;
    }

    public NotSoSimpleService(@Autowired  SomeClass objectField) {
        this.objectField = objectField;
    }

    @Autowired
    public void setSs(SimpleService ss) {
        this.ss = ss;
    }

    public SimpleService getSs() {
        return ss;
    }

    public double getaDouble() {
        return aDouble;
    }

    public void setaDouble(Double aDouble) {
        this.aDouble = aDouble;
    }

    public SomeClass getObjectField() {
        return objectField;
    }

    public void setObjectField(@Autowired SomeClass objectField) {
        this.objectField = objectField;
    }

    public int getSomeInt() {
        return someInt;
    }

    public void setSomeInt(int someInt) {
        this.someInt = someInt;
    }
}
