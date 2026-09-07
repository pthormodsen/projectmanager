package no.patreek.projectmanager.service;

import no.patreek.projectmanager.domain.entity.User;
import no.patreek.projectmanager.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Optional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findCurrentUser(Principal principal) {
        if (principal == null) {
            return Optional.empty();
        }
        return userRepository.findByUsername(principal.getName());
    }

    public User requireCurrentUser(Principal principal) {
        if (principal == null) {
            throw new UsernameNotFoundException("Signed-in user not found");
        }
        return findCurrentUser(principal)
            .orElseThrow(() -> new UsernameNotFoundException("Signed-in user not found"));
    }

    public boolean owns(User currentUser, User owner) {
        return owner != null && owner.getId().equals(currentUser.getId());
    }
}
