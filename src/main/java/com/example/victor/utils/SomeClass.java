package com.example.victor.utils;

import org.springframework.context.annotation.Bean;

public class SomeClass {

    private Integer integer;
    private String string;

    public SomeClass(Integer integer, String string) {
        this.integer = integer;
        this.string = string;
    }

    @Bean
    public SomeClass someClass(){
        System.out.println("Bean method Called!");
        return new SomeClass(1,"2");
    }
}
