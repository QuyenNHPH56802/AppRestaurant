package com.restaurant.server.repository;

import com.restaurant.server.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RolePermissionRepository
        extends JpaRepository<RolePermission, RolePermission.RolePermissionId> {

    @Query("SELECT rp FROM RolePermission rp WHERE rp.id.roleId = :roleId")
    List<RolePermission> findAllByRoleId(@Param("roleId") Long roleId);

    @Query("SELECT rp FROM RolePermission rp WHERE rp.id.permissionId = :permissionId")
    List<RolePermission> findAllByPermissionId(@Param("permissionId") Long permissionId);

    @Query(value = "SELECT p.code FROM permissions p " +
                   "JOIN role_permissions rp ON rp.permission_id = p.id " +
                   "JOIN roles r ON r.id = rp.role_id " +
                   "WHERE r.code = :roleCode",
           nativeQuery = true)
    List<String> findPermissionCodesByRoleCode(@Param("roleCode") String roleCode);
}
