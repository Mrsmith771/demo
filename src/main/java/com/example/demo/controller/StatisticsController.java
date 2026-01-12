package com.example.demo.controller;

import com.example.demo.model.Statistics;
import com.example.demo.repository.StatisticsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/stats")
public class StatisticsController {

    private final StatisticsRepository statisticsRepository;

    public StatisticsController(StatisticsRepository statisticsRepository) {
        this.statisticsRepository = statisticsRepository;
    }

    // Get user statistics
    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatistics(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String email = auth.getName();
        Optional<Statistics> statsOpt = statisticsRepository.findByUserEmail(email);

        if (statsOpt.isEmpty()) {
            // Create new statistics for user
            Statistics newStats = new Statistics(email);
            statisticsRepository.save(newStats);
            statsOpt = Optional.of(newStats);
        }

        Statistics stats = statsOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("adsBlocked", stats.getAdsBlocked());
        response.put("trackersBlocked", stats.getTrackersBlocked());
        response.put("timeSaved", String.format("%.1fh", stats.getTimeSaved()));

        return ResponseEntity.ok(response);
    }

    // Sync statistics from extension
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncStatistics(
            @RequestBody Map<String, Object> data,
            Authentication auth) {

        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String email = auth.getName();

        // Get or create statistics
        Statistics stats = statisticsRepository.findByUserEmail(email)
                .orElse(new Statistics(email));

        // Update statistics
        if (data.containsKey("adsBlocked")) {
            int adsBlocked = (Integer) data.get("adsBlocked");
            stats.setAdsBlocked(adsBlocked);
        }

        if (data.containsKey("trackersBlocked")) {
            int trackersBlocked = (Integer) data.get("trackersBlocked");
            stats.setTrackersBlocked(trackersBlocked);
        }

        // Recalculate time saved
        double timeSaved = (stats.getAdsBlocked() * 2.0 + stats.getTrackersBlocked() * 0.5) / 3600.0;
        stats.setTimeSaved(timeSaved);

        statisticsRepository.save(stats);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Statistics synced successfully");
        response.put("adsBlocked", stats.getAdsBlocked());
        response.put("trackersBlocked", stats.getTrackersBlocked());
        response.put("timeSaved", String.format("%.1fh", stats.getTimeSaved()));

        return ResponseEntity.ok(response);
    }

    // Increment ads blocked (called from extension)
    @PostMapping("/increment/ads")
    public ResponseEntity<Map<String, Object>> incrementAds(
            @RequestParam String email,
            @RequestParam(defaultValue = "1") int count) {

        Statistics stats = statisticsRepository.findByUserEmail(email)
                .orElse(new Statistics(email));

        stats.incrementAdsBlocked(count);
        statisticsRepository.save(stats);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Ads blocked count updated");
        response.put("adsBlocked", stats.getAdsBlocked());

        return ResponseEntity.ok(response);
    }

    // Increment trackers blocked
    @PostMapping("/increment/trackers")
    public ResponseEntity<Map<String, Object>> incrementTrackers(
            @RequestParam String email,
            @RequestParam(defaultValue = "1") int count) {

        Statistics stats = statisticsRepository.findByUserEmail(email)
                .orElse(new Statistics(email));

        stats.incrementTrackersBlocked(count);
        statisticsRepository.save(stats);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Trackers blocked count updated");
        response.put("trackersBlocked", stats.getTrackersBlocked());

        return ResponseEntity.ok(response);
    }
}
