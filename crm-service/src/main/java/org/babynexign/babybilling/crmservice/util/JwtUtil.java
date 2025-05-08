package org.babynexign.babybilling.crmservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token.replace("Bearer ", ""))
                .getBody();
    }

    private boolean isTokenExpired(String token) {
        return this.getAllClaimsFromToken(token).getExpiration().before(new Date());
    }

    public boolean isInvalid(String token) {
        try {
            return this.isTokenExpired(token);
        } catch (Exception e) {
            return true;
        }
    }

    public List<String> getRolesFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return (List<String>) claims.get("roles");
    }

    public String getMsisdnFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return (String) claims.get("msisdn");
    }

    public boolean hasAdminRole(String token) {
        List<String> roles = getRolesFromToken(token);
        return roles != null && roles.contains("ROLE_ADMIN");
    }
}
