package com.helper.vavahelper.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.helper.vavahelper.repositories.UserRepository;

@Service
public class AuthorizathionService implements UserDetailsService {

    private final UserRepository repository;

    public AuthorizathionService(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = repository.findByLogin(username);
        // O contrato do UserDetailsService proibe retornar null. Lancar a excecao faz o Spring
        // responder 401 (credenciais invalidas) em vez de um erro 500.
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return user;
    }
}
