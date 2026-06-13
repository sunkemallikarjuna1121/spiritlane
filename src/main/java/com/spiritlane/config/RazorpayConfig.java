package com.spiritlane.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorpayConfig {

    private static final Logger logger =
            LoggerFactory.getLogger(RazorpayConfig.class);

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {

        logger.info("[RazorpayConfig] razorpayClient : START");

        if (keyId == null || keyId.trim().isEmpty()) {
            logger.error("[RazorpayConfig] razorpayClient : Razorpay Key ID is missing");
            throw new IllegalArgumentException("Razorpay Key ID is missing");
        }

        if (keySecret == null || keySecret.trim().isEmpty()) {
            logger.error("[RazorpayConfig] razorpayClient : Razorpay Key Secret is missing");
            throw new IllegalArgumentException("Razorpay Key Secret is missing");
        }

        try {

            RazorpayClient razorpayClient =
                    new RazorpayClient(keyId, keySecret);

            logger.info("[RazorpayConfig] razorpayClient : Razorpay Client initialized successfully");
            logger.info("[RazorpayConfig] razorpayClient : END");

            return razorpayClient;

        } catch (RazorpayException ex) {

            logger.error(
                    "[RazorpayConfig] razorpayClient : Error initializing Razorpay Client",
                    ex
            );

            throw ex;
        }
    }
}