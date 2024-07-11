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

package org.springframework.security.webauthn.api;

import java.util.Arrays;
import java.util.List;

/**
 * <a href="https://www.w3.org/TR/webauthn-3/#dictdef-publickeycredentialdescriptor">PublicKeyCredentialDescriptor</a>
 * identifies a specific public key credential. It is used in create() to prevent creating duplicate credentials on the
 * same authenticator, and in get() to determine if and how the credential can currently be reached by the client. It
 * mirrors some fields of the PublicKeyCredential object returned by create() and get().
 *
 * @since 6.4
 * @author Rob Winch
 */
public class PublicKeyCredentialDescriptor {

	private final PublicKeyCredentialType type;

	private final Base64Url id;

	private final List<AuthenticatorTransport> transports;

	private PublicKeyCredentialDescriptor(PublicKeyCredentialType type, Base64Url id, List<AuthenticatorTransport> transports) {
		this.type = type;
		this.id = id;
		this.transports = transports;
	}

	/**
	 * The <a href="https://www.w3.org/TR/webauthn-3/#dom-publickeycredentialdescriptor-type">type</a> property contains
	 * the type of the public key credential the caller is referring to.
	 * @return the type
	 */
	public PublicKeyCredentialType getType() {
		return this.type;
	}

	/**
	 * The <a href="https://www.w3.org/TR/webauthn-3/#dom-publickeycredentialdescriptor-id">id</a> property contains the
	 * credential ID of the public key credential the caller is referring to.
	 * @return the id
	 */
	public Base64Url getId() {
		return this.id;
	}

	/**
	 * The <a href="https://www.w3.org/TR/webauthn-3/#dom-publickeycredentialdescriptor-transports">transports</a>
	 * property is an OPTIONAL member that contains a hint as to how the client might communicate with the managing
	 * authenticator of the public key credential the caller is referring to.
	 * @return the transports
	 */
	public List<AuthenticatorTransport> getTransports() {
		return this.transports;
	}

	/**
	 * Creates a new {@link PublicKeyCredentialDescriptorBuilder}
	 * @return a new {@link PublicKeyCredentialDescriptorBuilder}
	 */
	public static PublicKeyCredentialDescriptorBuilder builder() {
		return new PublicKeyCredentialDescriptorBuilder();
	}

	/**
	 * Used to create {@link PublicKeyCredentialDescriptor}
	 * @since 6.4
	 * @author Rob Winch
	 */
	public static final class PublicKeyCredentialDescriptorBuilder {
		private PublicKeyCredentialType type = PublicKeyCredentialType.PUBLIC_KEY;
		private Base64Url id;
		private List<AuthenticatorTransport> transports;

		private PublicKeyCredentialDescriptorBuilder() {
		}

		/**
		 * Sets the {@link #getType()} property.
		 * @param type the type
		 * @return the {@link PublicKeyCredentialDescriptorBuilder}
		 */
		public PublicKeyCredentialDescriptorBuilder type(PublicKeyCredentialType type) {
			this.type = type;
			return this;
		}

		/**
		 * Sets the {@link #getId()} property.
		 * @param id the id
		 * @return the {@link PublicKeyCredentialDescriptorBuilder}
		 */
		public PublicKeyCredentialDescriptorBuilder id(Base64Url id) {
			this.id = id;
			return this;
		}

		/**
		 * Sets the {@link #getTransports()} property.
		 * @param transports the transports
		 * @return the {@link PublicKeyCredentialDescriptorBuilder}
		 */
		public PublicKeyCredentialDescriptorBuilder transports(List<AuthenticatorTransport> transports) {
			this.transports = transports;
			return this;
		}

		/**
		 * Sets the {@link #getTransports()} property.
		 * @param transports the transports
		 * @return the {@link PublicKeyCredentialDescriptorBuilder}
		 */
		public PublicKeyCredentialDescriptorBuilder transports(AuthenticatorTransport... transports) {
			return transports(Arrays.asList(transports));
		}

		/**
		 * Create a new {@link PublicKeyCredentialDescriptor}
		 * @return a new {@link PublicKeyCredentialDescriptor}
		 */
		public PublicKeyCredentialDescriptor build() {
			return new PublicKeyCredentialDescriptor(this.type, this.id, this.transports);
		}

	}

}
