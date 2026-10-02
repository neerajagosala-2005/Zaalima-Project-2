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
    private final CodeGenerator codeGenerator;
    private final TimeProvider timeProvider;

    public TotpService() {

        secretGenerator =
                new DefaultSecretGenerator();

        codeGenerator =
                new DefaultCodeGenerator(
                        HashingAlgorithm.SHA1,
                        6
                );

        timeProvider =
                new SystemTimeProvider();

        DefaultCodeVerifier verifier =
                new DefaultCodeVerifier(
                        codeGenerator,
                        timeProvider
                );

        verifier.setTimePeriod(30);

        verifier.setAllowedTimePeriodDiscrepancy(1);

        codeVerifier = verifier;

        System.out.println(
                "TOTP LIBRARY SELF TEST: "
                        + testTotpLibrary()
        );
    }

    public String generateSecret() {

        return secretGenerator.generate();
    }

    public boolean verifyCode(
            String secret,
            String code) {

        System.out.println(
                "========== TOTP DEBUG =========="
        );

        System.out.println(
                "Secret present: "
                        + (secret != null && !secret.isBlank())
        );

        System.out.println(
                "Secret length: "
                        + (secret == null
                        ? 0
                        : secret.length())
        );

        System.out.println(
                "Code length: "
                        + (code == null
                        ? 0
                        : code.length())
        );

        if (secret == null || secret.isBlank()) {
            return false;
        }

        if (code == null || code.isBlank()) {
            return false;
        }

        String cleanCode =
                code.replaceAll("\\s+", "");

        boolean valid =
                codeVerifier.isValidCode(
                        secret,
                        cleanCode
                );

        System.out.println(
                "TOTP verification result: "
                        + valid
        );

        /*
         * Temporary diagnostic:
         * Generate the code that this server expects
         * for the CURRENT 30-second time window.
         *
         * We do NOT print the actual OTP.
         */
        try {

            long counter =
                    timeProvider.getTime() / 30;

            String serverCode =
                    codeGenerator.generate(
                            secret,
                            counter
                    );

            boolean exactMatch =
                    serverCode.equals(cleanCode);

            System.out.println(
                    "Server/Authenicator code match: "
                            + exactMatch
            );

        } catch (Exception e) {

            System.out.println(
                    "Server TOTP diagnostic error: "
                            + e.getMessage()
            );
        }

        System.out.println(
                "================================"
        );

        return valid;
    }

    // Temporary TOTP library self-test
    public boolean testTotpLibrary() {

        String testSecret =
                secretGenerator.generate();

        try {

            long counter =
                    timeProvider.getTime() / 30;

            String testCode =
                    codeGenerator.generate(
                            testSecret,
                            counter
                    );

            boolean result =
                    codeVerifier.isValidCode(
                            testSecret,
                            testCode
                    );

            System.out.println(
                    "========== TOTP LIBRARY TEST =========="
            );

            System.out.println(
                    "Library self-test result: "
                            + result
            );

            System.out.println(
                    "======================================="
            );

            return result;

        } catch (Exception e) {

            System.out.println(
                    "TOTP library test error: "
                            + e.getMessage()
            );

            return false;
        }
    }
}