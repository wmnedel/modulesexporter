package org.jahia.modules.handler;

import org.jahia.api.Constants;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.modules.model.EnvironmentInfo;
import org.jahia.modules.ResultMessage;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.webflow.execution.RequestContext;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**    
 * Class responsible to run the migrations and export results to webflow
 */
public class ModulesMigrationHandler {

    private static final Logger logger = LoggerFactory.getLogger(ModulesMigrationHandler.class);
    private List<ResultMessage> resultReport = new ArrayList<>();
    private StringBuilder errorMessage = new StringBuilder();

    /**
     * Get a list of modules available on the local/source instance
     * @param onlyStartedModules    Indicates if only started modules will be returned
     * @return List of installed local modules
     */
    private void getLocalModules(boolean onlyStartedModules) {

        resultReport.clear();

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
                List<String> siteSettingsPaths = new ArrayList<String>();
                List<String> serverSettingsPaths = new ArrayList<String>();
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
                        final JCRNodeWrapper node = (JCRNodeWrapper) iterator.nextNode();
                        siteSettingsPaths.add(node.getPath());
                    }

                } catch (RepositoryException e) {
                    logger.error(String.format("Cannot get JCR information from module %s/%s", moduleName, moduleVersion));
                    logger.error(e.toString());
                    siteSettingsPaths.add("JCR Error");
                }

                /* Check for modules with serverSettings view */
                try {
                    JCRSessionWrapper jcrNodeWrapper = JCRSessionFactory.getInstance().getCurrentSystemSession(Constants.EDIT_WORKSPACE, null, null);

                    NodeIterator iterator = jcrNodeWrapper.getWorkspace().getQueryManager().createQuery(serverSelect + modulePath, Query.JCR_SQL2).execute().getNodes();
                    if (iterator.hasNext()) {
                        final JCRNodeWrapper node = (JCRNodeWrapper) iterator.nextNode();
                        serverSettingsPaths.add(node.getPath());
                    }

                } catch (RepositoryException e) {
                    logger.error(String.format("Cannot get JCR information from module %s/%s", moduleName, moduleVersion));
                    logger.error(e.toString());
                    serverSettingsPaths.add("JCR Error");
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

                logger.info(String.format("moduleName=%s moduleVersion=%s moduleGroupId=%s nodeTypesMixin=%s serverSettingsPaths=%s siteSettingsPaths=%s useSpring=%s",
                        moduleName,
                        moduleVersion,
                        moduleGroupId,
                        nodeTypesWithLegacyJmix.toString(),
                        serverSettingsPaths.toString(),
                        siteSettingsPaths.toString(),
                        hasSpringBean));

                ResultMessage resultMessage = new ResultMessage(moduleName,
                        moduleVersion,
                        moduleGroupId,
                        nodeTypesWithLegacyJmix.toString(),
                        serverSettingsPaths.toString(),
                        siteSettingsPaths.toString(),
                        hasSpringBean);

                this.resultReport.add(resultMessage);
            }
        }
    }


    /**     
     * Execute the migration
     * @param environmentInfo   Object containing environment information read from frontend
     * @param context           Page context
     * @return                  true if OK; otherwise false
     * @throws RepositoryException if the JCR session from formNode cannot be open
     */
    public Boolean execute(final EnvironmentInfo environmentInfo,
                           RequestContext context) throws RepositoryException {

        logger.info("Starting modules report");

        getLocalModules(environmentInfo.isSrcStartedOnly());

        if (this.errorMessage.length() > 0) {
            context.getMessageContext().addMessage(new MessageBuilder().error()
                    .defaultText("An error encountered: " + this.errorMessage).build());
            return false;
        } else {
            context.getFlowScope().put("migrationReport", this.resultReport);
        }

        logger.info("Finishing modules report");

        return true;
    }
}
