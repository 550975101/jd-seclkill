package com.cctv.jdseckill.utils;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description:
 * @Author: wangqiang
 * @Date:2018/7/20 9:14
 * @创建时jdk版本 1.8
 */
public class HttpUtil {

    private static PoolingHttpClientConnectionManager cm;

    private static String ENCODING = "UTF-8";

    private static String CONTENT_TYPE = "application/json";

    private static RequestConfig requestConfig = null;

    static {
         requestConfig = RequestConfig.custom()
                 .setConnectTimeout(25000)
                 .setConnectionRequestTimeout(25000)
                 .setSocketTimeout(25000)
                 .build();
    }

    private static void init() {
        if (cm == null) {
            cm = new PoolingHttpClientConnectionManager();
            // 整个连接池最大连接数
            cm.setMaxTotal(50);
            // 每路由最大连接数，默认值是2
            cm.setDefaultMaxPerRoute(5);
        }
    }

    private static CloseableHttpClient getHttpClient() {
        init();
        return HttpClients.custom().setConnectionManager(cm).build();
    }


    /**
     * 发送get请求
     * @param url
     * @param params
     * @return
     */
    public static String httpGet(String url, HashMap<String, String> params) {
        CloseableHttpClient client = getHttpClient();
        if (null != params && !params.isEmpty()){
            url = new StringBuffer(url).append("?")
                    .append(getEncodeParamStr(params))
                    .toString();
        }
        System.out.println(url);
        HttpGet get = new HttpGet(url);
        get.setConfig(requestConfig);
        get.addHeader("User-Agent", "ApiSdk Client v0.1");
        return execute(client, get);
    }

    /**
     * 默认参数格式post请求
     * @param url
     * @param params
     * @return
     */
    public static String httpPostDefault(String url, HashMap<String, String> params) throws UnsupportedEncodingException {
        CloseableHttpClient client = getHttpClient();
        HttpPost post = new HttpPost(url);
        post.setConfig(requestConfig);
        post.addHeader("User-Agent", "ApiSdk Client v0.1");
        //装填参数
        List<NameValuePair> nvps = new ArrayList<>();
        if(params!=null && !params.isEmpty()){
            for (Map.Entry<String, String> entry : params.entrySet()) {
                nvps.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
        }
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(nvps, ENCODING);
        post.setEntity(entity);
        return execute(client, post);
    }

    /**
     * 发送带有令牌请求头 参数格式为json的post请求
     * @param url
     * @param paramJson
     * @param token
     * @return
     */
    public static String httpPostJsonHeaderToken(String url, String paramJson, String token){
        CloseableHttpClient client = getHttpClient();
        HttpPost post = new HttpPost(url);
        post.setConfig(requestConfig);
        post.addHeader("User-Agent", "ApiSdk Client v0.1");
        post.addHeader("token", token);
        //装填参数
        StringEntity stringEntity = new StringEntity(paramJson, ENCODING);
        stringEntity.setContentType(CONTENT_TYPE);
        stringEntity.setContentEncoding(ENCODING);
        post.setEntity(stringEntity);
        return execute(client, post);
    }

    /**
     * 发送json格式POST请求
     *
     * @param url
     * @param paramJson
     * @return
     */
    public static String httpPostJson(String url, String paramJson){
        CloseableHttpClient client = getHttpClient();
        HttpPost post = new HttpPost(url);
        post.setConfig(requestConfig);
        post.addHeader("User-Agent", "ApiSdk Client v0.1");
        StringEntity stringEntity = new StringEntity(paramJson, ENCODING);
        stringEntity.setContentType(CONTENT_TYPE);
        stringEntity.setContentEncoding(ENCODING);
        post.setEntity(stringEntity);
        return execute(client, post);
    }

    /**
     * 执行post请求
     * @param client
     * @param request
     * @return
     */
    private static String execute(CloseableHttpClient client, HttpRequestBase request){
        String result = null;
        CloseableHttpResponse response = null;
        try {
            response = client.execute(request);//发送请求获取响应数据
            int statusCode = response.getStatusLine().getStatusCode();
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
                System.out.println("response status is " + statusCode);
            }else {
                HttpEntity entity = response.getEntity();
                result = EntityUtils.toString(entity, "UTF-8");
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if (response != null)response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }
    }

    /**
     * get请求拼装参数
     *
     * @param parames
     * @return
     */
    private static String getEncodeParamStr(HashMap<String, String> parames){
        String str = "";
        Object[] keyArray = parames.keySet().toArray();
        for(int i = 0; i < keyArray.length; i++){
            String key = (String)keyArray[i];
            if(0 == i){
                str += (key + "=" + parames.get(key));
            }
            else{
                str += ("&" + key + "=" + parames.get(key));
            }
        }
        try {
            str = URLEncoder.encode(str, ENCODING)
                    .replace("%3A", ":")
                    .replace("%2F", "/")
                    .replace("%26", "&")
                    .replace("%3D", "=")
                    .replace("%3F", "?");
        } catch (Exception e) {
            throw new RuntimeException("参数字符串编码失败");
        }
        return str;
    }

}
