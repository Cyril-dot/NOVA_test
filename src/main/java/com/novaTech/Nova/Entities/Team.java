package com.novaTech.Nova.Entities;

import com.novaTech.Nova.Entities.Enums.TeamStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "teams",
        indexes = {
                @Index(name = "idx_team_id", columnList = "id"),
                @Index(name = "idx_team_name", columnList = "name"),
                @Index(name = "idx_team_description", columnList = "description"),
                @Index(name = "idx_team_created_at", columnList = "created_at")  // ✅ Fixed: use database column name
        }
)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;  // who created the team (Owner)

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TeamMember> members = new HashSet<>();

    @Column(name = "created_at")  // ✅ Added explicit column mapping
    private LocalDateTime createdAt;

    // ✅ Custom equals/hashCode based on ID only
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Team)) return false;
        Team team = (Team) o;
        return id != null && id.equals(team.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}