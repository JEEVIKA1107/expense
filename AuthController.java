
package com.example.expense_manager;

import java.sql.SQLException;
import java.util.Locale;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.crypto.password.PasswordEncoder;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            Model model) {

        String cleanName = name.trim();
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);

        if (cleanName.isBlank() || cleanEmail.isBlank()) {
            model.addAttribute("error",
                    "Name and email are required.");
            return "register";
        }

        if (cleanName.length() > 100 || cleanEmail.length() > 254) {
            model.addAttribute("error",
                    "Name or email is too long.");
            return "register";
        }

        if (!cleanEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            model.addAttribute("error",
                    "Please enter a valid email address.");
            return "register";
        }

        if (password.length() < 8 || password.length() > 72) {
            model.addAttribute("error",
                    "Password must be between 8 and 72 characters.");
            return "register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error",
                    "Passwords do not match.");
            return "register";
        }

        try {
            if (userRepository.emailExists(cleanEmail)) {
                model.addAttribute("error",
                        "An account with this email already exists.");
                return "register";
            }

            String passwordHash = passwordEncoder.encode(password);

            User user = new User(
                    0,
                    cleanName,
                    cleanEmail,
                    passwordHash
            );

            userRepository.saveUser(user);

            model.addAttribute("success",
                    "Account created! You can now sign in.");
            return "register";

        } catch (SQLException e) {
            model.addAttribute("error",
                    "Could not save your account. Please try again.");
            return "register";
        }
    }
}
