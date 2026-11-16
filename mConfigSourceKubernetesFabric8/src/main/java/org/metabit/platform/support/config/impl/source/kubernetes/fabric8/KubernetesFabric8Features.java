package org.metabit.platform.support.config.impl.source.kubernetes.fabric8;

import org.metabit.platform.support.config.ConfigFeature;
import org.metabit.platform.support.config.ConfigFeatureInterface;
import org.metabit.platform.support.config.ConfigFeatureBase;
import org.metabit.platform.support.config.ConfigFeatureRegistry;

/**
 * Configuration features for the Kubernetes source module.
 */
public final class KubernetesFabric8Features
{
    /**
     * If true, the Kubernetes environment check in test() will be bypassed.
     * Useful for integration testing outside of a real Kubernetes cluster.
     */
    public static final ConfigFeatureInterface BYPASS_TEST =
        new ConfigFeatureBase("KUBERNETES_BYPASS_TEST", ConfigFeature.ValueType.BOOLEAN, false);

    /**
     * Maximum size of a configuration key's value (in bytes) to be allowed for memory-based config layers.
     * Values larger than this will trigger a warning and be skipped.
     * Default: 1MB.
     */
    public static final ConfigFeatureInterface MAX_KEY_SIZE =
        new ConfigFeatureBase("KUBERNETES_MAX_KEY_SIZE", ConfigFeature.ValueType.NUMBER, 1024L * 1024L);

    /**
     * If true, secrets from Kubernetes will be allowed to be used as environment variables.
     * Default: false (for security reasons).
     */
    public static final ConfigFeatureInterface ALLOW_ENV_SECRETS =
        new ConfigFeatureBase("KUBERNETES_ALLOW_ENV_SECRETS", ConfigFeature.ValueType.BOOLEAN, false);

    /**
     * Additional directories to search for configuration files.
     * Default: /etc/config
     */
    public static final ConfigFeatureInterface CONFIG_DIR_PATHS =
        new ConfigFeatureBase("KUBERNETES_CONFIG_DIR_PATHS", ConfigFeature.ValueType.STRING, "/etc/config");

    /**
     * Additional directories to search for secret files.
     * Default: /etc/secrets
     */
    public static final ConfigFeatureInterface SECRETS_DIR_PATHS =
        new ConfigFeatureBase("KUBERNETES_SECRETS_DIR_PATHS", ConfigFeature.ValueType.STRING, "/etc/secrets");

    /**
     * Path to the Kubernetes ServiceAccount token.
     * Default: /var/run/secrets/kubernetes.io/serviceaccount/token
     */
    public static final ConfigFeatureInterface SA_TOKEN_PATH =
        new ConfigFeatureBase("KUBERNETES_SA_TOKEN_PATH", ConfigFeature.ValueType.STRING, "/var/run/secrets/kubernetes.io/serviceaccount/token");

    /**
     * API endpoint of the Kubernetes API server.
     * Default: https://kubernetes.default.svc
     */
    public static final ConfigFeatureInterface API_ENDPOINT =
        new ConfigFeatureBase("KUBERNETES_API_ENDPOINT", ConfigFeature.ValueType.STRING, "https://kubernetes.default.svc");

    /**
     * namespace for resource handles without one. If not set, the namespace of the pod's ServiceAccount
     * ({@link #SA_NAMESPACE_PATH}) is used.
     */
    public static final ConfigFeatureInterface NAMESPACE =
        new ConfigFeatureBase("KUBERNETES_NAMESPACE", ConfigFeature.ValueType.STRING, "");

    /**
     * Path to the Kubernetes ServiceAccount namespace.
     * Default: /var/run/secrets/kubernetes.io/serviceaccount/namespace
     */
    public static final ConfigFeatureInterface SA_NAMESPACE_PATH =
        new ConfigFeatureBase("KUBERNETES_SA_NAMESPACE_PATH", ConfigFeature.ValueType.STRING, "/var/run/secrets/kubernetes.io/serviceaccount/namespace");

    /**
     * Path to the Kubernetes ServiceAccount CA certificate.
     * Default: /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
     */
    public static final ConfigFeatureInterface SA_CA_CERT_PATH =
        new ConfigFeatureBase("KUBERNETES_SA_CA_CERT_PATH", ConfigFeature.ValueType.STRING, "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt");

    /**
     * Allow sending the ServiceAccount token over plain http, and accept any server certificate
     * when no CA certificate is available. For local development and tests only.
     * Default: false
     */
    public static final ConfigFeatureInterface ALLOW_INSECURE_TRANSPORT =
        new ConfigFeatureBase("KUBERNETES_ALLOW_INSECURE_TRANSPORT", ConfigFeature.ValueType.BOOLEAN, false);

    static
    {
        ConfigFeatureRegistry.register(BYPASS_TEST);
        ConfigFeatureRegistry.register(MAX_KEY_SIZE);
        ConfigFeatureRegistry.register(ALLOW_ENV_SECRETS);
        ConfigFeatureRegistry.register(CONFIG_DIR_PATHS);
        ConfigFeatureRegistry.register(SECRETS_DIR_PATHS);
        ConfigFeatureRegistry.register(SA_TOKEN_PATH);
        ConfigFeatureRegistry.register(API_ENDPOINT);
        ConfigFeatureRegistry.register(NAMESPACE);
        ConfigFeatureRegistry.register(SA_NAMESPACE_PATH);
        ConfigFeatureRegistry.register(SA_CA_CERT_PATH);
        ConfigFeatureRegistry.register(ALLOW_INSECURE_TRANSPORT);
    }

    private KubernetesFabric8Features()
    {
    }
}
