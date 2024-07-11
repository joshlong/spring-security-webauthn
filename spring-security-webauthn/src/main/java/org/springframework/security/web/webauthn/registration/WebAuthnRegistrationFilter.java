/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.security.web.webauthn.registration;

import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.webauthn.api.Base64Url;
import org.springframework.security.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.webauthn.management.RelyingPartyPublicKey;
import org.springframework.security.webauthn.management.RelyingPartyRegistrationRequest;
import org.springframework.security.webauthn.management.CredentialRecord;
import org.springframework.security.webauthn.management.UserCredentialRepository;
import org.springframework.security.webauthn.management.WebAuthnRelyingPartyOperations;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * Authenticates {@code PublicKeyCredential<AuthenticatorAssertionResponse>} that is parsed from the body of the
 * {@link HttpServletRequest} using the {@link #setConverter(HttpMessageConverter)}. An example request is
 * provided below:
 *
 * <pre>
 * {
 * 	"publicKey": {
 * 		"credential": {
 * 			"id": "dYF7EGnRFFIXkpXi9XU2wg",
 * 			"rawId": "dYF7EGnRFFIXkpXi9XU2wg",
 * 			"response": {
 * 				"attestationObject": "o2NmbXRkbm9uZWdhdHRTdG10oGhhdXRoRGF0YViUy9GqwTRaMpzVDbXq1dyEAXVOxrou08k22ggRC45MKNhdAAAAALraVWanqkAfvZZFYZpVEg0AEHWBexBp0RRSF5KV4vV1NsKlAQIDJiABIVggQjmrekPGzyqtoKK9HPUH-8Z2FLpoqkklFpFPQVICQ3IiWCD6I9Jvmor685fOZOyGXqUd87tXfvJk8rxj9OhuZvUALA",
 * 				"clientDataJSON": "eyJ0eXBlIjoid2ViYXV0aG4uY3JlYXRlIiwiY2hhbGxlbmdlIjoiSl9RTi10SFJYRWVKYjlNcUNrWmFPLUdOVmlibXpGVGVWMk43Z0ptQUdrQSIsIm9yaWdpbiI6Imh0dHBzOi8vZXhhbXBsZS5sb2NhbGhvc3Q6ODQ0MyIsImNyb3NzT3JpZ2luIjpmYWxzZX0",
 * 				"transports": [
 * 					"internal",
 * 					"hybrid"
 * 				],
 * 				"publicKeyAlgorithm": -7,
 * 				"publicKey": "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEQjmrekPGzyqtoKK9HPUH-8Z2FLpoqkklFpFPQVICQ3L6I9Jvmor685fOZOyGXqUd87tXfvJk8rxj9OhuZvUALA",
 * 				"authenticatorData": "y9GqwTRaMpzVDbXq1dyEAXVOxrou08k22ggRC45MKNhdAAAAALraVWanqkAfvZZFYZpVEg0AEHWBexBp0RRSF5KV4vV1NsKlAQIDJiABIVggQjmrekPGzyqtoKK9HPUH-8Z2FLpoqkklFpFPQVICQ3IiWCD6I9Jvmor685fOZOyGXqUd87tXfvJk8rxj9OhuZvUALA"
 * 			},
 * 			"type": "public-key",
 * 			"clientExtensionResults": {},
 * 			"authenticatorAttachment": "platform"
 * 		},
 * 		"label": "1password"
 * }
 * </pre>
 *
 * @since 6.4
 * @author Rob Winch
 */
public class WebAuthnRegistrationFilter extends OncePerRequestFilter {

	static final String DEFAULT_REGISTER_CREDENTIAL_URL = "/webauthn/register";

	private final WebAuthnRelyingPartyOperations rpOptions;

	private final UserCredentialRepository userCredentials;

	private HttpMessageConverter<Object> converter = new MappingJackson2HttpMessageConverter(JsonMapper.builder()
			.findAndAddModules().build());

	private PublicKeyCredentialCreationOptionsRepository creationOptionsRepository = new HttpSessionPublicKeyCredentialCreationOptionsRepository();

	private RequestMatcher registerCredentialMatcher = antMatcher(HttpMethod.POST, DEFAULT_REGISTER_CREDENTIAL_URL);

	private RequestMatcher removeCredentialMatcher = antMatcher(HttpMethod.POST, "/webauthn/register/{id}");

	public WebAuthnRegistrationFilter(UserCredentialRepository userCredentials, WebAuthnRelyingPartyOperations rpOptions) {
		Assert.notNull(userCredentials, "userCredentials must not be null");
		Assert.notNull(rpOptions, "rpOptions must not be null");
		this.userCredentials = userCredentials;
		this.rpOptions = rpOptions;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		if (this.registerCredentialMatcher.matches(request)) {
			registerCredential(request, response);
			return;
		}
		RequestMatcher.MatchResult removeMatchResult = this.removeCredentialMatcher.matcher(request);
		if (removeMatchResult.isMatch()) {
			String id = removeMatchResult.getVariables().get("id");
			removeCredential(request, response, id);
			return;
		}
		filterChain.doFilter(request, response);
	}

	/**
	 * Set the {@link HttpMessageConverter} to read the {@link WebAuthnRegistrationRequest} and write the response. The default
	 * is {@link MappingJackson2HttpMessageConverter}.
	 * @param converter the {@link HttpMessageConverter} to use. Cannot be null.
	 */
	public void setConverter(HttpMessageConverter<Object> converter) {
		Assert.notNull(converter, "converter cannot be null");
		this.converter = converter;
	}

	/**
	 * Sets the {@link PublicKeyCredentialCreationOptionsRepository} to use. The default is {@link HttpSessionPublicKeyCredentialCreationOptionsRepository}.
	 * @param creationOptionsRepository the {@link PublicKeyCredentialCreationOptionsRepository} to use. Cannot be null.
	 */
	public void setCreationOptionsRepository(PublicKeyCredentialCreationOptionsRepository creationOptionsRepository) {
		Assert.notNull(creationOptionsRepository, "creationOptionsRepository cannot be null");
		this.creationOptionsRepository = creationOptionsRepository;
	}

	private void registerCredential(HttpServletRequest request, HttpServletResponse response) throws IOException {
		WebAuthnRegistrationRequest registrationRequest = readRegistrationRequest(request);
		if (registrationRequest == null) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return;
		}
		PublicKeyCredentialCreationOptions options = this.creationOptionsRepository.load(request);
		if (options == null) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return;
		}
		this.creationOptionsRepository.save(request, response, null);
		CredentialRecord credentialRecord = this.rpOptions.registerCredential(new RelyingPartyRegistrationRequest(options, registrationRequest.getPublicKey()));
		SuccessfulUserRegistrationResponse registrationResponse = new SuccessfulUserRegistrationResponse(credentialRecord);
		ServletServerHttpResponse outputMessage = new ServletServerHttpResponse(response);
		this.converter.write(registrationResponse, MediaType.APPLICATION_JSON, outputMessage);
	}

	private WebAuthnRegistrationRequest readRegistrationRequest(HttpServletRequest request) {
		HttpInputMessage inputMessage = new ServletServerHttpRequest(request);
		try {
			return (WebAuthnRegistrationRequest) this.converter.read(WebAuthnRegistrationRequest.class, inputMessage);
		}
		catch (Exception e) {
			return null;
		}
	}

	private void removeCredential(HttpServletRequest request, HttpServletResponse response, String id) throws IOException {
		this.userCredentials.delete(Base64Url.fromBase64(id));
		response.sendRedirect("/webauthn/register?success");
	}

	// FIXME: make private
	public static class WebAuthnRegistrationRequest {
		private RelyingPartyPublicKey publicKey;

		public RelyingPartyPublicKey getPublicKey() {
			return this.publicKey;
		}

		public void setPublicKey(RelyingPartyPublicKey publicKey) {
			this.publicKey = publicKey;
		}
	}

	// FIXME: make private
	// FIXME: expose userCredential as a getter (need to update JSON mapping)
	public static class SuccessfulUserRegistrationResponse {
		private final CredentialRecord credentialRecord;

		public SuccessfulUserRegistrationResponse(CredentialRecord credentialRecord) {
			this.credentialRecord = credentialRecord;
		}

		public boolean isSuccess() {
			return true;
		}

	}

}
