/**
 * CAT Core support plugin project.
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
package org.cdsframework.rs.util;

import org.cdsframework.rs.core.support.CatResourceConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Pattern.Flag;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlTransient;
import org.cdsframework.annotation.Column;
import org.cdsframework.annotation.Columns;
import org.cdsframework.annotation.EnumAccess;
import org.cdsframework.annotation.GeneratedValue;
import org.cdsframework.annotation.Id;
import org.cdsframework.annotation.Ignore;
import org.cdsframework.annotation.JndiReference;
import org.cdsframework.annotation.OrderBy;
import org.cdsframework.annotation.ParentChildRelationship;
import org.cdsframework.annotation.ParentChildRelationships;
import org.cdsframework.annotation.ReferenceDTO;
import org.cdsframework.base.BaseDTO;
import org.cdsframework.enumeration.GenerationSource;
import org.cdsframework.group.None;
import org.cdsframework.util.ClassUtils;
import org.cdsframework.util.DTOUtils;
import org.cdsframework.util.LogUtils;
import org.cdsframework.util.StringUtils;

/**
 *
 * @author sdn
 */
public class CatResourceConfigUtils {

    private static final LogUtils logger = LogUtils.getLogger(CatResourceConfigUtils.class);
    private static final java.util.regex.Pattern IGNORED_FIELD_NAME_PATTERN = java.util.regex.Pattern.compile(
            ".*\\$.*|serialVersionUID|dtoState",
            java.util.regex.Pattern.CASE_INSENSITIVE
            | java.util.regex.Pattern.MULTILINE);

    public static void preProcessCatResourceConfig(CatResourceConfig catResourceConfig) {
        String simpleName = catResourceConfig.getDtoClass().getSimpleName();
        catResourceConfig.setBaseHeader(StringUtils.unCamelize(simpleName.substring(0, simpleName.toLowerCase().indexOf("dto"))));
        initializeFieldMap(catResourceConfig);
        initializeAnnotationMap(catResourceConfig);
        initializeDefaultOrderByList(catResourceConfig);
        intializeParentChildMap(catResourceConfig);
    }

    public static void postProcessCatResourceConfig(CatResourceConfig catResourceConfig) {
        initializeSortPositions(catResourceConfig);
    }

    /**
     * Process a dto class and populate the supplied field map.
     *
     * @param catResourceConfig
     */
    private static void initializeFieldMap(CatResourceConfig catResourceConfig) {
        final String METHODNAME = "initializeFieldMap ";
        Map<String, Map<String, Object>> fieldMap = catResourceConfig.getFieldMap();
        Class<? extends BaseDTO> dtoClass = catResourceConfig.getDtoClass();
        logger.info(METHODNAME, "processing dto class: ", dtoClass);
        Class<?> superclass = dtoClass;
        while (superclass != null) {
            processDtoFields(fieldMap, superclass);
            superclass = superclass.getSuperclass();
        }
    }

    /**
     * Process a dto class and populate the parentChildMap
     *
     * @param catResourceConfig
     */
    private static void intializeParentChildMap(CatResourceConfig catResourceConfig) {
        Class<?> dtoClass = catResourceConfig.getDtoClass();
        Map<String, Map<String, Object>> parentChildMap = catResourceConfig.getParentChildMap();
        ParentChildRelationships parentChildRelationships = dtoClass.getAnnotation(ParentChildRelationships.class);
        if (parentChildRelationships != null) {
            ParentChildRelationship[] parentChildRelationshipArray = parentChildRelationships.value();
            if (parentChildRelationshipArray != null) {
                for (ParentChildRelationship parentChildRelationship : parentChildRelationshipArray) {
                    Map<String, Object> entry = new HashMap<>();
                    String resourceName = ClassUtils.getResourceName(parentChildRelationship.childDtoClass());
                    entry.put("childDtoClass", parentChildRelationship.childDtoClass());
                    entry.put("childQueryClass", parentChildRelationship.childQueryClass().getSimpleName());
                    entry.put("isAutoRetrieve", parentChildRelationship.isAutoRetrieve());
                    entry.put("childResourceName", resourceName);
                    entry.put("childResourcePlugin", getPluginFromClass(parentChildRelationship.childDtoClass()));
                    for (Method m : dtoClass.getDeclaredMethods()) {
                        if (m.getReturnType() == List.class) {
                            Type[] actualTypeArguments = ((ParameterizedType) m.getGenericReturnType()).getActualTypeArguments();
                            if (actualTypeArguments.length > 0
                                    && actualTypeArguments[0] == parentChildRelationship.childDtoClass()) {
                                XmlElementRef xmlElementRef = m.getAnnotation(XmlElementRef.class);
                                if (xmlElementRef != null) {
                                    entry.put("childAttributeName", xmlElementRef.name());
                                    break;
                                }
                            }
                        }
                    }
                    parentChildMap.put(resourceName, entry);
                }
            }
        }
    }

