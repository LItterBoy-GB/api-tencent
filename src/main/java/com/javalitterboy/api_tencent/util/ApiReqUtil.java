package com.javalitterboy.api_tencent.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * @program: api_tencent
 * @description:
 * @author: JavaLitterBoy
 * @create: 2018-11-02 14:18
 **/
@Component
public class ApiReqUtil {

    @Value("${api.secret_key}")
    private String secret_key;
    @Value("${api.secret_id}")
    private String secret_id;

    private static Logger logger = LoggerFactory.getLogger(ApiReqUtil.class);

    // 发起API请求 get方式
    public Object apiRequestGet(Map<String, Object> params, String url_path) throws IOException {
        Map<String, Object> map = handle_params(params, "GET", url_path);
        HttpGet httpGet = new HttpGet("https://" + url_path + "?" + hashMapToGetParams(map, true));

        HttpClient client = sslClient();
        HttpResponse response = client.execute(httpGet);

        return handle_response(response);
    }

    // 发起API请求 post方式
    public Object apiRequestPost(Map<String, Object> params, String url_path) throws IOException {
        Map<String, Object> map = handle_params(params, "POST", url_path);
        HttpPost httpPost = new HttpPost("https://" + url_path);
        logger.info("请求链接:https://" + url_path);
        httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");

        // POST请求也要对 参数进行url_encode  不知为何无法使用json提交
        String req_data = hashMapToGetParams(map, true);
        logger.info("请求数据:" + req_data);
        StringEntity entity = new StringEntity(req_data, Charset.forName("UTF-8"));
        httpPost.setEntity(entity);
        HttpClient client = sslClient();
        HttpResponse response = client.execute(httpPost);
        return handle_response(response);
    }

    private Object handle_response(HttpResponse response) throws IOException {
        if (response.getStatusLine().getStatusCode() == 200) {
            HttpEntity resEntity = response.getEntity();
            String message = EntityUtils.toString(resEntity, "utf-8");
            JSONObject object = JSON.parseObject(message);
            logger.info("响应数据:" + message);
            if (((int) object.get("code")) == 0) {
                return object.get("data");
            } else {
                logger.error((String) object.get("message"));
                return null;
            }
        } else {
            logger.error("请求失败:http响应码" + response.getStatusLine().getStatusCode());
            return null;
        }
    }

    // 获取支持ssl 的客户端
    private HttpClient sslClient() {
        try {
            // 在调用SSL之前需要重写验证方法，取消检测SSL
            X509TrustManager trustManager = new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] xcs, String str) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] xcs, String str) {
                }
            };
            SSLContext ctx = SSLContext.getInstance(SSLConnectionSocketFactory.TLS);
            ctx.init(null, new TrustManager[]{trustManager}, null);
            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(ctx, NoopHostnameVerifier.INSTANCE);
            // 创建Registry
            RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD_STRICT)
                    .setExpectContinueEnabled(Boolean.TRUE).setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))
                    .setProxyPreferredAuthSchemes(Collections.singletonList(AuthSchemes.BASIC)).build();
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .register("https", socketFactory).build();
            // 创建ConnectionManager，添加Connection配置信息
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            return HttpClients.custom().setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(requestConfig).build();
        } catch (KeyManagementException | NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    //请求参数处理  添加公共请求参数 添加签名
    private Map<String, Object> handle_params(Map<String, Object> params, String req_method, String url_path) throws UnsupportedEncodingException {
        params.put("Timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("Nonce", String.valueOf((long) (Math.random() * 100000)));
        params.put("SecretId", this.secret_id);
        params.put("Region", "gz");
        String sign = sign(params, req_method, url_path);
        logger.info("签名:" + sign);
        params.put("Signature", sign);       // 添加签名
        return params;
    }

    // 签名处理
    private String sign(Map<String, Object> params, String req_method, String url_path) throws UnsupportedEncodingException {
        if (params == null)
            return null;

        // 参数排序
        Map<String, Object> sortMap = new TreeMap<>(String::compareTo);
        sortMap.putAll(params);

        // 根据签名算法进行hash
        String sign_method = "HmacSHA1";    // 默认 HmacSHA1
        if ("HmacSHA256".equals(sortMap.get("SignatureMethod"))) {
            sign_method = "HmacSHA256";
        }

        // 拼接字符串
        String sb = req_method + url_path + "?" + hashMapToGetParams(sortMap, false);
        logger.info("原文字符串:" + sb);
        return sha_HMAC(sb, sign_method);
    }

    // hashMap 转 get 请求参数
    private String hashMapToGetParams(Map<String, Object> map, boolean is_url_encode) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        Set<String> keys = map.keySet();
        if (is_url_encode) {
            for (String key : keys) {
                if("recordLine".equals(key)){
                    sb.append(URLEncoder.encode(key, "UTF-8"))
                            .append("=")
                            .append("%E9%BB%98%E8%AE%A4")
                            .append("&");

                }else {
                    sb.append(URLEncoder.encode(key, "UTF-8"))
                            .append("=")
                            .append(URLEncoder.encode(String.valueOf(map.get(key)), "UTF-8"))
                            .append("&");
                }
            }

        } else {
            for (String key : keys) {
                sb.append(key)
                        .append("=")
                        .append(map.get(key))
                        .append("&");
            }
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    private String sha_HMAC(String message, String method) {
        try {
            SecretKeySpec secret = new SecretKeySpec(this.secret_key.getBytes(), method);
            Mac HMAC = Mac.getInstance(method);
            HMAC.init(secret);
            byte[] bytes = HMAC.doFinal(message.getBytes());
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            logger.error(method + " ===========" + e.getMessage());
        }
        return null;
    }
}