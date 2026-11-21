package dev.ldworski.couponservice.domain.port.out;

public interface TransactionPort {

    void inTransaction(Runnable operation);
}
