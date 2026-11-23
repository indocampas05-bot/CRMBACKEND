package com.indocrm.indocrm.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.indocrm.indocrm.model.CommunicationMessage;

public interface CommunicationMessageRepository
        extends MongoRepository<CommunicationMessage, String> {

    List<CommunicationMessage> findByRecipientTypeOrderByCreatedAtAsc(String recipientType);

    @Query("""
        {
          '$or': [
            { 'senderUsername': ?0, 'recipientUsername': ?1, 'recipientType': 'DIRECT' },
            { 'senderUsername': ?1, 'recipientUsername': ?0, 'recipientType': 'DIRECT' }
          ]
        }
        """)
    List<CommunicationMessage> findDirectConversation(String firstUsername, String secondUsername);

    Optional<CommunicationMessage> findByAttachmentId(String attachmentId);

    // ─────────────────────────────────────────────────────────────
    // NEW: single-query unread counts (replaces loading every message)
    // ─────────────────────────────────────────────────────────────

    /** Unread direct messages sent TO `reader` FROM `sender`. */
    @Query(value = """
        {
          'recipientType': 'DIRECT',
          'senderUsername': ?0,
          'recipientUsername': ?1,
          'readBy': { '$ne': ?1 }
        }
        """, count = true)
    long countUnreadDirect(String sender, String reader);

    /** Unread group messages not yet read by `reader` and not sent by them. */
    @Query(value = """
        {
          'recipientType': 'GROUP',
          'senderUsername': { '$ne': ?0 },
          'readBy': { '$ne': ?0 }
        }
        """, count = true)
    long countUnreadGroup(String reader);
}