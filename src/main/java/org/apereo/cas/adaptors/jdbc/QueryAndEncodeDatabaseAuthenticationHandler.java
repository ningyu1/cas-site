package org.apereo.cas.adaptors.jdbc;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.AccountNotFoundException;
import javax.security.auth.login.FailedLoginException;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.crypto.hash.ConfigurableHashService;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.HashRequest;
import org.apache.shiro.util.ByteSource;
import org.apereo.cas.authentication.Credential;
import org.apereo.cas.authentication.HandlerResult;
import org.apereo.cas.authentication.PreventedException;
import org.apereo.cas.authentication.UsernamePasswordCredential;
import org.apereo.cas.authentication.handler.SaltPasswordEncoder;
import org.apereo.cas.ticket.ServiceTicketImpl;
import org.apereo.cas.ticket.Ticket;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.TicketGrantingTicketImpl;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.util.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

/**
 * A JDBC querying handler that will pull back the password and the private salt
 * value for a user and validate the encoded password using the public salt
 * value. Assumes everything is inside the same database table. Supports
 * settings for number of iterations as well as private salt.
 *
 * <p>
 * This handler uses the hashing method defined by Apache Shiro's
 * {@link org.apache.shiro.crypto.hash.DefaultHashService}. Refer to the
 * Javadocs to learn more about the behavior. If the hashing behavior and/or
 * configuration of private and public salts does nto meet your needs, a
 * extension can be developed to specify alternative methods of encoding and
 * digestion of the encoded password.
 * </p>
 * 
 * @author Misagh Moayyed
 * @author Charles Hasegawa
 * @since 4.1.0
 */
public class QueryAndEncodeDatabaseAuthenticationHandler extends AbstractJdbcUsernamePasswordAuthenticationHandler {
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	private List<String> userCache = new ArrayList<String>();

    /**
     * The Algorithm name.
     */
    protected String algorithmName;

    /**
     * The Sql statement to execute.
     */
    protected String sql;

    /**
     * The Password field name.
     */
    protected String passwordFieldName = "password";

    /**
     * The Salt field name.
     */
    protected String saltFieldName = "salt";

    /**
     * The Number of iterations field name.
     */
    protected String numberOfIterationsFieldName;

    /**
     * The number of iterations. Defaults to 0.
     */
    protected long numberOfIterations;

    /**
     * The static/private salt.
     */
    protected String staticSalt;

    @Override
    protected HandlerResult doAuthentication(final Credential credential) throws GeneralSecurityException,
            PreventedException {
        final UsernamePasswordCredential originalUserPass = (UsernamePasswordCredential) credential;

        if (StringUtils.isBlank(originalUserPass.getUsername())) {
            throw new AccountNotFoundException("Username is null.");
        }

        if (StringUtils.isBlank(originalUserPass.getPassword())) {
            throw new FailedLoginException("Password is null.");
        }

        return authenticateUsernamePasswordInternal(originalUserPass, originalUserPass.getPassword());
    }

    @Override
    protected HandlerResult authenticateUsernamePasswordInternal(final UsernamePasswordCredential transformedCredential)
            throws GeneralSecurityException, PreventedException {

        if (StringUtils.isBlank(this.sql) || StringUtils.isBlank(this.algorithmName) || getJdbcTemplate() == null) {
            throw new GeneralSecurityException("Authentication handler is not configured correctly");
        }

        final String username = transformedCredential.getUsername();
        try {
            // Get password and salt
            final Map<String, Object> rows = getJdbcTemplate().queryForMap(this.sql, username);
            final String encodedPassword = rows.get("password").toString();
            final String dbSalt = rows.get("salt").toString();
            SaltPasswordEncoder passwordEncoder = new SaltPasswordEncoder();
            passwordEncoder.setSalt(dbSalt);
            if (!passwordEncoder.matches(transformedCredential.getPassword(), encodedPassword)) {
                throw new FailedLoginException("Password does not match value on record.");
            }
			// 登录成功，踢掉前一个相同登录的人
//			isValidateAlreadyLogged(username);
//			userCache.add(username);
            return createHandlerResult(transformedCredential, this.principalFactory.createPrincipal(username), null);

        } catch (final IncorrectResultSizeDataAccessException e) {
            if (e.getActualSize() == 0) {
                throw new AccountNotFoundException(username + " not found with SQL query");
            } else {
                throw new FailedLoginException("Multiple records found for " + username);
            }
        } catch (final DataAccessException e) {
            throw new PreventedException("SQL exception while executing query for " + username, e);
        }

    }

