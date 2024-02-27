/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.config.annotation.web.configurers;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.webauthn.authentication.PublicKeyCredentialRequestOptionsFilter;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationFilter;
import org.springframework.security.web.webauthn.registration.DefaultWebAuthnRegistrationPageGeneratingFilter;
import org.springframework.security.web.webauthn.registration.PublicKeyCredentialCreationOptionsFilter;
import org.springframework.security.web.webauthn.registration.WebAuthnRegistrationFilter;
import org.springframework.security.webauthn.api.registration.PublicKeyCredentialRpEntity;
import org.springframework.security.webauthn.authentication.WebAuthnAuthenticationProvider;
import org.springframework.security.webauthn.management.*;

import java.util.Map;
import java.util.Set;

public class PasskeysConfigurer<B extends HttpSecurityBuilder<B>>
		extends AbstractHttpConfigurer<PasskeysConfigurer<B>, B> {

	@Override
	public void init(B builder) throws Exception {
		super.init(builder);
	}

	@Override
	public void configure(B http) throws Exception {
		UserDetailsService userDetailsService = getSharedOrBean(http, UserDetailsService.class);
		PublicKeyCredentialUserEntityRepository userEntities = userEntityRepository();
		MapUserCredentialRepository userCredentials = userCredentialRepository();
		YubicoWebAuthnRelyingPartyOperations rpOperations = webAuthnRelyingPartyOperations(userEntities, userCredentials);
		WebAuthnAuthenticationFilter webAuthnAuthnFilter = new WebAuthnAuthenticationFilter();
		webAuthnAuthnFilter.setAuthenticationManager(new ProviderManager(new WebAuthnAuthenticationProvider(rpOperations, userDetailsService)));
		http.addFilterBefore(webAuthnAuthnFilter, BasicAuthenticationFilter.class);
		http.addFilterAfter(new WebAuthnRegistrationFilter(userCredentials, rpOperations), AuthorizationFilter.class);
		http.addFilterAfter(new PublicKeyCredentialCreationOptionsFilter(rpOperations), AuthorizationFilter.class);
		http.addFilterAfter(new DefaultWebAuthnRegistrationPageGeneratingFilter(userEntities, userCredentials), AuthorizationFilter.class);
		http.addFilterBefore(new PublicKeyCredentialRequestOptionsFilter(rpOperations), AuthorizationFilter.class);
		DefaultLoginPageGeneratingFilter loginPageGeneratingFilter = http
				.getSharedObject(DefaultLoginPageGeneratingFilter.class);
		if (loginPageGeneratingFilter != null) {
			loginPageGeneratingFilter.setPasskeysEnabled(true);
			loginPageGeneratingFilter.setResolveHeaders((request) -> {
				CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
				return Map.of( csrfToken.getHeaderName(), csrfToken.getToken());
			});
		}
	}

	private <C> C getSharedOrBean(B http, Class<C> type) {
		C shared = http.getSharedObject(type);
		if (shared != null) {
			return shared;
		}
		return getBeanOrNull(type);
	}

	private <T> T getBeanOrNull(Class<T> type) {
		ApplicationContext context = getBuilder().getSharedObject(ApplicationContext.class);
		if (context == null) {
			return null;
		}
		try {
			return context.getBean(type);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return null;
		}
	}


	private MapUserCredentialRepository userCredentialRepository() {
		return new MapUserCredentialRepository();
	}

	private PublicKeyCredentialUserEntityRepository userEntityRepository() {
		return new MapPublicKeyCredentialUserEntityRepository();
	}

	private YubicoWebAuthnRelyingPartyOperations webAuthnRelyingPartyOperations(PublicKeyCredentialUserEntityRepository userEntities, UserCredentialRepository userCredentials) {
		YubicoWebAuthnRelyingPartyOperations result =  new YubicoWebAuthnRelyingPartyOperations(userEntities, userCredentials, PublicKeyCredentialRpEntity.builder()
				.id("example.localhost")
				.name("Spring Security Relying Party")
				.build(),
				Set.of("https://example.localhost:8443"));
		return result;
	}
}
