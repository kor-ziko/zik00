package com.zik00.shop.service.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisRefreshTokenStore {
    private static final String REFRESH_PREFIX = "shop:auth:refresh:";
    private static final String USED_REFRESH_PREFIX = "shop:auth:used-refresh:";
    private static final String FAMILY_PREFIX = "shop:auth:family:";
    private static final String FAMILY_ACCESS_PREFIX = "shop:auth:family-access:";
    private static final String FAMILY_OWNER_PREFIX = "shop:auth:family-owner:";
    private static final String FAMILY_USED_REFRESH_PREFIX = "shop:auth:family-used-refresh:";
    private static final String USER_FAMILIES_PREFIX = "shop:auth:user-families:";
    private static final String ACTIVE_ACCESS_PREFIX = "shop:auth:active-access:";
    private static final String BLOCKED_ACCESS_PREFIX = "shop:auth:blocked-access:";
    private static final String REVOKED_FAMILY_PREFIX = "shop:auth:revoked-family:";
    private static final String VALUE_SEPARATOR = "\n";

    private static final DefaultRedisScript<Long> SAVE_FAMILY_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('exists', KEYS[1]) == 1 then
                return 0
            end
            redis.call('psetex', KEYS[2], ARGV[1], ARGV[2])
            redis.call('psetex', KEYS[3], ARGV[1], ARGV[3])
            local previousAccessTokenIds = redis.call('smembers', KEYS[4])
            for _, tokenId in ipairs(previousAccessTokenIds) do
                if redis.call('exists', ARGV[6] .. tokenId) == 0 then
                    redis.call('srem', KEYS[4], tokenId)
                end
            end
            redis.call('sadd', KEYS[4], ARGV[4])
            redis.call('pexpire', KEYS[4], ARGV[1])
            redis.call('psetex', KEYS[5], ARGV[5], '1')
            redis.call('psetex', KEYS[6], ARGV[1], ARGV[7])
            redis.call('sadd', KEYS[7], ARGV[8])
            redis.call('pexpire', KEYS[7], ARGV[1])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<String> CONSUME_SCRIPT = new DefaultRedisScript<>("""
            local stored = redis.call('get', KEYS[1])
            if stored then
                redis.call('del', KEYS[1])
                redis.call('psetex', KEYS[2], ARGV[1], ARGV[2])
                redis.call('sadd', KEYS[3], ARGV[3])
                redis.call('pexpire', KEYS[3], ARGV[1])
                return 'CONSUMED\\n' .. stored
            end
            local reusedFamily = redis.call('get', KEYS[2])
            if reusedFamily then
                return 'REUSED\\n' .. reusedFamily
            end
            return 'MISSING'
            """, String.class);

    private static final DefaultRedisScript<Long> REVOKE_FAMILY_SCRIPT = new DefaultRedisScript<>("""
            redis.call('psetex', KEYS[1], ARGV[1], '1')
            local currentRefreshHash = redis.call('get', KEYS[2])
            if currentRefreshHash then
                redis.call('del', ARGV[2] .. currentRefreshHash)
            end
            local usedRefreshHashes = redis.call('smembers', KEYS[5])
            for _, refreshHash in ipairs(usedRefreshHashes) do
                redis.call('del', ARGV[7] .. refreshHash)
            end
            local accessTokenIds = redis.call('smembers', KEYS[3])
            for _, tokenId in ipairs(accessTokenIds) do
                if redis.call('exists', ARGV[3] .. tokenId) == 1 then
                    redis.call('psetex', ARGV[4] .. tokenId, ARGV[5], '1')
                    redis.call('del', ARGV[3] .. tokenId)
                end
            end
            redis.call('del', KEYS[2])
            redis.call('del', KEYS[3])
            local ownerAccessId = redis.call('get', KEYS[4])
            if ownerAccessId then
                local userFamiliesKey = ARGV[8] .. ownerAccessId
                redis.call('srem', userFamiliesKey, ARGV[6])
                if redis.call('scard', userFamiliesKey) == 0 then
                    redis.call('del', userFamiliesKey)
                end
            end
            redis.call('del', KEYS[4])
            redis.call('del', KEYS[5])
            return #accessTokenIds
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public RedisRefreshTokenStore(
            StringRedisTemplate redisTemplate,
            @Value("${shop.jwt.access-token-ttl:PT15M}") Duration accessTtl,
            @Value("${shop.jwt.refresh-token-ttl:P14D}") Duration refreshTtl
    ) {
        this.redisTemplate = redisTemplate;
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
    }

    public void save(JwtService.JwtPair pair, String refreshTokenHash, String accessId) {
        long refreshTtlMillis = ttlMillis(pair.refreshExpiresAt(), "Refresh Token");
        long accessTtlMillis = ttlMillis(pair.accessExpiresAt(), "Access Token");
        Long saved = redisTemplate.execute(
                SAVE_FAMILY_SCRIPT,
                List.of(
                        revokedFamilyKey(pair.familyId()),
                        refreshKey(refreshTokenHash),
                        familyKey(pair.familyId()),
                        familyAccessKey(pair.familyId()),
                        activeAccessKey(pair.accessTokenId()),
                        familyOwnerKey(pair.familyId()),
                        userFamiliesKey(accessId)
                ),
                Long.toString(refreshTtlMillis),
                accessId + VALUE_SEPARATOR + pair.familyId(),
                refreshTokenHash,
                pair.accessTokenId(),
                Long.toString(accessTtlMillis),
                ACTIVE_ACCESS_PREFIX,
                accessId,
                pair.familyId()
        );
        if (!Long.valueOf(1L).equals(saved)) {
            throw new InvalidJwtException("폐기된 로그인 세션에서는 토큰을 발급할 수 없습니다.");
        }
    }

    public ConsumeResult consume(String tokenHash, String familyId, Instant expiresAt) {
        String result = redisTemplate.execute(
                CONSUME_SCRIPT,
                List.of(
                        refreshKey(tokenHash),
                        usedRefreshKey(tokenHash),
                        familyUsedRefreshKey(familyId)
                ),
                Long.toString(ttlMillis(expiresAt, "Refresh Token")),
                familyId,
                tokenHash
        );
        if (result == null || "MISSING".equals(result)) {
            return ConsumeResult.missing();
        }
        if (result.startsWith("REUSED\n")) {
            return ConsumeResult.reused(result.substring("REUSED\n".length()));
        }
        if (!result.startsWith("CONSUMED\n")) {
            return ConsumeResult.missing();
        }

        String[] values = result.substring("CONSUMED\n".length()).split(VALUE_SEPARATOR, -1);
        if (values.length != 2 || values[0].isBlank() || values[1].isBlank()) {
            return ConsumeResult.missing();
        }
        return ConsumeResult.consumed(values[0], values[1]);
    }

    public void revokeFamily(String familyId) {
        if (familyId == null || familyId.isBlank()) {
            return;
        }
        redisTemplate.execute(
                REVOKE_FAMILY_SCRIPT,
                List.of(
                        revokedFamilyKey(familyId),
                        familyKey(familyId),
                        familyAccessKey(familyId),
                        familyOwnerKey(familyId),
                        familyUsedRefreshKey(familyId)
                ),
                Long.toString(refreshTtl.toMillis()),
                REFRESH_PREFIX,
                ACTIVE_ACCESS_PREFIX,
                BLOCKED_ACCESS_PREFIX,
                Long.toString(accessTtl.toMillis()),
                familyId,
                USED_REFRESH_PREFIX,
                USER_FAMILIES_PREFIX
        );
    }

    public void revokeAllForUser(String accessId) {
        if (accessId == null || accessId.isBlank()) {
            return;
        }
        var familyIds = redisTemplate.opsForSet().members(userFamiliesKey(accessId));
        if (familyIds == null || familyIds.isEmpty()) {
            return;
        }
        familyIds.forEach(this::revokeFamily);
        redisTemplate.delete(userFamiliesKey(accessId));
    }

    public boolean isAccessTokenRevoked(String tokenId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(blockedAccessKey(tokenId)));
    }

    private long ttlMillis(Instant expiresAt, String tokenType) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            throw new InvalidJwtException(tokenType + " 만료 시간이 올바르지 않습니다.");
        }
        return Math.max(1L, ttl.toMillis());
    }

    private String refreshKey(String hash) {
        return REFRESH_PREFIX + hash;
    }

    private String usedRefreshKey(String hash) {
        return USED_REFRESH_PREFIX + hash;
    }

    private String familyKey(String familyId) {
        return FAMILY_PREFIX + familyId;
    }

    private String familyAccessKey(String familyId) {
        return FAMILY_ACCESS_PREFIX + familyId;
    }

    private String familyOwnerKey(String familyId) {
        return FAMILY_OWNER_PREFIX + familyId;
    }

    private String familyUsedRefreshKey(String familyId) {
        return FAMILY_USED_REFRESH_PREFIX + familyId;
    }

    private String userFamiliesKey(String accessId) {
        return USER_FAMILIES_PREFIX + accessId;
    }

    private String activeAccessKey(String tokenId) {
        return ACTIVE_ACCESS_PREFIX + tokenId;
    }

    private String blockedAccessKey(String tokenId) {
        return BLOCKED_ACCESS_PREFIX + tokenId;
    }

    private String revokedFamilyKey(String familyId) {
        return REVOKED_FAMILY_PREFIX + familyId;
    }

    public enum ConsumeStatus {
        CONSUMED,
        REUSED,
        MISSING
    }

    public record ConsumeResult(
            ConsumeStatus status,
            String accessId,
            String familyId
    ) {
        private static ConsumeResult consumed(String accessId, String familyId) {
            return new ConsumeResult(ConsumeStatus.CONSUMED, accessId, familyId);
        }

        private static ConsumeResult reused(String familyId) {
            return new ConsumeResult(ConsumeStatus.REUSED, null, familyId);
        }

        private static ConsumeResult missing() {
            return new ConsumeResult(ConsumeStatus.MISSING, null, null);
        }
    }
}
