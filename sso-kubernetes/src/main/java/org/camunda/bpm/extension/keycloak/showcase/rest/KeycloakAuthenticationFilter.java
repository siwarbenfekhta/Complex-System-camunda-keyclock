package org.camunda.bpm.extension.keycloak.showcase.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.camunda.bpm.engine.IdentityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.util.StringUtils;

/**
 * Keycloak Authentication Filter - used for REST API Security.
 */
public class KeycloakAuthenticationFilter implements Filter {

	/** This class' logger. */
	private static final Logger LOG = LoggerFactory.getLogger(KeycloakAuthenticationFilter.class);
	
	/** Access to Camunda's IdentityService. */
	private IdentityService identityService;
	
	/** Access to the OAuth2 client service. */
	OAuth2AuthorizedClientService clientService;
	
	/**
	 * Creates a new KeycloakAuthenticationFilter.
	 * @param identityService access to Camunda's IdentityService
	 */
	public KeycloakAuthenticationFilter(IdentityService identityService, OAuth2AuthorizedClientService clientService) {
		this.identityService = identityService;
		this.clientService = clientService;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		System.out.println("see");
	    // Extract user-name-attribute of the JWT / OAuth2 token
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		String userId = null;
		if (authentication instanceof JwtAuthenticationToken) {
			System.out.println("test1-58") ;
			userId = ((JwtAuthenticationToken)authentication).getTokenAttributes().get("preferred_username").toString();
		} else if (authentication.getPrincipal() instanceof OidcUser) {
			System.out.println("test1-61") ;
			userId = ((OidcUser)authentication.getPrincipal()).getName();
		} else {
			System.out.println("yes");
			throw new ServletException("Invalid authentication request token");
		}
        if (StringUtils.isEmpty(userId)) {
        	throw new ServletException("Unable to extract user-name-attribute from token");
        }

        LOG.debug("Extracted userId from bearer token: {}", userId);
        System.out.println("see there" + userId) ;
        try {
        	identityService.setAuthentication(userId, getUserGroups(userId));
        	chain.doFilter(request, response);
        } finally {
        	identityService.clearAuthentication();
        }
	}

    /**
     * Queries the groups of a given user.
     * @param userId the user's ID
     * @return list of groups the user belongs to
     */
    private List<String> getUserGroups(String userId){
        List<String> groupIds = new ArrayList<>();
        // query groups using KeycloakIdentityProvider plugin
        identityService.createGroupQuery().groupMember(userId).list()
        	.forEach( g -> groupIds.add(g.getId()));
        return groupIds;
    }
}
