package net.marcussoft.training.webauthn.webauthn.repository;

import com.google.common.collect.Maps;
import net.marcussoft.training.webauthn.webauthn.entity.Credential;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author ono_takahiko
 * @since 2020/01/10
 */
@Repository
public class CredentialRepository {

	private Map<byte[], List<Credential>> credentials = Maps.newHashMap();

	public List<Credential> findByUserId(byte[] userId) {
		return credentials.getOrDefault(userId, Collections.emptyList());
	}

	public Credential findByCredentialId(byte[] credentialId) {
		for (List<Credential> credentialList : credentials.values()) {
			for (Credential credential : credentialList) {
				if (Arrays.equals(credential.getId(), credentialId)) {
					return credential;
				}
			}
		}
		return null;
	}

	public void insert(Credential credential) {
		credentials.computeIfAbsent(credential.getUserId(), s -> new ArrayList<>()).add(credential);
	}

	public void updateCounter(byte[] credentialId, long counter) {
		findByCredentialId(credentialId).setSignatureCounter(counter);
	}

}
