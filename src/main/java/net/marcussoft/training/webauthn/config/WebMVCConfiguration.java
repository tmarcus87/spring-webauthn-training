package net.marcussoft.training.webauthn.config;

import net.marcussoft.training.webauthn.util.JsonUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * @author ono_takahiko
 * @since 2020/01/07
 */
@Configuration
public class WebMVCConfiguration implements WebMvcConfigurer {

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		for (HttpMessageConverter<?> converter : converters) {
			if (converter instanceof MappingJackson2HttpMessageConverter) {
				((MappingJackson2HttpMessageConverter) converter).setObjectMapper(JsonUtils.MAPPER);
			}
		}
	}
}
