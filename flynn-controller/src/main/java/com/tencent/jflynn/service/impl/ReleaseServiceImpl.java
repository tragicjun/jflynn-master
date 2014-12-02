package com.tencent.jflynn.service.impl;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.rits.cloning.Cloner;
import com.tencent.jflynn.dao.ArtifactDao;
import com.tencent.jflynn.dao.FormationDao;
import com.tencent.jflynn.dao.ReleaseDao;
import com.tencent.jflynn.domain.App;
import com.tencent.jflynn.domain.Artifact;
import com.tencent.jflynn.domain.Program;
import com.tencent.jflynn.domain.Release;
import com.tencent.jflynn.dto.ReleaseRequest;
import com.tencent.jflynn.service.ReleaseService;
import com.tencent.jflynn.service.SchedulerService;
import com.tencent.jflynn.utils.IdGenerator;
import com.tencent.jflynn.utils.ShellCommandExecutor;

@Service
public class ReleaseServiceImpl implements ReleaseService {
	private static final Logger LOG = Logger.getLogger(ReleaseServiceImpl.class);
			
	@Autowired
	private ReleaseDao releaseDao;
	@Autowired
	private ArtifactDao artifactDao;
	@Autowired
	private FormationDao formationDao;
	@Autowired
	private SchedulerService scheduler;
	
	@Value("${httpServerUrl}")
	private String httpServerUrl;
	@Value("${svnImage:tegdsf/svn}")
	private String svnImage;
	@Value("${slugBuilderImage:tegdsf/slugbuilder}")
	private String slugBuilderImage;
	@Value("${slugRunnerImage:tegdsf/slugrunner}")
	private String slugRunnerImage;
	@Value("${slugBuildScript:slugBuild.sh}")
	private String slugBuildScript;
	
	private static final Pattern PATTERN_TYPES = Pattern.compile(".*declares types -> (.*)");
	private static final Cloner cloner = new Cloner();
	
	public Release getReleaseById(String releaseId){
		return releaseDao.queryById(releaseId);
	}
	
	public List<Release> getReleasesByAppId(String appId){
		return releaseDao.queryByAppId(appId);
	}
	
	public List<Program> getPrograms(String releaseId){
		return releaseDao.queryPrograms(releaseId);
	}
	
	public Release createRelease(App app, ReleaseRequest req){
		Release baseRelease = null;
		//based on the specified version or the current release of the app
		if(req.getBaseVersion() != null){
			baseRelease = releaseDao.queryByAppIdAndVersion(app.getId(), req.getBaseVersion());
			req.setComment("Rollback to version " + req.getBaseVersion());
		}else if(app.getReleaseID() != null){
			baseRelease = releaseDao.queryById(app.getReleaseID());
		}
		
		Release release = null;
		if(baseRelease == null){
			release = new Release();
			release.setAppID(app.getId());
		}else{
			release = cloner.deepClone(baseRelease);
		}
		
		release.setId(IdGenerator.generate());
		release.setVersion(app.getLatestVersion() + 1);
		release.setTag(req.getComment());
		release.setCreateTime(new Timestamp(System.currentTimeMillis()));
		
		//build new artifact if either svnURL or dockerImage is specified
		if(req.getSvnURL() != null){
			handleSvnDeploy(app, release, req);
		}else if(req.getImageURI() != null){
			handleImageDeploy(app, release, req);
		}
		
		//update application environment variables
		if(req.getAppEnv() != null){
			for(Map.Entry<String, String> e : req.getAppEnv().entrySet()){
				release.getAppEnv().put(e.getKey(), e.getValue());
			}
		}
		
		if(req.getPrograms() != null){
			for(Program program : req.getPrograms()){
				release.getPrograms().put(program.getName(), program);
			}
		}
		
		if(req.getDeletePrograms() != null){
			for(String programName : req.getDeletePrograms()){
				release.getPrograms().remove(programName);
			}
		}
		
		//if no processes in the release, create a "default" one
		if(release.getPrograms().size() == 0){
			Program p = new Program();
			p.setName("default");
			release.getPrograms().put(p.getName(), p);
		}
		
		releaseDao.insert(release);
		LOG.info("Created release for appName=" + app.getName() + " release=" + release);
		
		return release;
	}
	
	private void handleImageDeploy(App app, Release release, ReleaseRequest req){
		//create artifact and release object
		Artifact artifact = new Artifact();
		artifact.setId(IdGenerator.generate());
		artifact.setUri(req.getImageURI());
		artifact.setCreateTime(new Timestamp(System.currentTimeMillis()));
		artifactDao.insert(artifact);
		LOG.info("Created artifact for appName=" + app.getName() + " artifact=" + artifact);
		
		release.setArtifactID(artifact.getId());
	}
	
	private void handleSvnDeploy(App app, Release release, ReleaseRequest req){
		String fileName = app.getName() + "-" + System.currentTimeMillis();
		Map<String,String> env = new HashMap<String,String>();
		env.put("SVN_URL", req.getSvnURL());
		env.put("APP_NAME", fileName);
		env.put("IMAGE_SVN", svnImage);
		env.put("HTTP_SERVER_URL", httpServerUrl);
		env.put("IMAGE_SLUGBUILDER", slugBuilderImage);
		
		String cmd = slugBuildScript;
		String out = ShellCommandExecutor.execute(cmd, env);
		System.out.println(out);
		//Grep output and extract process types
		Matcher m = PATTERN_TYPES.matcher(out);
		String [] processTypes = null;
		if(m.find()){
			processTypes = m.group(1).split(", ");
		}
		
		//create artifact and release object
		Artifact artifact = new Artifact();
		artifact.setId(IdGenerator.generate());
		artifact.setUri(slugRunnerImage);
		artifact.setCreateTime(new Timestamp(System.currentTimeMillis()));
		artifactDao.insert(artifact);
		LOG.info("Created artifact for appName=" + app.getName() + " artifact=" + artifact);
		
		release.setArtifactID(artifact.getId());
		release.getAppEnv().put("SLUG_URL", httpServerUrl + "/slugs/" + fileName + ".tgz");
		if(processTypes != null){
			for(String type : processTypes){
				type = type.trim();
				Program ptype = new Program();
				ptype.setName(type);
				ptype.setCmd("start " + type);
				release.getPrograms().put(type, ptype);
			}
		}
	}
}
