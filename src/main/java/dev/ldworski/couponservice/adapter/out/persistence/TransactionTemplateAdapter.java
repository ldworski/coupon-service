package dev.ldworski.couponservice.adapter.out.persistence;

import dev.ldworski.couponservice.domain.port.out.TransactionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class TransactionTemplateAdapter implements TransactionPort {

    private final TransactionTemplate transactionTemplate;

    public TransactionTemplateAdapter(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void inTransaction(Runnable operation) {
        transactionTemplate.executeWithoutResult(_ -> operation.run());
    }
}
