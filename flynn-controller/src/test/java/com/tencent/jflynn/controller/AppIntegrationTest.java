package com.tencent.jflynn.controller;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

import com.tencent.jflynn.boot.JFlynnMain;
import com.tencent.jflynn.domain.App;
import com.tencent.jflynn.domain.Program;
import com.tencent.jflynn.domain.Release;
import com.tencent.jflynn.dto.ReleaseRequest;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = JFlynnMain.class)
@WebAppConfiguration
@IntegrationTest("work.mode:test")
public class AppIntegrationTest {
	private RestTemplate restTemplate = new TestRestTemplate();
	private final String appName = "myapp";
	private final String baseURL = "http://localhost:58080";
	
	@Before
	public void setUp(){
		//create app
		restTemplate.postForEntity(baseURL+"/apps/create/" + appName, 
				null, String.class).getBody();
	}
	
	@After
	public void tearsDown(){
		restTemplate.delete(baseURL+"/apps/delete/" + appName);
	}
	
	@Test
	public void testCreateApp(){
		//create app
		String appName = "new-myapp";
		String id = restTemplate.postForEntity(baseURL+"/apps/create/" + appName, 
				null, String.class).getBody();
		assertNotNull(id);
		//get app and check
		App app = restTemplate.getForEntity(baseURL+"/apps/get/" + appName, App.class).getBody();
		assertNotNull(app);
		assertEquals(appName, app.getName());
	}
	
	@Test
	public void testDeployAppWithSVN(){
		//deploy app
		ReleaseRequest req = new ReleaseRequest();
		req.setSvnURL("http://svn.com");
		req.setComment("deploy new code");
		restTemplate.postForEntity(baseURL+"/apps/deploy/"+appName, req, Void.class);
		
		//get app releases and check
		Release[] releases = restTemplate.getForEntity(baseURL+"/releases/list/app/"+appName, Release[].class).getBody();
		assertNotNull(releases);
		assertEquals(1, releases.length);
		Release release = releases[0];
		assertNotNull(release.getArtifactID());
		assertNotNull(release.getAppID());
		assertNotNull(release.getId());
		assertEquals(1, release.getVersion());
		assertEquals(req.getComment(), release.getTag());
		assertTrue(release.getPrograms().size() >= 1);
		assertNotNull(release.getAppEnv().get("SLUG_URL"));
	}
	
	@Test
	public void testDeployAppWithImage(){
		//deploy app
		ReleaseRequest req = new ReleaseRequest();
		req.setImageURI("tegdsf/routercenter");
		req.setComment("deploy new code");
		restTemplate.postForEntity(baseURL+"/apps/deploy/"+appName, req, Void.class);
		
		//get app releases and check
		Release[] releases = restTemplate.getForEntity(baseURL+"/releases/list/app/"+appName, Release[].class).getBody();
		assertNotNull(releases);
		assertEquals(1, releases.length);
		Release release = releases[0];
		assertNotNull(release.getArtifactID());
		assertNotNull(release.getAppID());
		assertNotNull(release.getId());
		assertEquals(1, release.getVersion());
		assertEquals(req.getComment(), release.getTag());
		assertTrue(release.getPrograms().size() >= 1);
		assertNull(release.getAppEnv().get("SLUG_URL"));
	}
	
	@Test
	public void testDeployAppWithRelEnv(){
		//deploy app
		ReleaseRequest req = new ReleaseRequest();
		req.setImageURI("tegdsf/routercenter");
		req.setAppEnv(new HashMap<String,String>());
		req.getAppEnv().put("URL", "http://dsf");
		restTemplate.postForEntity(baseURL+"/apps/deploy/"+appName, req, Void.class);
		
		//get app releases and check
		Release[] releases = restTemplate.getForEntity(baseURL+"/releases/list/app/"+appName, Release[].class).getBody();
		assertNotNull(releases);
		assertEquals(1, releases.length);
		Release release = releases[0];
		assertNotNull(release.getArtifactID());
		assertNotNull(release.getAppID());
		assertNotNull(release.getId());
		assertEquals(1, release.getVersion());
		assertTrue(release.getPrograms().size() >= 1);
		assertEquals(req.getAppEnv().get("URL"),
				release.getAppEnv().get("URL"));
	}
	
