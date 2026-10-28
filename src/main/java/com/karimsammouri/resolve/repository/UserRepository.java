package com.karimsammouri.resolve.repository;

import com.karimsammouri.resolve.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