    /**
     * Process a dto class and populate the default sort order
     *
     * @param catResourceConfig
     */
    private static void initializeDefaultOrderByList(CatResourceConfig catResourceConfig) {
        final String METHODNAME = "initializeDefaultOrderByList ";
        Class<?> dtoClass = catResourceConfig.getDtoClass();
        Map<String, Map<String, Object>> fieldMap = catResourceConfig.getFieldMap();
        logger.info(METHODNAME, "processing field map: ", fieldMap);
        OrderBy orderBy = dtoClass.getAnnotation(OrderBy.class);
        Map<String, Map<String, Object>> defaultSortOrderList = catResourceConfig.getDefaultSortOrderList();
        if (orderBy != null) {
            String orderByString = orderBy.fields();
            if (!StringUtils.isEmpty(orderByString)) {
                for (String orderByField : orderByString.split(",")) {
                    String sortDirection = "asc";
                    int ascIndex = orderByField.toUpperCase().indexOf(" ASC");
                    int descIndex = orderByField.toUpperCase().indexOf(" DESC");
                    if (ascIndex > -1) {
                        sortDirection = "asc";
                        orderByField = orderByField.substring(0, ascIndex).trim();
                    }
                    if (descIndex > -1) {
                        sortDirection = "desc";
                        orderByField = orderByField.substring(0, descIndex).trim();
                    }
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile(".*\\((.*)\\).*");
                    java.util.regex.Matcher matcher = p.matcher(orderByField);
                    if (matcher.matches()) {
                        orderByField = matcher.group(1).trim();
                    } else {
                        orderByField = orderByField.trim();
                    }
                    String key = null;
                    for (Entry<String, Map<String, Object>> item : fieldMap.entrySet()) {
                        logger.info(METHODNAME, "processing fieldMap entry: ", item);
                        Map<String, Object> itemMap = item.getValue();
                        if (itemMap != null) {
                            Object columnNames = itemMap.get("columnNames");
                            if (columnNames instanceof List) {
                                List<String> columns = (List) columnNames;
                                for (String column : columns) {
                                    if (column != null && column.equals(orderByField)) {
                                        key = item.getKey();
                                        break;
                                    }
                                }
                                if (key != null) {
                                    break;
                                }
                            }
                        }
                    }
                    if (key != null) {
                        Map<String, Object> entryMap = new HashMap<>();
                        entryMap.put("columnName", orderByField);
                        entryMap.put("direction", sortDirection);
                        defaultSortOrderList.put(key, entryMap);
                    } else {
                        logger.warn(METHODNAME, "key not found for column name: ", orderByField);
                    }
                }
            }
        }
    }

