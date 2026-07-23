package com.tickets.managementtickets.ticket.application.service;

import com.tickets.managementtickets.shared.application.exception.ConflictException;
import com.tickets.managementtickets.shared.application.exception.ValidationException;
import com.tickets.managementtickets.shared.application.port.HashingService;
import com.tickets.managementtickets.shared.application.port.JsonCodec;
import com.tickets.managementtickets.ticket.application.port.IdempotencyPolicy;
import com.tickets.managementtickets.ticket.application.port.IdempotencyRecordRepositoryPort;
import com.tickets.managementtickets.ticket.domain.model.IdempotencyRecord;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

final class TicketIdempotencyHandler {

    private final IdempotencyRecordRepositoryPort idempotencyRecordRepository;
    private final HashingService hashingService;
    private final JsonCodec jsonCodec;
    private final IdempotencyPolicy idempotencyPolicy;
    private final Clock clock;

    TicketIdempotencyHandler(
        IdempotencyRecordRepositoryPort idempotencyRecordRepository,
        HashingService hashingService,
        JsonCodec jsonCodec,
        IdempotencyPolicy idempotencyPolicy,
        Clock clock
    ) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.hashingService = hashingService;
        this.jsonCodec = jsonCodec;
        this.idempotencyPolicy = idempotencyPolicy;
        this.clock = clock;
    }

    void requireKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ValidationException("IDEMPOTENCY_KEY_REQUIRED", "The idempotency key is required.");
        }
    }

    String hashCreateRequest(String userId, TicketService.CreateTicketRequest request) {
        return hashingService.hash(
            userId + "|"
                + normalize(request.title()) + "|"
                + normalize(request.description()) + "|"
                + request.categoryId() + "|"
                + request.priority().name()
        );
    }

    Optional<TicketService.TicketDetailResponse> findStoredCreateResponse(
        String idempotencyKey,
        String userId,
        String requestHash
    ) {
        Optional<IdempotencyRecord> existingRecord = idempotencyRecordRepository.findByIdempotencyKeyAndUserId(idempotencyKey, userId);
        if (existingRecord.isEmpty()) {
            return Optional.empty();
        }

        IdempotencyRecord record = existingRecord.get();
        if (!record.requestHash().equals(requestHash)) {
            throw new ConflictException("IDEMPOTENCY_KEY_CONFLICT", "The idempotency key was already used with a different payload.");
        }
        return Optional.of(jsonCodec.deserialize(record.responseBody(), TicketService.TicketDetailResponse.class));
    }

    void storeCreateResponse(
        String idempotencyKey,
        String userId,
        String requestHash,
        String resourceId,
        TicketService.TicketDetailResponse response
    ) {
        idempotencyRecordRepository.save(IdempotencyRecord.create(
            idempotencyKey,
            userId,
            requestHash,
            201,
            jsonCodec.serialize(response),
            resourceId,
            clock.instant().plus(idempotencyPolicy.getRecordTtlHours(), ChronoUnit.HOURS)
        ));
    }

    void purgeExpiredRecords() {
        idempotencyRecordRepository.deleteByExpiresAtBefore(clock.instant());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
