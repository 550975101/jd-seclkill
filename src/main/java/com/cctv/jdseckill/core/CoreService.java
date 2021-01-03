package com.cctv.jdseckill.core;

import com.cctv.jdseckill.utils.ImagePanel;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.swing.*;
import java.io.*;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author 封心
 */
public class CoreService {

    ObjectMapper objectMapper = new ObjectMapper();

    String token = "";

    String qrcode = "";

    String ticket = "";


    PoolingHttpClientConnectionManager cm;

    String ENCODING = "UTF-8";

    String CONTENT_TYPE = "application/json";

    RequestConfig requestConfig = null;

    // 全局请求设置
    RequestConfig globalConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();
    // 创建cookie store的本地实例
    CookieStore cookieStore = new BasicCookieStore();
    // 创建HttpClient上下文
    HttpClientContext context = HttpClientContext.create();

    {
        requestConfig = RequestConfig.custom()
                .setConnectTimeout(25000)
                .setConnectionRequestTimeout(25000)
                .setSocketTimeout(25000)
                .build();
    }

    private void init() {
        if (cm == null) {
            cm = new PoolingHttpClientConnectionManager();
            // 整个连接池最大连接数
            cm.setMaxTotal(50);
            // 每路由最大连接数，默认值是2
            cm.setDefaultMaxPerRoute(5);
        }
    }

    private CloseableHttpClient getHttpClient() {
        init();
        context.setCookieStore(cookieStore);
        return HttpClients.custom().setConnectionManager(cm).setDefaultRequestConfig(globalConfig).setDefaultCookieStore(cookieStore).build();
    }

