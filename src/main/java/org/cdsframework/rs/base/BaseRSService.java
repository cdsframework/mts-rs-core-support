package org.cdsframework.rs.base;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.cdsframework.base.BaseDTO;
import org.cdsframework.dto.PropertyBagDTO;
import org.cdsframework.dto.SessionDTO;
import org.cdsframework.ejb.local.GeneralMGRInterface;
import org.cdsframework.enumeration.DTOState;
import org.cdsframework.enumeration.Operation;
import org.cdsframework.exceptions.AuthenticationException;
import org.cdsframework.exceptions.AuthorizationException;
import org.cdsframework.exceptions.ConstraintViolationException;
import org.cdsframework.exceptions.MtsException;
import org.cdsframework.exceptions.NotFoundException;
import org.cdsframework.exceptions.ValidationException;
import org.cdsframework.rs.support.CoreConfiguration;
import org.cdsframework.rs.support.CoreRsConstants;
import org.cdsframework.client.MtsMGRClient;
import org.cdsframework.ejb.local.SecurityMGRInterface;
import org.cdsframework.enumeration.Environment;
import org.cdsframework.enumeration.FieldType;
import org.cdsframework.enumeration.LogLevel;
import org.cdsframework.rs.util.PropertyBagUtils;
import org.cdsframework.rs.utils.CommonRsUtils;
import org.cdsframework.util.ClassUtils;
import org.cdsframework.util.DTOProperty;
import org.cdsframework.util.DTOUtils;
import org.cdsframework.util.LogUtils;
import org.cdsframework.util.ObjectUtils;
import org.cdsframework.util.StringUtils;
import org.cdsframework.util.support.CoreConstants;

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
/**
 *
 * @author HLN Consulting, LLC
 */
public abstract class BaseRSService {

    protected LogUtils logger;
    @Context
    private UriInfo uriContext;
    @Context
    private ServletContext servletContext;
    private static Map<String, Class<? extends BaseDTO>> dtoClassMap;
    private static boolean configured = false;

    /**
     * Creates a new instance of DohmhResource
     */
    public BaseRSService() {
        this(BaseRSService.class);
    }

    public BaseRSService(Class logClass) {
        logger = LogUtils.getLogger(logClass);
    }

    public GeneralMGRInterface getGeneralMGR() throws MtsException {
        return MtsMGRClient.getGeneralMGR(CoreConfiguration.isMtsUseRemote());
    }

    public SecurityMGRInterface getSecurityMGR() throws MtsException {
        return MtsMGRClient.getSecurityMGR(CoreConfiguration.isMtsUseRemote());
    }

    @PostConstruct
    public void postConstructor() {
        initialize();
    }

    public void initialize() {
        final String METHODNAME = "initialize ";

        long start = System.nanoTime();
        try {
            if (!configured) {
                JsonInclude.Include jsonInclude = CoreConfiguration.getJsonInclude();
                boolean returnStackTrace = CoreConfiguration.isReturnStackTrace();
                boolean mtsUseRemote = CoreConfiguration.isMtsUseRemote();
                boolean gzipSupport = CoreConfiguration.isGzipSupport();
                boolean loggingFilter = CoreConfiguration.isLoggingFilter();
                Environment environment = CoreConfiguration.getEnvironment();
                logger.info(METHODNAME, "jsonInclude: ", jsonInclude,
                        " returnStackTrace: ", returnStackTrace,
                        " mtsUseRemote=", mtsUseRemote,
                        " gzipSupport=", gzipSupport,
                        " loggingFilter=", loggingFilter,
                        " environment=", environment);
                configured = true;
            }

            //
            // Get the generated class list accross all org.cdsframework.dto jars
            // To do, split map up, one for the child class see expand
            // and one for the passed in resource name in both the json string on POST/PUT
            // and on the path in GETs
            //
            if (dtoClassMap == null) {
                dtoClassMap = ClassUtils.getDtoClassMap();
            }

        } catch (Exception e) {
            logger.error(METHODNAME, "An Unexpected Exception has occurred; Message: " + e.getMessage(), e);
        } finally {
            logger.logDuration(LogLevel.DEBUG, METHODNAME, start);
        }
    }

    public <T extends BaseDTO> T findByPrimaryKeyMain(Object primaryKey, Class<T> classType, List<String> expand, String property, String sessionId) throws MtsException, NotFoundException, AuthenticationException, AuthorizationException, ValidationException {
        final String METHODNAME = "findByPrimaryKeyMain ";
//        logger.debug(METHODNAME, "primaryKey=", primaryKey, " sessionId=", sessionId);

        // Get the Primary Key DTO
        BaseDTO dto = getPrimaryKeyDTO(primaryKey, classType);

        // Create the Property Bag
        PropertyBagDTO propertyBagDTO = PropertyBagUtils.getJsonPropertyBagDTO(property);
        propertyBagDTO.setChildClassDTOs(getChildClassDTOs(expand));

        return (T) getGeneralMGR().findByPrimaryKey(dto, getSessionDTO(sessionId), propertyBagDTO);
    }

    public BaseDTO findByPrimaryKeyMain(Object primaryKey, String resource, List<String> expand, String property, String sessionId) throws MtsException, NotFoundException, AuthenticationException, AuthorizationException, ValidationException {
        return findByPrimaryKeyMain(primaryKey, getClassForResource(resource), expand, property, sessionId);
    }

