package com.civic.civicbackend.service;

import com.civic.civicbackend.model.User;
import com.civic.civicbackend.repository.UserRepository;
import com.civic.civicbackend.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public AuthService(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public User register(User user) {

    if (repo.findByEmail(user.getEmail()) != null) {
        throw new RuntimeException("Email already registered");
    }

    user.setPassword(encoder.encode(user.getPassword()));
    user.setRole("USER"); // force role
    return repo.save(user);
    }

    public User getUserByEmail(String email) {
    return repo.findByEmail(email);
}


    public String login(String email, String password) {
        User u = repo.findByEmail(email);
        if (u != null && encoder.matches(password, u.getPassword())) {
            return JwtUtil.generateToken(u.getEmail(), u.getRole());
        }
        throw new RuntimeException("Invalid credentials");
    }
}
