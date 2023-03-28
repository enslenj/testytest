package edu.ohsu.cmp.coach.controller;

import edu.ohsu.cmp.coach.entity.MyPatient;
import edu.ohsu.cmp.coach.exception.ConfigurationException;
import edu.ohsu.cmp.coach.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.coach.model.recommendation.Audience;
import edu.ohsu.cmp.coach.service.PatientService;
import edu.ohsu.cmp.coach.session.SessionService;
import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

@Controller
public class SessionController extends BaseController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private SessionService sessionService;

    @Autowired
    private PatientService patientService;

    @GetMapping("health")
    public String health() {
        return "health";
    }

    @GetMapping("launch-ehr")
    public String launchEHR(Model model) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("clientId", env.getProperty("smart.ehr.clientId"));
        model.addAttribute("scope", env.getProperty("smart.ehr.scope"));
        model.addAttribute("redirectUri", env.getProperty("smart.ehr.redirectUri"));
        return "launch-ehr";
    }

    @GetMapping("launch-patient")
    public String launchPatient(Model model) {
        model.addAttribute("applicationName", applicationName);
        model.addAttribute("clientId", env.getProperty("smart.patient.clientId"));
        model.addAttribute("scope", env.getProperty("smart.patient.scope"));
        model.addAttribute("redirectUri", env.getProperty("smart.patient.redirectUri"));
        model.addAttribute("iss", env.getProperty("smart.patient.iss"));
        return "launch-patient";
    }

    @PostMapping("prepare-session")
    public ResponseEntity<?> prepareSession(HttpSession session,
                                            @RequestParam String clientId,
                                            @RequestParam String serverUrl,
                                            @RequestParam String bearerToken,
                                            @RequestParam String patientId,
                                            @RequestParam String userId,
                                            @RequestParam("audience") String audienceStr) throws ConfigurationException {

        FHIRCredentials credentials = new FHIRCredentials(clientId, serverUrl, bearerToken, patientId, userId);
        Audience audience = Audience.fromTag(audienceStr);

        MyPatient myPatient = patientService.getMyPatient(patientId);

        if (myPatient.getConsentGranted()) {
            sessionService.prepareSession(session.getId(), credentials, audience);

            return ResponseEntity.ok("session configured successfully");

        } else {
            // consent hasn't been granted yet.  cache session data somewhere well-segregated from
            // the UserWorkspace, a UserWorkspace must only be set up for authorized users.
            // HomeController.view will handle the next step of this workflow
            sessionService.prepareProvisionalSession(session.getId(), credentials, audience);

            return ResponseEntity.ok("session provisionally established - CONSENT REQUIRED");
        }
    }

    @GetMapping("logout")
    public String logout(HttpSession session) {
        userWorkspaceService.shutdown(session.getId());
        return "logout";
    }

    @PostMapping("clear-session")
    public ResponseEntity<?> clearSession(HttpSession session) {
        userWorkspaceService.shutdown(session.getId());
        return ResponseEntity.ok("session cleared");
    }

    @PostMapping("refresh")
    public ResponseEntity<?> refresh(HttpSession session) {
        logger.info("refreshing data for session=" + session.getId());
        UserWorkspace workspace = userWorkspaceService.get(session.getId());
        workspace.clearCaches();
        workspace.populate();
        return ResponseEntity.ok("refreshing");
    }

    @PostMapping("clear-supplemental-data")
    public ResponseEntity<?> clearSupplementalData(HttpSession session) {
        logger.info("clearing supplemental data for session=" + session.getId());
        UserWorkspace workspace = userWorkspaceService.get(session.getId());
        workspace.clearSupplementalData();
        return ResponseEntity.ok("supplemental data cleared");
    }
}
