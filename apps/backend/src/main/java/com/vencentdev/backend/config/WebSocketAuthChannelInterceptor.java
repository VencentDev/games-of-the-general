package com.vencentdev.backend.config;

import com.vencentdev.backend.auth.oauth.OAuthProviderAuthenticationToken;
import com.vencentdev.backend.auth.oauth.OAuthProviderUserInfoClient;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

  private static final String BEARER_PREFIX = "Bearer ";

  private final OAuthProviderUserInfoClient userInfoClient;

  public WebSocketAuthChannelInterceptor(OAuthProviderUserInfoClient userInfoClient) {
    this.userInfoClient = userInfoClient;
  }

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

    if (accessor.getCommand() != StompCommand.CONNECT) {
      return message;
    }

    String accessToken = bearerToken(accessor.getNativeHeader(HttpHeaders.AUTHORIZATION));
    if (accessToken == null) {
      throw new AuthenticationCredentialsNotFoundException("Missing websocket bearer token");
    }

    var user = userInfoClient.resolve(accessToken);
    if (user == null) {
      throw new AuthenticationCredentialsNotFoundException("Invalid websocket bearer token");
    }

    accessor.setUser(new OAuthProviderAuthenticationToken(user, accessToken));
    return message;
  }

  private String bearerToken(List<String> values) {
    if (values == null || values.isEmpty()) {
      return null;
    }

    String authorization = values.get(0);
    if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
      return null;
    }

    String token = authorization.substring(BEARER_PREFIX.length()).trim();
    return token.isEmpty() ? null : token;
  }
}
