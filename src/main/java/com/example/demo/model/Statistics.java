package com.example.demo.model;

import jakarta.persistence.*;

@Entity
@Table(name = "statistics")
public class Statistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private Integer adsBlocked = 0;

    @Column(nullable = false)
    private Integer trackersBlocked = 0;

    @Column(nullable = false)
    private Double timeSaved = 0.0; // in hours

    public Statistics() {}

    public Statistics(String userEmail) {
        this.userEmail = userEmail;
        this.adsBlocked = 0;
        this.trackersBlocked = 0;
        this.timeSaved = 0.0;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public Integer getAdsBlocked() { return adsBlocked; }
    public void setAdsBlocked(Integer adsBlocked) { this.adsBlocked = adsBlocked; }

    public Integer getTrackersBlocked() { return trackersBlocked; }
    public void setTrackersBlocked(Integer trackersBlocked) { this.trackersBlocked = trackersBlocked; }

    public Double getTimeSaved() { return timeSaved; }
    public void setTimeSaved(Double timeSaved) { this.timeSaved = timeSaved; }

    // Helper methods
    public void incrementAdsBlocked(int count) {
        this.adsBlocked += count;
        // Assume each ad takes 2 seconds to load
        this.timeSaved += (count * 2.0) / 3600.0; // Convert to hours
    }

    public void incrementTrackersBlocked(int count) {
        this.trackersBlocked += count;
        // Assume each tracker takes 0.5 seconds
        this.timeSaved += (count * 0.5) / 3600.0; // Convert to hours
    }
}