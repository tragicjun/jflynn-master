package com.tencent.jflynn.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.tencent.jflynn.domain.App;
import com.tencent.jflynn.domain.Formation;
import com.tencent.jflynn.exception.ObjectNotFoundException;
import com.tencent.jflynn.service.AppService;
import com.tencent.jflynn.service.FormationService;
import com.wordnik.swagger.annotations.Api;

@RestController
@Api("formations")
@RequestMapping("/formations")
public class FormationControllers {
	@Autowired
	private FormationService formationService;
	
	@Autowired
	private AppService appService;
	
	@RequestMapping(value="/getAll", method=RequestMethod.GET, produces="application/json")
    public List<Formation> getAll() {
    	return formationService.getAllFormations();
    }
	
	@RequestMapping(value="/get/app/{appName}", method=RequestMethod.GET)
	public Formation getAppFormation(@PathVariable("appName") String appName){
		App app = appService.getAppByName(appName);
    	if(app == null){
    		throw new ObjectNotFoundException();
    	}
		
		return formationService.getAppFormation(app.getId());
	}
}
