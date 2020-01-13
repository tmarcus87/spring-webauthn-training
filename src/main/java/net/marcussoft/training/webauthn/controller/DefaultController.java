package net.marcussoft.training.webauthn.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.marcussoft.training.webauthn.webauthn.WebAuthnService;
import net.marcussoft.training.webauthn.webauthn.entity.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * @author ono_takahiko
 * @since 2020/01/07
 */
@Controller
@AllArgsConstructor
public class DefaultController {

	private WebAuthnService webAuthnService;

	@GetMapping("/")
	public ModelAndView index() {
		ModelAndView mav = new ModelAndView();
		mav.setViewName("index");
		return mav;
	}

	@GetMapping("/attestation/options")
	@ResponseBody
	public PublicKeyCredentialCreationOptions attestationOptions(
			@RequestParam("email") String email,
			HttpSession httpSession) {
		User user = webAuthnService.findOrCreateUser(email);
		PublicKeyCredentialCreationOptions options = webAuthnService.createOptions(user);
		httpSession.setAttribute("user", user);
		httpSession.setAttribute("challenge", options);
		return options;
	}

	@PostMapping("/attestation/result")
	@ResponseBody
	public void attestationResult(
			@RequestBody AttestationResultParam body,
			HttpSession httpSession) throws JsonProcessingException {

		User user = (User) httpSession.getAttribute("user");
		PublicKeyCredentialCreationOptions options = (PublicKeyCredentialCreationOptions) httpSession.getAttribute("challenge");

		webAuthnService.creationFinish(user, options.getChallenge(), body.getClientDataJSON(), body.getAttestationObject());
	}

	@GetMapping("/assertion/options")
	@ResponseBody
	public PublicKeyCredentialRequestOptions assertionOptions(
			@RequestParam("email") String email,
			HttpSession httpSession) {
		User user = webAuthnService.find(email);
		if (user == null) {
			throw new RuntimeException("User is not found");
		}
		PublicKeyCredentialRequestOptions options = webAuthnService.requestOptions(user);
		httpSession.setAttribute("assertionOption", options);
		return options;
	}

	@PostMapping("/assertion/result")
	@ResponseBody
	public void assertionResult(
			@RequestBody AuthenticationResultParam params,
			HttpSession httpSession) throws IOException {
		PublicKeyCredentialRequestOptions options = (PublicKeyCredentialRequestOptions) httpSession.getAttribute("assertionOption");

		webAuthnService.assertionFinish(
				options.getChallenge(),
				params.getCredentialId(),
				params.getClientDataJSON(),
				params.getAuthenticatorData(),
				params.getSignature(),
				params.getUserHandle());
	}

	@Data
	public static class AttestationResultParam {
		private byte[] clientDataJSON;
		private byte[] attestationObject;
	}

	@Data
	public static class AuthenticationResultParam {
		private byte[] credentialId;
		private byte[] clientDataJSON;
		private byte[] authenticatorData;
		private byte[] signature;
		private byte[] userHandle;
	}


}