    public <T extends BaseDTO> Response findByQueryListMain(String filter, Class<T> classType, List<String> expand, String property, String sessionId) throws MtsException, NotFoundException, AuthenticationException, AuthorizationException, ValidationException, ConstraintViolationException {
        final String METHODNAME = "findByQueryListMain ";
        List<T> resultDTOs = new ArrayList<T>();
        logger.debug(METHODNAME, "filter ", filter, " property ", property);

        // Evaluate property
        PropertyBagDTO propertyBagDTO = PropertyBagUtils.getJsonPropertyBagDTO(property);
        propertyBagDTO.setChildClassDTOs(getChildClassDTOs(expand));

        // Evaluate filter
        T queryDTO = getQueryMap(classType, filter, propertyBagDTO, true);
//        T queryDTO = getQueryMap(classType, filter);

        resultDTOs = getGeneralMGR().findByQueryList(queryDTO, getSessionDTO(sessionId), propertyBagDTO);
        GenericEntity<List<T>> genericEntity = new GenericEntity<List<T>>(resultDTOs) {
        };

        Response response = null;
        Object oJQueryDataTable = propertyBagDTO.get("jQueryDataTable");
        if (oJQueryDataTable != null) {
            JQueryDataTable jQueryDataTable = new JQueryDataTable();
            SessionDTO sessionDTO = getSessionDTO(sessionId);

            //
            // This executes for every page with JQueryDataTable, 
            // its likely that the database has cached the results so its shouldnt be an issue
            //
            // This query gets the filtered rowcount
            queryDTO.getQueryMap().put(CoreConstants.LAZY_ROWCOUNT, true);
            Long rowCount = getGeneralMGR().findObjectByQuery(queryDTO, sessionDTO, Long.class, propertyBagDTO);
            jQueryDataTable.setRecordsFiltered(rowCount);

            String queryClass = propertyBagDTO.getQueryClass();
            if (logger.isDebugEnabled()) {
                logger.debug(METHODNAME, "queryClass=", queryClass);
            }

            Object oRecordsTotal = propertyBagDTO.get("JQueryDataTableRecordsTotal");
            if (oRecordsTotal != null) {
                // If its not FindAll, set it to FindAll to get the total rowcount
                if (!queryClass.equalsIgnoreCase("FindAll")) {
                    queryDTO.getQueryMap().clear();
                    queryDTO.getQueryMap().put(CoreConstants.LAZY, true);
                    queryDTO.getQueryMap().put(CoreConstants.LAZY_ROWCOUNT, true);
                    propertyBagDTO.setQueryClass("FindAll");
                    if (logger.isDebugEnabled()) {
                        logger.debug(METHODNAME, "invoking queryClass=", queryClass);
                    }
                    rowCount = getGeneralMGR().findObjectByQuery(queryDTO, sessionDTO, Long.class, propertyBagDTO);
                }
            }

            // This is the total count
            jQueryDataTable.setRecordsTotal(rowCount);
            jQueryDataTable.setData(resultDTOs);
            response = Response.ok(jQueryDataTable).build();
        } else {
            response = Response.ok(genericEntity).build();
        }
        return response;
    }

    protected <T extends BaseDTO> Response getReportMain(String filter, String resource, String property, String reportParameters, String sessionId)
            throws MtsException, NotFoundException, AuthenticationException, AuthorizationException, ValidationException, ConstraintViolationException {
        final String METHODNAME = "getReportMain ";

        Response response = null;
        PropertyBagDTO propertyBagDTO = PropertyBagUtils.getJsonPropertyBagDTO(property);
        Class<? extends BaseDTO> classType = getClassForResource(resource);
        //BaseDTO queryDTO = getQueryMap(classType, filter);
        BaseDTO queryDTO = getQueryMap(classType, filter, propertyBagDTO, false);
        addReportParameters(queryDTO, reportParameters);

        String filename = getFilename(queryDTO, propertyBagDTO, sessionId);
        StreamingOutput streamingOutput = getReport(queryDTO, propertyBagDTO, sessionId);
        if (StringUtils.isEmpty(filename)) {
            response = Response.ok(streamingOutput).build();
        } else {
            String contentType = null;
            int periodPos = filename.toUpperCase().indexOf(".");
            if (periodPos >= 0) {
                String fileExtension = filename.substring(periodPos + 1);
                if (fileExtension.equalsIgnoreCase("pdf")) {
                    contentType = "application/pdf";
                } else if (fileExtension.equalsIgnoreCase("xls")) {
                    contentType = "application/vnd.ms-excel";
                } else if (fileExtension.equalsIgnoreCase("txt")) {
                    contentType = "text/plain";
                }

            }
            Response.ResponseBuilder responseBuilder = Response.ok(streamingOutput);
            responseBuilder.header("content-disposition", "attachment; filename=" + filename);
            if (!StringUtils.isEmpty(contentType)) {
                responseBuilder.header("content-type", contentType);
            }
            //response = Response.ok(streamingOutput).header("content-disposition","attachment; filename=" + filename).build();
            response = responseBuilder.build();

        }
        return response;
    }

