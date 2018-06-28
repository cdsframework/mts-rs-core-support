/**
 * The MTS support core project contains client related utilities, data transfer objects and remote EJB interfaces for communication with the CDS Framework Middle Tier Service.
 *
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
 * SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING,
 * BUT NOT LIMITED TO, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE COPYRIGHT HOLDERS, IF ANY, OR DEVELOPERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES, OR OTHER LIABILITY OF ANY KIND, ARISING FROM, OUT OF, OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information about this software, see https://www.hln.com/services/open-source/ or send
 * correspondence to ice@hln.com.
 */
package org.cdsframework.rs.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import org.cdsframework.rs.util.ResourceUtil;
import org.cdsframework.util.LogUtils;


/**
 *
 * @author HLN Consulting, LLC
 */
public class CoreLoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static LogUtils logger = LogUtils.getLogger(CoreLoggingFilter.class);

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final String METHODNAME = "filter ";
        StringBuilder sb = new StringBuilder();
        sb.append("User: ").append(requestContext.getSecurityContext().getUserPrincipal() == null ? "unknown"
                : requestContext.getSecurityContext().getUserPrincipal());
        String path = requestContext.getUriInfo().getPath();
        sb.append(" - Path: ").append(path);
        sb.append(" - Header: ").append(requestContext.getHeaders());
        String entity = ResourceUtil.getEntityBody(requestContext);
        sb.append(" - Entity: ").append(entity);
        logger.info(METHODNAME, "HTTP REQUEST=", sb.toString());
//        logger.debug(METHODNAME, "resourceInfo.getResourceClass()=", resourceInfo.getResourceClass());
//        logger.debug(METHODNAME, "resourceInfo.getResourceMethod()=", resourceInfo.getResourceMethod());
//        Collection<String> propertyNames = requestContext.getPropertyNames();
//        for (String property : propertyNames) {
//            logger.debug(METHODNAME, "property=", property);
//        }
//        UriInfo uriInfo = requestContext.getUriInfo();
//        logger.debug(METHODNAME, "uriInfo=", uriInfo);

    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        final String METHODNAME = "filter ";
        StringBuilder sb = new StringBuilder();
        sb.append("Header: ").append(responseContext.getHeaders());
        CoreJacksonJsonProvider coreJacksonJsonProvider = new CoreJacksonJsonProvider();
        ObjectMapper objectMapper = coreJacksonJsonProvider.getContext(ObjectMapper.class);
        sb.append(" - Entity: ").append(objectMapper.writeValueAsString(responseContext.getEntity()));
        sb.append(" - Entity: ").append(responseContext.getEntity());
        logger.info(METHODNAME, "HTTP RESPONSE=", sb.toString());
    }
}
