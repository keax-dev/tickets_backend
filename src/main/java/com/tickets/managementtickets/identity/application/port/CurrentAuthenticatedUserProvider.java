package com.tickets.managementtickets.identity.application.port;

import com.tickets.managementtickets.identity.application.model.AuthenticatedUser;

public interface CurrentAuthenticatedUserProvider {

    AuthenticatedUser requireCurrentUser();
}
