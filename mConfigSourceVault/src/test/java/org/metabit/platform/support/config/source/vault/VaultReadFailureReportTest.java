package org.metabit.platform.support.config.source.vault;

import com.bettercloud.vault.VaultException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vault is a secret store: read failures are reported by error type and HTTP status, never with the message,
 * which may quote response content.
 */
class VaultReadFailureReportTest
{
    @Test
    void messageIsNotReported()
        {
        String report = VaultConfigStorage.describeReadFailure(new VaultException("Unexpected token 's3cr3t-VALUE'", 500));
        assertEquals("VaultException (HTTP 500)", report);
        }

    @Test
    void withoutStatusOnlyTheType()
        {
        String report = VaultConfigStorage.describeReadFailure(new IllegalStateException("s3cr3t-VALUE"));
        assertEquals("IllegalStateException", report);
        }
}
//___EOF___
