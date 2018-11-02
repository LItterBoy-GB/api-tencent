package com.javalitterboy.api_tencent.util;

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
public class Utils {

    private static final String SECRET_KEY = "xxxx";
    private static final String SECRET_ID = "xxxx";

    // 发起API请求 get方式
    public static JSONObject apiRequestGet(Map<String, String> params, String url_path) throws IOException {
        Map<String, String> map = handle_params(params, "GET", url_path);
        HttpGet httpGet = new HttpGet("https://" + url_path + "?" + hashMapToGetParams(map, true));

        HttpClient client = sslClient();
        HttpResponse response = client.execute(httpGet);

        if (response.getStatusLine().getStatusCode() == 200) {
            HttpEntity resEntity = response.getEntity();
            String message = EntityUtils.toString(resEntity, "utf-8");
            System.out.println(message);
        } else {
            System.out.println("请求失败");
        }

        return null;
    }

    // 发起API请求 post方式
    public static JSONObject apiRequestPost(Map<String, String> params, String url_path) throws IOException {
        Map<String, String> map = handle_params(params, "POST", url_path);
        HttpPost httpPost = new HttpPost("https://" + url_path);
        httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");

        // POST请求也要对 参数进行url_encode  不知为何无法使用json提交
        StringEntity entity = new StringEntity(hashMapToGetParams(map,true), Charset.forName("UTF-8"));
        httpPost.setEntity(entity);
        HttpClient client = sslClient();
        HttpResponse response = client.execute(httpPost);

        if (response.getStatusLine().getStatusCode() == 200) {
            HttpEntity resEntity = response.getEntity();
            String message = EntityUtils.toString(resEntity, "utf-8");
            System.out.println(message);
        } else {
            System.out.println("请求失败");
        }
        return null;
    }

    // 获取支持ssl 的客户端
    private static HttpClient sslClient() {
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
    private static Map<String, String> handle_params(Map<String, String> params, String req_method, String url_path) throws UnsupportedEncodingException {
        params.put("Timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("Nonce", String.valueOf((long) (Math.random() * 100000)));
        params.put("SecretId", SECRET_ID);
        params.put("Region", "gz");
        params.put("Signature", sign(params, req_method, url_path));       // 添加签名
        return params;
    }

    // 签名处理
    private static String sign(Map<String, String> params, String req_method, String url_path) throws UnsupportedEncodingException {
        if (params == null)
            return null;

        // 参数排序
        Map<String, String> sortMap = new TreeMap<>(String::compareTo);
        sortMap.putAll(params);

        // 根据签名算法进行hash
        String sign_method = "HmacSHA1";    // 默认 HmacSHA1
        if ("HmacSHA256".equals(sortMap.get("SignatureMethod"))) {
            sign_method = "HmacSHA256";
        }

        // 拼接字符串
        String sb = req_method + url_path + "?" + hashMapToGetParams(sortMap, false);
        System.out.println("原文字符串:" + sb);
        System.out.println("签名:" + sha_HMAC(sb, sign_method));
        return sha_HMAC(sb, sign_method);
    }

    // hashMap 转 get 请求参数
    private static String hashMapToGetParams(Map<String, String> map, boolean is_url_encode) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        Set<String> keys = map.keySet();
        if (is_url_encode) {
            for (String key : keys) {
                sb.append(URLEncoder.encode(key, "UTF-8"))
                        .append("=")
                        .append(URLEncoder.encode(map.get(key), "UTF-8"))
                        .append("&");
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

    private static String sha_HMAC(String message, String method) {
        try {
            SecretKeySpec secret_key = new SecretKeySpec(SECRET_KEY.getBytes(), method);
            Mac HMAC = Mac.getInstance(method);
            HMAC.init(secret_key);
            byte[] bytes = HMAC.doFinal(message.getBytes());
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            System.out.println("Error " + method + " ===========" + e.getMessage());
        }
        return null;
    }
}