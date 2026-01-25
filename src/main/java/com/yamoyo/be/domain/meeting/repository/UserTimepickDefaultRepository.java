package com.yamoyo.be.domain.meeting.repository;

import com.yamoyo.be.domain.meeting.entity.UserTimepickDefault;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserTimepickDefaultRepository extends JpaRepository<UserTimepickDefault, Long> {
    Optional<UserTimepickDefault> findByUserId(Long userId);
}
