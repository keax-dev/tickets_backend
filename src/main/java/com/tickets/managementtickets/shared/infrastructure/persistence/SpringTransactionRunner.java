package com.tickets.managementtickets.shared.infrastructure.persistence;

import com.tickets.managementtickets.shared.application.port.TransactionRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
public class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate readOnlyTransactionTemplate;
    private final TransactionTemplate requiredTransactionTemplate;

    public SpringTransactionRunner(PlatformTransactionManager transactionManager) {
        this.readOnlyTransactionTemplate = new TransactionTemplate(transactionManager);
        this.readOnlyTransactionTemplate.setReadOnly(true);
        this.requiredTransactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T readOnly(Supplier<T> action) {
        return readOnlyTransactionTemplate.execute(status -> action.get());
    }

    @Override
    public <T> T required(Supplier<T> action) {
        return requiredTransactionTemplate.execute(status -> action.get());
    }
}
