package com.gym.easychatjava;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.gym.easychatjava.mapper")
public class EasyChatJavaApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyChatJavaApplication.class, args);
    }

}
