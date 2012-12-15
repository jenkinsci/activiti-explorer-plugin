package org.jenkinsci.plugins.activiti_explorer;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import hudson.Extension;
import hudson.model.User;
import hudson.tasks.Mailer.UserProperty;
import org.jenkinsci.plugins.activiti_explorer.dto.JenkinsService;
import org.jenkinsci.plugins.activiti_explorer.dto.UserDTO;
import org.jenkinsci.plugins.activiti_explorer.dto.UserQueryDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author Kohsuke Kawaguchi
 */
@Extension
public class JenkinsServiceImpl implements JenkinsService {
    public List<UserDTO> query(UserQueryDTO q) {
        if (q.id!=null) {
            User u = User.get(q.id, false);
            if (u!=null)
                return Collections.singletonList($(u));
            else
                return Collections.emptyList();
        }

        return query(Predicates.and(
                checker(new Function<User, String>() {
                    public String apply(User u) {
                        UserProperty m = u.getProperty(UserProperty.class);
                        return m != null ? m.getAddress() : "";
                    }
                }, literalAndLike(q.email, q.emailLike)),

                checker(new Function<User, String>() {
                    public String apply(User u) {
                        String f = u.getFullName();
                        int idx = f.lastIndexOf(' ');
                        if (idx > 0) return f.substring(0, idx);
                        else return f;
                    }
                }, literalAndLike(q.firstName, q.firstNameLike)),

                checker(new Function<User, String>() {
                    public String apply(User u) {
                        String f = u.getFullName();
                        int idx = f.lastIndexOf(' ');
                        if (idx > 0) return f.substring(idx);
                        else return f;
                    }
                }, literalAndLike(q.lastName, q.lastNameLike))));
    }

    private Predicate<User> checker(final Function<User,String> function, final Predicate<String> condition) {
        return new Predicate<User>() {
            public boolean apply(User input) {
                return condition.apply(function.apply(input));
            }
        };
    }

    private Predicate<String> literalAndLike(final String exact, String like) {
        if (exact==null && like==null)      return Predicates.alwaysTrue();

        final Pattern p = (like!=null) ? Pattern.compile(like.replace("%",".*").replace('_','?')) : null;
        return new Predicate<String>() {
            public boolean apply(String input) {
                return (exact != null && exact.equals(input))
                    || (p != null && p.matcher(input).matches());
            }
        };
    }

    private List<UserDTO> query(Predicate<User> pred) {
        List<UserDTO> r = new ArrayList<UserDTO>();
        for (User u : User.getAll())
            if (pred.apply(u))
                r.add($(u));
        return r;
    }


    private UserDTO $(User u) {
        UserDTO user = new UserDTO();
        user.id = u.getId();
        user.fullName = u.getFullName();

        // RANT: first name & last name considered harmful. See http://www.w3.org/International/questions/qa-personal-names
        int idx = user.fullName.lastIndexOf(' ');
        if (idx>0) {
            // arbitrarily split a name into two portions. this is totally error prone,
            // but at least it works when the name consists of two tokens
            user.firstName = user.fullName.substring(0,idx);
            user.lastName = user.fullName.substring(idx+1);
        } else {
            // there's really nothing sane we can do. bail out.
            user.firstName = u.getFullName();
            user.lastName = "";
        }

        user.isAdmin = false;
        user.isUser = true;

        return user;
    }
}
