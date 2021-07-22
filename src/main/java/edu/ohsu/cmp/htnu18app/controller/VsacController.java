package edu.ohsu.cmp.htnu18app.controller;

import edu.ohsu.cmp.htnu18app.cache.CacheData;
import edu.ohsu.cmp.htnu18app.cache.SessionCache;
import edu.ohsu.cmp.htnu18app.entity.vsac.ValueSet;
import edu.ohsu.cmp.htnu18app.service.ConceptService;
import edu.ohsu.cmp.htnu18app.service.ValueSetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/vsac")
public class VsacController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ValueSetService valueSetService;

    @Autowired
    private ConceptService conceptService;

    @GetMapping("valueset")
    @Transactional
    public ResponseEntity<ValueSet> get(HttpSession session,
                                        @RequestParam("oid") String oid) {

        // get the cache, even though we don't do anything with it (yet?  maybe at all?)
        // we do this to ensure the user is authenticated, as a means of preventing unauthorized access
        // (this call will throw a SessionMissingException if the user is not authenticated)
        CacheData cache = SessionCache.getInstance().get(session.getId());

        ValueSet valueSet = valueSetService.getValueSet(oid);
        logger.info("got ValueSet: " + valueSet);

        return new ResponseEntity<>(valueSet, HttpStatus.OK);
    }
}
