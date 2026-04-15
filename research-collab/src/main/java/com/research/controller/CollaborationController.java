package com.research.controller;

import com.research.model.*;
import com.research.repository.*;
import com.research.service.AuthService;
import com.research.service.CollaborationService;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;

/**
 * CollaborationController — Controller layer for Collaboration & Project Management use case.
 *
 * @author Member 3
 * @usecase Collaboration Requests, Project Management, Team Chat, Updates & Public Opinions
 *
 * Design Pattern demonstrated: Observer (publication notifications trigger email via n8n)
 * Design Principle demonstrated: DIP — depends on repository interfaces, not implementations
 *
 * MVC Role: Controller — mediates between CollaborationView/MyResearchesView (View)
 *           and CollaborationService + Repositories (Model)
 */
@Controller
public class CollaborationController {

    private final CollaborationService collaborationService;
    private final ResearchProjectRepository projectRepository;
    private final ResearchPaperRepository paperRepository;
    private final UserRepository userRepository;
    private final ProjectMessageRepository messageRepository;
    private final ProjectUpdateRepository updateRepository;
    private final PublicOpinionRepository opinionRepository;

    public CollaborationController(CollaborationService collaborationService,
                                    ResearchProjectRepository projectRepository,
                                    ResearchPaperRepository paperRepository,
                                    UserRepository userRepository,
                                    ProjectMessageRepository messageRepository,
                                    ProjectUpdateRepository updateRepository,
                                    PublicOpinionRepository opinionRepository) {
        this.collaborationService = collaborationService;
        this.projectRepository = projectRepository;
        this.paperRepository = paperRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.updateRepository = updateRepository;
        this.opinionRepository = opinionRepository;
    }

    // ═══════════════════════════════════════════════════════════
    // Collaboration Requests
    // ═══════════════════════════════════════════════════════════

    /** Get incoming collaboration requests for the current user. */
    public List<CollaborationRequest> getInboxRequests() {
        User currentUser = AuthService.getCurrentUser();
        return collaborationService.getPendingRequestsForUser(currentUser.getUserId());
    }

    /** Get sent collaboration requests by the current user. */
    public List<CollaborationRequest> getSentRequests() {
        User currentUser = AuthService.getCurrentUser();
        return collaborationService.getSentRequests(currentUser.getUserId());
    }

    /** Accept a collaboration request. */
    public void acceptRequest(Long requestId) {
        collaborationService.acceptRequest(requestId);
    }

    /** Decline a collaboration request. */
    public void declineRequest(Long requestId) {
        collaborationService.rejectRequest(requestId);
    }

    /** Send a collaboration request (current user as sender). */
    public void sendRequest(Long receiverId, Long projectId, String message) {
        User currentUser = AuthService.getCurrentUser();
        collaborationService.sendRequest(
                currentUser.getUserId(), receiverId, projectId, message);
    }

    /** Send a collaboration request with explicit sender ID. */
    public void sendRequestFrom(Long senderId, Long receiverId, Long projectId, String message) {
        collaborationService.sendRequest(senderId, receiverId, projectId, message);
    }

    // ═══════════════════════════════════════════════════════════
    // Project Management
    // ═══════════════════════════════════════════════════════════

    /** Get all projects the current user owns or is a member of. */
    public List<ResearchProject> getMyProjects() {
        User currentUser = AuthService.getCurrentUser();
        List<ResearchProject> owned = new ArrayList<>();
        try {
            if (currentUser instanceof Researcher)
                owned = projectRepository.findByOwner((Researcher) currentUser);
        } catch (Exception ignored) {}

        List<ResearchProject> member = new ArrayList<>();
        try { member = projectRepository.findByMemberId(currentUser.getUserId()); }
        catch (Exception ignored) {}

        List<ResearchProject> all = new ArrayList<>(owned);
        for (ResearchProject p : member) {
            if (all.stream().noneMatch(x -> x.getProjectId().equals(p.getProjectId())))
                all.add(p);
        }
        return all;
    }

