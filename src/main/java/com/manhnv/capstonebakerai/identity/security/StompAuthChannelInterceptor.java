package com.manhnv.capstonebakerai.identity.security;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;

/** Authenticates STOMP CONNECT frames with the same access token the REST API
 * uses: the WebSocket handshake can't carry the HttpOnly refresh cookie, and
 * Spring Security's servlet filter chain doesn't run for the STOMP sub-protocol. */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // Must use the accessor already attached to the inbound message (kept
        // mutable by StompSubProtocolHandler), not StompHeaderAccessor.wrap(),
        // which builds a detached copy whose mutations never reach the session.
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            accessor.setUser(authenticate(accessor));
        }
        return message;
    }

    private UsernamePasswordAuthenticationToken authenticate(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadCredentialsException("Missing bearer token for WebSocket handshake");
        }
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(authHeader.substring(7));
        } catch (Exception ex) {
            throw new BadCredentialsException("Invalid token for WebSocket handshake");
        }
        return new UsernamePasswordAuthenticationToken(jwt.getSubject(), null, List.of());
    }
}
