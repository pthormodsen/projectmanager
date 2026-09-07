package no.patreek.projectmanager.config;

import no.patreek.projectmanager.domain.entity.User;
import no.patreek.projectmanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean securityEnabled;
    private final String username;
    private final String password;
    private final String email;

    public AdminUserInitializer(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        @Value("${app.security.enabled:true}") boolean securityEnabled,
        @Value("${app.security.username:}") String username,
        @Value("${app.security.password:}") String password,
        @Value("${app.security.email:admin@projectmanager.local}") String email
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityEnabled = securityEnabled;
        this.username = username;
        this.password = password;
        this.email = email;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!securityEnabled || username.isBlank() || password.isBlank()) {
            return;
        }

        User user = userRepository.findByUsername(username)
            .orElseGet(User::new);

        user.setUsername(username);
        user.setEmail(user.getEmail() == null || user.getEmail().isBlank() ? email : user.getEmail());
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);
    }
}