    private <T extends BaseDTO> StreamingOutput getReport(T queryDTO, PropertyBagDTO propertyBagDTO, String sessionId)
            throws MtsException, NotFoundException, AuthenticationException, AuthorizationException, ValidationException, ConstraintViolationException {
        final String METHODNAME = "getReport ";

        final byte[] report = getGeneralMGR().getReport(queryDTO, getSessionDTO(sessionId), propertyBagDTO);
//        if (logger.isDebugEnabled()) {
        logger.info(METHODNAME, "report.length=", report.length);
        logger.info(METHODNAME, "report=", new String(report));
//        }

        StreamingOutput streamingOutput = new StreamingOutput() {
            @Override
            public void write(OutputStream out) throws IOException, WebApplicationException {
                out.write(report);
                out.flush();
            }
        };
        return streamingOutput;
    }

    protected Response exportDataMain(String filter, String resource, String property, String sessionId)
            throws MtsException, NotFoundException, AuthenticationException, AuthorizationException, ValidationException, ConstraintViolationException {
        final String METHODNAME = "getReportMain ";

        Response response = null;
        PropertyBagDTO propertyBagDTO = PropertyBagUtils.getJsonPropertyBagDTO(property);
        Class<? extends BaseDTO> classType = getClassForResource(resource);

        BaseDTO queryDTO = getQueryMap(classType, filter, propertyBagDTO, false);
        //BaseDTO queryDTO = getQueryMap(classType, filter);
        Map<String, byte[]> exportData = exportData(queryDTO, propertyBagDTO, sessionId);

        GenericEntity<Map<String, byte[]>> genericEntity = new GenericEntity<Map<String, byte[]>>(exportData) {
        };

        response = Response.ok(genericEntity).build();

        return response;
    }

    private <T extends BaseDTO> Map<String, byte[]> exportData(T queryDTO, PropertyBagDTO propertyBagDTO, String sessionId)
            throws ValidationException, NotFoundException, MtsException, AuthenticationException, AuthorizationException {
        final String METHODNAME = "exportData ";
        Map<String, byte[]> result = getGeneralMGR().exportData(queryDTO, getSessionDTO(sessionId), propertyBagDTO);
        if (logger.isDebugEnabled()) {
            logger.debug(METHODNAME, "result=", result);
        }
        return result;
    }

    protected Response importDataMain(String resource, String property, String sessionId)
            throws MtsException, NotFoundException, AuthenticationException, AuthorizationException, ValidationException, ConstraintViolationException {
        final String METHODNAME = "importDataMain ";
        PropertyBagDTO propertyBagDTO = PropertyBagUtils.getJsonPropertyBagDTO(property);
//        if (fileInputStream != null) {
//            try {
//                StringBuilder stringBuilder = new StringBuilder();
//                Reader reader = new InputStreamReader(fileInputStream);
//                int ch = reader.read();
//                while (ch >= 0) {
//                    stringBuilder.append(ch);
//                    ch = reader.read();
//                }
//                propertyBagDTO.put("payload", stringBuilder.toString());
//            } catch (IOException e) {
//                logger.error(METHODNAME, e);
//            }
//        }
        Class<? extends BaseDTO> classType = getClassForResource(resource);
        importData(classType, propertyBagDTO, sessionId);
        return Response.ok().build();
    }

    private void importData(Class dtoClass, PropertyBagDTO propertyBagDTO, String sessionId)
            throws ValidationException, NotFoundException, MtsException, AuthenticationException, AuthorizationException, ConstraintViolationException {
        getGeneralMGR().importData(dtoClass, getSessionDTO(sessionId), propertyBagDTO);
    }

//    private <T extends BaseDTO> StreamingOutput getReportMain(String filter, Class<T> classType, PropertyBagDTO propertyBagDTO, 
//            String sessionId) throws MtsException, NotFoundException, AuthenticationException, AuthorizationException, ValidationException, ConstraintViolationException {
//        final String METHODNAME = "getReportMain ";
//
//        // Evaluate filter
//        T queryDTO = getQueryMap(classType, filter);
//        final byte[] report = getGeneralMGR().getReport(queryDTO, getSessionDTO(sessionId), propertyBagDTO);
//        StreamingOutput streamingOutput = new StreamingOutput() {
//            @Override
//            public void write(OutputStream out) throws IOException, WebApplicationException {
//                out.write(report);
//            }
//        };
//        return streamingOutput;
//    }      
    protected <T extends BaseDTO> String getFilename(T queryDTO, PropertyBagDTO propertyBagDTO, String sessionId) {
        return StringUtils.getHashId(32);
    }

    private <T extends BaseDTO, S extends Object> Response findByObjectQueryListMain(String filter, Class<T> classType, Class<S> responseType,
            PropertyBagDTO propertyBagDTO, String sessionId) throws MtsException, NotFoundException, AuthenticationException, AuthorizationException, ValidationException, ConstraintViolationException {
        final String METHODNAME = "findByObjectQueryListMain ";

        // Evaluate filter
        //T queryDTO = getQueryMap(classType, filter);
        T queryDTO = getQueryMap(classType, filter, propertyBagDTO, true);

        List<S> resultDTOs = new ArrayList<S>();
        resultDTOs = getGeneralMGR().findObjectByQueryList(queryDTO, getSessionDTO(sessionId), responseType, propertyBagDTO);
        GenericEntity<List<S>> genericEntity = new GenericEntity<List<S>>(resultDTOs) {
        };
        return Response.ok(genericEntity).build();
    }

