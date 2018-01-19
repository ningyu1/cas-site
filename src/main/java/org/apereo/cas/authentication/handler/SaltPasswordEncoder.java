/**
 * Copyright (c) 2016, Jiuye SCM and/or its affiliates. All rights reserved.
 *
 */
package org.apereo.cas.authentication.handler;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * @author jiuye
 *
 */
@Component
public class SaltPasswordEncoder implements PasswordEncoder {

    private String salt;

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    /**
     * Add salt to password
     * 
     * @param password
     * @return
     */
    private String mergePasswordAndSalt(CharSequence password) {
        if (password == null) {
            password = "";
        }

        if ((salt == null) || "".equals(salt)) {
            return password.toString();
        } else {
            return password + "{" + salt.toString() + "}";
        }
    }

    public String encode(final CharSequence password) {
        if (password == null) {
            return null;
        }

        try {
            final byte[] pswBytes = mergePasswordAndSalt(password).getBytes("utf-8");
            final String encoded = Hex.encodeHexString(DigestUtils.getDigest("SHA-1").digest(pswBytes));
            return encoded;
        } catch (final Exception e) {
        }

        return null;
    }

    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        final String encodedRawPassword = StringUtils.isNotBlank(rawPassword) ? encode(rawPassword.toString()) : null;
        final boolean matched = StringUtils.equals(encodedRawPassword, encodedPassword);
        return matched;
    }

}
