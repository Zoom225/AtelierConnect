package com.atelierconnect.service;

import com.atelierconnect.dto.request.LoginRequest;
import com.atelierconnect.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
