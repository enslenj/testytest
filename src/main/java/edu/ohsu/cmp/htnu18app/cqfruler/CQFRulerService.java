package edu.ohsu.cmp.htnu18app.cqfruler;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.cqfruler.model.*;
import edu.ohsu.cmp.htnu18app.entity.HomeBloodPressureReading;
import edu.ohsu.cmp.htnu18app.model.BloodPressureModel;
import edu.ohsu.cmp.htnu18app.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.htnu18app.service.HomeBloodPressureReadingService;
import edu.ohsu.cmp.htnu18app.service.PatientService;
import edu.ohsu.cmp.htnu18app.util.HttpUtil;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

@Service
public class CQFRulerService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private String cdsHooksEndpointURL;

    @Autowired
    private PatientService patientService;

    @Autowired
    private HomeBloodPressureReadingService hbprService;

    public CQFRulerService(@Value("${cqfruler.cdshooks.endpoint.url}") String cdsHooksEndpointURL) {
        this.cdsHooksEndpointURL = cdsHooksEndpointURL;
    }

    public List<CDSHook> getCDSHooks() throws IOException {
        logger.info("getting " + cdsHooksEndpointURL);

        String json = HttpUtil.get(cdsHooksEndpointURL);

// json object for testing w/o CQF ruler call
//        String json = "{  \"services\": [    {      \"hook\": \"patient-view\",      \"name\": \"Hypertension\",      \"title\": \"OHSU Hypertension\",      \"description\": \"This PlanDefinition identifies hypertension\",      \"id\": \"plandefinition-Hypertension\",      \"prefetch\": {        \"item1\": \"Patient?_id\\u003d{{context.patientId}}\",        \"item2\": \"Observation?subject\\u003dPatient/{{context.patientId}}\\u0026code\\u003dhttp://loinc.org|55284-4\",        \"item3\": \"Encounter?patient\\u003dPatient/{{context.patientId}}\",        \"item4\": \"Condition?patient\\u003dPatient/{{context.patientId}}\\u0026code\\u003dhttp://snomed.info/sct|111438007,http://snomed.info/sct|123799005,http://snomed.info/sct|123800009,http://snomed.info/sct|14973001,http://snomed.info/sct|169465000,http://snomed.info/sct|194783001,http://snomed.info/sct|194785008,http://snomed.info/sct|194788005,http://snomed.info/sct|194791005,http://snomed.info/sct|199008003,http://snomed.info/sct|26078007,http://snomed.info/sct|28119000,http://snomed.info/sct|31992008,http://snomed.info/sct|39018007,http://snomed.info/sct|39727004,http://snomed.info/sct|427889009,http://snomed.info/sct|428575007,http://snomed.info/sct|48552006,http://snomed.info/sct|57684003,http://snomed.info/sct|73410007,http://snomed.info/sct|74451002,http://snomed.info/sct|89242004\",        \"item5\": \"Procedure?patient\\u003dPatient/{{context.patientId}}\\u0026code\\u003dhttp://snomed.info/sct|164783007,http://snomed.info/sct|413153004,http://snomed.info/sct|448489007,http://snomed.info/sct|448678005,http://www.ama-assn.org/go/cpt|93784,http://www.ama-assn.org/go/cpt|93786,http://www.ama-assn.org/go/cpt|93788,http://www.ama-assn.org/go/cpt|93790\"      }    }  ]}\n";

        Gson gson = new GsonBuilder().create();
        CDSServices services = gson.fromJson(json, new TypeToken<CDSServices>(){}.getType());

        return services.getHooks();
    }

    public List<Card> executeHook(String sessionId, String hookId) throws IOException {
        CacheData cache = SessionCache.getInstance().get(sessionId);
        if ( ! cache.containsCards(hookId) ) {
            Patient p = patientService.getPatient(sessionId);

            Bundle bpBundle = patientService.getBloodPressureObservations(sessionId);

            // inject home blood pressure readings into Bundle for evaluation by CQF Ruler
            List<HomeBloodPressureReading> hbprList = hbprService.getHomeBloodPressureReadings(sessionId);
            for (HomeBloodPressureReading item : hbprList) {
                String uuid = UUID.randomUUID().toString();

                Encounter e = buildEncounter(uuid, p.getId(), item.getReadingDate());
                bpBundle.addEntry().setFullUrl("http://hl7.org/fhir/Encounter/" + e.getId()).setResource(e);

                Observation o = buildObservation(uuid, p.getId(), e.getId(), item);

                // todo: should the URL be different?
                bpBundle.addEntry().setFullUrl("http://hl7.org/fhir/Observation/" + o.getId()).setResource(o);
            }

            FHIRCredentialsWithClient fcc = cache.getFhirCredentialsWithClient();

            HookRequest request = new HookRequest(fcc.getCredentials(), p, bpBundle);

            MustacheFactory mf = new DefaultMustacheFactory();
            Mustache mustache = mf.compile("cqfruler/hookRequest.mustache");
            StringWriter writer = new StringWriter();
            mustache.execute(writer, request).flush();

            logger.info("hookRequest = " + writer.toString());

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Content-Type", "application/json; charset=UTF-8");

            String json = HttpUtil.post(cdsHooksEndpointURL + "/" + hookId, headers, writer.toString());

// json object for testing w/o CQF ruler call
//            String json = "{  \"cards\": [    {      \"summary\": \"Hypertension Diagnosis\",      \"indicator\": \"info\",      \"source\": {        \"label\": \"Info for those with normal blood pressure\",        \"url\": \"https://en.wikipedia.org/wiki/Blood_pressure\"      }    },    {      \"summary\": \"Patient may have Stage 1 Hypertension.\",      \"indicator\": \"warning\",      \"detail\": \"Consider diagnosis of stage 1 HTN.\",      \"source\": {}    }  ]}";

            Gson gson = new GsonBuilder().create();
            CDSHookResponse response = gson.fromJson(json, new TypeToken<CDSHookResponse>(){}.getType());

            cache.setCards(hookId, response.getCards());
        }

        return cache.getCards(hookId);
    }

