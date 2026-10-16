package cn.training.erp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name="erp.integrations.auto-dispatch",havingValue="true",matchIfMissing=true)
public class IntegrationScheduler {
    private final IntegrationService service;
    public IntegrationScheduler(IntegrationService service){this.service=service;}
    @Scheduled(initialDelay=20000,fixedDelay=15000) public void send(){
        try{service.runQueue();}catch(RuntimeException ignored){}
    }
}
