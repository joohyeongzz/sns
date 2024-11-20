package com.joohyeong.sns.global.config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ReplicationRoutingDataSource extends AbstractRoutingDataSource {

    private int slaveCount;
    private AtomicInteger counter = new AtomicInteger(0);

    public ReplicationRoutingDataSource(int slaveCount) {
        this.slaveCount = slaveCount;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();

        if (isReadOnly && slaveCount > 0) {
            int idx = counter.getAndIncrement() % slaveCount;
            log.debug("Routing to slave{}", idx);
            return "slave" + idx;
        }
        log.debug("Routing to master");
        return "master";
    }
}

