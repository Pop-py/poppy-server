package com.poppy.domain.user.repository;

import com.poppy.domain.user.entity.Role;
import com.poppy.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {
    Optional<User> findByEmail(String email);
    Boolean existsByNickname(String nickname);
    List<User> findByRole(Role role);
}
