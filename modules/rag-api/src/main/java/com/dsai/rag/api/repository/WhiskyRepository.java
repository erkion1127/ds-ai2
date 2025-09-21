package com.dsai.rag.api.repository;

import com.dsai.rag.api.entity.Whisky;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhiskyRepository extends JpaRepository<Whisky, Long> {

    List<Whisky> findByTypeOrderByRatingDesc(String type);

    List<Whisky> findByRegionOrderByRatingDesc(String region);

    List<Whisky> findByPriceRangeLessThanEqualOrderByRatingDesc(Integer priceRange);

    List<Whisky> findByFlavorProfileContainingIgnoreCase(String flavor);

    List<Whisky> findByOccasionContainingIgnoreCase(String occasion);

    @Query("SELECT w FROM Whisky w WHERE w.age >= :minAge AND w.age <= :maxAge ORDER BY w.rating DESC")
    List<Whisky> findByAgeRange(@Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge);

    @Query("SELECT w FROM Whisky w WHERE " +
           "LOWER(w.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(w.brand) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(w.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(w.tastingNotes) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Whisky> searchWhiskies(@Param("keyword") String keyword);

    List<Whisky> findTop10ByOrderByRatingDesc();

    List<Whisky> findByIsAvailableTrueOrderByRatingDesc();
}