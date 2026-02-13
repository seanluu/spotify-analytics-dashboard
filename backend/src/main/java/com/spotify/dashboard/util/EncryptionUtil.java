package com.spotify.dashboard.util;

import org.jasypt.util.text.BasicTextEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EncryptionUtil {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionUtil.class);
    private final BasicTextEncryptor encryptor;
    private final boolean encryptionEnabled;

    public EncryptionUtil(
            @Value("${encryption.secret-key:}") String secretKey,
            Environment environment) {
        
        boolean isProduction = "prod".equalsIgnoreCase(environment.getProperty("spring.profiles.active", ""))
            || "production".equalsIgnoreCase(environment.getProperty("spring.profiles.active", ""));
        
        if (secretKey == null || secretKey.isEmpty() || secretKey.equals("change-this-secret-key-in-production")) {
            if (isProduction) {
                logger.error("ENCRYPTION_SECRET_KEY is not set — tokens will be stored in plain text!");
            } else {
                logger.warn("Encryption disabled (no secret key set). OK for development.");
            }
            this.encryptionEnabled = false;
            this.encryptor = null;
            return;
        }
        
        this.encryptionEnabled = true;
        this.encryptor = new BasicTextEncryptor();
        this.encryptor.setPassword(secretKey);
        logger.info("Encryption enabled with secure key");
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return null;
        }
        
        if (!encryptionEnabled) {
            logger.debug("Encryption disabled - storing token in plain text (development only)");
            return plainText;
        }
        
        return encryptor.encrypt(plainText);
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            return null;
        }
        
        if (!encryptionEnabled) {
            logger.debug("Encryption disabled - returning stored value as-is (development only)");
            return encryptedText;
        }
        
        try {
            return encryptor.decrypt(encryptedText);
        } catch (Exception e) {
            logger.error("Failed to decrypt token - it may have been encrypted with a different key", e);
            throw new RuntimeException("Failed to decrypt token", e);
        }
    }
}