/**
 * CAT Core support plugin project.
 *
 * Copyright (C) 2016 New York City Department of Health and Mental Hygiene, Bureau of Immunization
 * Contributions by HLN Consulting, LLC
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version. You should have received a copy of the GNU Lesser
 * General Public License along with this program. If not, see
 * <http://www.gnu.org/licenses/> for more details.
 *
 * The above-named contributors (HLN Consulting, LLC) are also licensed by the
 * New York City Department of Health and Mental Hygiene, Bureau of Immunization
 * to have (without restriction, limitation, and warranty) complete irrevocable
 * access and rights to this project.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; THE SOFTWARE IS PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE COPYRIGHT HOLDERS, IF ANY, OR DEVELOPERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES, OR OTHER LIABILITY OF ANY KIND, ARISING FROM, OUT OF, OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information about this software, see
 * https://www.hln.com/services/open-source/ or send correspondence to
 * ice@hln.com.
 */
package org.cdsframework.rs.core.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.bind.annotation.XmlTransient;
import org.cdsframework.base.BaseDTO;
import org.cdsframework.rs.util.CatResourceConfigUtils;
import org.cdsframework.util.ClassUtils;
import org.cdsframework.util.LogUtils;

/**
 *
 * @author sdn
 */
public abstract class CatResourceConfig implements CatResourceConfigInterface {

    private final Map<String, Object> defaultValues = new HashMap<>();
    private final Map<String, Map<String, Object>> fieldMap = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> defaultSortOrderList = new LinkedHashMap<>();
    private final Map<String, String> dataTableColumnMap = new LinkedHashMap<>();
    private String baseHeader;
    private final Class<? extends BaseDTO> dtoClass;
    @XmlTransient
    protected LogUtils logger = LogUtils.getLogger(CatResourceConfig.class);
    private String initialQueryClass = "FindAll";
    private String globalSearchQueryClass = "ByGeneralProperties";
    private int pageSize = 10;
    private String crudRsUri;
    private final String resourceName;
    private String globalSearchFieldName = "text";
    private final List<String> primaryKeyList = new ArrayList<>();
    private final Map<String, Map<String, Object>> parentChildMap = new HashMap<>();

    /**
     * Get the value of primaryKeyList
     *
     * @return the value of primaryKeyList
     */
    public List<String> getPrimaryKeyList() {
        return primaryKeyList;
    }

    @SuppressWarnings("LeakingThisInConstructor")
    public CatResourceConfig(Class<? extends BaseDTO> dtoClass) {
        this.dtoClass = dtoClass;
        this.resourceName = ClassUtils.getResourceName(dtoClass);
        logger = LogUtils.getLogger(resourceName + CatResourceConfig.class.getSimpleName());
        CatResourceConfigUtils.preProcessCatResourceConfig(this);
    }

    public Map<String, Map<String, Object>> getParentChildMap() {
        return parentChildMap;
    }

    public String getGlobalSearchFieldName() {
        return globalSearchFieldName;
    }

    public void setGlobalSearchFieldName(String globalSearchFieldName) {
        this.globalSearchFieldName = globalSearchFieldName;
    }

    public String getGlobalSearchQueryClass() {
        return globalSearchQueryClass;
    }

    public void setGlobalSearchQueryClass(String globalSearchQueryClass) {
        this.globalSearchQueryClass = globalSearchQueryClass;
    }

    /**
     * Get the value of initialQueryClass
     *
     * @return the value of initialQueryClass
     */
    public String getInitialQueryClass() {
        return initialQueryClass;
    }

    /**
     * Set the value of initialQueryClass
     *
     * @param initialQueryClass new value of initialQueryClass
     */
    public void setInitialQueryClass(String initialQueryClass) {
        this.initialQueryClass = initialQueryClass;
    }

    /**
     * Get the value of dtoClass
     *
     * @return the value of dtoClass
     */
    public Class<? extends BaseDTO> getDtoClass() {
        return dtoClass;
    }

    public void registerDefaultValue(String fieldName, Object value) {
        final String METHODNAME = "registerDefaultValue ";
        logger.info(METHODNAME, "registering default value: ", fieldName, " - ", value);
        Map<String, Object> fieldAttributeMap = fieldMap.get(fieldName);
        if (fieldAttributeMap == null) {
            throw new IllegalArgumentException("field " + fieldName + " missing from fieldMap: " + fieldMap.keySet());
        }
        defaultValues.put(fieldName, value);
        logger.info(METHODNAME, "defaultValues=", defaultValues);
    }

    public void removeFieldFromSort(String fieldName) {
        Map<String, Object> fieldAttributeMap = fieldMap.get(fieldName);
        if (fieldAttributeMap == null) {
            throw new IllegalArgumentException("field " + fieldName + " missing from fieldMap: " + fieldMap.keySet());
        }
        fieldAttributeMap.put("sortable", Boolean.FALSE);
    }

    public void addDataTableColumn(String fieldName) {
        addDataTableColumn(fieldName, fieldName);
    }

    public void addDataTableColumn(String fieldName, String columnTitle) {
        final String METHODNAME = "addDataTableColumn ";
        String matchingFieldName = fieldName;
        int dotIndexOf = fieldName.indexOf(".");
        if (dotIndexOf != -1) {
            matchingFieldName = fieldName.substring(0, dotIndexOf);
            logger.info(METHODNAME, "found dot index of - looking for ", fieldName);
        }
        if (!fieldMap.containsKey(matchingFieldName)) {
            throw new IllegalArgumentException("field " + fieldName + " missing from fieldMap: " + fieldMap.keySet());
        }
        dataTableColumnMap.put(fieldName, columnTitle);
    }

    public Map<String, String> getDataTableColumnMap() {
        return dataTableColumnMap;
    }

    /**
     * Get the value of resourceName
     *
     * @return the value of resourceName
     */
    public String getResourceName() {
        return resourceName;
    }

    public Map<String, Map<String, Object>> getDefaultSortOrderList() {
        return defaultSortOrderList;
    }

    public Map<String, Object> getDefaultValues() {
        return defaultValues;
    }

    public Map<String, Map<String, Object>> getFieldMap() {
        return fieldMap;
    }

    /**
     * Get the value of baseHeader
     *
     * @return the value of baseHeader
     */
    public String getBaseHeader() {
        return baseHeader;
    }

    /**
     * Set the value of baseHeader
     *
     * @param baseHeader new value of baseHeader
     */
    public void setBaseHeader(String baseHeader) {
        this.baseHeader = baseHeader;
    }

    /**
     * Get the value of pageSize
     *
     * @return the value of pageSize
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Set the value of pageSize
     *
     * @param pageSize new value of pageSize
     */
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    /**
     * Get the value of crudRsUri
     *
     * @return the value of crudRsUri
     */
    public String getCrudRsUri() {
        return crudRsUri;
    }

    /**
     * Set the value of crudRsUri
     *
     * @param crudRsUri new value of crudRsUri
     */
    public void setCrudRsUri(String crudRsUri) {
        this.crudRsUri = crudRsUri;
    }

    @Override
    public String toString() {
        return "ResourceConfig{" + "defaultValues=" + defaultValues + ", baseHeader=" + baseHeader + ", dtoClass=" + dtoClass + '}';
    }
}
