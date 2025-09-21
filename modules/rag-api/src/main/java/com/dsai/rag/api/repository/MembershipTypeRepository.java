package com.dsai.rag.api.repository;

import com.dsai.rag.api.entity.MembershipType;
import com.dsai.rag.api.entity.MembershipType.MembershipCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipTypeRepository extends JpaRepository<MembershipType, Long> {
    
    Optional<MembershipType> findByTypeCode(String typeCode);
    
    Optional<MembershipType> findByTypeName(String typeName);
    
    List<MembershipType> findByCategory(MembershipCategory category);
    
    List<MembershipType> findByIsActiveTrue();
    
    List<MembershipType> findByCategoryAndIsActiveTrue(MembershipCategory category);
    
    @Query("SELECT mt FROM MembershipType mt WHERE mt.sessionCount IS NOT NULL AND mt.isActive = true")
    List<MembershipType> findSessionBasedTypes();
    
    @Query("SELECT mt FROM MembershipType mt WHERE mt.durationMonths IS NOT NULL AND mt.isActive = true")
    List<MembershipType> findDurationBasedTypes();
    
    Long countByIsActiveTrue();
}