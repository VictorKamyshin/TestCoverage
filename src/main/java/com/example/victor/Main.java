package com.example.victor;

import com.example.victor.services.NotSoSimpleService;
import com.example.victor.services.SimpleService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class Main {
    public static void main(String[] args){
        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
    }

}
