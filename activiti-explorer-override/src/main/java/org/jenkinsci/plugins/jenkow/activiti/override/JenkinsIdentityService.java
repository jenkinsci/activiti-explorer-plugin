package org.jenkinsci.plugins.jenkow.activiti.override;

import org.activiti.engine.IdentityService;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.GroupQuery;
import org.activiti.engine.identity.Picture;
import org.activiti.engine.identity.User;
import org.activiti.engine.identity.UserQuery;
import org.activiti.engine.impl.Page;
import org.activiti.engine.impl.UserQueryImpl;
import org.activiti.engine.impl.identity.Account;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.jenkinsci.plugins.activiti_explorer.dto.JenkinsService;
import org.jenkinsci.plugins.activiti_explorer.dto.UserDTO;
import org.jenkinsci.plugins.activiti_explorer.dto.UserQueryDTO;
import org.springframework.web.context.ServletContextAware;

import javax.servlet.ServletContext;
import java.util.AbstractList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * {@link IdentityService} backed by Jenkins.
 *
 * This is still a work in progress. Requires UserQuery and GroupQuery implementation.
 *
 * @author Kohsuke Kawaguchi
 */
public class JenkinsIdentityService implements IdentityService, ServletContextAware {
    private JenkinsService jenkinsService;

    public void setServletContext(ServletContext servletContext) {
        this.jenkinsService = (JenkinsService)servletContext.getAttribute(JenkinsService.class.getName());
    }

    @Override
    public User newUser(String userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveUser(User user) {
        // no-op
    }

    @Override
    public UserQuery createUserQuery() {
        return new UserQueryImpl() {
            @Override
            public List<User> executeList(CommandContext _, Page page) {
                UserQueryDTO q = new UserQueryDTO();
                q.email = email;
                q.emailLike  = emailLike;
                q.firstName = firstName;
                q.firstNameLike = firstNameLike;
                q.lastName = lastName;
                q.lastNameLike = lastNameLike;
                q.id = id;
                final List<UserDTO> v = jenkinsService.query(q);
                return new AbstractList<User>() {
                    @Override
                    public User get(int index) {
                        UserDTO u = v.get(index);
                        return new ImmutableUser(u.id,u.firstName,u.lastName,u.email);
                    }

                    @Override
                    public int size() {
                        return v.size();
                    }
                };
            }

            @Override
            public long executeCount(CommandContext _) {
                return executeList(_,null).size();
            }
        };
    }

    @Override
    public void deleteUser(String userId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Group newGroup(String groupId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public GroupQuery createGroupQuery() {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveGroup(Group group) {
        // no-op
    }

    @Override
    public void deleteGroup(String groupId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createMembership(String userId, String groupId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteMembership(String userId, String groupId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean checkPassword(String userId, String password) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setAuthenticatedUserId(String authenticatedUserId) {
    }

    @Override
    public void setUserPicture(String userId, Picture picture) {
        // no-op
    }

    @Override
    public Picture getUserPicture(String userId) {
        return null;
    }

    @Override
    public void setUserInfo(String userId, String key, String value) {
        // no-op
        // TODO: possibly support this by storing stuff as UserProperty on Jenkins
    }

    @Override
    public String getUserInfo(String userId, String key) {
        return null;
    }

    @Override
    public List<String> getUserInfoKeys(String userId) {
        return Collections.emptyList();
    }

    @Override
    public void deleteUserInfo(String userId, String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUserAccount(String userId, String userPassword, String accountName, String accountUsername, String accountPassword, Map<String, String> accountDetails) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getUserAccountNames(String userId) {
        return Collections.singletonList(userId);
    }

    @Override
    public Account getUserAccount(final String userId, final String userPassword, final String accountName) {
        return new Account() {
            @Override
            public String getName() {
                return userId;
            }

            @Override
            public String getUsername() {
                return userId;
            }

            @Override
            public String getPassword() {
                return userPassword;
            }

            @Override
            public Map<String, String> getDetails() {
                return Collections.emptyMap();
            }
        };
    }

    @Override
    public void deleteUserAccount(String userId, String accountName) {
        throw new UnsupportedOperationException();
    }
}
