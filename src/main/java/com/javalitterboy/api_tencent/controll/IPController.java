package com.javalitterboy.api_tencent.controll;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.javalitterboy.api_tencent.service.RecordService;
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

    @Resource
    private RecordService recordService;

    @GetMapping("/update_ip")
    public String update_ip(@RequestParam("ip") String ip){
        try {
            String subDomains[] = {"*","www"};
            for(String subDomain:subDomains) {
                JSONArray records = recordService.record_list("stustyle.cn", subDomain, "A");
                for (Object object : records) {
                    recordService.update_record("stustyle.cn", (int) (((JSONObject) object).get("id")),
                            subDomain, "A", "默认", ip, null, null);
                }
            }
            return "请求成功";
        } catch (IOException e) {
            e.printStackTrace();
            return "请求失败";
        }
    }
}
