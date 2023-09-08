package springredis.demo.controller;

import javax.validation.Valid;
import springredis.demo.entity.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.springframework.boxes.oauth.starter.bean.SignUpRequest;

import springredis.demo.repository.UserRepository;

@RestController
public class AuthController {

    private static final int minPasswordLength = 8;  // example minimum length
    private static final int maxPasswordLength = 50; // example maximum length



    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean isValidPassword(String password) {
        if (password.length() < minPasswordLength || password.length() > maxPasswordLength) {
            return false;
        }
        if (!password.matches(".*[A-Z].*")) { // contains an uppercase letter
            return false;
        }
        if (!password.matches(".*[a-z].*")) { // contains a lowercase letter
            return false;
        }
        if (!password.matches(".*[0-9].*")) { // contains a number
            return false;
        }
        if (!password.matches(".*[!@#$%^&*].*")) { // contains a special character
            return false;
        }
        return true;
    }

    @PostMapping("/signup")
public ResponseEntity<String> signUp(@RequestBody User user) {

    // Validate the provided password
    if (!isValidPassword(user.getPassword_hash())) {
        return new ResponseEntity<>("Invalid password format", HttpStatus.BAD_REQUEST);
    }

    if (userRepository.findByUsername(user.getUsername()) != null) {
        return new ResponseEntity<>("Username already exists", HttpStatus.BAD_REQUEST);
    }

    if (userRepository.findByContactEmail(user.getContactEmail()) != null) {
        return new ResponseEntity<>("Email already exists", HttpStatus.BAD_REQUEST);
    }

    if (user.getPassword_hash().length() > 50) {
        return new ResponseEntity<>("Password length exceeds the maximum limit (50 characters).", HttpStatus.BAD_REQUEST);
    }

    // ... rest of the signup logic ...

    return new ResponseEntity<>("Signup successful", HttpStatus.CREATED);
}



    /* 
    @PostMapping("/signup")
public ResponseEntity<String> signUp(@RequestBody User user) {

    if (userRepository.findByUsername(user.getUsername()) != null) {
        return new ResponseEntity<>("Username already exists", HttpStatus.BAD_REQUEST);
    }

    if (userRepository.findByContactEmail(user.getContactEmail()) != null) {
        return new ResponseEntity<>("Email already exists", HttpStatus.BAD_REQUEST);
    }

    if (user.getPassword_hash().length() > 50) {
        return new ResponseEntity<>("Password length exceeds the maximum limit (50 characters).", HttpStatus.BAD_REQUEST);
    }

    // ... rest of the signup logic ...

    return new ResponseEntity<>("Signup successful", HttpStatus.CREATED);
}
*/


    /* 
    @PostMapping("/signup")
    public String signUp(@RequestBody User user) {

        if (userRepository.findByUsername(user.getUsername()) != null) {
            return "Username already exists"; // Return an appropriate response indicating the error
        }

        if (userRepository.findByContactEmail(user.getContactEmail()) != null) {
            return "Email already exists";
        }

        if (user.getPassword_hash().length() > 50) {
            return "Password length exceeds the maximum limit (50 characters)."; // Return an appropriate response indicating the error
        }

        String hashedPassword = passwordEncoder.encode(user.getPassword_hash());
        user.setPassword_hash(hashedPassword);

        // Set the new fields
        user.setCompanyId(user.getCompanyId());
        user.setContactName(user.getContactName());
        user.setContactPhone(user.getContactPhone());
        user.setAddress(user.getAddress());
        // Set other fields as needed...

        userRepository.save(user);

        return "Signup successful"; // Return a success response
    }
    */

    @PostMapping("/login")
public ResponseEntity<String> login(@RequestBody User user) {
    User existingUser = userRepository.findByUsername(user.getUsername());

    if (existingUser != null && passwordEncoder.matches(user.getPassword_hash(), existingUser.getPassword_hash())) {
        return new ResponseEntity<>("Login successful", HttpStatus.OK);
    }

    return new ResponseEntity<>("Invalid username or password", HttpStatus.UNAUTHORIZED);
}


    /* 
    @PostMapping("/login")
    public String login(@RequestBody User user) {
        // Find the user by username
        User existingUser = userRepository.findByUsername(user.getUsername());

        if (existingUser != null && passwordEncoder.matches(user.getPassword_hash(), existingUser.getPassword_hash())) {
            return "Login successful"; // Return a success response
        }

        return "Invalid username or password"; // Return an appropriate response indicating the error
    }
    */


    @PutMapping("/update/{userId}")
public ResponseEntity<String> updateUser(@PathVariable Long userId, @RequestBody User updatedUser) {
    User existingUser = userRepository.findById(userId).orElse(null);

    if (existingUser == null) {
        return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
    }

    // ... rest of the update logic ...

    return new ResponseEntity<>("User updated successfully", HttpStatus.OK);
}


    /* 
    @PutMapping("/update/{userId}")
    public String updateUser(@PathVariable Long userId, @RequestBody User updatedUser) {
        // Find the user by userId
        User existingUser = userRepository.findById(userId).orElse(null);

        // Check if the user exists
        if (existingUser == null) {
            return "User not found"; // Return an appropriate response indicating the error
        }


        existingUser.setUsername(updatedUser.getUsername());
        existingUser.setContactEmail(updatedUser.getContactEmail());
        existingUser.setAvatarUrl(updatedUser.getAvatarUrl());
        // Update other fields as needed...

        // Save the updated user
        userRepository.save(existingUser);

        return "User updated successfully"; // Return a success response
    }
    */


    @DeleteMapping("/delete/{userId}")
public ResponseEntity<String> deleteUser(@PathVariable Long userId) {
    User existingUser = userRepository.findById(userId).orElse(null);

    if (existingUser == null) {
        return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
    }

    // ... rest of the delete logic ...

    return new ResponseEntity<>("User deleted successfully", HttpStatus.OK);
}



    /* 
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
    */







}

    
