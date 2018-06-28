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

import java.util.Map;
import org.cdsframework.dto.PropertyBagDTO;
import org.cdsframework.exceptions.MtsException;
import org.cdsframework.rs.utils.CommonRsUtils;
import org.cdsframework.util.JsonUtils;
import org.cdsframework.util.LogUtils;
import org.cdsframework.util.StringUtils;
import org.cdsframework.util.support.CoreConstants;

/**
 *
 * @author HLN Consulting, LLC
 */
public class PropertyBagUtils {
    private static final LogUtils logger = LogUtils.getLogger(PropertyBagUtils.class.getName());
    
    public static PropertyBagDTO getJsonPropertyBagDTO(String jsonUrlEncodedString) throws MtsException {
        final String METHODNAME = "getJsonPropertyBagDTO ";

        PropertyBagDTO propertyBagDTO = new PropertyBagDTO();
        if (!StringUtils.isEmpty(jsonUrlEncodedString)) {
            Map<String, Object> propertyMap = CommonRsUtils.getMapFromEncodedString(jsonUrlEncodedString);
            propertyBagDTO.getPropertyMap().putAll(propertyMap);
            for (Map.Entry<String, Object> entry : propertyBagDTO.getPropertyMap().entrySet()) {
                logger.debug(METHODNAME, "entry.getKey())=", entry.getKey(),
                        " entry.getValue()=", entry.getValue(),
                        " entry.getValue().getClass().getSimpleName()=", entry.getValue().getClass().getSimpleName());
            }
        }

//        // Swap in default find by query class
//        if (!StringUtils.isEmpty(defaultFindByQueryClass)) {
//            String queryClass = (String) propertyBagDTO.get(CoreConstants.QUERY_CLASS);
//            if (StringUtils.isEmpty(queryClass)) {
//                propertyBagDTO.put(CoreConstants.QUERY_CLASS, defaultFindByQueryClass);
//            }
//        }
        
        // Extract query class and reposition
        String queryClass = (String) propertyBagDTO.get(CoreConstants.QUERY_CLASS);
        if (!StringUtils.isEmpty(queryClass)) {
            propertyBagDTO.setQueryClass(queryClass);
        }

        // Extract operationName and reposition
        String operationName = (String) propertyBagDTO.get(CoreConstants.OPERATION_NAME);
        if (!StringUtils.isEmpty(operationName)) {
            propertyBagDTO.setOperationName(operationName);
        }
        
        return propertyBagDTO;
    }    
    
}
