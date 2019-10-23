package org.jahia.modules;

import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.modules.model.EnvironmentInfo;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**    
 * Class for modules migration
 */
public class MigrateModules {

    private static final String BUNDLESINFO_URL = "/modules/api/bundles/*/_info/";
    private Logger logger = LoggerFactory.getLogger(MigrateModules.class);

    private String targetHostName;
    private String targetHttpScheme;

    private int targetPort;
    private String targetUserName;
    private String targetPassword;

    private StringBuilder errorMessage;
    private List<ResultMessage> resultReport;

    public String getErrorMessage() {
        return this.errorMessage.toString();
    }

    public void setErrorMessage(String message) {
        this.errorMessage.append("</br>" + message);
    }

    public List<ResultMessage> getResultReport() {
        return this.resultReport;
    }

    public void setReportMessage(ResultMessage message) {
        this.resultReport.add(message);
    }

    /**
     * Method to start the migration of modules from source to target
     * @param environmentInfo The environment information read from front-end and system properties
     * @param jcrNodeWrapper Node wrapper information
     * @return True if modules were sucessfully migrated, otherwise False
     * @throws RepositoryException If the JCR node cannot be accessed
     */
    public boolean migrateModules(EnvironmentInfo environmentInfo,
                                  JCRNodeWrapper jcrNodeWrapper) throws RepositoryException {

        this.errorMessage = new StringBuilder();
        this.resultReport = new ArrayList<>();

        setConnectionsDetails(environmentInfo);
        HttpConnectionHelper targetConnection = new HttpConnectionHelper(
                targetHostName, targetHttpScheme, targetPort, targetUserName, targetPassword);

        JSONObject targetBundlesJsonObj = getTargetModules(targetConnection);

        if (targetBundlesJsonObj == null){
            return false;
        }

        List<JahiaModule> targetParsedModules = parseTargetModules(
                JahiaModule.JAHIA_BUNDLE_TYPE,
                targetBundlesJsonObj);

        if (targetParsedModules == null) {
            setErrorMessage("Cannot parse modules information for target host " + targetHostName);
            return false;
        }

        List<JahiaModule> sourceModules = getLocalModules();

        if (sourceModules.size() == 0) {
            setErrorMessage("Cannot read modules information from source environment");
            return false;
        }

        int installedModules = installModules(jcrNodeWrapper.getSession(),
                targetConnection,
                sourceModules,
                targetParsedModules,
                environmentInfo.isAutoStart());

        if (installedModules == 0) {
            setErrorMessage("Unable to find any modules to install in the target instance, all modules already migrated?");
            return false;
        }

        return true;
    }

    /**
     * Install modules that have newer versions or modules that do not exist in the target,
     * @param jcrSessionWrapper the sessionWrapper to query the JCR for the module's binary
     * @param targetConnection connection to the target instance to install modules with JAHIA REST API
     * @param sourceModules the available modules of the source instance
     * @param targetParsedModules the availalbe modules in the target instance
     * @param autoStart start the module in the target instance upon installation
     * @return The number of installed modules
     */
    private int installModules(JCRSessionWrapper jcrSessionWrapper,
                               HttpConnectionHelper targetConnection,
                               List<JahiaModule> sourceModules,
                               List<JahiaModule> targetParsedModules,
                               boolean autoStart) {
        int installedModules = 0;
        for (JahiaModule sourceModule : sourceModules) {
            boolean install = true;
            for (JahiaModule targetModule : targetParsedModules) {
                if (targetModule.newerVersion(sourceModule) || targetModule.equals(sourceModule)) {
                    install = false;
                    break;
                }
            }
            if (install) {
                String result = JahiaModule.installModule(jcrSessionWrapper, targetConnection, sourceModule, autoStart);
                try {
                    JSONObject rootJsonObject = new JSONObject(result);
                    JSONArray bundleInfo = rootJsonObject.getJSONArray("bundleInfos");
                    JSONObject bundleDetails = new JSONObject(bundleInfo.getString(0));

                    String bundleName = bundleDetails.getString("symbolicName");
                    String version = bundleDetails.getString("version");
                    String bundleKey = bundleDetails.getString("key");
                    String message = rootJsonObject.getString("message");

                    ResultMessage resultMessage = new ResultMessage(bundleName, version, bundleKey, message);
                    this.resultReport.add(resultMessage);
                    String logMessage = String.format("Module %s-%s was installed in host %s result: %s", bundleName, version, targetHostName, message);
                    logger.info(logMessage);
                    installedModules++;
                } catch (JSONException e) {
                    logger.error("Error while migrating " + result, e);
                    this.setErrorMessage(result);
                }
            }
        }

        return installedModules;

    }