    /**
     * Initialize the sort order positions according to the data table list.
     *
     * @param catResourceConfig
     */
    private static void initializeSortPositions(CatResourceConfig catResourceConfig) {
        final String METHODNAME = "initializeSortPositions ";
        Map<String, String> dataTableColumnMap = catResourceConfig.getDataTableColumnMap();
        List<String> dataTableColumnList = new LinkedList<>(dataTableColumnMap.keySet());
        for (Entry<String, Map<String, Object>> item : catResourceConfig.getDefaultSortOrderList().entrySet()) {
            int indexOf = dataTableColumnList.indexOf(item.getKey());
            item.getValue().put("columnPosition", indexOf);
            if (indexOf == -1) {
                logger.warn(METHODNAME, "column not found in dataTableColumnList: ", item.getKey());
            }
        }
    }

    /**
     * Process the field map attributes.
     *
     * @param fieldMap
     * @param klass
     */
    private static void processDtoFields(
            Map<String, Map<String, Object>> fieldMap,
            Class<?> klass) {
        final String METHODNAME = "processDtoFields ";
        for (Field field : klass.getDeclaredFields()) {
            XmlTransient xmlTransient = field.getAnnotation(XmlTransient.class);
            JsonProperty jsonProperty = field.getAnnotation(JsonProperty.class);
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();
            if ((xmlTransient == null || jsonProperty != null) && !fieldMap.containsKey(fieldName) && !IGNORED_FIELD_NAME_PATTERN.matcher(fieldName).matches()) {
//            if (xmlTransient == null && !fieldMap.containsKey(fieldName) && !IGNORED_FIELD_NAME_PATTERN.matcher(fieldName).matches()) {
                Map<String, Object> fieldAttribueMap = fieldMap.get(fieldName);
                if (fieldAttribueMap == null) {
                    fieldAttribueMap = new HashMap<>();
                    fieldAttribueMap.put("sortable", Boolean.TRUE);
                    fieldAttribueMap.put("defaultContent", "");
                    fieldAttribueMap.put("columnNames", getColumnNames(field));
                    fieldMap.put(fieldName, fieldAttribueMap);
                    if (fieldType.isEnum()) {
                        String keyGetter;
                        EnumAccess enumAccess = field.getAnnotation(EnumAccess.class);
                        if (enumAccess != null) {
                            keyGetter = enumAccess.getter();
                        } else {
                            keyGetter = "toString";
                        }
                        Map<String, Object> enumerationMap = new LinkedHashMap<>();
                        enumerationMap.put("enumType", fieldType.getCanonicalName());
                        Map<String, String> enumMap = new LinkedHashMap<>();
                        for (Object item : fieldType.getEnumConstants()) {
                            String label;
                            String key;
                            try {
                                Method getLabelMethod = fieldType.getMethod("getLabel");
                                label = (String) getLabelMethod.invoke(item);
                            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                                try {
                                    Method valueMethod = fieldType.getMethod("value");
                                    label = (String) valueMethod.invoke(item);
                                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException x) {
                                    logger.error(e);
                                    label = x.getMessage();
                                }
                            }
                            try {
                                Method getKeyMethod = fieldType.getMethod(keyGetter);
                                key = (String) getKeyMethod.invoke(item);
                            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                                logger.error(e);
                                key = e.getMessage();
                            }
                            enumMap.put(key, label);
                        }
                        enumerationMap.put("enumMap", enumMap);
                        fieldAttribueMap.put("enum", enumerationMap);
                    }
                }
                fieldAttribueMap.put("label", getFieldTitle(fieldName));
            } else {
                if (fieldMap.containsKey(fieldName)) {
                    logger.info(METHODNAME, "duplicate field name encountered in class: ", klass, " - ", fieldName);
                } else if (IGNORED_FIELD_NAME_PATTERN.matcher(fieldName).matches()) {
                    logger.info(METHODNAME, "field name found in ignored list: ", klass, " - ", fieldName);
                } else if ((xmlTransient != null && jsonProperty == null)) {
                    logger.info(METHODNAME, "field name found ignored due to XmlTransient annotation: ", klass, " - ", fieldName);
                }
            }
        }
    }

