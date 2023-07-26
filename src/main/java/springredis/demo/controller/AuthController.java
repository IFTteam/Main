package springredis.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import springredis.demo.entity.User;
import springredis.demo.repository.UserRepository;

@RestController
public class AuthController {

    @Autowired
    private UserRepository userRepository;
<<<<<<< Updated upstream
=======

    @Autowired
    private PasswordEncoder passwordEncoder;

    // This method handles the sign-up functionality when a POST request is made to the /signup endpoint.
    // RequestBody:  binds the request body to the User object passed as a parameter.
    @PostMapping("/signup")
    public String signUp(@RequestBody User user) {
        // Check if the username is already taken
        if (userRepository.findByUsername(user.getUsername()) != null) {
            return "Username already exists"; // Return an appropriate response indicating the error
        }
        // Check if the provided username already exists in the database by using the UserRepository method findByUsername. If a user with the same username exists, it returns an appropriate response indicating the error.

        if (userRepository.findByEmail(user.getEmail()) != null) {
            return "Email already exists";
        }
>>>>>>> Stashed changes

    @Autowired
    private PasswordEncoder passwordEncoder;

    // This method handles the sign-up functionality when a POST request is made to the /signup endpoint.
    // RequestBody:  binds the request body to the User object passed as a parameter.
    @PostMapping("/signup")
    public String signUp(@RequestBody User user) {
        // Check if the username is already taken
        if (userRepository.findByUsername(user.getUsername()) != null) {
            return "Username already exists"; // Return an appropriate response indicating the error
        }
        // Check if the provided username already exists in the database by using the UserRepository method findByUsername. If a user with the same username exists, it returns an appropriate response indicating the error.

        if (userRepository.findByEmail(user.getEmail()) != null) {
            return "Email already exists";
        }


        // After validating the username, this code block hashes the password using the PasswordEncoder. The hashed password is then set on the User object.
        // Hash the password before saving it
        String hashedPassword = passwordEncoder.encode(user.getPassword_hash());
        user.setPassword_hash(hashedPassword);

        // Save the user to the database
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

        // After validating the username, this code block hashes the password using the PasswordEncoder. The hashed password is then set on the User object.
        // Hash the password before saving it
        String hashedPassword = passwordEncoder.encode(user.getPassword_hash());
        user.setPassword_hash(hashedPassword);

        // Save the user to the database
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


}