package com.shipparts.extcontroller;

import com.shipparts.annotation.ExtController;
import com.shipparts.annotation.ExtRequestMapping;

@ExtController
@ExtRequestMapping("/test")
public class ExtIndexController {
    @ExtRequestMapping("/index")
    public String index(){
        return "index";
    }
}
