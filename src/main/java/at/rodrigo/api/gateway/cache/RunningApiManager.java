package at.rodrigo.api.gateway.cache;

import at.rodrigo.api.gateway.entity.Api;
import at.rodrigo.api.gateway.entity.Path;
import at.rodrigo.api.gateway.entity.RunningApi;
import at.rodrigo.api.gateway.entity.Verb;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
@Slf4j
public class RunningApiManager {

    @Autowired
    private HazelcastInstance hazelcastInstance;

    //@Autowired
    //private RunningApiListener runningApiListener;

    @PostConstruct
    public void addListener() {
        getCachedApi().addEntryListener(new RunningApiListener(), true );
    }

    public void runApi(String routeId, Api api, String path, Verb verb) {
        RunningApi runningApi = new RunningApi();
        runningApi.setId(api.getId());
        runningApi.setRouteId(routeId);
        runningApi.setDisabled(false);
        runningApi.setFailedCalls(0);

        runningApi.setContext(api.getContext());
        runningApi.setAudience(api.getAudience());
        runningApi.setEndpoint(api.getEndpoint());
        runningApi.setJwsEndpoint(api.getJwsEndpoint());
        runningApi.setEndpointType(api.getEndpointType());
        runningApi.setSecured(api.isSecured());
        runningApi.setPath(path);
        runningApi.setVerb(verb);
        runningApi.setBlockIfInError(api.isBlockIfInError());

        if(api.isBlockIfInError()) {
            if(api.isUnblockAfter()) {
                runningApi.setUnblockAfterMinutes(api.getUnblockAfterMinutes());
            } else {
                runningApi.setUnblockAfterMinutes(-1);
            }
            runningApi.setUnblockAfter(api.isUnblockAfter());
            runningApi.setMaxAllowedFailedCalls(api.getMaxAllowedFailedCalls());
        } else {
            runningApi.setUnblockAfterMinutes(-1);
            runningApi.setMaxAllowedFailedCalls(-1);
        }
        getCachedApi().put(routeId, runningApi);
    }

    private RunningApi getRunningApi(String routeId) {
        if(getCachedApi().containsKey(routeId)) {
            return getCachedApi().get(routeId);
        } else {
            return null;
        }
    }

    public boolean blockApi(String routeId) {
        RunningApi runningApi = getRunningApi(routeId);
        if(runningApi != null) {
            if(runningApi.getFailedCalls() == runningApi.getMaxAllowedFailedCalls()) {
                runningApi.setDisabled(true);
                getCachedApi().put(routeId, runningApi);
                return true;
            } else {
                runningApi.setFailedCalls(runningApi.getFailedCalls() + 1);
                getCachedApi().put(routeId, runningApi);
                return false;
            }
        } else {
            return false;
        }
    }

    private IMap<String, RunningApi> getCachedApi() {
        return hazelcastInstance.getMap(CacheConstants.RUNNING_API_IMAP_NAME);
    }

    public List<RunningApi> getDisabledRunningApis() {
        List<RunningApi> disabledRunningApis = new ArrayList<>();
        IMap<String, RunningApi> runningApis = getCachedApi();
        Iterator<String> i = runningApis.keySet().iterator();
        while(i.hasNext()) {
            String routeId = i.next();
            if(runningApis.get(routeId).isDisabled() && !runningApis.get(routeId).isRemoved()) {
                disabledRunningApis.add(runningApis.get(routeId));
            }
        }
        return disabledRunningApis;
    }

    public List<RunningApi> getRemovedRunningApis() {
        List<RunningApi> removedRunningApis = new ArrayList<>();
        IMap<String, RunningApi> runningApis = getCachedApi();
        Iterator<String> i = runningApis.keySet().iterator();
        while(i.hasNext()) {
            String routeId = i.next();
            if(runningApis.get(routeId).isDisabled() && runningApis.get(routeId).isRemoved() && runningApis.get(routeId).isUnblockAfter()) {
                removedRunningApis.add(runningApis.get(routeId));
            }
        }
        return removedRunningApis;
    }

    public void saveRunningApi(RunningApi runningApi) {
        this.getCachedApi().put(runningApi.getRouteId(), runningApi);
    }

    public int count() {
        return getCachedApi().size();
    }

}