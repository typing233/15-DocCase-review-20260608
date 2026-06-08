package com.doccase.email.health;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.doccase.email.domain.entity.EmailAccount;
import com.doccase.email.mapper.EmailAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImapHealthIndicator implements HealthIndicator {

    private final EmailAccountMapper accountMapper;

    @Override
    public Health health() {
        LambdaQueryWrapper<EmailAccount> activeQuery = new LambdaQueryWrapper<>();
        activeQuery.eq(EmailAccount::getIsEnabled, true).eq(EmailAccount::getStatus, 1);
        long activeCount = accountMapper.selectCount(activeQuery);

        LambdaQueryWrapper<EmailAccount> errorQuery = new LambdaQueryWrapper<>();
        errorQuery.eq(EmailAccount::getIsEnabled, true).eq(EmailAccount::getStatus, 0);
        long errorCount = accountMapper.selectCount(errorQuery);

        if (errorCount > activeCount) {
            return Health.down()
                    .withDetail("activeAccounts", activeCount)
                    .withDetail("errorAccounts", errorCount)
                    .build();
        }

        return Health.up()
                .withDetail("activeAccounts", activeCount)
                .withDetail("errorAccounts", errorCount)
                .build();
    }
}
