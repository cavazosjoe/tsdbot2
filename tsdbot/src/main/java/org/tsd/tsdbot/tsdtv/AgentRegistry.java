package org.tsd.tsdbot.tsdtv;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tsd.rest.v1.tsdtv.Heartbeat;
import org.tsd.rest.v1.tsdtv.HeartbeatResponse;
import org.tsd.tsdbot.Constants;
import org.tsd.tsdbot.tsdtv.job.JobQueue;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class AgentRegistry {

    private static final Logger log = LoggerFactory.getLogger(AgentRegistry.class);

    private final Map<String, OnlineAgent> onlineAgents = new ConcurrentHashMap<>();
    private final TSDTVAgentDao tsdtvAgentDao;
    private final JobQueue jobQueue;

    @Inject
    public AgentRegistry(ExecutorService executorService,
                         TSDTVAgentDao tsdtvAgentDao,
                         JobQueue jobQueue) {
        this.tsdtvAgentDao = tsdtvAgentDao;
        this.jobQueue = jobQueue;
        executorService.submit(new ConnectedAgentReaper());
    }

    public HeartbeatResponse handleHeartbeat(Heartbeat heartbeat, String ipAddress) throws BlacklistedAgentException {
        TSDTVAgent agent = tsdtvAgentDao.getAgentByAgentId(heartbeat.getAgentId());
        if (agent == null) {
            // this is an agent we've never seen before
            agent = new TSDTVAgent();
            agent.setAgentId(heartbeat.getAgentId());
            agent.setStatus(AgentStatus.unregistered);
            agent.setLastHeartbeatFrom(ipAddress);
        } else if (AgentStatus.blacklisted.equals(agent.getStatus())) {
            throw new BlacklistedAgentException(heartbeat.getAgentId());
        } else {
            agent.setLastHeartbeatFrom(ipAddress);
        }

        tsdtvAgentDao.saveAgent(agent);

        onlineAgents.put(agent.getAgentId(), new OnlineAgent(agent, heartbeat));

        HeartbeatResponse response = new HeartbeatResponse();
        response.setSleepSeconds(5);

        return response;
    }

    public void registerAgent(String agentId) {
        setAgentStatus(agentId, AgentStatus.registered);
    }

    public void blacklistAgent(String agentId) {
        setAgentStatus(agentId, AgentStatus.blacklisted);
    }

    public Set<OnlineAgent> getOnlineAgents() {
        return new HashSet<>(onlineAgents.values());
    }

    private void setAgentStatus(String agentId, AgentStatus status) {
        TSDTVAgent agent = tsdtvAgentDao.getAgentByAgentId(agentId);
        if (agent == null) {
            throw new IllegalArgumentException("No known agent matching ID " + agentId);
        }
        agent.setStatus(status);
        tsdtvAgentDao.saveAgent(agent);
        if (status.equals(AgentStatus.blacklisted)) {
            onlineAgents.remove(agentId);
        }
    }

    private class ConnectedAgentReaper implements Runnable {

        private boolean shutdown = false;

        void shutdown() {
            this.shutdown = true;
        }

        @Override
        public void run() {
            while (!shutdown) {
                LocalDateTime cutoff = LocalDateTime
                        .now()
                        .minus(2*Constants.TSDTV.AGENT_HEARTBEAT_PERIOD_MILLIS, ChronoUnit.MILLIS);
                List<String> expiredAgentIds = onlineAgents
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getValue().getLastHeartbeat().isBefore(cutoff))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList());
                for (String agentId : expiredAgentIds) {
                    onlineAgents.remove(agentId);
                    jobQueue.handleOfflineAgent(agentId);
                }
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
                } catch (InterruptedException e) {
                    log.error("Interrupted");
                    shutdown();
                }
            }
        }
    }
}
