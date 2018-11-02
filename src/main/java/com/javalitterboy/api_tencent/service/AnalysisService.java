package com.javalitterboy.api_tencent.service;

import com.javalitterboy.api_tencent.util.Utils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;

/**
 * @program: api_tencent
 * @description: 解析服务
 * @author: JavaLitterBoy
 * @create: 2018-11-02 13:59
 **/
@Service
public class AnalysisService {
  public String[] get_record_list(String subDomain,String recordType) throws IOException {
      HashMap<String,String> hashMap = new HashMap<>();
      hashMap.put("Action","RecordList");
      hashMap.put("domain","stustyle.cn");
      if(subDomain!=null)
        hashMap.put("subDomain",subDomain);
      if(recordType!=null)
          hashMap.put("recordType",recordType);
      Utils.apiRequestPost(hashMap,"cns.api.qcloud.com/v2/index.php");
      return null;
  }
  public static void main(String args[]) throws IOException{
        new AnalysisService().get_record_list(null,null);
  }

}
