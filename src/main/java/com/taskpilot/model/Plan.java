package com.taskpilot.model;

import jakarta.persistence.*;

import java.util.List;

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

    @Column(nullable = false)
    private int maxFileSize;

    /**
     * This allows a single plan (e.g., "Pro") to be associated with
     * multiple Stripe Price IDs (e.g., one for monthly, one for yearly).
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "plan_stripe_prices", joinColumns = @JoinColumn(name = "plan_id"))
    @Column(name = "stripe_price_id", nullable = false)
    private List<String> stripePriceIds;

    // Default constructor for JPA
    public Plan() {
    }

    public Plan(String name, int requestsPerMonth, int requestsPerDay, int maxFileSize, List<String> stripePriceIds) {
        this.name = name;
        this.requestsPerMonth = requestsPerMonth;
        this.requestsPerDay = requestsPerDay;
        this.maxFileSize = maxFileSize;
        this.stripePriceIds = stripePriceIds;
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

    public int getMaxFileSize() {
        return maxFileSize;
    }

    public List<String> getStripePriceIds() {
        return stripePriceIds;
    }

}