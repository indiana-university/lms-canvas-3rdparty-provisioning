package edu.iu.uits.lms.provisioning.controller;

import edu.iu.uits.lms.lti.security.LtiAuthenticationToken;
import edu.iu.uits.lms.provisioning.config.ToolConfig;
import edu.iu.uits.lms.lti.controller.LtiAuthenticationTokenAwareController;
import edu.iu.uits.lms.lti.security.LtiAuthenticationProvider;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@Log4j
public class ProvisioningController extends LtiAuthenticationTokenAwareController {

    @RequestMapping("/index")
    @Secured(LtiAuthenticationProvider.LTI_USER_ROLE)
    public ModelAndView index(Model model) {
        LtiAuthenticationToken token = getTokenWithoutContext();
        String name = (String)token.getPrincipal();
        model.addAttribute("name", name);
        return new ModelAndView("index");
    }

    @RequestMapping(value = "/accessDenied")
    public String accessDenied() {
        return "accessDenied";
    }
}