    public Response findByObjectQueryListMain(String filter, String resource, String property, String sessionId) throws MtsException, NotFoundException, AuthenticationException, AuthorizationException, ValidationException, ConstraintViolationException {
        final String METHODNAME = "findByObjectQueryListMain ";
        if (logger.isDebugEnabled()) {
            logger.debug(METHODNAME, "filter ", filter, " property ", property);
        }

        PropertyBagDTO propertyBagDTO = PropertyBagUtils.getJsonPropertyBagDTO(property);
        Class responseType = null;
        try {
            Object oResponseType = propertyBagDTO.get("responseType");
            if (oResponseType != null) {
                responseType = Class.forName((String) oResponseType);
            } else {
                throw new NotFoundException("responseType must be specified in the property query parameters, i.e; (java.lang.Long, etc)");
            }
        } catch (ClassNotFoundException ex) {
            throw new MtsException("An " + ex.getClass().getSimpleName() + " has occurred; Message: " + ex.getMessage(), ex);
        }
        return findByObjectQueryListMain(filter, getClassForResource(resource), responseType, propertyBagDTO, sessionId);
    }

    public Response findByQueryListMain(String filter, String resource, List<String> expand, String property, String sessionId) throws MtsException, NotFoundException, AuthenticationException, AuthorizationException, ValidationException, ConstraintViolationException {
        final String METHODNAME = "findByQueryListMain ";
        return findByQueryListMain(filter, getClassForResource(resource), expand, property, sessionId);
    }

    public Response saveMain(BaseDTO dto, Operation operation, String property, String sessionId)
            throws ValidationException, NotFoundException, MtsException, AuthenticationException, AuthorizationException, ConstraintViolationException {
        final String METHODNAME = "saveMain ";

        // Extract propertyBagDTO from jsonProperty string
        PropertyBagDTO propertyBagDTO = PropertyBagUtils.getJsonPropertyBagDTO(property);

        // Extract returnResource flag (String or Boolean support)
        Object oReturnResource = propertyBagDTO.get(CoreRsConstants.RS_RETURN_RESOURCE);
//        logger.debug(METHODNAME, "oReturnResource=", oReturnResource);
        boolean returnResource = false;
        if (oReturnResource != null) {
//            logger.debug(METHODNAME, "oReturnResource.getClass().getCanonicalName()=", oReturnResource.getClass().getCanonicalName());
            if (oReturnResource instanceof String) {
                returnResource = Boolean.parseBoolean((String) oReturnResource);
            } else if (oReturnResource instanceof Boolean) {
                returnResource = (Boolean) oReturnResource;
            } else {
                // Wrong type
            }
        }

        return getResponse(save(dto, operation, propertyBagDTO, sessionId), returnResource, operation);
    }

    private BaseDTO save(BaseDTO dto, Operation operation, PropertyBagDTO propertyBagDTO, String sessionId)
            throws ValidationException, NotFoundException, MtsException, AuthenticationException, AuthorizationException, ConstraintViolationException {
        final String METHODNAME = "save ";
//        logger.debug(METHODNAME, "operation=", operation, " dto.getClass().getCanonicalName()=", dto.getClass().getCanonicalName());

        String dtoName = dto.getClass().getSimpleName();
        String resource = StringUtils.unCamelize(dtoName.substring(0, dtoName.toLowerCase().indexOf("dto")));

        if (operation == Operation.ADD) {
            //
            // On a POST (create) request, the DTO State must be NEW which typically will be when the client POSTs the json payload
            // Just in case the client submitted any other DTOState, this will force it
            //
            DTOUtils.setDTOState(dto, DTOState.NEW);

        } else if (operation == Operation.UPDATE) {
            //
            // A rest client (curl, C##, Soap) will be stateless, unless it chooses to provide the DTOState 
            //
            // The logic that follows forces the MTS tier to locate the original DTO and apply any of the client 
            // requested changes on to it. With that MTS will be able to determine what has changed.
            //
            // Typically RS clients (Curl, C##, SoapUI etc) will not provide the DTOState
            //
            // On a GET request the DTOState will be UNSET and any changes will leave the DTO with UNSET DTOState 
            //
            // On a PUT (update) request the client can submit the JSON payload with out specifying the DTOState
            // (although the GET would have included an UNSET DTOState). In this case MTS will receive a DTOState of NEW for the PUT
            // Note if the JSON payload does NOT include the primary key, the PUT will return a 404
            //
            DTOState operationDTOState = dto.getOperationDTOState();
            boolean rsClient = ObjectUtils.objectToBoolean(propertyBagDTO.get(CoreRsConstants.RS_CLIENT));
            if (logger.isDebugEnabled()) {
                logger.debug(METHODNAME, "dto.getClass().getSimpleName()=", dto.getClass().getSimpleName(), " rsClient=", rsClient, " operationDTOState=", operationDTOState);
            }

            //
            // Stateless clients that are not aware of DTOState will create a json string without DTO State
            // As a result the default behavior is to refresh the DTO in the MTS BaseBO layer
            //
            if (operationDTOState != DTOState.UPDATED) {
                propertyBagDTO.put(CoreRsConstants.REFRESH_DTO, true);
            }
            // Since the propertyChangeEventMap served via a RSClient is not serializabled (JSON) 
            // There is a recursive issue PropertyChangeEvent source refers to the DTO which refers to the propertyChangeEventMap
            // A potential fix to make PropertyChangeEvent would be to exclude the 
            // PropertyChangeEvent.source nested property from the serializer but that isnt 
            // so simple as nested properties require custom logic, search for Jackon ignore nested properties
            //
            // We have to refresh the DTO in the EJB tier, see baseBO logic to see how the refresh occurs
            // Refresh is needed for NON RSClients like curl or pure javascript clients
            //

        } else if (operation == Operation.DELETE) {
            //
            // On a DELETE request, DTO State gets forced to DELETED, as long a the primary key exists and BO/Database logic is in place
            // to handle referential issues the delete will succeed
            //
            dto.delete(true);
        }

        boolean customSave = ObjectUtils.objectToBoolean(propertyBagDTO.get("customSave"));

        // Get the Session
        SessionDTO sessionDTO = getSessionDTO(sessionId);
        BaseDTO resultDTO;

        if (customSave) {
            propertyBagDTO.getPropertyMap().remove("customSave");
            resultDTO = (BaseDTO) getGeneralMGR().customSave(dto, sessionDTO, propertyBagDTO);
        } else {
            resultDTO = (BaseDTO) getGeneralMGR().save(dto, sessionDTO, propertyBagDTO);
        }

        return resultDTO;
    }

