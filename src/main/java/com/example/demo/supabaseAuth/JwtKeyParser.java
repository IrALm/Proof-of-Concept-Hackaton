package com.example.demo.supabaseAuth;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

public final class JwtKeyParser {

    private JwtKeyParser() {
    }

    public static PublicKey parseRsaPublicKey(JsonNode jwk) {
        try {
            String n = jwk.get("n").asText();
            String e = jwk.get("e").asText();

            byte[] modulusBytes = Base64.getUrlDecoder().decode(n);
            byte[] exponentBytes = Base64.getUrlDecoder().decode(e);

            BigInteger modulus = new BigInteger(1, modulusBytes);
            BigInteger exponent = new BigInteger(1, exponentBytes);

            RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot build RSA public key from JWKS", ex);
        }
    }
}

