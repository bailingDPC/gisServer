package com.gisserver.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author bailing
 */
@RestController
@RequestMapping("/user")
public class Account
{
    @PostMapping("/login")
    public Object login(){
        return new Object();
    }
}