	@Test
	public void testDeployAppWithProcCmd(){
		//deploy app
		ReleaseRequest req = new ReleaseRequest();
		req.setImageURI("tegdsf/routercenter");
		Program program = new Program();
		program.setName("web");
		program.setCmd("new cmd");
		req.setSavePrograms(new Program[1]);
		req.getSavePrograms()[0] = program;
		restTemplate.postForEntity(baseURL+"/apps/deploy/"+appName, req, Void.class);
		
		//get app releases and check
		Release[] releases = restTemplate.getForEntity(baseURL+"/releases/list/app/"+appName, Release[].class).getBody();
		assertNotNull(releases);
		assertEquals(1, releases.length);
		Release release = releases[0];
		assertNotNull(release.getArtifactID());
		assertNotNull(release.getAppID());
		assertNotNull(release.getId());
		assertEquals(1, release.getVersion());
		assertTrue(release.getPrograms().size() >= 1);
		assertEquals(program.getCmd(), release.getPrograms().get("web").getCmd());
	}
	
	@Test
	public void testDeployAppWithProcEpt(){
		//deploy app
		ReleaseRequest req = new ReleaseRequest();
		req.setImageURI("tegdsf/routercenter");
		Program program = new Program();
		program.setName("web");
		program.setEntrypoint("new entrypoint");
		req.setSavePrograms(new Program[1]);
		req.getSavePrograms()[0] = program;
		
		restTemplate.postForEntity(baseURL+"/apps/deploy/"+appName, req, Void.class);
		//get app releases and check
		Release[] releases = restTemplate.getForEntity(baseURL+"/releases/list/app/"+appName, Release[].class).getBody();
		assertNotNull(releases);
		assertEquals(1, releases.length);
		Release release = releases[0];
		assertNotNull(release.getArtifactID());
		assertNotNull(release.getAppID());
		assertNotNull(release.getId());
		assertEquals(1, release.getVersion());
		assertTrue(release.getPrograms().size() >= 1);
		assertEquals(program.getEntrypoint(),
				release.getPrograms().get("web").getEntrypoint());
	}
	
	@Test
	public void testDeployAppWithProcEnv(){
		//deploy app
		ReleaseRequest req = new ReleaseRequest();
		req.setImageURI("tegdsf/routercenter");
		Program program = new Program();
		program.setName("web");
		program.getEnv().put("URL", "http://dsf");
		req.setSavePrograms(new Program[1]);
		req.getSavePrograms()[0] = program;
		
		restTemplate.postForEntity(baseURL+"/apps/deploy/"+appName, req, Void.class);
		//get app releases and check
		Release[] releases = restTemplate.getForEntity(baseURL+"/releases/list/app/"+appName, Release[].class).getBody();
		assertNotNull(releases);
		assertEquals(1, releases.length);
		Release release = releases[0];
		assertNotNull(release.getArtifactID());
		assertNotNull(release.getAppID());
		assertNotNull(release.getId());
		assertEquals(1, release.getVersion());
		assertTrue(release.getPrograms().size() >= 1);
		assertEquals(program.getEnv().get("URL"),
				release.getPrograms().get("web").getEnv().get("URL"));
	}
	
//	@Test
//	public void testScaleApp(){
//		//deploy app
//		DeployRequest req = new DeployRequest();
//		req.setDockerImage("tegdsf/routercenter");
//		req.setProgramEnv(new HashMap<String, Map<String,String>>());
//		req.getProgramEnv().put("web", new HashMap<String,String>());
//		req.getProgramEnv().get("web").put("URL", "http://dsf");
//		restTemplate.postForEntity(baseURL+"/apps/deploy/"+appName, req, Void.class);
//		
//		//scale app
//		ScaleRequest sreq = new ScaleRequest();
//		sreq.setProgramReplica(new HashMap<String,Integer>());
//		sreq.getProgramReplica().put("web", 1);
//		restTemplate.postForEntity(baseURL+"/apps/scale/"+appName, sreq, Void.class);
//		
//		//get app formation and check
//		Formation formation = restTemplate.getForEntity(baseURL+"/formations/get/app/"+appName, Formation.class).getBody();
//		assertNotNull(formation);
//		assertNotNull(formation.getAppID());
//		assertNotNull(formation.getReleaseID());
//		assertTrue(formation.getProgramReplica().size() > 0);
//		assertEquals(sreq.getProgramReplica().get("web"), 
//				formation.getProgramReplica().get("web"));
//	}
}
