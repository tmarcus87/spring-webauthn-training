package net.marcussoft.training.webauthn.webauthn.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ono_takahiko
 * @since 2020/01/10
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Credential {

	byte[] id;

	byte[] userId;

	byte[] publicKey;

	long signatureCounter;

}
