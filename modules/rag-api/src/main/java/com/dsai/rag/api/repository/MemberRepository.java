package com.dsai.rag.api.repository;

import com.dsai.rag.api.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    
    Optional<Member> findByMemberCode(String memberCode);
    
    Optional<Member> findByMemberName(String memberName);
    
    List<Member> findByStatus(String status);
    
    Page<Member> findByStatus(String status, Pageable pageable);
    
    @Query("SELECT m FROM Member m WHERE m.memberName LIKE %:keyword% OR m.memberCode LIKE %:keyword%")
    List<Member> searchByKeyword(String keyword);
    
    @Query("SELECT COUNT(m) FROM Member m WHERE m.status = 'ACTIVE'")
    Long countActiveMembers();
    
    Long countByStatus(String status);
}