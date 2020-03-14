package com.javalitterboy.api_tencent.controll;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.javalitterboy.api_tencent.service.RecordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @program: api_tencent
 * @description:
 * @author: JavaLitterBoy
 * @create: 2018-11-02 17:54
 **/
@RestController
public class IPController {

    @Value("api.domain")
    private String domain;

    @Value("api.sub_domains")
    private String[] subDomains;

    @Resource
    private RecordService recordService;

    private static Logger logger = LoggerFactory.getLogger(IPController.class);

    @GetMapping("/update_ip")
    public String update_ip(@RequestParam("ip") String ip, @RequestParam("sub") String subDomain) {
        try {
            logger.info("更换ip:" + ip);
            JSONArray records = recordService.recordList(domain, subDomain, "A");
            for (Object object : records) {
                recordService.updateRecord(domain, (String) (((JSONObject) object).get("id")),
                        subDomain, "A", "0", ip, null, null);
            }
            return "success";
        } catch (IOException e) {
            e.printStackTrace();
            return "fail";
        }
    }
}