    /**
     * Get a JSONObject for the modules of the target server using Jahia REST API.
     * @param targetConnection Object with connection information about the target
     * @return JSON Object containing information about target modules
     */
    private JSONObject getTargetModules(HttpConnectionHelper targetConnection) {
        String responseBundlesTarget = targetConnection.executeGetRequest(BUNDLESINFO_URL);

        if (responseBundlesTarget == null) {
            setErrorMessage(targetConnection.getErrorMessage());
            return null;
        }

        JSONObject targetBundlesJsonObj = null;

        try {
            targetBundlesJsonObj = new JSONObject(responseBundlesTarget);
            return targetBundlesJsonObj;

        } catch (Exception e) {
            logger.error("Error parsing JSON from target host " + targetConnection.getHostName(), e);
            setErrorMessage("Error parsing JSON from target host " + targetConnection.getHostName());
        }

        return null;
    }

    /**
     * Parse modules to simpler List object
     * @param desiredType Type of bundle
     * @param nodesJsonObj JSON object with nodes to be parsed
     * @return JSON Object containing information about target modules
     */
    public List<JahiaModule> parseTargetModules(String desiredType, JSONObject nodesJsonObj) {
        List<JahiaModule> moduleList = new ArrayList<>();
        JSONArray jahiaNodes = nodesJsonObj.names();

        try {
            String nodeName = jahiaNodes.getString(0);
            JSONObject nodeModulesJsonObj = (JSONObject) nodesJsonObj.get(nodeName);
            JSONArray nodeModules = nodeModulesJsonObj.names();
            for (int j = 0; j < nodeModules.length(); j++) {
                String moduleName = nodeModules.getString(j);
                JSONObject moduleInfo = (JSONObject) nodeModulesJsonObj.get(moduleName);
                String type = moduleInfo.getString("type");


                if (type.equalsIgnoreCase(desiredType)) {
                    int left = moduleName.indexOf('/');
                    int right = moduleName.lastIndexOf('/');
                    int end = moduleName.length();
                    left = (left == right) ? 0 : left;

                    String parsedModuleName = moduleName.substring(left, right).replace("/", "");
                    String parsedModuleVersion = moduleName.substring(right, end).replace("/", "");

                    JahiaModule jahiaModule = new JahiaModule(parsedModuleName, parsedModuleVersion);
                    moduleList.add(jahiaModule);

                }
            }

        } catch (JSONException e) {
            logger.error("Error parsing module information. Reason JSONException", e);
            this.setErrorMessage("Error parsing module information. Reason JSONException");
            return new ArrayList<>();
        }

        return moduleList;
    }

    /**
     * Get a list of modules available on the local/source instance
     * @return List of installed local modules
     */
    private List<JahiaModule> getLocalModules() {

        Map<Bundle, JahiaTemplatesPackage> installedModules = ServicesRegistry.getInstance().getJahiaTemplateManagerService().getRegisteredBundles();

        List<JahiaModule> localModules = new ArrayList<>();
        for (Map.Entry<Bundle, JahiaTemplatesPackage> module : installedModules.entrySet()) {

            String moduleType = module.getValue().getModuleType();
            if (moduleType.equalsIgnoreCase("module")) {
                String moduleName = module.getValue().getId();
                Version version = module.getKey().getVersion();

                String moduleVersion = version.toString();
                String moduleState = module.getValue().getState().toString();

                JahiaModule jmodule = new JahiaModule(moduleName, moduleVersion, moduleState);
                localModules.add(jmodule);
            }

        }

        return localModules;
    }

    /**
     * Set connection details for source/target
     * @param environmentInfo Environment information read from Front End
     * @param url URL object
     */
    private void setConnectionsDetails(EnvironmentInfo environmentInfo) {

        targetHostName = environmentInfo.getRemoteHost();
        targetUserName = environmentInfo.getRemoteToolsUser();
        targetPassword = environmentInfo.getRemoteToolsPwd();
        targetHttpScheme = environmentInfo.getRemoteScheme();

        if (environmentInfo.getRemotePort() != 0) {
            targetPort = environmentInfo.getRemotePort();
        }
    }
}
