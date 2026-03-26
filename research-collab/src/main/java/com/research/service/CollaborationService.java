package com.research.service;

import com.research.model.*;
import com.research.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * CollaborationService - Member 3's primary use case.
 * Manages collaboration requests between researchers and experts.
 *
 * Design Principle: SRP - only handles collaboration workflows.
 * Design Principle: DIP - depends on repository abstractions.
 */
@Service
public class CollaborationService {

    private final CollaborationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final ResearchProjectRepository projectRepository;
    private final NotificationService notificationService;

    public CollaborationService(CollaborationRequestRepository requestRepository,
                                UserRepository userRepository,
                                ResearchProjectRepository projectRepository,
                                NotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.notificationService = notificationService;
    }

    /**
     * Send a collaboration request.
     * Activity diagram: Researcher → Collaborate & Find Researcher → Send Request
     */
    @Transactional
    public CollaborationRequest sendRequest(Long senderId, Long receiverId,
                                             Long projectId, String message) {
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        User receiver = userRepository.findById(receiverId)
            .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));

        CollaborationRequest request = new CollaborationRequest();
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setMessage(message);

        if (projectId != null) {
            ResearchProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));
            request.setProject(project);
        }

        CollaborationRequest saved = requestRepository.save(request);

        // Notify receiver (in-app notification)
        notificationService.notify(receiver,
            "New collaboration request from " + sender.getName(),
            "COLLAB_REQUEST");

        return saved;
    }

    /**
     * Accept a collaboration request.
     * Activity diagram: Reviewer → Approve Paper / Collaborator → Join Project
     */
    @Transactional
    public CollaborationRequest acceptRequest(Long requestId) {
        CollaborationRequest request = getRequestById(requestId);
        request.accept();

        // If linked to a project, add sender as member
        if (request.getProject() != null) {
            request.getProject().addMember(request.getSender());
            projectRepository.save(request.getProject());
        }

        // Notify sender
        notificationService.notify(request.getSender(),
            request.getReceiver().getName() + " accepted your collaboration request!",
            "COLLAB_ACCEPTED");

        return requestRepository.save(request);
    }

    /**
     * Reject a collaboration request.
     */
    @Transactional
    public CollaborationRequest rejectRequest(Long requestId) {
        CollaborationRequest request = getRequestById(requestId);
        request.reject();

        notificationService.notify(request.getSender(),
            request.getReceiver().getName() + " declined your collaboration request.",
            "COLLAB_REJECTED");

        return requestRepository.save(request);
    }

    public List<CollaborationRequest> getPendingRequestsForUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return requestRepository.findByReceiverAndStatus(
                user, CollaborationRequest.RequestStatus.PENDING);
    }

    public List<CollaborationRequest> getSentRequests(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return requestRepository.findBySender(user);
    }

    private CollaborationRequest getRequestById(Long id) {
        return requestRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Request not found: " + id));
    }
}
