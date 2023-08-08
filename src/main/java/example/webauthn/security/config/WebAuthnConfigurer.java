package example.webauthn.security.config;

import org.springframework.security.web.webauthn.WebAuthnRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.webauthn.DefaultWebAuthnLoginPageGeneratingFilter;
import org.springframework.security.web.webauthn.DefaultWebAuthnRegistrationGeneratingFilter;
import org.springframework.security.web.webauthn.MultiFactorExceptionTranslationFilter;
import org.springframework.security.web.webauthn.WebAuthnLoginFilter;
import org.springframework.security.web.webauthn.WebAuthnManager;
import org.springframework.security.web.webauthn.WebAuthnRegistrationFilter;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Rob Winch
 */
public class WebAuthnConfigurer extends
		AbstractHttpConfigurer<WebAuthnConfigurer, HttpSecurity> {

	public static WebAuthnConfigurer webAuthn() {
		return new WebAuthnConfigurer();
	}

	@Override
	public void configure(HttpSecurity http) throws Exception {
		WebAuthnRepository authenticators = authnAuthenticatorRepository(http);
		Function<HttpServletRequest, Map<String, String>> csrf = request -> {
			CsrfToken token = (CsrfToken)request.getAttribute(CsrfToken.class.getName());
			return token == null ? Collections.emptyMap() : Collections.singletonMap(token.getParameterName(), token.getToken());
		};
		WebAuthnManager manager = new WebAuthnManager(authenticators);
		DefaultWebAuthnRegistrationGeneratingFilter registration = new DefaultWebAuthnRegistrationGeneratingFilter(
				manager);
		registration.setResolveHiddenInputs(csrf);
		DefaultWebAuthnLoginPageGeneratingFilter login = new DefaultWebAuthnLoginPageGeneratingFilter(
				manager);
		login.setResolveHiddenInputs(csrf);
		http
				.addFilterBefore(registration,
						DefaultLoginPageGeneratingFilter.class)
				.addFilterBefore(login,
						DefaultLoginPageGeneratingFilter.class)
				.addFilterAfter(new MultiFactorExceptionTranslationFilter(), ExceptionTranslationFilter.class)
				.addFilterBefore(new WebAuthnRegistrationFilter(manager), UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(new WebAuthnLoginFilter(manager),
						UsernamePasswordAuthenticationFilter.class);
	}

	WebAuthnRepository authnAuthenticatorRepository(HttpSecurity http) {
		WebAuthnRepository bean = getBeanOrNull(WebAuthnRepository.class);
		if (bean != null) {
			return bean;
		}
		return new WebAuthnRepository();
	}


	private <T> T getBeanOrNull(Class<T> type) {
		ApplicationContext context = getBuilder().getSharedObject(ApplicationContext.class);
		if (context == null) {
			return null;
		}
		String[] names =  context.getBeanNamesForType(type);
		if (names.length == 1) {
			return (T) context.getBean(names[0]);
		}
		return null;
	}
}
