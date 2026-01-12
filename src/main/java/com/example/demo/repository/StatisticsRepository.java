package com.example.demo.repository;

import com.example.demo.model.Statistics;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StatisticsRepository extends JpaRepository<Statistics, Long> {
    Optional<Statistics> findByUserEmail(String userEmail);
}