package edu.iu.uits.lms.provisioning.job;

import edu.iu.uits.lms.common.batch.BatchJob;
import edu.iu.uits.lms.iuonly.model.errorcontact.ErrorContactPostForm;
import edu.iu.uits.lms.iuonly.services.ErrorContactServiceImpl;
import edu.iu.uits.lms.provisioning.service.EmailSummaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@Profile("resultsemail")
public class ResultsEmailJob implements BatchJob {


    private EmailSummaryService emailSummaryService;
    private ConfigurableApplicationContext ctx;

    @Autowired
    private ResultsEmailJob job;

    @Autowired
    private ErrorContactServiceImpl errorContactService;

    public ResultsEmailJob(EmailSummaryService emailSummaryService, ConfigurableApplicationContext ctx) {
        this.emailSummaryService = emailSummaryService;
        this.ctx = ctx;
    }

    private void resultsEmail() throws IOException {
        log.info("ResultsEmail job running!");
        emailSummaryService.processResults();
    }

    @Override
    public void run() {

        try {
            job.resultsEmail();
        } catch (Exception e) {
            log.error("Caught exception performing results email", e);

            ErrorContactPostForm errorContactPostForm = new ErrorContactPostForm();
            errorContactPostForm.setJobCode(getJobCode());
            errorContactPostForm.setMessage("The Department Results Email job has unexpectedly failed");

            errorContactService.postEvent(errorContactPostForm);
        }

        ctx.close();
    }

    public String getJobCode() {
        return "DeptProvResultsEmailJob";
    }
}
