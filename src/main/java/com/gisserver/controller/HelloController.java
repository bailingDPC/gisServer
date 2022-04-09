package com.gisserver.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gisserver.domain.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author bailing
 */
@Controller
@RequestMapping("/hello")
public class HelloController
{
    @GetMapping("/test")
    @ResponseBody
    public String testController(){
        return "hello world, 你好 springBoot";
    }

    @PostMapping("/post")
    @ResponseBody
    public Map testPostRequest(@RequestBody Map body){
        String name = (String) body.get("name");
        Integer age = (Integer) body.get("age");
        System.out.println("用户:" + name + " 的年龄是" + age + " 岁");
        return body;
    }

    @PostMapping("/user")
    @ResponseBody
    public User testPostUser(@RequestBody User user){
        System.out.println(user);
        return user;
    }

    @GetMapping("/getjson")
    @ResponseBody
    public String testObjectToJSON() throws JsonProcessingException {
        User user = new User();
        user.setAge(23);
        user.setName("风雪中的白灵");
        user.setEmail("1938097427@qq.com");
        user.setPhone("18821690396");
        ObjectMapper mapper = new ObjectMapper();
        String jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(user);
        return jsonString;
    }
}
