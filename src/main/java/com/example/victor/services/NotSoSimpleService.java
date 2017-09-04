package com.example.victor.services;

import com.example.victor.utils.SomeClass;
import org.springframework.beans.factory.annotation.Autowired;

public class NotSoSimpleService {

    private SomeClass objectField;

    private int someInt;

    private double aDouble;

    public double getaDouble() {
        return aDouble;
    }

    public void setaDouble(double aDouble) {
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