////////////////////////////////////////////////////////////////////////
// private methods
//

    private Encounter buildEncounter(String uuid, String patientId, Date date) {
        Encounter e = new Encounter();

        e.setId("encounter-" + uuid);
        e.setStatus(Encounter.EncounterStatus.FINISHED);
        e.getClass_().setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode").setCode("AMB").setDisplay("ambulatory");

        e.setSubject(new Reference().setReference(patientId));

        Calendar start = Calendar.getInstance();
        start.setTime(date);
        start.add(Calendar.MINUTE, -1);

        Calendar end = Calendar.getInstance();
        end.setTime(date);
        end.add(Calendar.MINUTE, 1);

        e.getPeriod().setStart(start.getTime()).setEnd(end.getTime());

        return e;
    }

    private Observation buildObservation(String uuid, String patientId, String encounterId, HomeBloodPressureReading item) {
        // adapted from https://www.programcreek.com/java-api-examples/?api=org.hl7.fhir.dstu3.model.Observation

        Observation o = new Observation();

        o.setId("observation-" + uuid);
        o.setSubject(new Reference().setReference(patientId));
        o.setEncounter(new Reference().setReference(encounterId));
        o.setStatus(Observation.ObservationStatus.FINAL);
        o.getCode().addCoding().setCode(BloodPressureModel.CODE).setSystem(BloodPressureModel.SYSTEM);
        o.setEffective(new DateTimeType(item.getReadingDate()));

        Observation.ObservationComponentComponent systolic = new Observation.ObservationComponentComponent();
        systolic.getCode().addCoding().setCode(BloodPressureModel.SYSTOLIC_CODE).setSystem(BloodPressureModel.SYSTEM);
        systolic.setValue(new Quantity());
        systolic.getValueQuantity().setCode(BloodPressureModel.VALUE_CODE);
        systolic.getValueQuantity().setSystem(BloodPressureModel.VALUE_SYSTEM);
        systolic.getValueQuantity().setUnit(BloodPressureModel.VALUE_UNIT);
        systolic.getValueQuantity().setValue(item.getSystolic());
        o.getComponent().add(systolic);

        Observation.ObservationComponentComponent diastolic = new Observation.ObservationComponentComponent();
        diastolic.getCode().addCoding().setCode(BloodPressureModel.DIASTOLIC_CODE).setSystem(BloodPressureModel.SYSTEM);
        diastolic.setValue(new Quantity());
        diastolic.getValueQuantity().setCode(BloodPressureModel.VALUE_CODE);
        diastolic.getValueQuantity().setSystem(BloodPressureModel.VALUE_SYSTEM);
        diastolic.getValueQuantity().setUnit(BloodPressureModel.VALUE_UNIT);
        diastolic.getValueQuantity().setValue(item.getDiastolic());
        o.getComponent().add(diastolic);

        return o;
    }
}
