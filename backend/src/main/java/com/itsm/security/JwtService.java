package com.itsm.security;

import com.itsm.config.ItsmProperties;
import com.itsm.domain.user.Role;
import com.itsm.domain.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/** Issues and validates stateless HMAC-SHA256 signed JWTs. */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;
    private final Clock clock;

    public JwtService(ItsmProperties props, Clock clock) {
        byte[] bytes = props.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) throw new IllegalStateException("itsm.jwt.secret must be at least 32 bytes");
        this.key = Keys.hmacShaKeyFor(bytes);
        this.ttl = Duration.ofMinutes(props.getJwt().getExpirationMinutes());
        this.clock = clock;
    }

    public String issue(User user) {
        Instant now = Instant.now(clock);
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("name", user.getFullName())
                .claim("code", user.getCode())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public Optional<AuthenticatedUser> parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(new AuthenticatedUser(Long.valueOf(c.getSubject()), c.get("email", String.class),
                    Role.valueOf(c.get("role", String.class)), c.get("name", String.class), c.get("code", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Duration ttl() { return ttl; }
}