    /**
     * Generate the field title from the field name.
     *
     * @param fieldName
     * @return
     */
    private static String getFieldTitle(String fieldName) {
        String result = StringUtils.unCamelize(fieldName);
        result = result.replace("Id", "ID");
        result = result.replace("Last Mod ", "Last Modified ");
        return result;
    }

    /**
     * Process a dto class and populate the supplied annotation map.
     *
     * Supports: NotNull, Size, Id, Pattern
     *
     * @param annotationMap
     * @param primaryKeyList
     * @param dtoClass
     */
    private static void initializeAnnotationMap(CatResourceConfig catResourceConfig) {
        final String METHODNAME = "initializeAnnotationMap ";
        Map<String, Map<String, Object>> fieldMap = catResourceConfig.getFieldMap();
        List<String> primaryKeyList = catResourceConfig.getPrimaryKeyList();
        Class<? extends BaseDTO> dtoClass = catResourceConfig.getDtoClass();
        logger.info(METHODNAME, "processing dto class: ", dtoClass);
        Class<?> superclass = dtoClass;
        while (superclass != null) {
            processDtoFieldAnnotations(fieldMap, primaryKeyList, superclass);
            superclass = superclass.getSuperclass();
        }
    }

    /**
     * Process the various field level annotations to the field map.
     *
     * @param fieldMap
     * @param primaryKeyList
     * @param klass
     */
    private static void processDtoFieldAnnotations(Map<String, Map<String, Object>> fieldMap, List<String> primaryKeyList, Class<?> klass) {
        final String METHODNAME = "processDtoFieldAnnotations ";
        for (Field field : klass.getDeclaredFields()) {
            String fieldName = field.getName();
            logger.info(METHODNAME, "processing field: ", fieldName, "; class=", klass);
            Map<String, Object> mapEntry = fieldMap.get(fieldName);
            if (mapEntry == null) {
                mapEntry = new HashMap<>();
            }
            for (Annotation annotation : field.getAnnotations()) {
                Class<? extends Annotation> annotationClass = annotation.annotationType();
                logger.info(METHODNAME, "found annotation field: ", fieldName, " - ", annotationClass, " - ", klass);
                if (annotationClass == NotNull.class) {
                    mapEntry.put("nullable", Boolean.FALSE);
                    logger.info(METHODNAME, "found NotNull on field: ", fieldName);
                } else if (annotationClass == Size.class) {
                    Size size = (Size) annotation;
                    mapEntry.put("min", size.min());
                    mapEntry.put("max", size.max());
                    logger.info(METHODNAME, "found Size on field: ", fieldName, "; min: ", size.min(), "; max: ", size.max());
                } else if (annotationClass == Id.class) {
                    primaryKeyList.add(fieldName);
                    logger.info(METHODNAME, "found primary key on field: ", fieldName);
                } else if (annotationClass == ReferenceDTO.class) {
                    ReferenceDTO referenceDTO = (ReferenceDTO) annotation;
                    Class<?> fieldType = field.getType();
                    mapEntry.put("referenceResourcePlugin", getPluginFromClass(fieldType));
                    mapEntry.put("referenceDTO", field.getType());
                    mapEntry.put("referenceResourceName", ClassUtils.getResourceName(fieldType));
                    mapEntry.put("discardChildren", referenceDTO.discardChildren());
                    mapEntry.put("isDeleteCascade", referenceDTO.isDeleteCascade());
                    mapEntry.put("isNotFoundAllowed", referenceDTO.isNotFoundAllowed());
                    mapEntry.put("isUpdateable", referenceDTO.isUpdateable());
                    logger.info(METHODNAME, "found ReferenceDTO field: ", fieldName, " - ", fieldType);
                } else if (annotationClass == GeneratedValue.class) {
                    GeneratedValue generatedValue = (GeneratedValue) annotation;
                    if (generatedValue.source() == GenerationSource.FOREIGN_CONSTRAINT) {
                        Map<String, Object> generatedSourceMap = new HashMap<>();
                        generatedSourceMap.put("source", generatedValue.source().toString());
                        generatedSourceMap.put("dataSource", generatedValue.dataSource());
                        generatedSourceMap.put("fieldName", generatedValue.fieldName());
                        Map<String, String> sourceClassMap = new HashMap<>();
                        for (Class<?> sourceClass : generatedValue.sourceClass()) {
                            sourceClassMap.put(sourceClass.getCanonicalName(), ClassUtils.getResourceName(sourceClass));
                        }
                        generatedSourceMap.put("sourceClassMap", sourceClassMap);
                        mapEntry.put("generatedSourceMap", generatedSourceMap);
                        logger.info(METHODNAME, "found generatedValue on field: ", fieldName, " - ", generatedSourceMap);
                    }
                } else if (annotationClass == Pattern.class) {
                    Pattern pattern = (Pattern) annotation;
                    mapEntry.put("regex", pattern.regexp());
                    mapEntry.put("message", pattern.message());
                    List<String> flags = new ArrayList<>();
                    for (Flag flag : pattern.flags()) {
                        flags.add(flag.toString());
                    }
                    mapEntry.put("flags", flags);
                    logger.info(METHODNAME, "found Pattern on field: ", fieldName, "; regex: ", pattern.regexp(), "; message: ", pattern.message(), "; flags: ", flags);
                }
            }
            if (!mapEntry.isEmpty() && !fieldMap.containsKey(fieldName)) {
                fieldMap.put(fieldName, mapEntry);
            }
        }
    }

