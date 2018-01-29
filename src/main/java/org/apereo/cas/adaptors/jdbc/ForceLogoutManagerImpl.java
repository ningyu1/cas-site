package org.apereo.cas.adaptors.jdbc;

import java.util.Collection;

import org.apereo.cas.ticket.ServiceTicketImpl;
import org.apereo.cas.ticket.Ticket;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.TicketGrantingTicketImpl;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.util.ApplicationContextProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

@EnableTransactionManagement(proxyTargetClass = true)
@Transactional(transactionManager = "ticketTransactionManager", readOnly = false)
@Component("forceLogoutManager")
@EnableConfigurationProperties(ForceLogoutProerties.class)
public class ForceLogoutManagerImpl implements ForceLogoutManager {
	
	private final Logger log = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
    private ForceLogoutProerties forceLogoutProerties;

	/**
	 * 登录成功，踢掉前一个相同登录的人
	 * 
	 * @param username
	 */
	public void doLogout(final String username) {
		if (!forceLogoutProerties.isForceLogout()) {
			return;
		}
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
			}
		}
	}
}
