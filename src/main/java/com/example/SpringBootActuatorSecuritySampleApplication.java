package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@SpringBootApplication
public class SpringBootActuatorSecuritySampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootActuatorSecuritySampleApplication.class, args);
    }

    @Configuration
    static class SecurityConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(AuthenticationManagerBuilder auth) throws Exception {
            auth.inMemoryAuthentication()
                    .withUser("actuator").password("password").roles("ACTUATOR").and()
                    .withUser("user").password("password").roles("USER");
        }
    }
}
