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
package org.cdsframework.rs.base;

import org.cdsframework.rs.provider.CORSResponseFilter;
import org.cdsframework.rs.provider.CoreInterceptor;
import org.cdsframework.rs.provider.CoreLoggingFilter;
import org.cdsframework.rs.provider.GenericExceptionMapper;
import org.cdsframework.rs.support.CoreConfiguration;
import org.cdsframework.util.LogUtils;
import org.glassfish.jersey.client.filter.EncodingFilter;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;

/**
 *
 * @author HLN Consulting, LLC
 */
public class BaseResourceConfig extends ResourceConfig {
    private final LogUtils logger = LogUtils.getLogger(getClass());
    
    public BaseResourceConfig() {
        final String METHODNAME = "constructor ";
        logger.info(METHODNAME, "CoreConfiguration.isGzipSupport()=", CoreConfiguration.isGzipSupport());
        logger.info(METHODNAME, "CoreConfiguration.isLoggingFilter()=", CoreConfiguration.isLoggingFilter());

        register(JacksonFeature.class);
        register(GenericExceptionMapper.class);
        
        if (CoreConfiguration.isLoggingFilter()) {
            register(LoggingFeature.class);
            register(CoreLoggingFilter.class);
        }
        if (CoreConfiguration.isGzipSupport()) {
            register(EncodingFilter.class);
            register(GZipEncoder.class);
//            register(DeflateEncoder.class);            
        }
        // Handles Resource lookup from path and GZIP read/write
        register(CoreInterceptor.class);
        
        // Handle Cross-Origin Resource Sharing
        register(CORSResponseFilter.class);

        // handle multipart form submission
        register(MultiPartFeature.class);

    }
    
}