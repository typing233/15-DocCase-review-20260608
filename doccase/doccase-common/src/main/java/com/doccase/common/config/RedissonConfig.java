package com.doccase.common.config;

import com.doccase.common.util.DistributedLockUtil;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Bean
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + host + ":" + port;
        var serverConfig = config.useSingleServer().setAddress(address);
        if (password != null && !password.isEmpty()) {
            serverConfig.setPassword(password);
        }
        return Redisson.create(config);
    }

    @Bean
    @ConditionalOnMissingBean(DistributedLockUtil.class)
    public DistributedLockUtil distributedLockUtil(RedissonClient redissonClient) {
        return new DistributedLockUtil(redissonClient);
    }
}
