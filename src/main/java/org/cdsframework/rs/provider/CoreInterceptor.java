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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import org.apache.commons.io.IOUtils;
import org.cdsframework.rs.support.CoreRsConstants;
import org.cdsframework.rs.util.ResourceUtil;
import org.cdsframework.util.LogUtils;
import org.codehaus.jettison.json.JSONException;

/**
 *
 * @author HLN Consulting, LLC
 */
@Provider
public class CoreInterceptor implements ReaderInterceptor, WriterInterceptor {

    private static LogUtils logger = LogUtils.getLogger(CoreInterceptor.class);

    @Context
    private ResourceInfo resourceInfo;
    
    @Context
    private HttpServletRequest httpServletRequest;
    
    @Override
    public void aroundWriteTo(WriterInterceptorContext writerInterceptorContext) throws IOException, WebApplicationException {
        final String METHODNAME = "aroundWriteTo ";

        //
        // This checks the header to see if the requestor wants a gzipped response
        // If so, the response is gzipped and return with content-encoding for the caller to unzip
        //
        String acceptEncoding = httpServletRequest.getHeader("accept-encoding");
//        logger.debug(METHODNAME, "acceptEncoding=", acceptEncoding);
        if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
            final OutputStream outputStream = writerInterceptorContext.getOutputStream();
            writerInterceptorContext.setOutputStream(new GZIPOutputStream(outputStream));
            writerInterceptorContext.getHeaders().putSingle("Content-Encoding", "gzip"); 
        }
        writerInterceptorContext.proceed();
    }
    
    @Override
    public Object aroundReadFrom(ReaderInterceptorContext readerInterceptorContext) throws IOException, WebApplicationException {
        final String METHODNAME = "aroundReadFrom ";
        
//        List<String> contentEncoding = readerInterceptorContext.getHeaders().get("Content-Encoding");
//        logger.debug(METHODNAME, "contentEncoding=", contentEncoding);
        List<String> acceptEncoding = readerInterceptorContext.getHeaders().get("Accept-Encoding");
//        logger.debug(METHODNAME, "acceptEncoding=", acceptEncoding);

        // If the request header indicates that context-encoding gziped, its decompressed
        if (acceptEncoding != null && acceptEncoding.contains("gzip")) {
//            logger.debug(METHODNAME, "context decompressed");
            readerInterceptorContext.setInputStream(new GZIPInputStream(readerInterceptorContext.getInputStream()));
        }

        StringBuffer requestURL = httpServletRequest.getRequestURL();
//        logger.debug(METHODNAME, "requestURL=", requestURL);                
        String requestURI = httpServletRequest.getRequestURI();

        if (requestURI.contains(CoreRsConstants.GENERAL_RS_ROOT)) {        
            String method = httpServletRequest.getMethod();
            if (!method.equalsIgnoreCase("GET")) {
                boolean byPass = false;
                if (logger.isDebugEnabled()) {
                    logger.debug(METHODNAME, "CoreRsConstants.BYPASSINTERCEPTOR=", CoreRsConstants.BYPASSINTERCEPTOR);
                }
                
                if (CoreRsConstants.BYPASSINTERCEPTOR != null) {
                    for (String byPassInterceptor : CoreRsConstants.BYPASSINTERCEPTOR) {
                        if (requestURI.contains(byPassInterceptor)) {
                            byPass = true;
                            break;
                        }
                    }
                }
                if (logger.isDebugEnabled()) {
                    logger.debug(METHODNAME, "byPass=", byPass);
                }
                
                if (!byPass) {
                    String resourceName = ResourceUtil.getResourceNameFromPath(httpServletRequest.getPathInfo());
                    if (logger.isDebugEnabled()) {
                        logger.debug(METHODNAME, "resourceName=", resourceName);
                    }

                    // For just the general rs service we need to parse out the resourceName from the path
                    try {
                        String json = IOUtils.toString(readerInterceptorContext.getInputStream()); 
    //                    logger.debug(METHODNAME, "json=", json);
                        // Add the resource to the json string for the deserializer
                        json = ResourceUtil.addResourceName(resourceName, json);
                        // Convert the entity back and store it
                        InputStream inputStream = IOUtils.toInputStream(json);            
                        readerInterceptorContext.setInputStream(inputStream);
                    } catch (JSONException ex) {
                        logger.error(METHODNAME, ex);
                    }
                }
            }
        }
        return readerInterceptorContext.proceed();
    }
    
}
