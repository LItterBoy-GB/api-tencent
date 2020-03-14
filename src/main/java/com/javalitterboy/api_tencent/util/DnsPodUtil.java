package com.javalitterboy.api_tencent.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
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
 * DnsPodUtil
 *
 * @author 14183
 */
@Component
public class DnsPodUtil {

    @Value("${api.login_token}")
    private String loginToken;

    private static Logger logger = LoggerFactory.getLogger(DnsPodUtil.class);

    // 发起API请求 post方式
    public JSONObject apiRequestPost(Map<String, Object> params, String urlPath) throws IOException {
        Map<String, Object> map = handleParams(params);
        HttpPost httpPost = new HttpPost("https://" + urlPath);
        logger.info("请求链接:https://" + urlPath);
        httpPost.setHeader("Content-type", "application/x-www-form-urlencoded");

        // POST请求也要对 参数进行url_encode  不知为何无法使用json提交
        String reqData = hashMapToGetParams(map);
        logger.info("请求数据:" + reqData);
        StringEntity entity = new StringEntity(reqData, Charset.forName("UTF-8"));
        httpPost.setEntity(entity);
        HttpClient client = sslClient();
        HttpResponse response = client.execute(httpPost);
        return handleResponse(response);
    }

    private JSONObject handleResponse(HttpResponse response) throws IOException {
        if (response.getStatusLine().getStatusCode() == 200) {
            HttpEntity resEntity = response.getEntity();
            String message = EntityUtils.toString(resEntity, "utf-8");
            JSONObject object = JSON.parseObject(message);
            logger.info("响应数据:" + message);
            if("1".equals(((JSONObject)(object.get("status"))).get("code"))){
                return object;
            } else {
                logger.error((String) ((JSONObject)(object.get("status"))).get("message"));
                return null;
            }
        } else {
            logger.error("请求失败:http响应码" + response.getStatusLine().getStatusCode());
            return null;
        }
    }

    /**
     * 获取支持ssl 的客户端
     * @return
     */
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

    /**
     * 请求参数处理  添加公共请求参数 添加签名
     * @param params
     * @return
     */
    private Map<String, Object> handleParams(Map<String, Object> params) {
        params.put("login_token", this.loginToken);
        params.put("format", "json");
        return params;
    }

    /**
     * hashMap 转 get 请求参数
     * @param map
     * @return
     * @throws UnsupportedEncodingException
     */
    private String hashMapToGetParams(Map<String, Object> map) throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        Set<String> keys = map.keySet();
        for (String key : keys) {
            sb.append(URLEncoder.encode(key, "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(String.valueOf(map.get(key)), "UTF-8"))
                    .append("&");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }
}
