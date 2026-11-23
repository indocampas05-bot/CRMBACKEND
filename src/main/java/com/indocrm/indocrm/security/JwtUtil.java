package com.indocrm.indocrm.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private Long expirationMs;

    private volatile Key signingKey;

    /**
     * Accepts a Base64 secret (same key as before, so existing tokens stay valid) AND
     * also a plain-text secret. Previously a plain-text jwt.secret (or one with a stray
     * space / newline) made Base64.decode() throw on every login -> HTTP 500 -> nobody
     * could log in.
     */
    private Key getSigningKey() {
        Key key = signingKey;
        if (key == null) {
            synchronized (this) {
                if (signingKey == null) {
                    signingKey = buildKey();
                }
                key = signingKey;
            }
        }
        return key;
    }

    private Key buildKey() {
        String s = secret == null ? "" : secret.trim();
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(s);
        } catch (IllegalArgumentException notBase64) {
            keyBytes = s.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            // HS256 needs >= 256 bits; stretch short secrets deterministically.
            try {
                keyBytes = MessageDigest.getInstance("SHA-256").digest(keyBytes);
            } catch (Exception e) {
                throw new IllegalStateException("Cannot build JWT signing key", e);
            }
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
