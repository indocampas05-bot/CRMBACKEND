// BACKEND: src/main/java/com/indocrm/indocrm/repository/CallRepository.java

package com.indocrm.indocrm.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.indocrm.indocrm.model.Call;

@Repository
public interface CallRepository extends MongoRepository<Call, String> {
    
    List<Call> findByLeadId(String leadId);
    
    List<Call> findByAgentUsername(String agentUsername);
    
    List<Call> findByAgentUsernameOrderByCreatedAtDesc(String agentUsername);
    
    List<Call> findByCallStatus(String callStatus);
    
    List<Call> findByOutcome(String outcome);
    
    @Query("{ 'startedAt': { $gte: ?0, $lte: ?1 } }")
    List<Call> findByStartedAtBetween(Date start, Date end);
    
    @Query("{ 'agentUsername': ?0, 'startedAt': { $gte: ?1, $lte: ?2 } }")
    List<Call> findByAgentUsernameAndStartedAtBetween(String agentUsername, Date start, Date end);
    
    long countByAgentUsernameAndCallStatus(String agentUsername, String callStatus);
    
    long countByAgentUsernameAndOutcome(String agentUsername, String outcome);
    
    long countByAgentUsernameAndStartedAtBetween(String agentUsername, Date start, Date end);
}
