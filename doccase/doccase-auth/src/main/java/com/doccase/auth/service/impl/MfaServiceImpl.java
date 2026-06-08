package com.doccase.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.doccase.auth.domain.entity.MfaSecret;
import com.doccase.auth.mapper.MfaSecretMapper;
import com.doccase.auth.service.MfaService;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MfaServiceImpl implements MfaService {

    private final MfaSecretMapper mfaSecretMapper;
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();

    @Override
    public String generateSecret(Long userId) {
        String secret = secretGenerator.generate();
        MfaSecret mfaSecret = mfaSecretMapper.selectOne(
                new LambdaQueryWrapper<MfaSecret>().eq(MfaSecret::getUserId, userId));
        if (mfaSecret == null) {
            mfaSecret = new MfaSecret();
            mfaSecret.setUserId(userId);
            mfaSecret.setSecretKey(secret);
            mfaSecret.setIsEnabled(0);
            mfaSecretMapper.insert(mfaSecret);
        } else {
            mfaSecret.setSecretKey(secret);
            mfaSecretMapper.updateById(mfaSecret);
        }
        return secret;
    }

    @Override
    public String getQrCodeUri(Long userId, String username) {
        MfaSecret mfaSecret = mfaSecretMapper.selectOne(
                new LambdaQueryWrapper<MfaSecret>().eq(MfaSecret::getUserId, userId));
        if (mfaSecret == null) return null;

        QrData data = new QrData.Builder()
                .label(username)
                .secret(mfaSecret.getSecretKey())
                .issuer("DocCase")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        return data.getUri();
    }

    @Override
    public boolean verifyCode(Long userId, String code) {
        MfaSecret mfaSecret = mfaSecretMapper.selectOne(
                new LambdaQueryWrapper<MfaSecret>().eq(MfaSecret::getUserId, userId));
        if (mfaSecret == null) return false;

        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator();
        CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        return verifier.isValidCode(mfaSecret.getSecretKey(), code);
    }

    @Override
    public void enableMfa(Long userId) {
        MfaSecret mfaSecret = mfaSecretMapper.selectOne(
                new LambdaQueryWrapper<MfaSecret>().eq(MfaSecret::getUserId, userId));
        if (mfaSecret != null) {
            mfaSecret.setIsEnabled(1);
            mfaSecretMapper.updateById(mfaSecret);
        }
    }

    @Override
    public void disableMfa(Long userId) {
        MfaSecret mfaSecret = mfaSecretMapper.selectOne(
                new LambdaQueryWrapper<MfaSecret>().eq(MfaSecret::getUserId, userId));
        if (mfaSecret != null) {
            mfaSecret.setIsEnabled(0);
            mfaSecretMapper.updateById(mfaSecret);
        }
    }

    @Override
    public boolean isMfaEnabled(Long userId) {
        MfaSecret mfaSecret = mfaSecretMapper.selectOne(
                new LambdaQueryWrapper<MfaSecret>().eq(MfaSecret::getUserId, userId));
        return mfaSecret != null && mfaSecret.getIsEnabled() == 1;
    }
}