    public Response newInstanceMain(String resource, String property, String sessionId)
            throws ValidationException, NotFoundException, MtsException, AuthenticationException, AuthorizationException, ConstraintViolationException {
        final String METHODNAME = "newInstanceMain ";
        if (logger.isDebugEnabled()) {
            logger.debug(METHODNAME);
        }
        PropertyBagDTO propertyBagDTO = PropertyBagUtils.getJsonPropertyBagDTO(property);
        return getResponse(getGeneralMGR().newInstance(getClassForResource(resource), getSessionDTO(sessionId), propertyBagDTO), true, null);
    }

    public String pingMain(@QueryParam("message") String message) throws ValidationException {
        final String METHODNAME = "ping ";
        if (logger.isDebugEnabled()) {
            logger.debug(METHODNAME, "called");
        }
        return message + " received at: " + new Date();
    }

    public SessionDTO getSessionDTO(String sessionId) {
        final String METHODNAME = "getSessionDTO ";
//        logger.debug(METHODNAME, "sessionId=", sessionId);
        SessionDTO sessionDTO = new SessionDTO();
        sessionDTO.setSessionId(sessionId);
        return sessionDTO;
    }

    private List<Class<? extends BaseDTO>> getChildClassDTOs(List<String> childclasses) throws MtsException {
        final String METHODNAME = "getChildClassDTOs ";

        List<Class<? extends BaseDTO>> childClassDTOs = new ArrayList<Class<? extends BaseDTO>>();
        if (childclasses != null && !childclasses.isEmpty()) {
            for (String childClass : childclasses) {
                childClassDTOs.add(getClassForResource(childClass));
            }
        }
//        logger.debug(METHODNAME, "childClassDTOs.size()=", childClassDTOs.size());
        return childClassDTOs;
    }

    protected Class<? extends BaseDTO> getClassForResource(String resource) throws MtsException {
        final String METHODNAME = "getClassForResource ";
//        logger.debug(METHODNAME, "resource=", resource, " looking up the class");
        Class cls = dtoClassMap.get(resource);
        if (cls == null) {
            throw new MtsException("The resource " + resource + " requested does not exist, please review the API");
        }
        return cls;

    }

