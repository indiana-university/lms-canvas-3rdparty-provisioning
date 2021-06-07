package edu.iu.uits.lms.provisioning.controller;

import edu.iu.uits.lms.common.session.CourseSessionService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.controller.LtiAuthenticationTokenAwareController;
import edu.iu.uits.lms.lti.security.LtiAuthenticationToken;
import edu.iu.uits.lms.provisioning.config.BackgroundMessage;
import edu.iu.uits.lms.provisioning.config.BackgroundMessageSender;
import edu.iu.uits.lms.provisioning.model.DeptAuthMessageSender;
import edu.iu.uits.lms.provisioning.model.NotificationForm;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.repository.DeptAuthMessageSenderRepository;
import edu.iu.uits.lms.provisioning.repository.UserRepository;
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

import javax.servlet.http.HttpSession;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/app")
@Slf4j
public class ProvisioningController extends LtiAuthenticationTokenAwareController {

    private static final String SESSION_KEY = "uploadedInfo";

    @Autowired
    private UserRepository userRepository;

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
        LtiAuthenticationToken token = getTokenWithoutContext();
        String name = (String)token.getPrincipal();

        List<String> groups = (List<String>)session.getAttribute(Constants.AVAILABLE_GROUPS_KEY);

        model.addAttribute("groups", groups);
        model.addAttribute("name", name);
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
        LtiAuthenticationToken token = getTokenWithoutContext();
        model.addAttribute("selectedGroup", deptDropdown);

        try {
            MultiValuedMap<DeptRouter.CSV_TYPES, FileContent> filesByType = FileParsingUtil.parseFiles(files, customUsersNotification);

            Long archiveId = deptRouter.zipOriginals(filesByType.get(DeptRouter.CSV_TYPES.ORIGINALS), deptDropdown, (String)token.getPrincipal());
            String username = (String)token.getPrincipal();
            if (customUsersNotification) {
                courseSessionService.addAttributeToSession(session, token.getContext(), SESSION_KEY,
                      new BackgroundMessage(filesByType, deptDropdown, null, archiveId, username));
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
        LtiAuthenticationToken token = getTokenWithoutContext();
        BackgroundMessage storedData = courseSessionService.getAttributeFromSession(session, token.getContext(), SESSION_KEY, BackgroundMessage.class);

        processFiles(storedData.getFilesByType(), storedData.getDepartment(), notifForm, storedData.getArchiveId(),
              (String)token.getPrincipal());
        courseSessionService.removeAttributeFromSession(session, token.getContext(), SESSION_KEY);
        model.addAttribute("uploadSuccess", true);

        return index(model, session);
    }

    @PostMapping(value = "/submit", params = "action=cancel")
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public ModelAndView cancel(@ModelAttribute NotificationForm notifForm, Model model, HttpSession session) {
        LtiAuthenticationToken token = getTokenWithoutContext();
        session.removeAttribute(SESSION_KEY);
        return index(model, session);
    }

    private void processFiles(MultiValuedMap<DeptRouter.CSV_TYPES, FileContent> filesByType, String department,
                              NotificationForm notificationForm, Long archiveId, String username) {

        //Chuck a message into the queue so that the user doesn't have to wait for results
        backgroundMessageSender.send(new BackgroundMessage(filesByType, department, notificationForm, archiveId, username));
    }

}
