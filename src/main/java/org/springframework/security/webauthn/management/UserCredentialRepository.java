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

package org.springframework.security.webauthn.management;

import org.springframework.security.webauthn.api.Base64Url;
import org.springframework.security.webauthn.api.BufferSource;

import java.util.List;

public interface UserCredentialRepository {

	void delete(Base64Url credentialId);

	void save(UserCredential userCredential);

	UserCredential findByCredentialId(Base64Url credentialId);

	List<UserCredential> findByUserId(BufferSource userId);
}
