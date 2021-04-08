package edu.iu.uits.lms.provisioning.job;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Profile("(!integration-tests & batch)")
@EnableScheduling
public class BatchJobRunner implements ApplicationContextAware, CommandLineRunner {

    @Autowired
    private ScheduledExecutorService executorService;
    @Autowired
    private List<BatchJob> jobs;
    private ConfigurableApplicationContext applicationContext;

    @Override
    public void run(String... args) throws Exception {
        for(BatchJob job : this.jobs) {
            this.executorService.execute(job);
        }
        this.executorService.shutdown();
        this.executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        applicationContext.close();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = (ConfigurableApplicationContext)applicationContext;
    }
}
