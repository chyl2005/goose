package com.mm.goose.controller;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ApiAliveController {

    /**
     * alive接口，检查服务是否正常
     *
     * @return
     */
    @RequestMapping(value = "/api/monitor/alive")
    @ResponseBody
    public Map<String, Object> monitorAlive() {
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("status", "ok");
        return result;
    }

}