    /** Create a new research project using the Builder pattern. */
    public ResearchProject createProject(String topic, String description,
                                          String domain, boolean lookingForCollaborators) {
        User currentUser = AuthService.getCurrentUser();
        ResearchProject project = new ResearchProject.Builder(topic, (Researcher) currentUser)
                .description(description)
                .domain(domain)
                .lookingForCollaborators(lookingForCollaborators)
                .build();
        return projectRepository.save(project);
    }

    /** Save/update a project. */
    public ResearchProject saveProject(ResearchProject project) {
        return projectRepository.save(project);
    }

    /** Get all open collaboration projects. */
    public List<ResearchProject> getOpenProjects() {
        return projectRepository.findByLookingForCollaboratorsTrue();
    }

    /** Get open projects filtered by domains (for interest-based filtering). */
    public List<ResearchProject> getOpenProjectsByDomains(List<String> domains) {
        return projectRepository.findByLookingForCollaboratorsTrueAndDomainIn(domains);
    }

    /** Get projects by status. */
    public List<ResearchProject> getProjectsByStatus(ResearchProject.ProjectStatus status) {
        return projectRepository.findByStatus(status);
    }

    /** Get all unique domains in the system. */
    public List<String> getAllDomains() {
        return projectRepository.findAllDomains();
    }

    /** Complete a project and create a paper from it. */
    public void completeProject(ResearchProject project, String paperTitle,
                                 String paperAbstract, String pdfPath) {
        project.setStatus(ResearchProject.ProjectStatus.COMPLETED);
        projectRepository.save(project);

        ResearchPaper paper = new ResearchPaper();
        paper.setTitle(paperTitle);
        paper.setAbstractText(paperAbstract);
        paper.setDomain(project.getDomain());
        paper.setLink(pdfPath);
        paper.setStatus(ResearchPaper.PaperStatus.PUBLISHED);
        paper.setResearcher(project.getOwner());

        StringBuilder authors = new StringBuilder(project.getOwner().getName());
        Long ownerId = project.getOwner().getUserId();
        for (User m : project.getMembers()) {
            if (!m.getUserId().equals(ownerId))
                authors.append(", ").append(m.getName());
        }
        paper.setAuthor(authors.toString());
        paper.setKeywords(project.getDomain());
        paperRepository.save(paper);
    }

    // ═══════════════════════════════════════════════════════════
    // User Management (for interest/domain updates)
    // ═══════════════════════════════════════════════════════════

    /** Save user entity (for updating interests, domains, etc.) */
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    /** Save a paper entity. */
    public ResearchPaper savePaper(ResearchPaper paper) {
        return paperRepository.save(paper);
    }

    /** Find papers by domain (for Following tab). */
    public List<ResearchPaper> findPapersByDomain(String domain) {
        return paperRepository.findByDomain(domain);
    }

    // ═══════════════════════════════════════════════════════════
    // Team Chat & Updates (using ResearchProject as parameter)
    // ═══════════════════════════════════════════════════════════

    /** Get chat messages for a project. */
    public List<ProjectMessage> getProjectMessages(ResearchProject project) {
        return messageRepository.findByProjectOrderBySentAtAsc(project);
    }

    /** Save a chat message. */
    public ProjectMessage saveMessage(ProjectMessage msg) {
        return messageRepository.save(msg);
    }

    /** Get updates/findings for a project. */
    public List<ProjectUpdate> getProjectUpdates(ResearchProject project) {
        return updateRepository.findByProjectOrderByPostedAtDesc(project);
    }

    /** Save a project update. */
    public ProjectUpdate saveUpdate(ProjectUpdate update) {
        return updateRepository.save(update);
    }

    // ═══════════════════════════════════════════════════════════
    // Public Opinions (Discover tab)
    // ═══════════════════════════════════════════════════════════

    /** Get public opinions for a project. */
    public List<PublicOpinion> getPublicOpinions(ResearchProject project) {
        return opinionRepository.findByProjectOrderByPostedAtDesc(project);
    }

    /** Save a public opinion. */
    public PublicOpinion saveOpinion(PublicOpinion opinion) {
        return opinionRepository.save(opinion);
    }

    /** Get the current user. */
    public User getCurrentUser() {
        return AuthService.getCurrentUser();
    }

    /** Check if user is logged in. */
    public boolean isLoggedIn() {
        return AuthService.isLoggedIn();
    }
}
