package com.javalitterboy.api_tencent.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.javalitterboy.api_tencent.util.DnsPodUtil;
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
    private DnsPodUtil apiReqUtil;

    /**
     * 获取指定域名解析记录
     * @param domain
     * @param subDomain
     * @param recordType
     * @return
     * @throws IOException
     */
    public JSONArray recordList(String domain, String subDomain, String recordType) throws IOException {
        HashMap<String, Object> hashMap = new HashMap<>(3);
        hashMap.put("domain", domain);
        if (subDomain != null) {
            hashMap.put("sub_domain", subDomain);
        }
        if (recordType != null) {
            hashMap.put("record_type", recordType);
        }
        JSONObject res = apiReqUtil.apiRequestPost(hashMap, "dnsapi.cn/Record.List");
        return (JSONArray) res.get("records");
    }

    /**
     * 根据id更新域名解析记录
     * @param domain
     * @param recordId
     * @param subDomain
     * @param recordType
     * @param recordLineId
     * @param value
     * @param ttl
     * @param mx
     * @return
     * @throws IOException
     */
    public JSONObject updateRecord(String domain, String recordId, String subDomain,
                                   String recordType, String recordLineId, String value, Integer ttl, Integer mx) throws IOException {
        HashMap<String, Object> hashMap = new HashMap<>(6);
        hashMap.put("domain", domain);
        hashMap.put("record_id", recordId);
        hashMap.put("sub_domain", subDomain);
        hashMap.put("record_type", recordType);
        hashMap.put("record_line", recordLineId);
        hashMap.put("record_line_id", recordLineId);
        hashMap.put("value", value);
        if(ttl!=null) {
            hashMap.put("ttl", ttl);
        }
        if(mx!=null) {
            hashMap.put("mx", mx);
        }else{
            hashMap.put("mx", 20);
        }
        return apiReqUtil.apiRequestPost(hashMap, "dnsapi.cn/Record.Modify");
    }
}
