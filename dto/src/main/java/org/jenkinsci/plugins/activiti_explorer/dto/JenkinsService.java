package org.jenkinsci.plugins.activiti_explorer.dto;

import java.util.List;

/**
 * Service exposed by the Jenkins side to the Activiti Explorer side.
 *
 * @author Kohsuke Kawaguchi
 */
public interface JenkinsService {
    List<UserDTO> query(UserQueryDTO query);
}
