package com.javalitterboy.api_tencent.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.javalitterboy.api_tencent.util.ApiReqUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;

/**
 * @program: api_tencent
 * @description: 解析服务
 * @author: JavaLitterBoy
 * @create: 2018-11-02 13:59
 **/
@Service
public class RecordService {

    @Resource
    private ApiReqUtil apiReqUtil;

    @Value("${api.host.cns}")
    private String cns_domain;

    @Value("${api.url}")
    private String url;

    // 获取指定域名解析记录
    public JSONArray record_list(String domain, String subDomain, String recordType) throws IOException {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("Action", "RecordList");
        hashMap.put("domain", domain);
        if (subDomain != null)
            hashMap.put("subDomain", subDomain);
        if (recordType != null)
            hashMap.put("recordType", recordType);
        JSONObject res = (JSONObject) apiReqUtil.apiRequestPost(hashMap, cns_domain+url);
        return (JSONArray) res.get("records");
    }

    // 根据id更新域名解析记录
    public JSONArray update_record(String domain, int recordId, String subDomain,
                                   String recordType, String recordLine, String value, Integer ttl, Integer mx) throws IOException {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("Action", "RecordModify");
        hashMap.put("domain", domain);
        hashMap.put("subDomain", subDomain);
        hashMap.put("recordType", recordType);
        hashMap.put("recordId", recordId);
        hashMap.put("recordLine", recordLine);
        hashMap.put("value", value);
        if(ttl!=null)
            hashMap.put("ttl", ttl);
        if(mx!=null)
            hashMap.put("mx", mx);
        return (JSONArray) apiReqUtil.apiRequestPost(hashMap, cns_domain+url);
    }
}
