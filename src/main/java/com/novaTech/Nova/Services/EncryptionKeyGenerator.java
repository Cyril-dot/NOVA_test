package com.novaTech.Nova.Services;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class EncryptionKeyGenerator {
    
    public static void main(String[] args) throws Exception {
        // Generate AES-256 key
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKey secretKey = keyGenerator.generateKey();
        
        // Convert to hex string
        byte[] keyBytes = secretKey.getEncoded();
        StringBuilder hexString = new StringBuilder();
        for (byte b : keyBytes) {
            hexString.append(String.format("%02x", b));
        }
        
        System.out.println("=".repeat(80));
        System.out.println("🔑 AES-256 ENCRYPTION KEY GENERATED");
        System.out.println("=".repeat(80));
        System.out.println(hexString.toString());
        System.out.println("=".repeat(80));
        System.out.println("⚠️  CRITICAL INSTRUCTIONS:");
        System.out.println("   1. Copy the key above");
        System.out.println("   2. Add to .env file: TOKEN_ENCRYPTION_KEY=<your_key>");
        System.out.println("   3. NEVER commit this key to Git");
        System.out.println("   4. Use different keys for dev/staging/production");
        System.out.println("   5. Keep this key SEPARATE from JWT_SECRET");
        System.out.println("=".repeat(80));
    }
}