    private static List<String> getColumnNames(Field field) {
        final String METHODNAME = "getColumnNames ";
        List<String> result = new ArrayList<>();
        Column[] columnArray = null;
        Columns columns = field.getAnnotation(Columns.class);
        if (columns == null) {
            Column column = field.getAnnotation(Column.class);
            if (column == null) {
                Ignore ignore = field.getAnnotation(Ignore.class);
                if (ignore == null) {
                    final String fieldName = field.getName();
                    column = getColumn(fieldName, true, true, true, false, false, None.class);
                }
            }
            if (column != null) {
                columnArray = new Column[]{column};
            }
        } else {
            columnArray = columns.value();
        }
        if (columnArray != null) {
            for (Column item : columnArray) {
                result.add(item.name());
            }
        }
        return result;
    }

    /**
     * Construct a new Column object with the supplied properties.
     *
     * @param fieldName
     * @param insertable
     * @param updateable
     * @param addToWhereUpdate
     * @param addToWhereDelete
     * @param resultSetClass
     * @return
     */
    private static Column getColumn(final String fieldName, final boolean selectable, final boolean insertable, final boolean updateable,
            final boolean addToWhereUpdate, final boolean addToWhereDelete, final Class resultSetClass) {
        return new Column() {
            @Override
            public String name() {
                return DTOUtils.getColumnName(fieldName);
            }

            @Override
            public boolean insertable() {
                return insertable;
            }

            @Override
            public boolean updateable() {
                return updateable;
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Column.class;
            }

            @Override
            public boolean addToWhereUpdate() {
                return addToWhereUpdate;
            }

            @Override
            public boolean addToWhereDelete() {
                return addToWhereDelete;
            }

            @Override
            public Class resultSetClass() {
                return resultSetClass;
            }

            @Override
            public boolean selectable() {
                return selectable;
            }
        };
    }

    private static String getPluginFromClass(Class<?> klass) {
        final String METHODNAME = "getPluginFromClass ";
        String result = null;
        JndiReference jndiReference = klass.getAnnotation(JndiReference.class);
        if (jndiReference != null) {
            String jndiRoot = jndiReference.root();
            if (jndiRoot != null) {
                int lastIndexOfDash = jndiRoot.lastIndexOf("-");
                if (lastIndexOfDash != -1) {
                    result = jndiRoot.substring(lastIndexOfDash + 1);
                    logger.info(METHODNAME, "found plugin for class ", klass, " - ", result);
                }
            }
        }
        return result;
    }
}
