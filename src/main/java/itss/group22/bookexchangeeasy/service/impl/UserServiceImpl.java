package itss.group22.bookexchangeeasy.service.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import itss.group22.bookexchangeeasy.dto.*;
import itss.group22.bookexchangeeasy.entity.AddressUnit;
import itss.group22.bookexchangeeasy.entity.ContactInfo;
import itss.group22.bookexchangeeasy.entity.Role;
import itss.group22.bookexchangeeasy.entity.User;
import itss.group22.bookexchangeeasy.enums.Gender;
import itss.group22.bookexchangeeasy.exception.ApiException;
import itss.group22.bookexchangeeasy.exception.ResourceNotFoundException;
import itss.group22.bookexchangeeasy.repository.AddressUnitRepository;
import itss.group22.bookexchangeeasy.repository.ContactInfoRepository;
import itss.group22.bookexchangeeasy.repository.RoleRepository;
import itss.group22.bookexchangeeasy.repository.UserRepository;
import itss.group22.bookexchangeeasy.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AddressUnitRepository addressUnitRepository;
    private final ContactInfoRepository contactInfoRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper mapper;

    @Value("${security.secret-key}")
    private String secretKey;

    @Override
    public AuthResponse authenticate(AuthRequest authRequest) {
        User user = userRepository.findByEmail(authRequest.getEmail())
                .orElseThrow(() -> new ApiException("Account does not exist", HttpStatus.FORBIDDEN));
        log.info("Found user: " + user.getEmail());

        if (!passwordEncoder.matches(authRequest.getPassword(), user.getPassword()))
            throw new ApiException("Incorrect password", HttpStatus.FORBIDDEN);

        String token = generateToken(user);
        log.info("Generated token");

        return AuthResponse.builder().accessToken(token).build();
    }

    @Override
    public void register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail()))
            throw new ApiException("Email already registered");

        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setName(registerRequest.getName());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setGender(Gender.valueOf(registerRequest.getGender()));
        user.setRoles(registerRequest.getRoles().stream().map(name ->
                roleRepository.findByName(name)
                        .orElseThrow(() -> new ResourceNotFoundException("role", "name", name))
        ).collect(Collectors.toSet()));

        if (registerRequest.getBirthDate().isAfter(LocalDate.now()))
            throw new ApiException("Invalid date of birth");
        user.setBirthDate(registerRequest.getBirthDate());

        AddressUnit province = addressUnitRepository.findById(registerRequest.getProvinceId())
                .orElseThrow(() -> new ResourceNotFoundException("Province", "id", registerRequest.getProvinceId()));
        AddressUnit district = addressUnitRepository.findById(registerRequest.getDistrictId())
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", registerRequest.getDistrictId()));
        AddressUnit commune = addressUnitRepository.findById(registerRequest.getCommuneId())
                .orElseThrow(() -> new ResourceNotFoundException("Commune", "id", registerRequest.getCommuneId()));
        ContactInfo contactInfo = ContactInfo.builder()
                .phoneNumber(registerRequest.getPhoneNumber())
                .province(province)
                .district(district)
                .commune(commune)
                .detailedAddress(registerRequest.getDetailedAddress())
                .build();
        contactInfo = contactInfoRepository.save(contactInfo);

        user.setContactInfo(contactInfo);
        user.setIsLocked(false);
        user.setIsVerified(false);

        userRepository.save(user);
    }

    private String generateToken(User user) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getEmail())
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()))
                .claim("id", user.getId())
                .claim("scope", buildScope(user))
                .build();
        Payload payload = new Payload(claimsSet.toJSONObject());
        JWSObject jwsObject = new JWSObject(header, payload);

        try {
            jwsObject.sign(new MACSigner(secretKey));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildScope(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) return "";
        var roles = user.getRoles().stream().map(Role::getName).toList();
        return String.join(" ", roles);
    }

    @Override
    public UserProfile getProfile(Long id) {
        User user = userRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
        UserProfile userProfile = mapper.map(user, UserProfile.class);
        userProfile.setGender(user.getGender().name());
        userProfile.setRoles(user.getRoles().stream().map(Role::getName).toList());
        userProfile.setProvince(AddressUnitDTO.builder()
                .id(user.getContactInfo().getProvince().getId()).
                name(user.getContactInfo().getProvince().getName()).build());
        userProfile.setDistrict(AddressUnitDTO.builder()
                .id(user.getContactInfo().getDistrict().getId()).
                name(user.getContactInfo().getDistrict().getName()).build());
        userProfile.setCommune(AddressUnitDTO.builder()
                .id(user.getContactInfo().getCommune().getId()).
                name(user.getContactInfo().getCommune().getName()).build());

        userProfile.setDetailedAddress(user.getContactInfo().getDetailedAddress());
        userProfile.setPhoneNumber(user.getContactInfo().getPhoneNumber());
        return userProfile;
    }

    @Override
    public void updateProfile(Long id, UserProfile userProfile) {
        User user = userRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
        user.setRoles(userProfile.getRoles().stream().map(name ->
                roleRepository.findByName(name)
                        .orElseThrow(() -> new ResourceNotFoundException("role", "name", name))
        ).collect(Collectors.toSet()));
        user.setGender(Gender.valueOf(userProfile.getGender()));

        if (userProfile.getBirthDate().isAfter(LocalDate.now()))
            throw new ApiException("Invalid date of birth");
        user.setBirthDate(userProfile.getBirthDate());

        AddressUnit province = addressUnitRepository.findById(userProfile.getProvince().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Province", "id", userProfile.getProvince().getId()));
        AddressUnit district = addressUnitRepository.findById(userProfile.getDistrict().getId())
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", userProfile.getDistrict().getId()));
        AddressUnit commune = addressUnitRepository.findById(userProfile.getCommune().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Commune", "id", userProfile.getCommune().getId()));
        ContactInfo contactInfo = ContactInfo.builder()
                .phoneNumber(userProfile.getPhoneNumber())
                .province(province)
                .district(district)
                .commune(commune)
                .detailedAddress(userProfile.getDetailedAddress())
                .build();
        contactInfoRepository.save(contactInfo);

        user.setContactInfo(contactInfo);
        user.setEmail(userProfile.getEmail());
        user.setName(userProfile.getName());
        userRepository.save(user);
    }

    @Override
    public void changePassword(Long id, ChangePasswordDTO changePasswordDTO) {
        User user = userRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
        if (!passwordEncoder.matches(changePasswordDTO.getOldPassword(), user.getPassword()))
            throw new ApiException("Incorrect password", HttpStatus.BAD_REQUEST);
        user.setPassword(passwordEncoder.encode(changePasswordDTO.getNewPassword()));
        userRepository.save(user);
    }

}