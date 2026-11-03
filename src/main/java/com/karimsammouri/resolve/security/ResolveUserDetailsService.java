package com.karimsammouri.resolve.security;

import com.karimsammouri.resolve.entity.User;
import com.karimsammouri.resolve.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ResolveUserDetailsService implements UserDetailsService {
    private final UserRepository userRepo;

    public ResolveUserDetailsService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

        return new ResolveUserDetails(user);
    }
}
