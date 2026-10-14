
package com.zaalima.iam_server.service;

import org.springframework.stereotype.Service;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

@Service
public class TotpService {

    private final SecretGenerator secretGenerator;
    private final CodeVerifier codeVerifier;

    public TotpService() {

        secretGenerator =
                new DefaultSecretGenerator();

        CodeGenerator codeGenerator =
                new DefaultCodeGenerator(
                        HashingAlgorithm.SHA1,
                        6
                );

        TimeProvider timeProvider =
                new SystemTimeProvider();

        DefaultCodeVerifier verifier =
                new DefaultCodeVerifier(
                        codeGenerator,
                        timeProvider
                );

        // TOTP codes change every 30 seconds
        verifier.setTimePeriod(30);

        // Allow one time period of clock difference
        verifier.setAllowedTimePeriodDiscrepancy(1);

        codeVerifier = verifier;
    }

    public String generateSecret() {

        return secretGenerator.generate();
    }

    public boolean verifyCode(
            String secret,
            String code) {

        if (secret == null || secret.isBlank()) {
            return false;
        }

        if (code == null || code.isBlank()) {
            return false;
        }

        String cleanCode =
                code.replaceAll("\\s+", "");

        return codeVerifier.isValidCode(
                secret,
                cleanCode
        );
    }
}
