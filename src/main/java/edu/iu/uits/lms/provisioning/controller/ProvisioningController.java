package edu.iu.uits.lms.provisioning.controller;

import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.controller.LtiAuthenticationTokenAwareController;
import edu.iu.uits.lms.lti.security.LtiAuthenticationToken;
import edu.iu.uits.lms.provisioning.model.DeptAuthMessageSender;
import edu.iu.uits.lms.provisioning.model.NotificationForm;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.repository.DeptAuthMessageSenderRepository;
import edu.iu.uits.lms.provisioning.repository.UserRepository;
import edu.iu.uits.lms.provisioning.service.DeptRouter;
import edu.iu.uits.lms.provisioning.service.ProvisioningResult;
import edu.iu.uits.lms.provisioning.service.exception.FileParsingException;
import edu.iu.uits.lms.provisioning.service.exception.FileProcessingException;
import edu.iu.uits.lms.provisioning.service.exception.FileUploadException;
import lombok.AllArgsConstructor;
import lombok.Data;
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

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class ProvisioningController extends LtiAuthenticationTokenAwareController {

    private static final String SESSION_KEY = "uploadedInfo";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeptAuthMessageSenderRepository deptAuthMessageSenderRepository;

    @Autowired
    private DeptRouter deptRouter;

    @RequestMapping("/index")
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public ModelAndView index(Model model) {
        LtiAuthenticationToken token = getTokenWithoutContext();
        String name = (String)token.getPrincipal();

        List<String> groups = (List<String>)token.getData().get(Constants.AVAILABLE_GROUPS_KEY);

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
                               Model model) {
        LtiAuthenticationToken token = getTokenWithoutContext();
        model.addAttribute("selectedGroup", deptDropdown);

        try {
            MultiValuedMap<DeptRouter.CSV_TYPES, FileContent> filesByType = deptRouter.parseFiles(files, customUsersNotification);

            if (customUsersNotification) {
                token.setData(SESSION_KEY, new SessionData(filesByType, deptDropdown, customUsersNotification, null));
                return notify(deptDropdown, model);
            }

            processFiles(filesByType, deptDropdown, null);

            model.addAttribute("uploadSuccess", true);

        } catch (FileParsingException | FileProcessingException | FileUploadException e) {
            model.addAttribute("fileErrors", e.getFileErrors());
        }

        return index(model);
    }

    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    private ModelAndView notify(String dept, Model model) {

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
    public ModelAndView submitNotification(@ModelAttribute NotificationForm notifForm, Model model) {
        LtiAuthenticationToken token = getTokenWithoutContext();
        SessionData storedData = (SessionData)token.getData().get(SESSION_KEY);

        try {
            processFiles(storedData.getFilesByType(), storedData.getDepartment(), notifForm);
            token.clearData(SESSION_KEY);
            model.addAttribute("uploadSuccess", true);
        } catch (FileProcessingException | FileUploadException e) {
            model.addAttribute("fileErrors", e.getFileErrors());
        }
        return index(model);
    }

    @PostMapping(value = "/submit", params = "action=cancel")
    @Secured(LTIConstants.INSTRUCTOR_AUTHORITY)
    public ModelAndView cancel(@ModelAttribute NotificationForm notifForm, Model model) {
        LtiAuthenticationToken token = getTokenWithoutContext();
        token.clearData(SESSION_KEY);
        return index(model);
    }

    private void processFiles(MultiValuedMap<DeptRouter.CSV_TYPES, FileContent> filesByType, String department,
                              NotificationForm notificationForm) throws FileUploadException, FileProcessingException {
        List<ProvisioningResult> provisioningResults = deptRouter.processFiles(department, filesByType, notificationForm);

        List<ProvisioningResult.FileObject> allFiles = new ArrayList<>();
        StringBuilder fullEmail = new StringBuilder();
        for (ProvisioningResult provisioningResult : provisioningResults) {
            ProvisioningResult.FileObject fileObject = provisioningResult.getFileObject();
            if (fileObject != null) {
                allFiles.add(fileObject);
            }
            fullEmail.append(provisioningResult.getEmailMessage() + "\r\n");
        }

        deptRouter.sendToCanvas(allFiles, department, fullEmail);
    }

    @Data
    @AllArgsConstructor
    private static class SessionData implements Serializable {
        private MultiValuedMap<DeptRouter.CSV_TYPES, FileContent> filesByType;
        private String department;
        private boolean customUsersNotification;
        private NotificationForm notificationForm;
    }

}
