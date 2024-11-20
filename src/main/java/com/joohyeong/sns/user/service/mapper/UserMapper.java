package com.joohyeong.sns.user.service.mapper;

import com.joohyeong.sns.user.domain.User;
import com.joohyeong.sns.user.dto.request.UserRegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final PasswordEncoder passwordEncoder;

    public User mapToUser(UserRegisterRequest dto) {
        return new User(dto.username(), dto.email(), passwordEncoder.encode(dto.password()));
    }

}
