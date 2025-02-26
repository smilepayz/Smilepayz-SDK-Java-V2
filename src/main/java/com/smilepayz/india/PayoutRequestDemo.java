package com.smilepayz.india;

import com.google.gson.Gson;
import com.smilepayz.india.bean.MerchantReq;
import com.smilepayz.india.bean.MoneyReq;
import com.smilepayz.india.bean.TradePayoutReq;
import com.smilepayz.india.common.AreaEnum;
import com.smilepayz.india.common.Constant;
import com.smilepayz.india.common.SignatureUtils;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * @Author Moore
 * @Date 2024/6/27 14:21
 **/
public class PayoutRequestDemo {
    @SneakyThrows
    public static void main(String[] args) {
        String env = "";
        String merchantId = "";
        String merchantSecret = "";
        String privateStr = "";
        String paymentMethod = "YES";
        String cashAccount = "";
        String ifscCode = "YES00012202121";
        BigDecimal amount = BigDecimal.valueOf(10000);

        doDisbursement(env,
                merchantId,
                merchantSecret,
                privateStr,
                paymentMethod,
                cashAccount,
                ifscCode,
                amount);

    }

    public static void doDisbursement(String env,
                                      String merchantId,
                                      String merchantSecret,
                                      String privateStr,
                                      String paymentMethod,
                                      String cashAccount,
                                      String ifscCode,
                                      BigDecimal amount) throws Exception {
        //url
        String endPointUlr = "/v2.0/disbursement/pay-out";

        //default sandbox
        String requestPath = Constant.baseUrlSanbox + endPointUlr;
        //production
        if (StringUtils.equals(env, "production")) {
            requestPath = Constant.baseUrl + endPointUlr;
        }


        String timestamp = ZonedDateTime.of(LocalDateTime.now(), ZoneId.of("UTC"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX"));
        System.out.println("timestamp = " + timestamp);

        AreaEnum areaEnum = AreaEnum.INDIA;

        //generate parameter
        String merchantOrderNo = (merchantId + UUID.randomUUID()).replaceAll("-", "")
                .substring(0, 32);
        String purpose = "Purpose For Disbursement from Java SDK";

        //moneyReq
        MoneyReq moneyReq = new MoneyReq();
        moneyReq.setCurrency(areaEnum.getCurrency().name());
        moneyReq.setAmount(amount);

        //merchantReq
        MerchantReq merchantReq = new MerchantReq();
        merchantReq.setMerchantId(merchantId);

        //payoutReq
        TradePayoutReq payoutReq = new TradePayoutReq();
        payoutReq.setOrderNo(merchantOrderNo);
        payoutReq.setPurpose(purpose);
        payoutReq.setMoney(moneyReq);
        payoutReq.setMerchant(merchantReq);
        payoutReq.setCallbackUrl("your.notify.url");
        payoutReq.setPaymentMethod(paymentMethod);
        payoutReq.setCashAccount(cashAccount);
        payoutReq.setIfscCode(ifscCode);
        payoutReq.setArea(AreaEnum.INDIA.getCode());

        //jsonStr by gson
        Gson gson = new Gson();
        String jsonStr = gson.toJson(payoutReq);
        System.out.println("jsonStr = " + jsonStr);

        //minify
        String minify = SignatureUtils.minify(jsonStr);
        System.out.println("minify = " + minify);

        //signature
        String content = String.join("|", timestamp, merchantSecret, minify);
        String signature = SignatureUtils.sha256RsaSignature(content, privateStr);

        // create httpClient
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(requestPath);
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.addHeader("X-TIMESTAMP", timestamp);
        httpPost.addHeader("X-SIGNATURE", signature);
        httpPost.addHeader("X-PARTNER-ID", merchantId);

        // set entity
        httpPost.setEntity(new StringEntity(jsonStr, StandardCharsets.UTF_8));
        System.out.println(requestPath);
        // send
        HttpResponse response = httpClient.execute(httpPost);

        // response
        HttpEntity httpEntity = response.getEntity();
        String responseContent = EntityUtils.toString(httpEntity, "UTF-8");
        System.out.println("responseContent = " + responseContent);

        // release
        EntityUtils.consume(httpEntity);

        System.out.println("======> request end ,request success");
    }
}
