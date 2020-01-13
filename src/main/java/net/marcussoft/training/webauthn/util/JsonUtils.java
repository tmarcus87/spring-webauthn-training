package net.marcussoft.training.webauthn.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

/**
 * @author ono_takahiko
 * @since 2020/01/07
 */
public class JsonUtils {

	public static ObjectMapper MAPPER;

	static {
		ObjectMapper MAPPER =
				new ObjectMapper()
						.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
						.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
						.setSerializationInclusion(JsonInclude.Include.NON_ABSENT)
						.registerModule(new Jdk8Module());
	}

}
