package org.metabit.testing.platform.support.config.samplecode;

import org.metabit.platform.support.config.*;

import java.net.URI;
import java.util.EnumSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SampleJettyHttps
{
    private static final String COMPANY_NAME     = "metabit";
    private static final String APPLICATION_NAME = "samplecode";
/*
    void startEmbeddedJettyWithSSL()
     {
     ConfigFactory configFactory = ConfigFactoryBuilder.create(COMPANY_NAME, APPLICATION_NAME)
             .setFeature(ConfigFeature.TEST_MODE,true)
             .build());
     Configuration jettyConfig = configFactory.getConfig("embeddedJetty");

     // ...
     // see e.g.
     // https://github.com/jetty/jetty.project/blob/jetty-9.4.x/examples/embedded/src/main/java/org/eclipse/jetty/embedded/LikeJettyXml.java



     // get the keystore via mConfig


     final Server server = new Server(jettyConfig.getInteger("port"));
     HttpConfiguration httpConfig = new HttpConfiguration();
     httpConfig.setSecureScheme("https");
     // these could be improved with mConfigSimple and schemes
     httpConfig.setSecurePort(jettyConfig.getInteger("SecurePort"));
     httpConfig.setOutputBufferSize(jettyConfig.getInteger("OutputBufferSize"));
     httpConfig.setRequestHeaderSize(jettyConfig.getInteger("RequestHeaderSize"));
     httpConfig.setResponseHeaderSize(jettyConfig.getInteger("ResponseHeaderSize"));
     httpConfig.setSendServerVersion(jettyConfig.getBoolean("SendServerVersion"));
     httpConfig.setSendDateHeader(jettyConfig.getBoolean("SendDateHeader"));

     // ...

     // === jetty-http.xml ===
     ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
     http.setPort(port);
     http.setIdleTimeout(jettyConfig.getInteger("IdleTimeout"));
     server.addConnector(http);

     // === jetty-https.xml ===
     // SSL Context Factory
     // instead of fixed paths, we use mConfig BLOB functionality to get the keystore.

     // we could read the contents right away, but jetty wants the path.
     Configuration keystore = configFactory.getConfigSpecial("keystore", EnumSet.allOf(ConfigScope.class), Map.of("type", "blob"));
     URI keystorePath = keystore.getSourceLocations().get(0).getURI("", null);

     SslContextFactory sslContextFactory = new SslContextFactory.Server();
     sslContextFactory.setKeyStorePath(keystorePath.toString());
     sslContextFactory.setKeyStorePassword("OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4");
     sslContextFactory.setKeyManagerPassword("OBF:1u2u1wml1z7s1z7a1wnl1u2g");
     sslContextFactory.setTrustStorePath(keystorePath.toString());
     sslContextFactory.setTrustStorePassword("OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4");



     }
    */

}
