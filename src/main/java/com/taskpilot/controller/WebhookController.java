package com.taskpilot.controller;

import com.google.gson.JsonSyntaxException;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import com.taskpilot.service.StripeWebhookService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhook")
public class WebhookController {

    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret}")
    private String endpointSecret;

    private final StripeWebhookService stripeWebhookService;

    public WebhookController(StripeWebhookService stripeWebhookService) {
        this.stripeWebhookService = stripeWebhookService;
    }

    /**
     * Initializes the Stripe API key when the application starts.
     */
    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
    }

    /**
     * Endpoint to receive and process webhook events from Stripe.
     *
     * @param payload   The raw JSON payload from the request body.
     * @param sigHeader The value of the 'Stripe-Signature' header.
     * @return An HTTP response (200 OK for success, 400 Bad Request for errors).
     */
    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event;

        // 1. Verify the event signature to ensure it came from Stripe
        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (JsonSyntaxException e) {
            logger.error("Webhook error: Invalid JSON payload.", e);
            return ResponseEntity.badRequest().body("Invalid payload");
        } catch (SignatureVerificationException e) {
            logger.error("Webhook error: Invalid signature.", e);
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        // 2. Pass the trusted event to the service for processing
        stripeWebhookService.handleStripeEvent(event);

        // 3. Return a 200 OK response to Stripe to acknowledge receipt
        return ResponseEntity.ok().build();
    }
}