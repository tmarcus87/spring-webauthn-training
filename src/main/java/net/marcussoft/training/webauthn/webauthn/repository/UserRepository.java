package net.marcussoft.training.webauthn.webauthn.repository;

import com.google.common.collect.Maps;
import net.marcussoft.training.webauthn.webauthn.entity.User;
import org.springframework.stereotype.Repository;

import java.security.SecureRandom;
import java.util.Map;

/**
 * @author ono_takahiko
 * @since 2020/01/10
 */
@Repository
public class UserRepository {

	private Map<String, User> users = Maps.newHashMap();

	public User find(String email) {
		return users.get(email);
	}

	public User findOrCreate(String email) {
		return users.computeIfAbsent(email, s -> {
			byte[] id = new byte[64];
			new SecureRandom().nextBytes(id);
			return new User(id, s);
		});
	}

}
