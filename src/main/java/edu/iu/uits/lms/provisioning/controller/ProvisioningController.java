package edu.iu.uits.lms.provisioning.controller;

import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.controller.LtiAuthenticationTokenAwareController;
import edu.iu.uits.lms.lti.security.LtiAuthenticationToken;
import edu.iu.uits.lms.provisioning.model.content.FileContent;
import edu.iu.uits.lms.provisioning.repository.UserRepository;
import edu.iu.uits.lms.provisioning.service.DeptRouter;
import edu.iu.uits.lms.provisioning.service.FileParsingException;
import lombok.extern.log4j.Log4j;
import org.apache.commons.collections4.MultiValuedMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Controller
@Log4j
public class ProvisioningController extends LtiAuthenticationTokenAwareController {

    @Autowired
    private UserRepository userRepository;

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

//        deptRouter.processFiles(deptDropdown, files, customUsersNotification);

        model.addAttribute("selectedGroup", deptDropdown);

        try {
            MultiValuedMap<DeptRouter.CSV_TYPES, FileContent> filesByType = deptRouter.parseFiles(files);
        } catch (FileParsingException e) {
            model.addAttribute("fileErrors", e.getFileErrors());
        }


        return new ModelAndView("index");
    }
}
