package org.springframework.security.webauthn.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = CredentialPropertiesOutputDeserializer.class)
abstract class CredentialPropertiesOutputMixin {
}
