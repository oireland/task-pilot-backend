package com.taskpilot.model;

import jakarta.persistence.*;

@Entity
@Table(name = "plans")
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private int requestsPerMonth;

    @Column(nullable = false)
    private int requestsPerDay;

    // Default constructor for JPA
    public Plan() {
    }

    public Plan(String name, int requestsPerMonth, int requestsPerDay) {
        this.name = name;
        this.requestsPerMonth = requestsPerMonth;
        this.requestsPerDay = requestsPerDay;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getRequestsPerMonth() {
        return requestsPerMonth;
    }

    public int getRequestsPerDay() {
        return requestsPerDay;
    }
}