package com.rajesh.milansetu.repository;

import com.rajesh.milansetu.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    @Query("select m from Member m join fetch m.event join fetch m.guardian where m.qrToken = :token and m.qrRevoked = false")
    Optional<Member> findActiveByToken(@Param("token") String token);

    @Query("select m from Member m join fetch m.event where m.guardian.id = :guardianId order by m.id")
    List<Member> findForGuardian(@Param("guardianId") Long guardianId);

    @Query("select m from Member m join fetch m.event join fetch m.guardian where m.id = :id")
    Optional<Member> findWithDetails(@Param("id") Long id);
}
