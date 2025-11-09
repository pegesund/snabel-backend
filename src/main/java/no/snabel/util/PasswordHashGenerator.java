package no.snabel.util;

import io.quarkus.elytron.security.common.BcryptUtil;

/**
 * Utility to generate BCrypt password hashes
 * Run with: ./mvnw compile exec:java -Dexec.mainClass="no.snabel.util.PasswordHashGenerator" -Dexec.args="snabeltann"
 */
public class PasswordHashGenerator {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: PasswordHashGenerator <password>");
            System.exit(1);
        }

        String password = args[0];
        String hash = BcryptUtil.bcryptHash(password);
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
    }
}
