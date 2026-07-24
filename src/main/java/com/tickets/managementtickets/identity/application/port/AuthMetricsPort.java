package com.tickets.managementtickets.identity.application.port;

import com.tickets.managementtickets.identity.domain.model.Role;

public interface AuthMetricsPort {

    AuthMetricsPort NO_OP = new AuthMetricsPort() {
        @Override
        public void recordLoginSuccess(Role role) {
        }

        @Override
        public void recordLoginFailure(String reason) {
        }

        @Override
        public void recordRefreshSuccess(Role role) {
        }

        @Override
        public void recordRefreshFailure(String reason) {
        }

        @Override
        public void recordRateLimitBlocked() {
        }
    };

    void recordLoginSuccess(Role role);

    void recordLoginFailure(String reason);

    void recordRefreshSuccess(Role role);

    void recordRefreshFailure(String reason);

    void recordRateLimitBlocked();
}
