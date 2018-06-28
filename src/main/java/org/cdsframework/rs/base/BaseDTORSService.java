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

import java.util.List;
import javax.annotation.PostConstruct;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.cdsframework.base.BaseDTO;
import org.cdsframework.enumeration.LogLevel;
import org.cdsframework.enumeration.Operation;
import org.cdsframework.exceptions.AuthenticationException;
import org.cdsframework.exceptions.AuthorizationException;
import org.cdsframework.exceptions.ConstraintViolationException;
import org.cdsframework.exceptions.MtsException;
import org.cdsframework.exceptions.NotFoundException;
import org.cdsframework.exceptions.ValidationException;
import org.cdsframework.rs.support.CoreRsConstants;
import org.cdsframework.util.ClassUtils;
import org.cdsframework.util.LogUtils;

/**
 *
 * @author HLN Consulting, LLC
 * @param <Dto>
 * 
 * Note: This class will at some point be deprecated
 */
public abstract class BaseDTORSService<Dto extends BaseDTO> extends BaseRSService {

    protected Class<Dto> dtoClass;
    private String defaultFindByQueryClass = null;
    
    /**
     * Creates a new instance of DohmhResource
     */
    public BaseDTORSService() {
        super(BaseDTORSService.class);
    }
    
    public BaseDTORSService(Class logClass) {
        logger = LogUtils.getLogger(logClass);
    }    

    @PostConstruct
    @Override
    public void postConstructor() {
        final String METHODNAME = "postConstructor ";
        long start = System.nanoTime();
        try {
            List<Class> typeArguments = ClassUtils.getTypeArguments(BaseDTORSService.class, getClass());
            if (dtoClass == null) {
                dtoClass = typeArguments.get(0);
            }
            initialize();
        } catch (Exception e) {
            logger.error(METHODNAME, "An Unexpected Exception has occurred; Message: " + e.getMessage(), e);
        } finally {
            logger.logDuration(LogLevel.DEBUG, METHODNAME, start);                                                
        }
    }

    public Class getDtoClass() {
        return dtoClass;
    }
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON) 
    @Produces(MediaType.APPLICATION_JSON) 
    public Response create(Dto dto, @QueryParam(CoreRsConstants.QUERYPARMPROPERTY) String property, @QueryParam(CoreRsConstants.QUERYPARMSESSION) String sessionId) throws ValidationException, NotFoundException, MtsException, AuthenticationException, AuthorizationException, ConstraintViolationException  {
        final String METHODNAME = "create ";
        if (logger.isDebugEnabled()) {
            logger.debug(METHODNAME, "dto=", dto, " property=", property, " sessionId=", sessionId);
        }
        return saveMain(dto, Operation.ADD, property, sessionId);
    }      

    @PUT
    @Path("{primaryKey}")     
    @Consumes(MediaType.APPLICATION_JSON) 
    @Produces(MediaType.APPLICATION_JSON) 
    public Response update(Dto dto, @PathParam(CoreRsConstants.PATHPARMPRIMARYKEY) String primaryKey, @QueryParam(CoreRsConstants.QUERYPARMPROPERTY) String property, @QueryParam(CoreRsConstants.QUERYPARMSESSION) String sessionId) throws ValidationException, NotFoundException, MtsException, AuthenticationException, AuthorizationException, ConstraintViolationException  {
        final String METHODNAME = "update ";
        if (logger.isDebugEnabled()) {
            logger.debug(METHODNAME, "dto=", dto, " dto.getDTOStates()=", dto.getDTOStates(), " property=", property, " sessionId=", sessionId);
        }
        // MTS Handles the case to refresh the record
        return saveMain(dto, Operation.UPDATE, property, sessionId);
    }      
    
    @DELETE
    @Path("{primaryKey}")     
    public Response delete(@PathParam(CoreRsConstants.PATHPARMPRIMARYKEY) String primaryKey, @QueryParam(CoreRsConstants.QUERYPARMPROPERTY) String property, 
            @QueryParam(CoreRsConstants.QUERYPARMSESSION) String sessionId) 
            throws ValidationException, NotFoundException, MtsException, AuthenticationException, AuthorizationException, ConstraintViolationException {
        final String METHODNAME = "delete ";
        if (logger.isDebugEnabled()) {
            logger.debug(METHODNAME, "primaryKey=", primaryKey, " property=", property, " sessionId=", sessionId);
        }
        
        // Get the DTO and delete it, its on your back end to deal with all the children (business or database layer)
        return saveMain(findByPrimaryKeyMain(primaryKey, dtoClass, null, property, sessionId), Operation.DELETE, property, sessionId);
    }    
    
    @GET
    @Path("{primaryKey}")
    @Produces(MediaType.APPLICATION_JSON)
    public Dto findByPrimaryKey(@PathParam(CoreRsConstants.PATHPARMPRIMARYKEY) String primaryKey, @QueryParam(CoreRsConstants.QUERYPARMEXPAND) List<String> expand, 
            @QueryParam(CoreRsConstants.QUERYPARMPROPERTY) String property, @QueryParam(CoreRsConstants.QUERYPARMSESSION) String sessionId) 
            throws MtsException, NotFoundException, AuthenticationException, AuthorizationException, ValidationException {
        final String METHODNAME = "findByPrimaryKey ";
        return findByPrimaryKeyMain(primaryKey, dtoClass, expand, property, sessionId);
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response findByQueryList(@QueryParam(CoreRsConstants.QUERYPARMFILTER) String filter, @QueryParam(CoreRsConstants.QUERYPARMEXPAND) List<String> expand,
            @QueryParam(CoreRsConstants.QUERYPARMPROPERTY) String property, @QueryParam(CoreRsConstants.QUERYPARMSESSION) String sessionId) 
            throws MtsException, NotFoundException, AuthenticationException, AuthorizationException, ValidationException, ConstraintViolationException {
        return findByQueryListMain(filter, dtoClass, expand, property, sessionId);
    }
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("ping")
    public String ping(@QueryParam("message") String message) throws ValidationException {
        return pingMain(message);
    }

}
