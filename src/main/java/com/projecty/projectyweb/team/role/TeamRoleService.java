package com.projecty.projectyweb.team.role;

import com.projecty.projectyweb.team.Team;
import com.projecty.projectyweb.team.TeamRepository;
import com.projecty.projectyweb.user.User;
import com.projecty.projectyweb.user.UserService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TeamRoleService {
    private final UserService userService;
    private final TeamRoleRepository teamRoleRepository;
    private final TeamRepository teamRepository;

    public TeamRoleService(UserService userService, TeamRoleRepository teamRoleRepository, TeamRepository teamRepository) {
		this.userService = userService;
        this.teamRoleRepository = teamRoleRepository;
        this.teamRepository = teamRepository;
    }

    public List<TeamRole> addTeamMembersByUsernames(Team team, List<String> usernames) {
        List<TeamRole> teamRoles = new ArrayList<>();
        if (usernames != null) {
            Set<User> users = userService.getUserSetByUsernamesWithoutCurrentUser(usernames);
            removeExistingUsersInTeamFromSet(users, team);
            users.forEach(user -> teamRoles.add(new TeamRole(TeamRoles.MEMBER, user, team)));
        }

        if (team.getTeamRoles() == null) {
            team.setTeamRoles(teamRoles);
        } else if (team.getTeamRoles().size() > 0) {
            team.getTeamRoles().addAll(teamRoles);
        }
        return teamRoles;
    }

    private List<TeamRole> saveTeamRoles(List<TeamRole> unsavedTeamRoles) {
        List<TeamRole> savedTeamRoles = new ArrayList<>();
        unsavedTeamRoles.forEach(t -> savedTeamRoles.add(teamRoleRepository.save(t)));
        return savedTeamRoles;
    }

    public List<TeamRole> addTeamRolesByUsernames(Team team, List<String> usernames) {
        List<TeamRole> unsavedTeamRoles = addTeamMembersByUsernames(team, usernames);
        return saveTeamRoles(unsavedTeamRoles);
    }


    public void addCurrentUserAsTeamManager(Team team) {
        User current = userService.getCurrentUser();
        TeamRole teamRole = new TeamRole(TeamRoles.MANAGER, current, team);
        if (team.getTeamRoles() == null) {
            List<TeamRole> teamRoles = new ArrayList<>();
            teamRoles.add(teamRole);
            team.setTeamRoles(teamRoles);
        } else {
            team.getTeamRoles().add(teamRole);
        }
    }

    public boolean isCurrentUserTeamManager(Team team) {
        User user = userService.getCurrentUser();
        Optional<TeamRole> teamRole = teamRoleRepository.findByTeamAndAndUser(team, user);
        return teamRole.map(role -> role.getName().equals(TeamRoles.MANAGER)).orElse(false);
    }

    public boolean hasCurrentUserRoleInTeam(Team team) {
        User user = userService.getCurrentUser();
        return teamRoleRepository.findByTeamAndAndUser(team, user).isPresent();
    }

    public Set<User> getTeamRoleUsers(Team team) {
        List<TeamRole> teamRoles = teamRoleRepository.findByTeam(team);
        Set<User> users = new HashSet<>();
        teamRoles.forEach(teamRole -> users.add(teamRole.getUser()));
        return users;
    }

    private void removeExistingUsersInTeamFromSet(Set<User> users, Team team) {
        if (team.getId() != null) {
            Set<User> existingUsers = getTeamRoleUsers(team);
            users.removeAll(existingUsers);
        }
    }

    public TeamRole changeTeamRole(TeamRole teamRole, TeamRole patchedValues) throws IllegalArgumentException {
        if (patchedValues.getName() != null)
            teamRole.setName(patchedValues.getName());
        return teamRoleRepository.save(teamRole);
    }

    private int getTeamManagersCount(Team team) {
        List<TeamRole> teamRoles = team.getTeamRoles();
        int managers = 0;
        for (TeamRole teamRole : teamRoles
        ) {
            if (teamRole.getName().equals(TeamRoles.MANAGER)) {
                managers++;
            }
        }
        return managers;
    }

    public List<TeamRole> getTeamRolesWhereManager(User user) {
        List<TeamRole> managerTeamRoles = new ArrayList<>();
        user.getTeamRoles().forEach(teamRole -> {
            if (teamRole.getName() == TeamRoles.MANAGER) {
                managerTeamRoles.add(teamRole);
            }
        });
        return managerTeamRoles;
    }

    public void leaveTeam(Team team, User user) throws NoManagersInTeamException {
    		Optional<TeamRole> optionalTeamRole = teamRoleRepository.findByTeamAndAndUser(team, user == null ? userService.getCurrentUser() : user);
            if (optionalTeamRole.isPresent()) {
            	team.getTeamRoles().remove(optionalTeamRole.get());
                if (getTeamManagersCount(team) == 0) {
                    throw new NoManagersInTeamException();
                }
                teamRepository.save(team);
            } else {
            	throw new NoManagersInTeamException();
            }
    }

	public Optional<TeamRole> findById(Long teamRoleId) {
		return teamRoleRepository.findById(teamRoleId);
	}

	public void delete(TeamRole teamRole) {
        Team team = teamRole.getTeam();
        team.getTeamRoles().remove(teamRole);
        teamRepository.save(team);
    }
}
