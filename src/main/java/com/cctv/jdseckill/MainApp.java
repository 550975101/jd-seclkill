package com.cctv.jdseckill;

import com.cctv.jdseckill.core.CoreService;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.*;

/**
 * @author 封心
 */
public class MainApp {
    public static void main(String[] args) {
        System.out.println("京东秒杀茅台脚本");
        System.out.println("出现二维码后，请您在10秒内扫码,完成请不要关闭窗口,最小化即可");
        InputStream config = MainApp.class.getClassLoader().getResourceAsStream("config.properties");
        Properties configProperties = new Properties();
        try {
            configProperties.load(config);
        } catch (IOException e) {
            System.out.println(LocalDateTime.now()+" 资源文件读取失败");
            System.exit(0);

        }
        CoreService coreService = new CoreService();
        coreService.getSkuTile(configProperties);
        coreService.loginByQrcode(configProperties);
        coreService.getQrCode(configProperties);
        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        coreService.getQrcodeTicket(configProperties);
        coreService.validateQrcodeTicket(configProperties);
        String userName = coreService.getUserName(configProperties);
//        String seckillUrl = coreService.getSeckillUrl(configProperties);
//        if (seckillUrl == null) {
//            System.out.println(LocalDateTime.now()+" 秒杀链接获取失败");
//            System.exit(0);
//        }
//        coreService.requestSeckillUrl(configProperties, seckillUrl,userName);
        String seckillInitInfo = coreService.getSeckillInitInfo(configProperties);
        Map<String, Object> seckillOrderData = coreService.getSeckillOrderData(configProperties, seckillInitInfo);
        Boolean flag = coreService.submitSeckillOrder(configProperties, seckillOrderData);
        ExecutorService executorService = Executors.newFixedThreadPool(1000);
        while (!flag) {
            coreService.requestSeckillCheckoutPage(configProperties);
            executorService.submit(() -> coreService.submitSeckillOrder(configProperties, seckillOrderData));
            try {
                Random random = new Random();
                int i = random.nextInt(200) + 100;
                Thread.sleep(i);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}