package com.tencent.jflynn.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.tencent.jflynn.domain.App;
import com.tencent.jflynn.dto.ProcessRequest;
import com.tencent.jflynn.exception.ObjectNotFoundException;
import com.tencent.jflynn.service.AppService;
import com.tencent.jflynn.service.ProcessService;
import com.wordnik.swagger.annotations.Api;

@RestController
@Api("processes")
@RequestMapping("/processes")
public class ProcessController {
	@Autowired
	private AppService appService;
	@Autowired
	private ProcessService processService;
	
	@RequestMapping(value="/list/{appName}", method=RequestMethod.GET, produces="application/json")
	public Process[] list(@PathVariable("appName") String appName,
			@RequestBody ProcessRequest req){
		App app = appService.getAppByName(appName);
    	if(app == null){
    		throw new ObjectNotFoundException();
    	}
    	return processService.list(appName, req);
	}
	
	@RequestMapping(value="/stop/{appName}", method=RequestMethod.PUT, consumes="application/json")
	public void stop(@PathVariable("appName") String appName,
			@RequestBody ProcessRequest req){
		App app = appService.getAppByName(appName);
    	if(app == null){
    		throw new ObjectNotFoundException();
    	}
    	processService.stop(appName, req);
	}
}