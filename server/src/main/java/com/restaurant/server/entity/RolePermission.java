package com.restaurant.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * V2.2 / V4: role_permissions matrix row. Composite PK (role_id, permission_id).
 * Use a single id class to model the link table.
 */
@Entity
@Table(name = "role_permissions")
public class RolePermission {

    @Embeddable
    public static class RolePermissionId implements Serializable {
        @Column(name = "role_id", nullable = false, columnDefinition = "INTEGER")
        private Long roleId;

        @Column(name = "permission_id", nullable = false, columnDefinition = "INTEGER")
        private Long permissionId;

        public RolePermissionId() {}
        public RolePermissionId(Long roleId, Long permissionId) {
            this.roleId = roleId;
            this.permissionId = permissionId;
        }
        public Long getRoleId() { return roleId; }
        public void setRoleId(Long roleId) { this.roleId = roleId; }
        public Long getPermissionId() { return permissionId; }
        public void setPermissionId(Long permissionId) { this.permissionId = permissionId; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RolePermissionId other)) return false;
            return Objects.equals(roleId, other.roleId) && Objects.equals(permissionId, other.permissionId);
        }
        @Override
        public int hashCode() { return Objects.hash(roleId, permissionId); }
    }

    @EmbeddedId
    private RolePermissionId id;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    @Column(name = "granted_by", columnDefinition = "INTEGER")
    private Long grantedBy;

    public RolePermission() {}

    public RolePermission(Long roleId, Long permissionId) {
        this.id = new RolePermissionId(roleId, permissionId);
    }

    public RolePermissionId getId() { return id; }
    public void setId(RolePermissionId id) { this.id = id; }
    public Instant getGrantedAt() { return grantedAt; }
    public void setGrantedAt(Instant grantedAt) { this.grantedAt = grantedAt; }
    public Long getGrantedBy() { return grantedBy; }
    public void setGrantedBy(Long grantedBy) { this.grantedBy = grantedBy; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RolePermission other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
