package org.jahia.modules.handler;

import org.jahia.modules.MigrateModules;
import org.jahia.modules.ResultMessage;
import org.jahia.modules.model.EnvironmentInfo;
import org.jahia.services.content.JCRNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.webflow.execution.RequestContext;

import javax.jcr.RepositoryException;
import java.util.List;

/**    
 * Class responsible to run the migrations and export results to webflow
 */
public class ModulesMigrationHandler {

    private static final Logger logger = LoggerFactory.getLogger(ModulesMigrationHandler.class);

    /**     
     * Execute the migration
     * @param environmentInfo   Object containing environment information read from frontend
     * @param formNode          JCR node information from frontend
     * @param context           Page context
     * @return                  true if OK; otherwise false
     * @throws RepositoryException if the JCR session from formNode cannot be open
     */
    public Boolean execute(final EnvironmentInfo environmentInfo,
                           final JCRNodeWrapper formNode,
                           RequestContext context) throws RepositoryException {

        logger.info("Starting modules migration");

        MigrateModules migrate = new MigrateModules();
        boolean result = migrate.migrateModules(environmentInfo, formNode);

        if (result) {
            List<ResultMessage> report  = migrate.getResultReport();

            context.getFlowScope().put("migrationReport", report);
            logger.info("Finishing modules migration");
           return true;
        } else {
            String errorMessage = migrate.getErrorMessage();

            context.getMessageContext().addMessage(new MessageBuilder().error()
                    .defaultText("An error encountered: " + errorMessage).build());
        }

        return false;
    }
}
