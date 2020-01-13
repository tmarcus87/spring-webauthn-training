package net.marcussoft.training.webauthn.webauthn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.data.AttestationConveyancePreference;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.AuthenticatorAttachment;
import com.webauthn4j.data.AuthenticatorSelectionCriteria;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import com.webauthn4j.data.PublicKeyCredentialDescriptor;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;
import com.webauthn4j.data.PublicKeyCredentialRpEntity;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.PublicKeyCredentialUserEntity;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.UserVerificationRequirement;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.authenticator.COSEKey;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import net.marcussoft.training.webauthn.util.JsonUtils;
import net.marcussoft.training.webauthn.webauthn.entity.Credential;
import net.marcussoft.training.webauthn.webauthn.entity.User;
import net.marcussoft.training.webauthn.webauthn.repository.CredentialRepository;
import net.marcussoft.training.webauthn.webauthn.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author ono_takahiko
 * @since 2020/01/10
 */
@Service
public class WebAuthnService {

	private PublicKeyCredentialRpEntity rp;

	private UserRepository userRepository;

	private CredentialRepository credentialRepository;

	public WebAuthnService(
			@Value("${rp.id:localhost}") String rpId,
			@Value("${rp.name:RPServer}") String rpName,
			UserRepository userRepository,
			CredentialRepository credentialRepository) {
		this.userRepository = userRepository;
		this.credentialRepository = credentialRepository;
		this.rp = new PublicKeyCredentialRpEntity(rpId, rpName);
	}

	public User findOrCreateUser(String email) {
		return userRepository.findOrCreate(email);
	}

	public User find(String email) {
		return userRepository.find(email);
	}

	public PublicKeyCredentialCreationOptions createOptions(User user) {
		PublicKeyCredentialUserEntity userInfo =
				new PublicKeyCredentialUserEntity(
						user.getId(),
						user.getEmail(),
						"");

		Challenge challenge = new DefaultChallenge();

		List<PublicKeyCredentialParameters> pubKeyCredParams =
				Arrays.asList(
						new PublicKeyCredentialParameters(
								PublicKeyCredentialType.PUBLIC_KEY,
								COSEAlgorithmIdentifier.ES256),
						new PublicKeyCredentialParameters(
								PublicKeyCredentialType.PUBLIC_KEY,
								COSEAlgorithmIdentifier.RS256));

		// 同一認証機の登録制限
		List<PublicKeyCredentialDescriptor> excludeCredentials =
				credentialRepository.findByUserId(user.getId()).stream()
						.map(c ->
								new PublicKeyCredentialDescriptor(
										PublicKeyCredentialType.PUBLIC_KEY,
										c.getId(),
										Collections.emptySet()))
						.collect(Collectors.toList());

		AuthenticatorSelectionCriteria authenticatorSelection =
				new AuthenticatorSelectionCriteria(
						AuthenticatorAttachment.PLATFORM,
						false,
						UserVerificationRequirement.REQUIRED);

		return new PublicKeyCredentialCreationOptions(
				rp,
				userInfo,
				challenge,
				pubKeyCredParams,
				TimeUnit.SECONDS.toMillis(60),
				excludeCredentials,
				authenticatorSelection,
				AttestationConveyancePreference.NONE,
				null);
	}

	public void creationFinish(
			User user,
			Challenge challenge,
			byte[] clientDataJSON,
			byte[] attestationObject) throws JsonProcessingException {

		Origin origin = Origin.create("http://localhost:8080");

		ServerProperty serverProperty =
				new ServerProperty(
						origin,
						rp.getId(),
						challenge,
						null);

		RegistrationRequest registrationRequest =
				new RegistrationRequest(attestationObject, clientDataJSON);
		RegistrationParameters registrationParameters =
				new RegistrationParameters(serverProperty, true);
		RegistrationData registrationData =
				WebAuthnManager.createNonStrictWebAuthnManager().parse(registrationRequest);
		WebAuthnManager.createNonStrictWebAuthnManager()
				.validate(registrationData, registrationParameters);

		byte[] credentialId =
				registrationData.getAttestationObject()
						.getAuthenticatorData()
						.getAttestedCredentialData()
						.getCredentialId();
		COSEKey publicKey =
				registrationData.getAttestationObject()
						.getAuthenticatorData()
						.getAttestedCredentialData()
						.getCOSEKey();

		long signatureCounter =
				registrationData.getAttestationObject()
						.getAuthenticatorData()
						.getSignCount();

		findOrCreateUser(user.getEmail());

		byte[] publicKeyBin = JsonUtils.MAPPER.writeValueAsBytes(publicKey);

		Credential credential = new Credential();
		credential.setId(credentialId);
		credential.setUserId(user.getId());
		credential.setPublicKey(publicKeyBin);
		credential.setSignatureCounter(signatureCounter);

		credentialRepository.insert(credential);
	}

	public PublicKeyCredentialRequestOptions requestOptions(User user) {
		Challenge challenge = new DefaultChallenge();

		List<Credential> credentials = credentialRepository.findByUserId(user.getId());

		List<PublicKeyCredentialDescriptor> allowCredentials =
				credentials.stream()
						.map(c -> new PublicKeyCredentialDescriptor(
								PublicKeyCredentialType.PUBLIC_KEY,
								c.getId(),
								new HashSet<>()))
						.collect(Collectors.toList());

		return new PublicKeyCredentialRequestOptions(
				challenge,
				TimeUnit.SECONDS.toMillis(60),
				rp.getId(),
				allowCredentials,
				UserVerificationRequirement.REQUIRED,
				null);
	}

	public void assertionFinish(
			Challenge challenge,
			byte[] credentialId,
			byte[] clientDataJSON,
			byte[] authenticatorData,
			byte[] signature,
			byte[] userHandle) throws IOException {
		Origin origin = Origin.create("http://localhost:8080");

		ServerProperty serverProperty =
				new ServerProperty(
						origin,
						rp.getId(),
						challenge,
						null);

		Credential credential =
				credentialRepository.findByCredentialId(credentialId);
		if (credential == null) {
			throw new RuntimeException("Credential is not found");
		}

		COSEKey publicKey = JsonUtils.MAPPER.readValue(credential.getPublicKey(), COSEKey.class);

		AuthenticationRequest authenticationRequest =
				new AuthenticationRequest(credentialId, userHandle, authenticatorData,
						clientDataJSON, signature);
		Authenticator authenticator =
				new AuthenticatorImpl(
						new AttestedCredentialData(null, credentialId, publicKey),
						null,
						credential.getSignatureCounter());
		AuthenticationParameters authenticationParameters =
				new AuthenticationParameters(serverProperty, authenticator, true);

		AuthenticationData response =
				WebAuthnManager.createNonStrictWebAuthnManager()
						.validate(authenticationRequest, authenticationParameters);

		credentialRepository.updateCounter(credentialId, response.getAuthenticatorData().getSignCount());
	}

}
