package com.firomsa.maaedBackend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.firomsa.maaedBackend.model.Role;
import com.firomsa.maaedBackend.model.Roles;

@DataJpaTest
public class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    public void RoleRepository_SaveAndFindByName_ReturnsRole() {
        // Arrange
        Role role = Role.builder()
                .name(Roles.CUSTOMER)
                .build();

        // Act
        Role saved = roleRepository.save(role);

        var found = roleRepository.findByName(Roles.CUSTOMER);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo(Roles.CUSTOMER);
    }

    @Test
    public void RoleRepository_FindByName_ReturnsEmptyWhenNotFound() {
        // Act
        var found = roleRepository.findByName(Roles.ADMIN);

        // Assert
        assertThat(found).isNotPresent();
    }
}
