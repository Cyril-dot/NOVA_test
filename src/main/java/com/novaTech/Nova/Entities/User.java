package com.novaTech.Nova.Entities;

import com.novaTech.Nova.Entities.Enums.Role;
import com.novaTech.Nova.Entities.Enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_username", columnList = "username"),
                @Index(name = "idx_users_status", columnList = "status"),
                @Index(name = "idx_users_role", columnList = "role")
        }
)
@Getter
@Setter
@ToString(exclude = {"profileImage"}) // Exclude large fields and any relationships you add later
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Email
    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "password", nullable = true, length = 255)
    private String password;

    /**
     * Profile image stored as Base64 encoded string
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "profile_image", columnDefinition = "TEXT")
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ===== MFA Fields =====

    @Column(name = "mfa_enabled", nullable = false)
    @Builder.Default
    private boolean mfaEnabled = false;

    @Column(name = "mfa_secret", length = 255)
    private String mfaSecret;

    @Column(name = "mfa_code")
    private Integer mfaCode;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "mfa_updated_at")
    private LocalDateTime mfaUpdatedAt;

    @Column(name = "oauth_provider", length = 50)
    private String oauthProvider;

    @Column(name = "oauth_provider_id", length = 255)
    private String oauthProviderId;

    // ===== Lifecycle Callbacks =====

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (lastLogin == null) {
            lastLogin = LocalDateTime.now();
        }
        if (status == null) {
            status = Status.ACTIVE;
        }
        if (role == null) {
            role = Role.USER;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== Custom equals and hashCode (IMPORTANT for JPA entities) =====

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id != null && id.equals(user.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    // ===== Helper Methods =====

    public boolean hasProfileImage() {
        return profileImage != null && !profileImage.isEmpty();
    }

    public int getProfileImageSize() {
        return hasProfileImage() ? profileImage.length() : 0;
    }

    public long getApproximateOriginalImageSizeBytes() {
        if (!hasProfileImage()) return 0;
        return (long) (profileImage.length() * 0.75);
    }

    public void clearProfileImage() {
        this.profileImage = null;
    }

    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
    }

    public void enableMfa(String secret) {
        this.mfaEnabled = true;
        this.mfaSecret = secret;
        this.mfaUpdatedAt = LocalDateTime.now();
    }

    public void disableMfa() {
        this.mfaEnabled = false;
        this.mfaSecret = null;
        this.mfaCode = null;
        this.generatedAt = null;
        this.mfaUpdatedAt = LocalDateTime.now();
    }

    public void updateMfaCode(Integer code) {
        this.mfaCode = code;
        this.generatedAt = LocalDateTime.now();
        this.mfaUpdatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return Status.ACTIVE.equals(this.status);
    }

    public boolean isAdmin() {
        return Role.ADMIN.equals(this.role);
    }

    public boolean isOAuth2User() {
        return oauthProvider != null && !oauthProvider.isEmpty();
    }

    public boolean isLocalUser() {
        return !isOAuth2User();
    }
}