    /**
     * """获取商品名称"""
     *
     * @param properties
     */
    public void getSkuTile(Properties properties) {
        HttpClient client = getHttpClient();
        String url = "https://item.jd.com/" + properties.get("sku_id") + ".html";
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("User-Agent", properties.getProperty("DEFAULT_USER_AGENT"));
        httpGet.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;" +
                "q=0.9,image/webp,image/apng,*/*;" +
                "q=0.8,application/signed-exchange;" +
                "v=b3");
        httpGet.addHeader("Connection", "keep-alive");
        try {
            HttpResponse response = client.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                System.out.println("response status is " + statusCode);
            } else {
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity, "UTF-8");
                Document document = Jsoup.parse(result);
                String title = document.title();
                System.out.println(LocalDateTime.now() + " 您抢购的商品为:" + title);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loginByQrcode(Properties properties) {
        HttpClient client = getHttpClient();
        String loginUrl = "https://passport.jd.com/new/login.aspx";
        HttpGet httpGet = new HttpGet(loginUrl);
        httpGet.addHeader("User-Agent", properties.getProperty("DEFAULT_USER_AGENT"));
        httpGet.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;" +
                "q=0.9,image/webp,image/apng,*/*;" +
                "q=0.8,application/signed-exchange;" +
                "v=b3");
        httpGet.addHeader("Connection", "keep-alive");
        try {
            HttpResponse response = client.execute(httpGet);
            int statusCode = response.getStatusLine().getStatusCode();
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                System.out.println("response status is " + statusCode);
            } else {
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity, "UTF-8");
                Document document = Jsoup.parse(result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getQrCode(Properties properties) {
        String url = "https://qr.m.jd.com/show";
        HashMap<String, Object> params = new HashMap<>();
        params.put("appid", "133");
        params.put("size", "147");
        params.put("t", System.currentTimeMillis() + "");
        Map<String, String> heads = new HashMap<>(4);
        heads.put("User-Agent", properties.getProperty("DEFAULT_USER_AGENT"));
        heads.put("Referer", "https://passport.jd.com/new/login.aspx");
        try {
            HttpResponse response = doGetRequest(url, params, heads);
            int statusCode = response.getStatusLine().getStatusCode();
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                System.out.println("response status is " + statusCode);
            } else {
                HttpEntity entity = response.getEntity();
                Header lastHeader = response.getLastHeader("set-cookie");
                HeaderElement[] elements = lastHeader.getElements();
                token = elements[0].getValue();
                Header firstHeader = response.getFirstHeader("set-cookie");
                HeaderElement[] firstHeaderElements = firstHeader.getElements();
                qrcode = firstHeaderElements[0].getValue();
                cookieStore = context.getCookieStore();
                System.out.println(LocalDateTime.now() + " token:" + token);
                InputStream inputStream = entity.getContent();
                //原先图片所在路径
                BufferedInputStream in = new BufferedInputStream(inputStream);
                //你要保存在哪个目录下面
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(".\\qr_code.png"));
                int i;
                while ((i = in.read()) != -1) {
                    out.write(i);
                }
                out.flush();
                out.close();
                in.close();
                ImagePanel imagePanel = new ImagePanel(new ImageIcon("qr_code.png").getImage());
                JFrame frame = new JFrame();
                frame.setContentPane(imagePanel);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setBounds(500, 300, 500, 500);
                frame.setVisible(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getQrcodeTicket(Properties properties) {
        String url = "https://qr.m.jd.com/check";
        HashMap<String, Object> params = new HashMap<>();
        params.put("appid", "133");
        params.put("callback", "jQuery" + (8999999 + (int) (Math.random() * 1000000)));
        params.put("token", token);
        params.put("_", System.currentTimeMillis() + "");
        Map<String, String> heads = new HashMap<>(4);
        heads.put("User-Agent", properties.getProperty("DEFAULT_USER_AGENT"));
        heads.put("Referer", "https://passport.jd.com/new/login.aspx");
        try {
            HttpResponse response = doGetRequest(url, params, heads);
            int statusCode = response.getStatusLine().getStatusCode();
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                System.out.println("response status is " + statusCode);
            } else {
                HttpEntity entity = response.getEntity();
                cookieStore = context.getCookieStore();
                String result = EntityUtils.toString(entity, "UTF-8");
                if (result.contains("ticket")) {
                    String substring = result.substring(14, result.length() - 1);
                    Map map = objectMapper.readValue(substring, Map.class);
                    ticket = map.get("ticket").toString();
                    System.out.println(LocalDateTime.now() + " 票据获取成功:" + ticket);
                } else {
                    System.out.println(LocalDateTime.now() + " 票据获取失败");
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void validateQrcodeTicket(Properties properties) {
        String url = "https://passport.jd.com/uc/qrCodeTicketValidation";
        HashMap<String, Object> params = new HashMap<>();
        params.put("t", ticket);
        Map<String, String> heads = new HashMap<>(4);
        heads.put("User-Agent", properties.getProperty("DEFAULT_USER_AGENT"));
        heads.put("Referer", "https://passport.jd.com/uc/login?ltype=logout");
        try {
            HttpResponse response = doGetRequest(url, params, heads);
            int statusCode = response.getStatusLine().getStatusCode();
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                System.out.println("response status is " + statusCode);
            } else {
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity, "UTF-8");
                if (result.contains("\"returnCode\":0")) {
                    System.out.println(LocalDateTime.now() + " 票据验证成功");
                } else {
                    System.out.println(LocalDateTime.now() + " 登录失败重新扫码登录");
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取用户信息
     * @param properties
     */
    public void getUserName(Properties properties) {
        String url = "https://passport.jd.com/user/petName/getUserInfoForMiniJd.action";
        HashMap<String, Object> params = new HashMap<>();
        params.put("callback", "jQuery" + (8999999 + (int) (Math.random() * 1000000)));
        params.put("_", System.currentTimeMillis() + "");
        Map<String, String> heads = new HashMap<>(4);
        heads.put("User-Agent", properties.getProperty("DEFAULT_USER_AGENT"));
        heads.put("Referer", "https://order.jd.com/center/list.action");
        try {
            HttpResponse response = doGetRequest(url, params, heads);
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity, "UTF-8");
            if (result.contains("nickName")) {
                String substring = result.substring(14, result.length() - 1);
                Map map = objectMapper.readValue(substring, Map.class);
                String nickName = map.get("nickName").toString();
                System.out.println(LocalDateTime.now() + " 您的昵称是: " + nickName);
            } else {
                System.out.println(LocalDateTime.now() + " 获取昵称失败");
                System.exit(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *获取商品的抢购链接
     *点击"抢购"按钮后，会有两次302跳转，最后到达订单结算页面
     *这里返回第一次跳转后的页面url，作为商品的抢购链接
     *:return: 商品的抢购链接
     * @param properties
     * @return
     */
    public String getSeckillUrl(Properties properties) {
        String newUrl;
        String url = "https://itemko.jd.com/itemShowBtn";
        Map<String, Object> params = new HashMap<>(4);
        params.put("callback", "jQuery" + (8999999 + (int) (Math.random() * 1000000)));
        params.put("skuId", properties.getProperty("sku_id"));
        params.put("from", "pc");
        params.put("_", System.currentTimeMillis() + "");
        Map<String, String> heads = new HashMap<>(4);
        heads.put("User-Agent", properties.getProperty("DEFAULT_USER_AGENT"));
        heads.put("Host", "itemko.jd.com");
        heads.put("Referer", "https://item.jd.com/" + properties.getProperty("sku_id") + ".html");
        try {
            HttpResponse response = doGetRequest(url, params, heads);
            HttpEntity entity = response.getEntity();
            String result = EntityUtils.toString(entity, "UTF-8");
            if (result.contains("url")) {
                String substring = result.substring(14, result.length() - 1);
                Map map = objectMapper.readValue(substring, Map.class);
                String seckillUrl = map.get("url").toString();
                if ("".equals(seckillUrl)) {
                    System.out.println(LocalDateTime.now() + " 抢购url为空,获取失败" + seckillUrl);
                } else {
                    System.out.println(LocalDateTime.now() + " 抢购url: " + seckillUrl);
                    newUrl = "https://" + seckillUrl;
                    newUrl = newUrl.replace("divide", "marathon").replace("user_routing", "captcha.html");
                    System.out.println(LocalDateTime.now() + " 真实抢购url: " + newUrl);
                    return newUrl;
                }
            } else {
                System.out.println(LocalDateTime.now() + " 抢购url为空,获取异常");
            }
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        return null;
    }

    /**
     * 访问商品的抢购链接（用于设置cookie)
     * @param properties
     * @param seckillUrl
     */
    public void requestSeckillUrl(Properties properties, String seckillUrl) {
        try {
            Map<String, Object> params = new HashMap<>(4);
            HashMap<String, String> heads = new HashMap<>();
            heads.put("User-Agent", properties.getProperty("DEFAULT_USER_AGENT"));
            heads.put("Host", "marathon.jd.com");
            heads.put("Referer", "https://item.jd.com/" + properties.getProperty("sku_id") + ".html");
            HttpResponse response = doGetRequest(seckillUrl, params, heads);
            int statusCode = response.getStatusLine().getStatusCode();
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                System.out.println("response status is " + statusCode);
            } else {
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity, "UTF-8");
                cookieStore = context.getCookieStore();
                System.out.println(LocalDateTime.now() + " 进步秒杀页面成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 访问抢购订单结算页面
     * @param properties
     */
    public void requestSeckillCheckoutPage(Properties properties) {
        String url = "https://marathon.jd.com/seckill/seckill.action";
        Map<String, Object> params = new HashMap<>(4);
        params.put("skuId", properties.getProperty("sku_id"));
        params.put("num", properties.getProperty("seckill_num"));
        params.put("rid", System.currentTimeMillis() / 1000 + "");
        Map<String, String> heads = new HashMap<>();
        heads.put("User-Agent", properties.getProperty("DEFAULT_USER_AGENT"));
        heads.put("Host", "marathon.jd.com");
        heads.put("Referer", "https://item.jd.com/" + properties.getProperty("sku_id") + ".html");
        try {
            HttpResponse response = doGetRequest(url, params, heads);
            int statusCode = response.getStatusLine().getStatusCode();
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                System.out.println("response status is " + statusCode);
            } else {
                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity, "UTF-8");
                System.out.println(LocalDateTime.now() + " 进入订单结算页面成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取秒杀初始化信息（包括：地址，发票，token）
     * @param properties
     * @return
     */
    public String getSeckillInitInfo(Properties properties) {
        String seckillInfo;
        HttpClient client = getHttpClient();
        String url = "https://marathon.jd.com/seckillnew/orderService/pc/init.action";
        Map<String, Object> params = new HashMap<>(4);
        params.put("sku", properties.getProperty("sku_id"));
        params.put("num", properties.getProperty("seckill_num"));
        params.put("isModifyAddress", "false");
        Map<String, String> heads = new HashMap<>(2);
        heads.put("User-Agent", properties.getProperty("DEFAULT_USER_AGENT"));
        heads.put("Host", "marathon.jd.com");
        try {
            URIBuilder ub = new URIBuilder();
            ub.setPath(url);
            ArrayList<NameValuePair> pairs = covertParams2NVPS(params);
            ub.setParameters(pairs);
            HttpPost httpPost = new HttpPost(ub.build().toString().substring(1));
            HttpResponse response = client.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
                System.out.println("response status is " + statusCode);
            }else {
                HttpEntity resEntity = response.getEntity();
                seckillInfo = EntityUtils.toString(resEntity, "UTF-8");
                System.out.println(LocalDateTime.now() + " 获取秒杀初始化信息、地址、发票、等:" + seckillInfo);
                return seckillInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 生成提交抢购订单所需的请求体参数
     * @param seckillInfo
     */
    public Map<String, Object> getSeckillOrderData(Properties properties,String seckillInfo) {
        Map<String, Object> params = new HashMap<>(32);
        try {
            JsonNode jsonNode = objectMapper.readTree(seckillInfo);
            params.put("skuId", properties.getProperty("sku_id"));
            params.put("num", properties.getProperty("seckill_num"));
            JsonNode addressList = jsonNode.findValue("addressList");
            JsonNode defaultAddress = addressList.get(0);
            JsonNode invoiceInfo = jsonNode.findValue("invoiceInfo");
            String token = jsonNode.findValue("token").asText();
            params.put("addressId", defaultAddress.findValue("id").asText());
            params.put("yuShou", "true");
            params.put("isModifyAddress", "false");
            params.put("name", defaultAddress.get("name").asText());
            params.put("provinceId", defaultAddress.get("provinceId").asText());
            params.put("cityId", defaultAddress.get("cityId").asText());
            params.put("countyId", defaultAddress.get("countyId").asText());
            params.put("addressDetail", defaultAddress.get("addressDetail").asText());
            params.put("mobile", defaultAddress.get("mobile").asText());
            params.put("mobileKey", defaultAddress.get("mobileKey").asText());
            params.put("email", defaultAddress.get("email").asText());
            params.put("postCode","");
            if (invoiceInfo != null) {
                params.put("invoiceTitle", invoiceInfo.get("invoiceTitle").asText());
                params.put("invoiceContent", invoiceInfo.get("invoiceContentType").asText());
                params.put("invoicePhone", invoiceInfo.get("invoicePhone").asText());
                params.put("invoicePhoneKey", invoiceInfo.get("invoicePhoneKey").asText());
                params.put("invoiceType", invoiceInfo.get("invoiceType").asText());
                params.put("invoice", "true");
            } else {
                params.put("invoiceTitle", -1);
                params.put("invoiceContent", 1);
                params.put("invoicePhone", "");
                params.put("invoicePhoneKey", "");
                params.put("invoice", "false");
            }
            params.put("invoiceTaxpayerNO", "");
            params.put("invoiceEmail", "");
            params.put("invoiceCompanyName", "");
            params.put("password", properties.getProperty("account"));
            params.put("codTimeType", 3);
            params.put("paymentType", 4);
            params.put("areaCode", "");
            params.put("overseas", 0);
            params.put("phone", "");
            params.put("eid", properties.getProperty("eid"));
            params.put("fp", properties.getProperty("fp"));
            params.put("token", token);
            params.put("pru", "");
            return params;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Boolean submitSeckillOrder(Properties properties,Map<String, Object> jsonMap) {
        HttpClient client = getHttpClient();
        String url = "https://marathon.jd.com/seckillnew/orderService/pc/submitOrder.action";
        Map<String, Object> params = new HashMap<>(4);
        params.put("skuId", properties.getProperty("sku_id"));
        Map<String, String> heads = new HashMap<>(2);
        heads.put("User-Agent", properties.getProperty("DEFAULT_USER_AGENT"));
        heads.put("Host", "marathon.jd.com");
        heads.put("Referer", "https://marathon.jd.com/seckill/seckill.action?skuId=" + properties.getProperty("sku_id") + "&num=" + properties.getProperty("seckill_num") + "&rid=" + System.currentTimeMillis() / 1000);
        if (jsonMap == null) {
            return false;
        }
        try {
            String jsonData = objectMapper.writeValueAsString(jsonMap);
            URIBuilder ub = new URIBuilder();
            ub.setPath(url);
            ArrayList<NameValuePair> pairs = covertParams2NVPS(params);
            ub.setParameters(pairs);
            HttpPost httpPost = new HttpPost(ub.build().toString().substring(1));
            //装填参数
            StringEntity stringEntity = new StringEntity(jsonData, ENCODING);
            stringEntity.setContentType(CONTENT_TYPE);
            stringEntity.setContentEncoding(ENCODING);
            httpPost.setEntity(stringEntity);
            HttpResponse response = client.execute(httpPost);
            int statusCode = response.getStatusLine().getStatusCode();
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK){
                System.out.println("response status is " + statusCode);
            }else {
                HttpEntity resEntity = response.getEntity();
                String result = EntityUtils.toString(resEntity, "UTF-8");
                JsonNode jsonNode = objectMapper.readTree(result);
                boolean success = jsonNode.findValue("success").asBoolean();
                if (success) {
                    String orderId = jsonNode.findValue("orderId").asText();
                    String totalMoney = jsonNode.findValue("totalMoney").asText();
                    String pcUrl = jsonNode.findValue("pcUrl").asText();
                    System.out.println(LocalDateTime.now() + " 抢购成功,订单号: " + orderId + " 总价:" + totalMoney + " 电脑付款链接: " + pcUrl);
                } else {
                    System.out.println(LocalDateTime.now() + " 抢购失败: " + result);
                }
                return success;
            }
        } catch (Exception e) {
            System.out.println(LocalDateTime.now() + " 抢购失败: " + "https://marathon.jd.com/koFail.html");
        }
        return false;
    }

    public HttpResponse doGetRequest(String url, Map<String, Object> params, Map<String, String> heads) {
        HttpClient client = getHttpClient();
        URIBuilder ub = new URIBuilder();
        ub.setPath(url);
        ArrayList<NameValuePair> pairs = covertParams2NVPS(params);
        ub.setParameters(pairs);
        try {
            final HttpGet httpGet = new HttpGet(ub.build().toString().substring(1));
            if (!heads.isEmpty()) {
                Set<String> keySet = heads.keySet();
                keySet.forEach(key -> {
                    httpGet.addHeader(key, heads.get(key));
                });
            }
            return client.execute(httpGet);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private String getEncodeParamStr(HashMap<String, String> parames) {
        String str = "";
        Object[] keyArray = parames.keySet().toArray();
        for (int i = 0; i < keyArray.length; i++) {
            String key = (String) keyArray[i];
            if (0 == i) {
                str += (key + "=" + parames.get(key));
            } else {
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

    private static ArrayList<NameValuePair> covertParams2NVPS(Map<String, Object> params) {
        ArrayList<NameValuePair> pairs = new ArrayList<NameValuePair>();
        if (params.size() > 0) {
            for (Map.Entry<String, Object> param : params.entrySet()) {
                pairs.add(new BasicNameValuePair(param.getKey(), String.valueOf(param.getValue())));
            }
        }
        return pairs;
    }

}
