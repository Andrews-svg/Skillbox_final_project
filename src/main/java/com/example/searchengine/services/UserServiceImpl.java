package com.example.searchengine.services;

import com.example.searchengine.exceptions.UsernameAlreadyExistsException;
import com.example.searchengine.models.AppUser;
import com.example.searchengine.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Autowired
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    @Transactional
    public AppUser registerNewUser(AppUser newUser) {
        Optional<AppUser> existingUser = userRepository.findByUsername(newUser.getUsername());
        if (existingUser.isPresent()) {
            throw new UsernameAlreadyExistsException("Пользователь с таким именем уже существует");
        }

        String encodedPassword = passwordEncoder.encode(newUser.getPassword());
        newUser.setPassword(encodedPassword);

        return userRepository.save(newUser);
    }


    @Override
    public Optional<AppUser> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }


    @Override
    @Transactional
    public AppUser updateProfile(AppUser updatedUser) {
        Optional<AppUser> currentUser = userRepository.findById(updatedUser.getId());
        if (currentUser.isEmpty()) {
            throw new EntityNotFoundException("Пользователь не найден");
        }

        AppUser dbUser = currentUser.get();
        dbUser.setFirstName(updatedUser.getFirstName());
        dbUser.setLastName(updatedUser.getLastName());
        dbUser.setRoles(updatedUser.getRoles());

        return userRepository.save(dbUser);
    }
}