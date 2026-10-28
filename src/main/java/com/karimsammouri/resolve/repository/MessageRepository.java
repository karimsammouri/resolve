package com.karimsammouri.resolve.repository;

import com.karimsammouri.resolve.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
}
