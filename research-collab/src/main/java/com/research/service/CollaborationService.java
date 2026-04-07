package com.research.service;

import com.research.model.*;
import com.research.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * CollaborationService - Member 3's primary use case.
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

    @Transactional
    public CollaborationRequest sendRequest(Long senderId, Long receiverId,
                                             Long projectId, String message) {
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new IllegalArgumentException("Sender not found"));
        User receiver = userRepository.findById(receiverId)
            .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));

        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Cannot send request to yourself.");
        }

        CollaborationRequest request = new CollaborationRequest();
        request.setSender(sender);
        request.setReceiver(receiver);
        request.setMessage(message);

        if (projectId != null) {
            ResearchProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found"));

            // Determine who will be ADDED when accepted:
            // If sender is the owner → the receiver will be added
            // If sender is NOT the owner → the sender will be added
            Long ownerId = project.getOwner() != null ? project.getOwner().getUserId() : null;
            boolean senderIsOwner = ownerId != null && ownerId.equals(senderId);
            User personToBeAdded = senderIsOwner ? receiver : sender;

            // Check if that person is already a member or owner
            boolean alreadyMember = project.getMembers().stream()
                .anyMatch(m -> m.getUserId().equals(personToBeAdded.getUserId()));
            boolean isOwner = ownerId != null && ownerId.equals(personToBeAdded.getUserId());

            if (alreadyMember || isOwner) {
                throw new IllegalArgumentException(
                    personToBeAdded.getName() + " is already part of '" + project.getTopic() + "'.");
            }

            // Check for existing pending request for same project + same pair
            List<CollaborationRequest> existing = requestRepository.findBySender(sender);
            for (CollaborationRequest r : existing) {
                if (r.getProject() != null && r.getProject().getProjectId().equals(projectId)
                    && r.getReceiver().getUserId().equals(receiverId)
                    && r.getStatus() == CollaborationRequest.RequestStatus.PENDING) {
                    throw new IllegalArgumentException(
                        "You already have a pending request for this project.");
                }
            }

            request.setProject(project);
        }

        CollaborationRequest saved = requestRepository.save(request);

        notificationService.notify(receiver,
            "New collaboration request from " + sender.getName(),
            "COLLAB_REQUEST");

        return saved;
    }

    @Transactional
    public CollaborationRequest acceptRequest(Long requestId) {
        CollaborationRequest request = getRequestById(requestId);
        request.accept();

        // If linked to a project, add the NON-OWNER user as member
        if (request.getProject() != null) {
            ResearchProject project = request.getProject();
            Long ownerId = project.getOwner() != null ? project.getOwner().getUserId() : null;

            // Determine who to add:
            // If sender is the owner (owner invited someone) → add receiver
            // If sender is NOT the owner (someone requested to join) → add sender
            User toAdd;
            if (ownerId != null && ownerId.equals(request.getSender().getUserId())) {
                toAdd = request.getReceiver();
            } else {
                toAdd = request.getSender();
            }

            // Safety: don't add owner as member
            if (ownerId == null || !ownerId.equals(toAdd.getUserId())) {
                project.addMember(toAdd);
                projectRepository.save(project);
            }
        }

        notificationService.notify(request.getSender(),
            request.getReceiver().getName() + " accepted your collaboration request!",
            "COLLAB_ACCEPTED");

        return requestRepository.save(request);
    }

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
