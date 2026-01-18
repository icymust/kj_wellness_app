package com.ndl.numbers_dont_lie.repository.nutrition;
import com.ndl.numbers_dont_lie.entity.nutrition.NutritionalPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface NutritionalPreferencesRepository extends JpaRepository<NutritionalPreferences, Long> {
    Optional<NutritionalPreferences> findByUserId(Long userId);
}
