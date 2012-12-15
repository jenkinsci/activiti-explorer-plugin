package org.jenkinsci.plugins.jenkow.activiti.override;

import org.activiti.engine.identity.User;

/**
 * @author Kohsuke Kawaguchi
 */
class ImmutableUser implements User {
    private final String id,firstName,lastName,email;

    public ImmutableUser(String id, String firstName, String lastName, String email) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        throw new UnsupportedOperationException();
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        throw new UnsupportedOperationException();
    }

    public void setLastName(String lastName) {
        throw new UnsupportedOperationException();
    }

    public String getLastName() {
        return lastName;
    }

    public void setEmail(String email) {
        throw new UnsupportedOperationException();
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return "";
    }

    public void setPassword(String string) {
        throw new UnsupportedOperationException();
    }
}
