package com.finpred.auth.service;

import com.finpred.auth.dto.*;
import com.finpred.auth.model.AuthProvider;
import com.finpred.auth.model.User;
import com.finpred.auth.repository.UserRepository;
import com.finpred.auth.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Serviço de autenticação — registro em 2 etapas, login, Google OAuth.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${google.client-id}")
    private String googleClientId;

    /**
     * Registro — Etapa 1: cria o usuário com email, nome, empresa e senha.
     * Retorna um JWT para que o frontend possa chamar a etapa 2.
     */
    @Transactional
    public AuthResponse registerStepOne(RegisterStepOneRequest request) {
        // Verificar se email já existe
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado");
        }

        // Criar usuário (perfil incompleto — sem businessType/legalNature)
        User user = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .companyName(request.getCompanyName())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider(AuthProvider.LOCAL)
                .build();

        user = userRepository.save(user);
        log.info("Usuário registrado (etapa 1): {} — {}", user.getId(), user.getEmail());

        // Gerar token JWT
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), Map.of(
                "username", user.getUsername(),
                "step", "register"
        ));

        boolean profileComplete = user.getBusinessType() != null && user.getLegalNature() != null;
        return AuthResponse.success(token, user.getId(), user.getEmail(), user.getUsername(), profileComplete);
    }

    /**
     * Registro — Etapa 2: completa o perfil com tipo de negócio e natureza jurídica.
     */
    @Transactional
    public UserProfileResponse registerStepTwo(Long userId, RegisterStepTwoRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        user.setBusinessType(request.getBusinessType());
        user.setLegalNature(request.getLegalNature());
        user = userRepository.save(user);

        log.info("Perfil completado (etapa 2): {} — {} / {}", user.getId(), user.getBusinessType(), user.getLegalNature());
        return UserProfileResponse.fromUser(user);
    }

    /**
     * Login com email e senha.
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("E-mail ou senha incorretos"));

        // Verificar se é usuário local (não Google)
        if (user.getProvider() != AuthProvider.LOCAL) {
            throw new IllegalArgumentException("Esta conta usa login social. Use o botão 'Entrar com Google'.");
        }

        // Verificar senha
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("E-mail ou senha incorretos");
        }

        log.info("Login realizado: {} — {}", user.getId(), user.getEmail());

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), Map.of(
                "username", user.getUsername()
        ));

        boolean profileComplete = user.getBusinessType() != null && user.getLegalNature() != null;
        return AuthResponse.success(token, user.getId(), user.getEmail(), user.getUsername(), profileComplete);
    }

    /**
     * Login com Google OAuth2.
     * Valida o ID token do Google, cria ou retorna o usuário existente.
     */
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        // Decodificar o JWT do Google (ID token) para extrair dados do usuário
        // Em produção, usar a biblioteca Google API Client para validação completa.
        // Para desenvolvimento, fazemos decode básico do payload JWT.
        Map<String, Object> payload = decodeGoogleIdToken(request.getCredential());

        String email = (String) payload.get("email");
        String name = (String) payload.get("name");

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Token do Google inválido: email ausente");
        }

        // Buscar ou criar usuário
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .username(name != null ? name : email.split("@")[0])
                    .provider(AuthProvider.GOOGLE)
                    .build();
            log.info("Novo usuário Google criado: {}", email);
            return userRepository.save(newUser);
        });

        // Se o usuário existia como LOCAL, não permitir login social
        if (user.getProvider() == AuthProvider.LOCAL) {
            throw new IllegalArgumentException("Esta conta já existe com login por senha. Use e-mail e senha.");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), Map.of(
                "username", user.getUsername(),
                "provider", "GOOGLE"
        ));

        boolean profileComplete = user.getBusinessType() != null && user.getLegalNature() != null;
        return AuthResponse.success(token, user.getId(), user.getEmail(), user.getUsername(), profileComplete);
    }

    /**
     * Atualiza o perfil do usuário.
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        user.setUsername(request.getUsername());
        user.setCompanyName(request.getCompanyName());
        user.setBusinessType(request.getBusinessType());
        user.setLegalNature(request.getLegalNature());

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            if (user.getProvider() != AuthProvider.LOCAL) {
                throw new IllegalArgumentException("Contas Google não permitem alteração de senha.");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user = userRepository.save(user);
        log.info("Perfil atualizado: {}", user.getId());
        
        return UserProfileResponse.fromUser(user);
    }

    /**
     * Retorna o perfil do usuário autenticado.
     */
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        return UserProfileResponse.fromUser(user);
    }

    /**
     * Decodifica o payload de um Google ID token (JWT).
     * NOTA: Em produção, use google-api-client para validação criptográfica completa.
     * Aqui fazemos decode do payload base64 para extrair email e name.
     */
    private Map<String, Object> decodeGoogleIdToken(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Token do Google malformado");
            }

            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

            @SuppressWarnings("unchecked")
            Map<String, Object> claims = mapper.readValue(payload, Map.class);

            // Verificar audience (deve ser o nosso Client ID)
            String aud = (String) claims.get("aud");
            if (!googleClientId.equals(aud) && !"placeholder".equals(googleClientId)) {
                log.warn("Google token audience mismatch: expected={}, got={}", googleClientId, aud);
            }

            return claims;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao decodificar Google ID token", e);
            throw new IllegalArgumentException("Token do Google inválido");
        }
    }
}
