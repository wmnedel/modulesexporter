package org.jahia.modules;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import java.io.InputStream;
import java.util.Objects;

public class JahiaModule {

    private static Logger logger = LoggerFactory.getLogger(JahiaModule.class);

    private final static int MAGIC_VERSION_LEVELS_NUMBER = 10;

    public final static int JAHIA_MODULE_SOURCE_HIGHER = 1;
    public final static int JAHIA_MODULE_TARGET_HIGHER = 2;
    public final static int JAHIA_MODULE_VERSION_EQUAL = 0;

    private final static String MODULE_MANAGEMENT_QUERY = "SELECT * FROM [jnt:moduleManagementBundle] where NAME() = '";
    private final static String BUNDLES_URL = "/modules/api/bundles";

    protected final static String JAHIA_MODULE_STATE_STARTED = "STARTED";
    protected final static String JAHIA_BUNDLE_TYPE = "MODULE";

    private String name;
    private String version;
    private String state;
    private long versionNumber;

    public JahiaModule(String name, String moduleVersion, String state) {
        this.name = name;
        this.version = moduleVersion;
        this.state = state;
        this.versionNumber = createVersionNumber(moduleVersion);
    }

    public JahiaModule(String name, String moduleVersion) {
        this.name = name;
        this.version = moduleVersion;
        this.versionNumber = createVersionNumber(moduleVersion);
    }

    private long createVersionNumber(String version) {
        String versionParsed = version.replaceAll("[^0-9]", "");
        for (int i = versionParsed.length(); i < MAGIC_VERSION_LEVELS_NUMBER; i++) {
            versionParsed = versionParsed + "0";
        }

        return new Long(versionParsed);
    }

    public String getNameAndVersion() {
        return name + "-" + version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public long getVersionNumber() {
        return versionNumber;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public static int compareVersions(JahiaModule source, JahiaModule target) {

        if (source.getVersionNumber() > target.getVersionNumber()) {
            return JAHIA_MODULE_SOURCE_HIGHER;
        } else if (target.getVersionNumber() > source.getVersionNumber()) {
            return JAHIA_MODULE_TARGET_HIGHER;
        } else {
            return JAHIA_MODULE_VERSION_EQUAL;
        }
    }

    public static String installModule(
            JCRSessionWrapper jcrSessionWrapper,
            HttpConnectionHelper connection,
            JahiaModule module, boolean autoStart) {

        logger.debug("Installing module " + module.getNameAndVersion() + " on host " + connection.getHostName());
        String query = MODULE_MANAGEMENT_QUERY + module.getNameAndVersion() + ".jar'";
        String report = "";

        try {
            NodeIterator iterator = jcrSessionWrapper
                    .getWorkspace()
                    .getQueryManager()
                    .createQuery(query, Query.JCR_SQL2)
                    .execute()
                    .getNodes();

            if (iterator.getSize() < 1) {
                report = "Unable to retreive the modules binary files from JCR";
                return report;
            }

            while (iterator.hasNext()) {
                final JCRNodeWrapper node = (JCRNodeWrapper) iterator.nextNode();
                String nodeName = node.getName();
                logger.debug("Migrating Module: " + nodeName);

                Node fileContent = node.getNode("jcr:content");
                InputStream content = fileContent.getProperty("jcr:data").getBinary().getStream();

                MultipartEntityBuilder builder = MultipartEntityBuilder.create();

                builder.addTextBody("start", Boolean.toString(autoStart), ContentType.TEXT_PLAIN);
                builder.addBinaryBody("bundle", content);

                HttpEntity multipart = builder.build();
                String result = connection.executePostRequest(BUNDLES_URL, multipart);

                if (result != null) {
                    return result;
                }
            }

        } catch (PathNotFoundException e) {
            report = "Module " + module.getNameAndVersion() + ".jar was not installed. Reason: PathNotFoundException";
            logger.error(report);
            logger.debug(e.getMessage(), e);
        } catch (InvalidQueryException e) {
            report = "Module " + module.getNameAndVersion() + ".jar was not installed. Reason: InvalidQueryException";
            logger.error(report);
            logger.debug(e.getMessage(), e);
        } catch (RepositoryException e) {
            report = "Module " + module.getNameAndVersion() + ".jar was not installed. Reason: RepositoryException";
            logger.error(report);
            logger.debug(e.getMessage(), e);
        }

        if (!report.isEmpty())
            return report;

            return "Unknown error while installing the module" + module.getNameAndVersion();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JahiaModule that = (JahiaModule) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(version, that.version);

    }

    public boolean newerVersion(Object o) {
        if (this == o) return false;
        if (o == null || getClass() != o.getClass()) return false;

        JahiaModule that = (JahiaModule) o;
        return name.equals(that.name) &&
                compareVersions(this, that) == JAHIA_MODULE_SOURCE_HIGHER;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, state);
    }
}
