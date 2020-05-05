package org.jahia.modules;

import org.jahia.api.Constants;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.modules.model.EnvironmentInfo;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *    
 * Class for modules migration
 */
public class MigrateModules {

    private Logger logger = LoggerFactory.getLogger(MigrateModules.class);

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
     * Get a list of modules available on the local/source instance
     * @param onlyStartedModules    Indicates if only started modules will be returned
     * @return List of installed local modules
     */
    private void getLocalModules(boolean onlyStartedModules) {

        Map<Bundle, JahiaTemplatesPackage> installedModules = ServicesRegistry.getInstance().getJahiaTemplateManagerService().getRegisteredBundles();

        for (Map.Entry<Bundle, JahiaTemplatesPackage> module : installedModules.entrySet()) {

            Bundle localBundle = module.getKey();
            JahiaTemplatesPackage localJahiaBundle = module.getValue();

            String moduleType = localJahiaBundle.getModuleType();

            if (moduleType.equalsIgnoreCase("module")) {

                String moduleName = localJahiaBundle.getId();
                String moduleState = localJahiaBundle.getState().toString().toLowerCase();

                if (onlyStartedModules == true && moduleState.contains("started") == false) {
                    continue;
                }

                Version version = localBundle.getVersion();
                String moduleVersion = version.toString();
                String moduleGroupId = localJahiaBundle.getGroupId();
                List<String> nodeTypesWithLegacyJmix = new ArrayList<String>();
                boolean hasSiteSettings = false;
                boolean hasServerSettings = false;
                boolean hasSpringBean = false;

                NodeTypeRegistry.JahiaNodeTypeIterator it = NodeTypeRegistry.getInstance().getNodeTypes(moduleName);
                for (ExtendedNodeType moduleNodeType : it) {
                    String[] declaredSupertypeNamesList = moduleNodeType.getDeclaredSupertypeNames();
                    for (String supertypeName : declaredSupertypeNamesList) {
                        if (supertypeName.toLowerCase().contains("jmix:cmcontentreedisplayable")) {
                            nodeTypesWithLegacyJmix.add(moduleNodeType.getName());
                            break;
                        }
                    }
                }

                String modulePath = String.format("AND ISDESCENDANTNODE (template, '/modules/%s/%s/')", moduleName, moduleVersion);
                final String siteSelect = "SELECT * FROM [jnt:template] As template WHERE template.[j:view] = 'siteSettings' ";
                final String serverSelect = "SELECT * FROM [jnt:template] As template WHERE template.[j:view] = 'serverSettings' ";

                /* Check for modules with siteSettings view */
                try {
                    JCRSessionWrapper jcrNodeWrapper = JCRSessionFactory.getInstance().getCurrentSystemSession(Constants.EDIT_WORKSPACE, null, null);

                    NodeIterator iterator = jcrNodeWrapper.getWorkspace().getQueryManager().createQuery(siteSelect + modulePath, Query.JCR_SQL2).execute().getNodes();
                    if (iterator.hasNext()) {
                        hasSiteSettings = true;
                    }

                } catch (RepositoryException e) {
                    logger.error(String.format("Cannot get JCR information from module %s/%s", moduleName, moduleVersion));
                    logger.error(e.toString());
                }

                /* Check for modules with serverSettings view */
                try {
                    JCRSessionWrapper jcrNodeWrapper = JCRSessionFactory.getInstance().getCurrentSystemSession(Constants.EDIT_WORKSPACE, null, null);

                    NodeIterator iterator = jcrNodeWrapper.getWorkspace().getQueryManager().createQuery(serverSelect + modulePath, Query.JCR_SQL2).execute().getNodes();
                    if (iterator.hasNext()) {
                        hasServerSettings = true;
                    }

                } catch (RepositoryException e) {
                    logger.error(String.format("Cannot get JCR information from module %s/%s", moduleName, moduleVersion));
                    logger.error(e.toString());
                }

                AbstractApplicationContext bundleContext = localJahiaBundle.getContext();
                if (bundleContext != null) {
                    if (bundleContext.getDisplayName() != null) {
                        String[] beanDefinitionNames = bundleContext.getBeanDefinitionNames();

                        for (String beanDefinitionName : beanDefinitionNames) {
                            if (beanDefinitionName.contains("springframework")) {
                                hasSpringBean = true;
                            }
                        }
                    }
                }

                String nodeTypesString = "";
                if (nodeTypesWithLegacyJmix != null) {
                    nodeTypesString = nodeTypesWithLegacyJmix.toString();
                }

                logger.error(moduleName +
                        "\t" + moduleVersion +
                        "\t" + moduleGroupId +
                        "\t" + nodeTypesString +
                        "\t" + hasServerSettings +
                        "\t" + hasSiteSettings +
                        "\t" + hasSpringBean);
            }
        }
    }

    /**
     * Method to start the migration of modules from source to target
     *
     * @param environmentInfo The environment information read from front-end and system properties
     * @return True if modules were sucessfully migrated, otherwise False
     * @throws RepositoryException If the JCR node cannot be accessed
     */
    public boolean migrateModules(EnvironmentInfo environmentInfo) throws RepositoryException {

        this.errorMessage = new StringBuilder();
        this.resultReport = new ArrayList<>();

        getLocalModules(environmentInfo.isSrcStartedOnly());

        return true;
    }
}
