package edu.iu.uits.lms.provisioning.controller;

import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.iuonly.services.DeptProvisioningUserServiceImpl;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.controller.OidcTokenAwareController;
import edu.iu.uits.lms.lti.service.OidcTokenUtils;
import edu.iu.uits.lms.provisioning.config.BackgroundMessage;
import edu.iu.uits.lms.provisioning.config.BackgroundMessageSender;
import edu.iu.uits.lms.provisioning.model.DeptAuthMessageSender;
import edu.iu.uits.lms.provisioning.model.NotificationForm;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.repository.DeptAuthMessageSenderRepository;
import edu.iu.uits.lms.provisioning.service.DeptRouter;
import edu.iu.uits.lms.provisioning.service.FileParsingUtil;
import edu.iu.uits.lms.provisioning.service.exception.FileParsingException;
import edu.iu.uits.lms.provisioning.service.exception.ZipException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.KeyValue;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import javax.servlet.http.HttpSession;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/app")
@Slf4j
public class ProvisioningController extends OidcTokenAwareController {

    private static final String SESSION_KEY = "uploadedInfo";

    @Autowired
    private DeptProvisioningUserServiceImpl deptProvisioningUserService;

    @Autowired
    private DeptAuthMessageSenderRepository deptAuthMessageSenderRepository;

    @Autowired
    private BackgroundMessageSender backgroundMessageSender;

    @Autowired
    private DeptRouter deptRouter;

    @Autowired
    private CourseSessionService courseSessionService;

    @RequestMapping("/index")
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public ModelAndView index(Model model, HttpSession session) {
        log.debug("/index");
        OidcAuthenticationToken token = getTokenWithoutContext();
        OidcTokenUtils tokenUtils = new OidcTokenUtils(token);
        String[] groups = tokenUtils.getCustomArray(Constants.AVAILABLE_GROUPS_KEY);
        if (groups != null) {
            Arrays.sort(groups);
        }
        model.addAttribute("groups", groups);
        return new ModelAndView("index");
    }

    @RequestMapping(value = "/accessDenied")
    public String accessDenied() {
        return "accessDenied";
    }

    @PostMapping("/upload")
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public ModelAndView upload(@RequestParam("deptDropdown") String deptDropdown,
                               @RequestParam("deptFileUpload") MultipartFile[] files,
                               @RequestParam(value = "customUsersNotification", required = false) boolean customUsersNotification,
                               Model model, HttpSession session) {
        log.debug("/upload");
        OidcAuthenticationToken token = getTokenWithoutContext();
        model.addAttribute("selectedGroup", deptDropdown);

        try {
            MultiValuedMap<DeptRouter.CSV_TYPES, FileContent> filesByType = FileParsingUtil.parseFiles(files, customUsersNotification);

            OidcTokenUtils tokenUtils = new OidcTokenUtils(token);
            String username = tokenUtils.getUserLoginId();

            Long archiveId = deptRouter.zipOriginals(filesByType.get(DeptRouter.CSV_TYPES.ORIGINALS), deptDropdown, username);

            if (customUsersNotification) {
                courseSessionService.addAttributeToSession(session, tokenUtils.getCourseId(), SESSION_KEY,
                      new BackgroundMessage(filesByType, deptDropdown, null, archiveId, username, Constants.SOURCE.APP));
                return notify(deptDropdown, model);
            }

            processFiles(filesByType, deptDropdown, null, archiveId, username);

            model.addAttribute("uploadSuccess", true);

        } catch (FileParsingException | ZipException e) {
            model.addAttribute("fileErrors", e.getFileErrors());
            model.addAttribute("checkedNotification", customUsersNotification);
        }

        return index(model, session);
    }

    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    private ModelAndView notify(String dept, Model model) {
        log.debug("notify");
        List<DeptAuthMessageSender> senders = deptAuthMessageSenderRepository.findByGroupCodeIgnoreCase(dept);
        String pattern = "{0} ({1})";

        List<KeyValue<String, String>> availableSenders = senders.stream()
              .map(sender -> new DefaultKeyValue<>(sender.getEmail(), MessageFormat.format(pattern, sender.getName(), sender.getEmail())))
              .collect(Collectors.toList());

        model.addAttribute("senders", availableSenders);
        model.addAttribute("notifForm", new NotificationForm());

        return new ModelAndView("notification");
    }

    @PostMapping("/submit")
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public ModelAndView submitNotification(@ModelAttribute NotificationForm notifForm, Model model, HttpSession session) {
        log.debug("/submit");
        OidcAuthenticationToken token = getTokenWithoutContext();
        OidcTokenUtils tokenUtils = new OidcTokenUtils(token);
        String username = tokenUtils.getUserLoginId();
        BackgroundMessage storedData = courseSessionService.getAttributeFromSession(session, tokenUtils.getCourseId(), SESSION_KEY, BackgroundMessage.class);

        processFiles(storedData.getFilesByType(), storedData.getDepartment(), notifForm, storedData.getArchiveId(), username);
        courseSessionService.removeAttributeFromSession(session, tokenUtils.getCourseId(), SESSION_KEY);
        model.addAttribute("uploadSuccess", true);

        return index(model, session);
    }

    @PostMapping(value = "/submit", params = "action=cancel")
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public ModelAndView cancel(@ModelAttribute NotificationForm notifForm, Model model, HttpSession session) {
        OidcAuthenticationToken token = getTokenWithoutContext();
        session.removeAttribute(SESSION_KEY);
        return index(model, session);
    }

    private void processFiles(MultiValuedMap<DeptRouter.CSV_TYPES, FileContent> filesByType, String department,
                              NotificationForm notificationForm, Long archiveId, String username) {

        //Chuck a message into the queue so that the user doesn't have to wait for results
        backgroundMessageSender.send(new BackgroundMessage(filesByType, department, notificationForm, archiveId, username, Constants.SOURCE.APP));
    }

}
