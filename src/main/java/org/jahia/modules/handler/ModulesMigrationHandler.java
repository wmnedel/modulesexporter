package org.jahia.modules.handler;

import org.jahia.modules.MigrateModules;
import org.jahia.modules.ResultMessage;
import org.jahia.modules.model.EnvironmentInfo;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.render.URLGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.binding.message.MessageBuilder;
import org.springframework.webflow.execution.RequestContext;

import java.util.List;

public class ModulesMigrationHandler {

    private static final Logger logger = LoggerFactory.getLogger(ModulesMigrationHandler.class);

    public Boolean execute(final EnvironmentInfo environmentInfo,
                           final JCRNodeWrapper formNode,
                           RequestContext context,
                           URLGenerator url) throws Exception {

        logger.info("Starting modules migration");

        MigrateModules migrate = new MigrateModules();
        boolean result = migrate.migrateModules(environmentInfo, formNode, url);

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