	/**
	 * 登录成功，踢掉前一个相同登录的人
	 * 
	 * @param username
	 */
	private void isValidateAlreadyLogged(final String username) {
		if (userCache.contains(username)) {
			TicketRegistry ticketRegistry = (TicketRegistry) ApplicationContextProvider.getApplicationContext()
					.getBean("ticketRegistry");
			final Collection<Ticket> ticketsInCache = ticketRegistry.getTickets();
			for (final Ticket ticket : ticketsInCache) {
				TicketGrantingTicket t = null;
				try {
					log.info("cast TicketGrantingTicketImpl");
					t = (TicketGrantingTicketImpl) ticket;
				} catch (Exception e) {
					log.error("cast TicketGrantingTicketImpl is error:", e);
					t = ((ServiceTicketImpl) ticket).getGrantingTicket();
				}
				if (t.getAuthentication().getPrincipal().getId().equals(username) && t.getId() != null) {
					/***
					 * 注销方法一 涉及到cookie的删除，但是无法获取response 该方法有待考究 未测试
					 */
					// centralAuthenticationService.destroyTicketGrantingTicket(t.getId());
					/***
					 * 注销方法二
					 */
					// t.expire();
					t.markTicketExpired();
					ticketRegistry.deleteTicket(t.getId());
					userCache.remove(username);
				}
			}
		}
	}

    /**
     * Digest encoded password.
     *
     * @param encodedPassword
     *            the encoded password
     * @param values
     *            the values retrieved from database
     * @return the digested password
     */
    protected String digestEncodedPassword(final String encodedPassword, final Map<String, Object> values) {
        final ConfigurableHashService hashService = new DefaultHashService();

        if (StringUtils.isNotBlank(this.staticSalt)) {
            hashService.setPrivateSalt(ByteSource.Util.bytes(this.staticSalt));
        }
        hashService.setHashAlgorithmName(this.algorithmName);

        Long numOfIterations = this.numberOfIterations;
        if (values.containsKey(this.numberOfIterationsFieldName)) {
            final String longAsStr = values.get(this.numberOfIterationsFieldName).toString();
            numOfIterations = Long.valueOf(longAsStr);
        }

        hashService.setHashIterations(numOfIterations.intValue());
        if (!values.containsKey(this.saltFieldName)) {
            throw new RuntimeException("Specified field name for salt does not exist in the results");
        }

        final String dynaSalt = values.get(this.saltFieldName).toString();
        final HashRequest request = new HashRequest.Builder().setSalt(dynaSalt).setSource(encodedPassword).build();
        return hashService.computeHash(request).toHex();
    }

    public void setAlgorithmName(final String algorithmName) {
        this.algorithmName = algorithmName;
    }

    public void setSql(final String sql) {
        this.sql = sql;
    }

    /**
     * Sets static/private salt to be combined with the dynamic salt retrieved
     * from the database. Optional.
     *
     * <p>
     * If using this implementation as part of a password hashing strategy, it
     * might be desirable to configure a private salt. A hash and the salt used
     * to compute it are often stored together. If an attacker is ever able to
     * access the hash (e.g. during password cracking) and it has the full salt
     * value, the attacker has all of the input necessary to try to brute-force
     * crack the hash (source + complete salt).
     * </p>
     *
     * <p>
     * However, if part of the salt is not available to the attacker (because it
     * is not stored with the hash), it is much harder to crack the hash value
     * since the attacker does not have the complete inputs necessary. The
     * privateSalt property exists to satisfy this private-and-not-shared part
     * of the salt.
     * </p>
     * <p>
     * If you configure this attribute, you can obtain this additional very
     * important safety feature.
     * </p>
     * 
     * @param staticSalt
     *            the static salt
     */
    public void setStaticSalt(final String staticSalt) {
        this.staticSalt = staticSalt;
    }

    public void setPasswordFieldName(final String passwordFieldName) {
        this.passwordFieldName = passwordFieldName;
    }

    public void setSaltFieldName(final String saltFieldName) {
        this.saltFieldName = saltFieldName;
    }

    public void setNumberOfIterationsFieldName(final String numberOfIterationsFieldName) {
        this.numberOfIterationsFieldName = numberOfIterationsFieldName;
    }

    public void setNumberOfIterations(final long numberOfIterations) {
        this.numberOfIterations = numberOfIterations;
    }

}
