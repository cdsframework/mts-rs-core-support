/*
 * Copyright (C) 2016 New York City Department of Health and Mental Hygiene, Bureau of Immunization
 * Contributions by HLN Consulting, LLC
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version. You should have received a copy of the GNU Lesser
 * General Public License along with this program. If not, see <http://www.gnu.org/licenses/> for more
 * details.
 *
 * The above-named contributors (HLN Consulting, LLC) are also licensed by the New York City
 * Department of Health and Mental Hygiene, Bureau of Immunization to have (without restriction,
 * limitation, and warranty) complete irrevocable access and rights to this project.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; THE
 *
 * SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING,
 * BUT NOT LIMITED TO, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE COPYRIGHT HOLDERS, IF ANY, OR DEVELOPERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES, OR OTHER LIABILITY OF ANY KIND, ARISING FROM, OUT OF, OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information about the this software, see http://www.hln.com/ice or send
 * correspondence to ice@hln.com.
 */
package org.cdsframework.rs.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.container.ContainerRequestContext;
import org.cdsframework.rs.provider.BaseDTODeserializer;
import org.cdsframework.rs.support.CoreRsConstants;
import org.cdsframework.util.LogUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.message.internal.ReaderWriter;

/**
 *
 * @author HLN Consulting, LLC
 */
public class ResourceUtil {

    private static final LogUtils logger = LogUtils.getLogger(ResourceUtil.class);

    public static String getResourceNameFromPath(String path) {
        final String METHODNAME = "getResourceNameFromPath ";
        logger.debug(METHODNAME, "path=", path);
        String resourceName = null;
        if (path.contains(CoreRsConstants.GENERAL_RS_ROOT)) {
            int resourcePos = path.indexOf(CoreRsConstants.GENERAL_RS_ROOT);
//            logger.debug(METHODNAME, "resourcePos=", resourcePos);
            resourceName = path.substring(resourcePos + CoreRsConstants.GENERAL_RS_ROOT.length() + 1);
//            logger.debug(METHODNAME, "resourceName=", resourceName);
            // trailing /
            if (resourceName.contains("/")) {
                resourcePos = resourceName.indexOf("/");
                resourceName = resourceName.substring(0, resourcePos);
            }
        }
        logger.debug(METHODNAME, "resourceName=", resourceName);
        return resourceName;
    }

    public static String addResourceName(String resourceName, String json) throws JSONException {
        final String METHODNAME = "addResourceName ";
        logger.debug(METHODNAME, "resourceName=", resourceName);
        if (json.startsWith("[")) {
            return json;
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            jsonObject.put(BaseDTODeserializer.resourcePropertyName, resourceName);
            return jsonObject.toString();
        } catch (JSONException e) {
            logger.error(METHODNAME, "failed - resouceName=", resourceName, "; json=", json);
            throw e;
        }
    }

    public static String getEntityBody(ContainerRequestContext requestContext) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = requestContext.getEntityStream();
        final StringBuilder b = new StringBuilder();
        try {
            ReaderWriter.writeTo(in, out);

            byte[] requestEntity = out.toByteArray();
            if (requestEntity.length == 0) {
                b.append("").append("\n");
            } else {
                b.append(new String(requestEntity)).append("\n");
            }
            requestContext.setEntityStream(new ByteArrayInputStream(requestEntity));

        } catch (IOException ex) {
            //Handle logging error
        }
        return b.toString();
    }
}
