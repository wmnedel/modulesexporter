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

/**    
 * Jahia module Class definition
 */
public class JahiaModule {

    private static Logger logger = LoggerFactory.getLogger(JahiaModule.class);

    public static final int JAHIA_MODULE_SOURCE_HIGHER = 1;
    public static final int JAHIA_MODULE_TARGET_HIGHER = 2;
    public static final int JAHIA_MODULE_VERSION_EQUAL = 0;
    protected static final String JAHIA_BUNDLE_TYPE = "MODULE";
    private static final String MODULE_MANAGEMENT_QUERY = "SELECT * FROM [jnt:moduleManagementBundle] where NAME() = '";
    private static final String BUNDLES_URL = "/modules/api/bundles";
    private static final int MAGIC_VERSION_LEVELS_NUMBER = 10;

    private String name;
    private String version;
    private String state;
    private long versionNumber;

    /**     
     * Constructor for JahiaModule
     * @param name          Module name
     * @param moduleVersion Module version
     * @param state         Module current state
     */
    public JahiaModule(String name, String moduleVersion, String state) {
        this.name = name;
        this.version = moduleVersion;
        this.state = state;
        this.versionNumber = createVersionNumber(moduleVersion);
    }

    /**     
     * Constructor for JahiaModule
     * @param name          Module name
     * @param moduleVersion Module version
     */
    public JahiaModule(String name, String moduleVersion) {
        this.name = name;
        this.version = moduleVersion;
        this.versionNumber = createVersionNumber(moduleVersion);
    }

    /**     
     * Creates a fixed lenght version number for further comparison. It appends up to 10 zeros at the end of the version
     * so the number can be compared
     * @param version Module version
     */
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

    /**     
     * Method to compare Jahia module versions
     * @param source    Source module object
     * @param target    Target module object
     * @return  JAHIA_MODULE_SOURCE_HIGHER if source version is higher
     *          JAHIA_MODULE_TARGET_HIGHER if target version is higher
     *          JAHIA_MODULE_VERSION_EQUAL if versions are equal
     */
    public static int compareVersions(JahiaModule source, JahiaModule target) {

        if (source.getVersionNumber() > target.getVersionNumber()) {
            return JAHIA_MODULE_SOURCE_HIGHER;
        } else if (target.getVersionNumber() > source.getVersionNumber()) {
            return JAHIA_MODULE_TARGET_HIGHER;
        } else {
            return JAHIA_MODULE_VERSION_EQUAL;
        }
    }

    /**     
     * Performs the module installation in Jahia
     * @param jcrSessionWrapper     The current JCR session
     * @param connection            Connection object
     * @param module                Jahia module object
     * @param autoStart             Indicates if the module will be started after installation in target
     * @return  Message to be reported in frontend for this module at the end of the migration
     */
    public static String installModule(
            JCRSessionWrapper jcrSessionWrapper,
            HttpConnectionHelper connection,
            JahiaModule module, boolean autoStart) {

        logger.debug("Installing module {} on host {}", module.getNameAndVersion(), connection.getHostName());
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
                logger.debug("Migration Module {}", nodeName);

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
            report = String.format("Module %s.jar was not installed. Reason: PathNotFoundException", module.getNameAndVersion());
            logger.error(report);
            logger.debug(e.getMessage(), e);
        } catch (InvalidQueryException e) {
            report = String.format("Module %s.jar was not installed. Reason: InvalidQueryException", module.getNameAndVersion());
            logger.error(report);
            logger.debug(e.getMessage(), e);
        } catch (RepositoryException e) {
            report = String.format("Module %s.jar was not installed. Reason: RepositoryException", module.getNameAndVersion());
            logger.error(report);
            logger.debug(e.getMessage(), e);
        }

        if (!report.isEmpty()) {
            return report;
        }

        return "Unknown error while installing the module" + module.getNameAndVersion();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JahiaModule that = (JahiaModule) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(version, that.version);
    }

    /**     
     * Indicates if the object passed as parameter has a newer version than "this"
     * @param o Object to be compared to this
     * @return  true if newer; false if older or equal
     */
    public boolean newerVersion(Object o) {
        if (this == o) {
            return false;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JahiaModule that = (JahiaModule) o;
        return name.equals(that.name) &&
                compareVersions(this, that) == JAHIA_MODULE_SOURCE_HIGHER;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, state);
    }
}
