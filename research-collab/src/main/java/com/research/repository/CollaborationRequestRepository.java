package com.research.repository;

import com.research.model.CollaborationRequest;
import com.research.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CollaborationRequestRepository
        extends JpaRepository<CollaborationRequest, Long> {
    List<CollaborationRequest> findBySender(User sender);
    List<CollaborationRequest> findByReceiver(User receiver);
    List<CollaborationRequest> findByReceiverAndStatus(
            User receiver, CollaborationRequest.RequestStatus status);
}
