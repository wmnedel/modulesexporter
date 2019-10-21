package org.jahia.modules;


import org.jahia.bin.Action;
import org.jahia.bin.ActionResult;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExportModules extends Action {

    private static Logger logger = LoggerFactory.getLogger(ExportModules.class);

    @Override
    public ActionResult doExecute(HttpServletRequest httpServletRequest, RenderContext renderContext, Resource resource, JCRSessionWrapper jcrSessionWrapper, Map<String, List<String>> map, URLResolver urlResolver) throws Exception {

        logger.info("Modules Download started");
        final String fileName = generateFileName();
        final String query = "SELECT * FROM [jnt:moduleManagementBundle]";
        int exportedModules = 0;

        try {
            HttpServletResponse response = renderContext.getResponse();
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=all-modules-" + fileName + ".zip");

            OutputStream outputStream = response.getOutputStream();
            ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
            NodeIterator iterator = jcrSessionWrapper.getWorkspace().getQueryManager().createQuery(query, Query.JCR_SQL2).execute().getNodes();

            while (iterator.hasNext()) {
                final JCRNodeWrapper node = (JCRNodeWrapper) iterator.nextNode();
                String nodeName = node.getName();
                logger.info("Compressing Node: " + nodeName);

                Node fileContent = node.getNode("jcr:content");
                InputStream content = fileContent.getProperty("jcr:data").getBinary().getStream();

                byte[] buffer = StreamUtils.copyToByteArray(content);
                ZipEntry zipEntry = new ZipEntry(nodeName);
                zipOutputStream.putNextEntry(zipEntry);
                zipOutputStream.write(buffer);
                zipOutputStream.closeEntry();
                content.close();
                exportedModules++;
            }
            outputStream.flush();
            outputStream.close();
            zipOutputStream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info(exportedModules + " modules have been exported");
        logger.info("Modules download finished");

        ActionResult result = new ActionResult(HttpServletResponse.SC_OK, "/settings." + resource.getNode().getName());

        return result;
    }

    private String generateFileName() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy-hhmmss");
        String formattedDate = dateFormat.format(cal.getTime());
        return "All-Modules-" + formattedDate;
    }
}
