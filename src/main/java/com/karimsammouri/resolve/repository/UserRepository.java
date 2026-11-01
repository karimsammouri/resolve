package com.karimsammouri.resolve.repository;

import com.karimsammouri.resolve.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByIdAndOrganization_Id(Long userId, Long organizationId);
    Page<User> findByOrganization_Id(Long organizationId, Pageable pageable);
    boolean existsByEmail(String email);
}
