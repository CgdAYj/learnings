package com.cgd.esjd.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author CaoGongde
 * @date 2020/4/28 21:11
 */
@Controller
public class IndexController {
    //url = https://search.jd.com/Search
    @GetMapping("/")
    public String index(){
        return "index";
    }

}
