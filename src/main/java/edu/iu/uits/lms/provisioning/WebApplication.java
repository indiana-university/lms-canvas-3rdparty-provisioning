package edu.iu.uits.lms.provisioning;

import edu.iu.uits.lms.canvas.config.EnableCanvasClient;
import edu.iu.uits.lms.common.batch.EnableBatch;
import edu.iu.uits.lms.common.samesite.EnableCookieFilter;
import edu.iu.uits.lms.common.server.GitRepositoryState;
import edu.iu.uits.lms.common.server.ServerInfo;
import edu.iu.uits.lms.common.server.ServerUtils;
import edu.iu.uits.lms.common.session.EnableCourseSessionService;
import edu.iu.uits.lms.email.config.EnableEmailClient;
import edu.iu.uits.lms.iuonly.config.EnableIuOnlyClient;
import edu.iu.uits.lms.lti.config.EnableGlobalErrorHandler;
import edu.iu.uits.lms.lti.config.EnableLtiClient;
import edu.iu.uits.lms.provisioning.config.ToolConfig;
import edu.iu.uits.lms.redis.config.EnableRedisConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Date;

@SpringBootApplication
@EnableGlobalErrorHandler
@Slf4j
@EnableRedisConfiguration
@EnableCookieFilter(ignoredRequestPatterns = {"/rest/**"})
@EnableLtiClient(toolKeys = {"lms_lti_3rdpartyprovisioning"})
@EnableCanvasClient
@EnableEmailClient
@EnableIuOnlyClient
@EnableCourseSessionService(sessionKey = "deptprov_course_session")
@EnableConfigurationProperties(GitRepositoryState.class)
@EnableBatch
public class WebApplication {

    @Autowired
    private ToolConfig toolConfig;

    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }

    @Autowired
    private GitRepositoryState gitRepositoryState;

    @Bean(name = ServerInfo.BEAN_NAME)
    ServerInfo serverInfo() {
        return ServerInfo.builder()
              .serverName(ServerUtils.getServerHostName())
              .environment(toolConfig.getEnv())
              .buildDate(new Date())
              .gitInfo(gitRepositoryState.getBranch() + "@" + gitRepositoryState.getCommitIdAbbrev())
              .artifactVersion(toolConfig.getVersion()).build();
    }

}
