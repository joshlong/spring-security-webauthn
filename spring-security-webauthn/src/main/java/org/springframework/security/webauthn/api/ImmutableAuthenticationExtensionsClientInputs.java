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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An immutable implementation of {@link AuthenticationExtensionsClientInputs}.
 *
 * @since 6.4
 * @author Rob Winch
 */
public class ImmutableAuthenticationExtensionsClientInputs implements AuthenticationExtensionsClientInputs {

	private final List<AuthenticationExtensionsClientInput> inputs;


	public ImmutableAuthenticationExtensionsClientInputs(List<AuthenticationExtensionsClientInput> inputs) {
		this.inputs = inputs;
	}

	public ImmutableAuthenticationExtensionsClientInputs(AuthenticationExtensionsClientInput... inputs) {
		this(Arrays.asList(inputs));
	}

	@Override
	public List<AuthenticationExtensionsClientInput> getInputs() {
		return this.inputs;
	}

}
