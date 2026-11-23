package com.indocrm.indocrm.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.indocrm.indocrm.model.GoogleOAuthToken;

public interface GoogleOAuthTokenRepository extends MongoRepository<GoogleOAuthToken,String> {
    Optional<GoogleOAuthToken> findByUsername(String username);
    Optional<GoogleOAuthToken> findByState(String state);
}

