package com.example.springbootdemo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * demo
 *
 * @author xiongLiang
 * @date 2018/5/18 16:54
 */
@RestController
public class HelloController {
    private static final Logger logger = LoggerFactory.getLogger(HelloController.class);

    @GetMapping("hello")
    public String hello() {
        logger.debug("哈哈，访问成功了");
        return "hello";
    }
}