    /*
       This extracts queryMap values associated with the foreign key fields
       Commented out for now as DAO's throughout code and dohmh rely on dto.getters for the values in the query
       All DAO's would need to be converted to rely exclusively on the queryMap
    
    public <T extends BaseDTO> T getQueryMap(Class<T> classType, String jsonString, PropertyBagDTO propertyBagDTO, boolean callSetter) throws MtsException {
        final String METHODNAME = "getQueryMap ";
        T queryDTO = getNewDTO(classType);
        if (!StringUtils.isEmpty(jsonString)) {
            Map<String, Object> filterMap = CommonRsUtils.getMapFromEncodedString(jsonString);
            queryDTO.getQueryMap().putAll(filterMap);

            //
            // Call setter analyzes the queryClass and based on the fields in the DTOProperty
            // It calls the setter on the QueryDTO, used primarly to call the setter on ChildQuery classes
            // For example ChildDTO.ByParentId, pass in parentId as a filter in queryMap, pass in ByParentId as a queryClas
            // Logic calls ChildDTO.setParentId passing the value in the queryMap.get(parentId);
            //
            
            if (callSetter && propertyBagDTO != null) {
                String queryClass = propertyBagDTO.getQueryClass();
                
                // Cant be empty and cant be ByGeneralProperties
                if (!StringUtils.isEmpty(queryClass) && !queryClass.equalsIgnoreCase(ByGeneralProperties.class.getSimpleName())) {
                    // Record extra time im handling Query Map assignment to DTO
                    Long start = System.nanoTime();

                    DTOTable dtoTable = DTOUtils.getDTOTable(classType);
                    Class dtoClassForName;
                    try {
                        dtoClassForName = ClassUtils.dtoClassForName(queryDTO, propertyBagDTO.getQueryClass());
                    } catch (NotFoundException ex) {
                        throw new MtsException("An " + ex.getClass().getSimpleName() + " has occurred, Message: " + ex.getMessage(), ex);
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug(METHODNAME, "dtoClassForName=", dtoClassForName, " ", dtoClassForName != null ? dtoClassForName.getCanonicalName() : "NULL");
                    }
                    Map<Class, List<DTOProperty>> parentForeignKeyMap = dtoTable.getParentForeignKeyMap();
                    if (parentForeignKeyMap != null && !parentForeignKeyMap.isEmpty()) {
                        List<DTOProperty> dtoProperties = parentForeignKeyMap.get(dtoClassForName);
                        if (dtoProperties != null && !dtoProperties.isEmpty()) {
                            for (DTOProperty dtoProperty : dtoProperties) {
                                Field field = dtoProperty.getField();
                                String fieldName = field.getName();
                                if (queryDTO.getQueryMap().containsKey(fieldName)) {
                                    Object fieldValue = queryDTO.getQueryMap().get(fieldName);
                                    // Set to accessible
                                    field.setAccessible(true);
                                    try {
                                        // Apply QueryMap value to specific QueryDTO field (Handles all FieldTypes except BaseDTO
                                        field.set(queryDTO, DTOProperty.getDataValue(field, fieldValue, queryDTO));
                                    } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException | NotFoundException ex) {
                                        throw new MtsException("An " + ex.getClass().getSimpleName() + " has occurred, Message: " + ex.getMessage(), ex);
                                    }
                                }
                            }
                        }
                    }
                    if (logger.isDebugEnabled()) {
                        logger.logDuration(LogLevel.DEBUG, METHODNAME, start);
                    }
                }
            }
        }
        return queryDTO;
    }
     */
 /* REVERTED Old Version not as selective */
    public <T extends BaseDTO> T getQueryMap(Class<T> classType, String jsonString, PropertyBagDTO propertyBagDTO, boolean callSetter) throws MtsException {
        //   public <T extends BaseDTO> T getQueryMap(Class<T> classType, String jsonString) throws MtsException {
        final String METHODNAME = "getQueryMap ";
        T queryDTO = getNewDTO(classType);
        if (!StringUtils.isEmpty(jsonString)) {
            Map<String, Object> filterMap = CommonRsUtils.getMapFromEncodedString(jsonString);
            queryDTO.getQueryMap().putAll(filterMap);

            // Limit scope to calls that really need the setter called
            if (callSetter) {
                // Record extra time im handling Query Map assignment to DTO
                Long start = System.nanoTime();

                for (Map.Entry<String, Object> entry : queryDTO.getQueryMap().entrySet()) {
                    try {
                        String key = entry.getKey();
                        // Ignore these fields as they are used for paging
                        if (key.equalsIgnoreCase(CoreConstants.LAZY) || key.equalsIgnoreCase(CoreConstants.LAZY_PAGE_SIZE)
                                || key.equalsIgnoreCase(CoreConstants.LAZY_ROW_OFFSET) || key.equalsIgnoreCase(CoreConstants.SORT_FIELD)
                                || key.equalsIgnoreCase(CoreConstants.SORT_ORDER) || key.equalsIgnoreCase(CoreConstants.LAZY_ROWCOUNT)) {
                            continue;
                        }
                        Field field = classType.getDeclaredField(key);
                        // Null check probably unecessary but do it anyway
                        if (field != null) {
                            //
                            // We cant handle BaseDTO for a number of reasons
                            // 1. The QueryDTO's declared Field is a referenceDTO and its QueryDTO.getQueryMap().getKey() would need to match its Field Name
                            // 2. The QueryDTO.getQueryMap().getValue would need to contain a structure that identifies the declared Fields of the referenceDTO
                            //
                            // For example:
                            //    Assuming the Main DTO contains a referenceDTO named where the name of the referenceDTO = refDTO
                            //    1. The QueryDTO.getQueryMap().getKey()='refDTO' --> key name matches QueryDTO declared field name
                            //    2. The QueryDTO.getQueryMap().getValue() = '{ stateCode: 'NY' }', refDTO.setStateCode would be called and assigned NY
                            //    
                            //
                            // Set to accessible
                            field.setAccessible(true);

                            // Get the dataValue
                            Object dataValue = DTOProperty.getDataValue(field, entry.getValue(), queryDTO);
                            // Apply dataValue to setter for the field on the DTO (Handles all FieldTypes except BaseDTO
                            field.set(queryDTO, dataValue);

                            if (logger.isDebugEnabled()) {
                                Object oFieldValue = field.get(queryDTO);
                                logger.debug(METHODNAME, "oFieldValue=", oFieldValue, " oFieldValue.getClass().getSimpleName()=", oFieldValue.getClass().getSimpleName());
                            }

                            //
                            // It its an Enum skip it as setting it on the queryMap will cause issues in the DAO code as it expects the enum code
                            // As DAO's where coded to deal with Strings for Enums/Booleans do not apply the value back on the QueryMap
                            FieldType fieldType = FieldType.getFieldType(field.getType());
                            if (fieldType != FieldType.Enumeration && !(dataValue instanceof Boolean)) {
                                // Since via the current filter is a human readable String, every filter value is a String
                                // An Integer, Long or Date is a String, which gets applied to the DTO via the dataValue above
                                // The dataValue is the propery data type which is then applied to the queryMap
                                // When the BO/DAO receives the queryMap all the keys provided they match the DTO's properties 
                                // will be the correct type. 
                                // 
                                // Note: Its still important to use ObjectUtils in your DAO/BO calls when refering 
                                // to values stored in the QueryMap, because there are going to be keys that are still strings
                                // These are the keys that didnt map to DTO properties. See NoSuchFieldException

                                // If we introduce a JSON encoding the issue of dataType associated with NON DTO mapped keys 
                                // is no longer an issue. There are other data type precision issues accross Number types
                                // Longs loose there precision if a Long stores an Integer, Double stored a Long etc, etc, as 
                                // well as the Human Readable interface issue, where a GET is harder to construct.
                                //
                                queryDTO.getQueryMap().put(key, dataValue);
                            }
                        }
                    } catch (NoSuchFieldException ex) {
                        // As there are alot of these, log as debug
                        if (logger.isDebugEnabled()) {
                            logger.warn(METHODNAME, "A NoSuchFieldException has occurred for classType=" + classType.getSimpleName()
                                    + " for queryMap.getKey()=" + entry.getKey() + "."
                                    + " This can occur if the field in the query map doesnt exist in the DTO, Message: " + ex.getMessage());
                        }
                    } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException | NotFoundException ex) {
                        throw new MtsException("An " + ex.getClass().getSimpleName() + " has occurred, Message: " + ex.getMessage(), ex);
                    }
                    logger.logDuration(LogLevel.DEBUG, METHODNAME, start);
                }
            }
        }
        return queryDTO;
    }

    public UriInfo getUriContext() {
        return uriContext;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    private <T extends BaseDTO> T getNewDTO(Class<T> classType) throws MtsException {
        final String METHODNAME = "getNewDTO ";
        T dto = null;
        try {
            dto = classType.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            logger.error(METHODNAME, "An ", ex.getClass().getSimpleName(), " has occurred; Message: ", ex.getMessage(), ex);
            throw new MtsException("An " + ex.getClass().getSimpleName() + " has occurred; Message: " + ex.getMessage(), ex);
        }
        return dto;
    }

    protected Response getResponse(BaseDTO dto, boolean returnResource, Operation operation) {
        final String METHODNAME = "getResponse ";
        UriBuilder builder = getUriContext().getAbsolutePathBuilder();

        if (dto != null) {
            String path = getPath(dto);
            builder.path(path);
        }

        Response.ResponseBuilder responseBuilder = null;
        if (operation == Operation.ADD) {
            responseBuilder = Response.created(builder.build());
        } else {
            responseBuilder = Response.ok();
        }
        Response response = null;
        if (dto != null && returnResource) {
            response = responseBuilder.entity(dto).build();
        } else {
            response = responseBuilder.build();
//            logger.debug(METHODNAME, "response=", response);
        }
        return response;
    }

    protected Object getPrimaryKeysFromPath(String resource, List<PathSegment> path) throws MtsException {

        List<String> keys = new ArrayList<String>();
        for (PathSegment segment : path) {
            keys.add(segment.getPath());
        }

        if (keys.size() == 1) {
            return keys.get(0);
        }

        final Class<? extends BaseDTO> resourceClass = getClassForResource(resource);
        final List<Field> pkFields = DTOUtils.getPrimaryKeyFields(resourceClass);
        final Map<String, Object> primaryKeys = new HashMap<String, Object>();
        Iterator<String> kx = keys.iterator();
        Iterator<Field> fx = pkFields.iterator();
        while (fx.hasNext()) {
            Object value = null;
            if (kx.hasNext()) {
                Field pkField = fx.next();
                value = kx.next();
                if (pkField.getType() == String.class) {
                    value = ObjectUtils.objectToString(value);
                } else if (pkField.getType() == Integer.class) {
                    value = ObjectUtils.objectToInteger(value);
                } else if (pkField.getType() == Long.class) {
                    value = ObjectUtils.objectToLong(value);
                } else {
                    throw new UnsupportedOperationException(String.format("Unsupported RS primary key class: %s for DTO class %s",
                            pkField.getType().getCanonicalName(), resourceClass.getCanonicalName()));
                }
                primaryKeys.put(pkField.getName(), value);
            } else {
                throw new ClientErrorException("Incorrect number of primary keys specified", Status.BAD_REQUEST);
            }
        }
        return primaryKeys;
    }

    @SuppressWarnings("unchecked")
    protected <T> T getQueryParameter(MultivaluedMap<String, String> requestParameters, String name, Class<T> requiredType, boolean required, boolean allowNull)
            throws MtsException, ValidationException {

        if (requestParameters.containsKey(name)) {
            String value = requestParameters.getFirst(name);
            if (value != null) {
                if (String.class.isAssignableFrom(requiredType)) {
                    return (T) value;
                } else if (Integer.class.isAssignableFrom(requiredType)) {
                    try {
                        return (T) Integer.valueOf(value);
                    } catch (IllegalArgumentException e) {
                        throw new ValidationException(String.format("Query parameter %s, invalid integer value: %s", name, e.getMessage()));
                    }
                } else if (Long.class.isAssignableFrom(requiredType)) {
                    try {
                        return (T) Long.valueOf(value);
                    } catch (IllegalArgumentException e) {
                        throw new ValidationException(String.format("Query parameter %s, invalid long value: %s", name, e.getMessage()));
                    }
                } else if (Boolean.class.isAssignableFrom(requiredType)) {
                    try {
                        return (T) Boolean.valueOf(value);
                    } catch (IllegalArgumentException e) {
                        throw new ValidationException(String.format("Query parameter %s, invalid boolean value: %s", name, e.getMessage()));
                    }
                } else if (Date.class.isAssignableFrom(requiredType)) {
                    return (T) ObjectUtils.objectToDate(value);
                } // See if the required type has a String constructor and create an instance using it
                else {
                    try {
                        Constructor<T> constructor = requiredType.getConstructor(String.class);
                        return (T) constructor.newInstance(value);
                    } catch (NoSuchMethodException e) {
                        throw new IllegalArgumentException("Invalid query parameter type: '" + requiredType.getCanonicalName() + "' has no string constructor.");
                    } catch (InvocationTargetException e) {
                        throw new ValidationException("Query parameter: " + name + e.getMessage());
                    } catch (SecurityException | IllegalAccessException | InstantiationException e) {
                        e.printStackTrace();
                        throw new MtsException("Error converting query parameter value: " + e.getMessage(), e);
                    }
                }
            } else if (allowNull) {
                return null;
            } else {
                throw new ValidationException("Query parameter '" + name + " may not be null");
            }
        } else if (required) {
            throw new ValidationException("Missing query parameter: " + name);
        }

        return null;
    }

    // Decode additional encoded parameter map for reports. Initial version doesn't handle dates.
    // this is not a problem for current Jasper reports since they all take date parameters as strings
    protected void addReportParameters(BaseDTO baseDTO, String parameterString) throws ValidationException {
        if (parameterString != null) {
            try {
                Map<String, Object> parameterMap = CommonRsUtils.getMapFromJsonEncodedString(parameterString);
                baseDTO.getQueryMap().putAll(parameterMap);
            } catch (IOException e) {
                throw new ValidationException("Unable to decode report parameter string: " + e.getMessage());
            }
        }
    }

    private String getPath(BaseDTO baseDTO) {
        final String METHODNAME = "getPath ";
        String path = "";
        Object primaryKey = baseDTO.getPrimaryKey();
        if (primaryKey instanceof Map) {
            List<Field> primaryKeyFields = baseDTO.getPrimaryKeyFields();
            Map<String, Object> primaryKeyMap = (Map<String, Object>) primaryKey;
            int size = primaryKeyFields.size();
//            logger.debug(METHODNAME, "size=", size);
            int counter = 1;
            for (Field field : primaryKeyFields) {
//                logger.debug(METHODNAME, "field=", field);
//                logger.debug(METHODNAME, "field.getName()=", field.getName());
                Object value = primaryKeyMap.get(field.getName());
//                logger.debug(METHODNAME, "value=", value);

                if (value instanceof BaseDTO) {
                    path += getPath((BaseDTO) value);
                } else {
                    path += value.toString();
                }
                if (counter != size) {
                    path += "/";
                }
                counter++;

            }
        } else {
            path = baseDTO.getPrimaryKey().toString();
        }
        return path;
    }

    private BaseDTO getDTOForResource(String resource) throws MtsException {
        BaseDTO dto = null;
        try {
            // Create the Primary Key DTO
            Class<? extends BaseDTO> baseDTOClass = getClassForResource(resource);
            dto = baseDTOClass.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new MtsException("An " + ex.getClass().getSimpleName() + " has occurred; Message: " + ex.getMessage(), ex);
        }
        return dto;
    }

    private <T extends BaseDTO> T getPrimaryKeyDTO(Object primaryKey, Class<T> classType) throws MtsException {
        final String METHODNAME = "getPrimaryKeyDTO ";
//        logger.debug(METHODNAME, "primaryKey=", primaryKey);

        // Create the Primary Key DTO
        T dto = getNewDTO(classType);

        // Convert incoming Object which is a string
        if (!(primaryKey instanceof Map)) {
            // Default incoming data type
            if (primaryKey instanceof String) {
                // Convert data to appropriate type
                List<Field> primaryKeyFields = dto.getPrimaryKeyFields();
                if (primaryKeyFields.size() == 1) {
                    Class primaryKeyClass = primaryKeyFields.get(0).getType();
                    if (primaryKeyClass == Long.class) {
                        primaryKey = Long.valueOf((String) primaryKey);
                    } else if (primaryKeyClass == Integer.class) {
                        primaryKey = Integer.valueOf((String) primaryKey);
                    }
                }
                // Will fail if Primary Key is a collection
                dto.setPrimaryKey(primaryKey);
            } else {
                // Caller passes in appropriate type
                dto.setPrimaryKey(primaryKey);
            }
        } else {
            dto.setPrimaryKey(primaryKey);
        }

        return dto;
    }

    private BaseDTO getPrimaryKeyDTO(Object primaryKey, String resource) throws MtsException {
        final String METHODNAME = "getPrimaryKeyDTO ";
//        logger.debug(METHODNAME, "primaryKey=", primaryKey, " resource", resource);

        BaseDTO dto = getDTOForResource(resource);

        // Convert incoming Object which is a string
        if (!(primaryKey instanceof Map)) {
            // Default incoming data type
            if (primaryKey instanceof String) {
                // Convert data to appropriate type
                List<Field> primaryKeyFields = dto.getPrimaryKeyFields();
                if (primaryKeyFields.size() == 1) {
                    Class primaryKeyClass = primaryKeyFields.get(0).getType();
                    if (primaryKeyClass == Long.class) {
                        primaryKey = Long.valueOf((String) primaryKey);
                    } else if (primaryKeyClass == Integer.class) {
                        primaryKey = Integer.valueOf((String) primaryKey);
                    }
                }
                // Will fail if Primary Key is a collection
                dto.setPrimaryKey(primaryKey);
            } else {
                // Caller passes in appropriate type
                dto.setPrimaryKey(primaryKey);
            }
        } else {
            dto.setPrimaryKey(primaryKey);
        }

        return dto;
    }
}
