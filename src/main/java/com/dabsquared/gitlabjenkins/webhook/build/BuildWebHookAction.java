package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import com.dabsquared.gitlabjenkins.webhook.WebHookAction;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Item;
import hudson.model.Job;
import hudson.security.Permission;
import hudson.util.HttpResponses;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerResponse2;

/**
 * @author Xinran Xiao
 */
abstract class BuildWebHookAction implements WebHookAction {

    private static final Logger LOGGER = Logger.getLogger(BuildWebHookAction.class.getName());

    abstract void processForCompatibility();

    abstract void execute();

    public final void execute(StaplerResponse2 response) {
        processForCompatibility();
        execute();
    }

    protected abstract static class TriggerNotifier implements Runnable {

        private final Item project;
        private final byte[] hashedSecretToken;
        private final Authentication authentication;

        public TriggerNotifier(Item project, String secretToken, Authentication authentication) {
            this.project = project;
            /* secretToken may be null, but we want constant time comparison of tokens */
            /* Remember secretToken was passed as null, then handle it as non-matchinng later */
            this.hashedSecretToken = secretToken != null ? hashedBytes(secretToken) : null;
            this.authentication = authentication;
        }

        @NonNull
        private static byte[] hashedBytes(@NonNull String token) {
            final String HASH_ALGORITHM = "SHA-256";
            try {
                MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
                return digest.digest(token.getBytes(StandardCharsets.UTF_8));
            } catch (NoSuchAlgorithmException e) {
                throw new AssertionError("Hash algorithm " + HASH_ALGORITHM + " not found", e);
            }
        }

        /* Constant time comparison of token argument and secretToken that was
         * passed to the constructor. If a null secretToken was passed to the
         * constructor, this method must still perform constant time comparison.
         */
        private boolean tokenMatches(@NonNull String token) {
            byte[] tokenBytes = hashedBytes(token);
            if (hashedSecretToken != null) {
                return MessageDigest.isEqual(tokenBytes, hashedSecretToken);
            }

            // assure the isEqual comparison compares same number of bytes
            byte[] secretTokenBytes = tokenBytes.clone();
            // change last byte to assure the isEqual comparison will not match
            secretTokenBytes[secretTokenBytes.length - 1] ^= 1 << 3;
            return MessageDigest.isEqual(tokenBytes, secretTokenBytes);
        }

        public void run() {
            GitLabPushTrigger trigger = GitLabPushTrigger.getFromJob((Job<?, ?>) project);
            if (trigger != null) {
                if (StringUtils.isEmpty(trigger.getSecretToken())) {
                    checkPermission(Item.BUILD, project);
                } else if (!tokenMatches(trigger.getSecretToken())) {
                    throw HttpResponses.errorWithoutStack(401, "Invalid token");
                }
                performOnPost(trigger);
            }
        }

        private void checkPermission(Permission permission, Item project) {
            if (((GitLabConnectionConfig)
                            Objects.requireNonNull(Jenkins.get().getDescriptor(GitLabConnectionConfig.class)))
                    .isUseAuthenticatedEndpoint()) {
                if (!project.getACL().hasPermission(authentication, permission)) {
                    String message = "%s is missing the %s/%s permission"
                            .formatted(authentication.getName(), permission.group.title, permission.name);
                    LOGGER.finest("Unauthorized (Did you forget to add API Token to the web hook ?)");
                    throw HttpResponses.errorWithoutStack(403, message);
                }
            }
        }

        protected abstract void performOnPost(GitLabPushTrigger trigger);
    }
}
