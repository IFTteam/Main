package springredis.demo.controller;
import springredis.demo.entity.User;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import springredis.demo.repository.UserRepository;

@RestController
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @PostMapping("/signup")
    public String signUp(@RequestBody User user) {

        if (userRepository.findByUsername(user.getUsername()) != null) {
            return "Username already exists"; // Return an appropriate response indicating the error
        }

        if (userRepository.findByEmail(user.getEmail()) != null) {
            return "Email already exists";
        }

        String hashedPassword = passwordEncoder.encode(user.getPassword_hash());
        user.setPassword_hash(hashedPassword);

        userRepository.save(user);

        return "Signup successful"; // Return a success response
    }

    // This method handles the login functionality when a POST request is made to the /login endpoint.
    // RequestBody: binds the request body to the User object passed as a parameter.
    @PostMapping("/login")
    public String login(@RequestBody User user) {
        // Find the user by username
        User existingUser = userRepository.findByUsername(user.getUsername());

        // Check if the user exists and validate the password
        // If the user exists and the provided password matches the hashed password in the database (using //passwordEncoder matches), it returns a success response indicating that the login was successful.
        //Otherwise, it returns an appropriate response indicating that the username or password is invalid.
        if (existingUser != null && passwordEncoder.matches(user.getPassword_hash(), existingUser.getPassword_hash())) {
            return "Login successful"; // Return a success response
        }

        return "Invalid username or password"; // Return an appropriate response indicating the error
    }



    @PutMapping("/update/{userId}")
    public String updateUser(@PathVariable Long userId, @RequestBody User updatedUser) {
        // Find the user by userId
        User existingUser = userRepository.findById(userId).orElse(null);

        // Check if the user exists
        if (existingUser == null) {
            return "User not found"; // Return an appropriate response indicating the error
        }

        // Update the fields of the existingUser with the fields from updatedUser
        // You can choose which fields are updatable based on your application's requirements.
        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setAvatarUrl(updatedUser.getAvatarUrl());
        // Update other fields as needed...

        // Save the updated user
        userRepository.save(existingUser);

        return "User updated successfully"; // Return a success response
    }

    @DeleteMapping("/delete/{userId}")
    public String deleteUser(@PathVariable Long userId) {
        // Find the user by userId
        User existingUser = userRepository.findById(userId).orElse(null);

        // Check if the user exists
        if (existingUser == null) {
            return "User not found"; // Return an appropriate response indicating the error
        }

        // Delete the user from the database
        userRepository.delete(existingUser);

        return "User deleted successfully"; // Return a success response
    }


}

