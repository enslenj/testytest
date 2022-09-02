package edu.ohsu.cmp.coach.service;

import edu.ohsu.cmp.coach.workspace.UserWorkspace;
import edu.ohsu.cmp.coach.entity.app.HomeBloodPressureReading;
import edu.ohsu.cmp.coach.repository.app.HomeBloodPressureReadingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class HomeBloodPressureReadingService extends AbstractService {

    @Autowired
    private HomeBloodPressureReadingRepository repository;

    public List<HomeBloodPressureReading> getHomeBloodPressureReadings(String sessionId) {
        UserWorkspace workspace = workspaceService.get(sessionId);
        return repository.findAllByPatId(workspace.getInternalPatientId());
    }

    public HomeBloodPressureReading create(String sessionId, HomeBloodPressureReading bpreading) {
        UserWorkspace workspace = workspaceService.get(sessionId);
        bpreading.setPatId(workspace.getInternalPatientId());
        bpreading.setCreatedDate(new Date());
        return repository.save(bpreading);
    }

    public void delete(String sessionId, Long id) {
        UserWorkspace workspace = workspaceService.get(sessionId);
        repository.deleteByIdForPatient(id, workspace.getInternalPatientId());
    }

    public void deleteAll(String sessionId) {
        UserWorkspace workspace = workspaceService.get(sessionId);
        repository.deleteAllByPatId(workspace.getInternalPatientId());
    }
}
