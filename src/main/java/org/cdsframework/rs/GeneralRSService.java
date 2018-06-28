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
package org.cdsframework.rs;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.cdsframework.base.BaseDTO;
import org.cdsframework.dto.PropertyBagDTO;
import org.cdsframework.enumeration.Operation;
import org.cdsframework.exceptions.AuthenticationException;
import org.cdsframework.exceptions.AuthorizationException;
import org.cdsframework.exceptions.ConstraintViolationException;
import org.cdsframework.exceptions.MtsException;
import org.cdsframework.exceptions.NotFoundException;
import org.cdsframework.exceptions.ValidationException;
import org.cdsframework.rs.base.BaseRSService;
import org.cdsframework.rs.support.CoreRsConstants;
import org.cdsframework.rs.util.PropertyBagUtils;
import org.glassfish.jersey.media.multipart.FormDataParam;

/**
 *
 * @author HLN Consulting, LLC
 */
@Path(CoreRsConstants.GENERAL_RS_ROOT)
public class GeneralRSService extends BaseRSService {

    public GeneralRSService() {
        super(GeneralRSService.class);
    }

    public GeneralRSService(Class logClass) {
        super(logClass);
    }

    @POST
    @Path("{resource}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(BaseDTO dto, @PathParam(CoreRsConstants.QUERYPARMRESOURCE) String resource,
            @QueryParam(CoreRsConstants.QUERYPARMPROPERTY) String property, @QueryParam(CoreRsConstants.QUERYPARMSESSION) String sessionId)
            throws ValidationException, NotFoundException, MtsException, AuthenticationException, AuthorizationException, ConstraintViolationException {
        final String METHODNAME = "create ";
        if (logger.isDebugEnabled()) {
            logger.debug(METHODNAME, "dto=", dto, " property=", property, " sessionId=", sessionId);
        }
        return saveMain(dto, Operation.ADD, property, sessionId);
    }

    @GET
    @Path("{resource}/newInstance")
    @Produces(MediaType.APPLICATION_JSON)
    public Response newInstance(@PathParam(CoreRsConstants.QUERYPARMRESOURCE) String resource,
            @QueryParam(CoreRsConstants.QUERYPARMPROPERTY) String property, @QueryParam(CoreRsConstants.QUERYPARMSESSION) String sessionId)
            throws ValidationException, NotFoundException, MtsException, AuthenticationException, AuthorizationException, ConstraintViolationException {
        final String METHODNAME = "newInstance ";
        if (logger.isDebugEnabled()) {
            logger.debug(METHODNAME, "property=", property, " sessionId=", sessionId);
        }
        return newInstanceMain(resource, property, sessionId);
    }

    @PUT
    @Path("{resource}/{primaryKey:.+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(BaseDTO dto, @PathParam(CoreRsConstants.PATHPARMPRIMARYKEY) String primaryKey, @PathParam(CoreRsConstants.QUERYPARMRESOURCE) String resource,
            @QueryParam(CoreRsConstants.QUERYPARMPROPERTY) String property, @QueryParam(CoreRsConstants.QUERYPARMSESSION) String sessionId)
            throws ValidationException, NotFoundException, MtsException, AuthenticationException, AuthorizationException, ConstraintViolationException {
        final String METHODNAME = "update ";
        if (logger.isDebugEnabled()) {
            logger.debug(METHODNAME, "dto=", dto, " property=", property, " sessionId=", sessionId);
        }
        // MTS Handles the case to refresh the record - note that primary keys must be in the entity itself
        return saveMain(dto, Operation.UPDATE, property, sessionId);
    }

    @DELETE
    @Path("{resource}/{primaryKey:.+}")
    public Response delete(@PathParam(CoreRsConstants.PATHPARMPRIMARYKEY) List<PathSegment> primaryKeys, @PathParam(CoreRsConstants.QUERYPARMRESOURCE) String resource,
            @QueryParam(CoreRsConstants.QUERYPARMPROPERTY) String property, @QueryParam(CoreRsConstants.QUERYPARMSESSION) String sessionId)
            throws ValidationException, NotFoundException, MtsException, AuthenticationException, AuthorizationException, ConstraintViolationException {
        final String METHODNAME = "delete ";
        if (logger.isDebugEnabled()) {
            logger.debug(METHODNAME, "primaryKey=", primaryKeys, " resource=", resource, " property=", property, " sessionId=", sessionId);
        }

        // Get the DTO and delete it, its on your back end to deal with all the children (business or database layer)
        return saveMain(findByPrimaryKeyMain(getPrimaryKeysFromPath(resource, primaryKeys), resource, null, null, sessionId), Operation.DELETE, property, sessionId);
    }

    @GET
    @Path("{resource}/{primaryKey:.+}")
    @Produces(MediaType.APPLICATION_JSON)
    public BaseDTO findByPrimaryKey(@PathParam(CoreRsConstants.PATHPARMPRIMARYKEY) List<PathSegment> primaryKeys, @PathParam(CoreRsConstants.QUERYPARMRESOURCE) String resource,
            @QueryParam(CoreRsConstants.QUERYPARMEXPAND) List<String> expand, @QueryParam(CoreRsConstants.QUERYPARMPROPERTY) String property, @QueryParam(CoreRsConstants.QUERYPARMSESSION) String sessionId)
            throws MtsException, NotFoundException, AuthenticationException, AuthorizationException, ValidationException {
        final String METHODNAME = "findByPrimaryKey ";
        return findByPrimaryKeyMain(getPrimaryKeysFromPath(resource, primaryKeys), resource, expand, property, sessionId);
    }

    @GET
    @Path("{resource}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response findByQueryList(@QueryParam(CoreRsConstants.QUERYPARMFILTER) String filter, @PathParam(CoreRsConstants.QUERYPARMRESOURCE) String resource,
            @QueryParam(CoreRsConstants.QUERYPARMEXPAND) List<String> expand, @QueryParam(CoreRsConstants.QUERYPARMPROPERTY) String property, @QueryParam(CoreRsConstants.QUERYPARMSESSION) String sessionId)
            throws MtsException, NotFoundException, AuthenticationException, AuthorizationException, ValidationException, ConstraintViolationException {
        return findByQueryListMain(filter, resource, expand, property, sessionId);
    }

    @GET
    @Path("{resource}/object")
    @Produces({MediaType.APPLICATION_JSON})
    public Response findObjectByQueryList(@QueryParam(CoreRsConstants.QUERYPARMFILTER) String filter, @PathParam(CoreRsConstants.QUERYPARMRESOURCE) String resource,
            @QueryParam(CoreRsConstants.QUERYPARMPROPERTY) String property, @QueryParam(CoreRsConstants.QUERYPARMSESSION) String sessionId)
            throws MtsException, NotFoundException, AuthenticationException, AuthorizationException, ValidationException, ConstraintViolationException {
        return findByObjectQueryListMain(filter, resource, property, sessionId);
    }

    @POST
    @Path("{resource}/import")
    @Produces({MediaType.APPLICATION_JSON})
//    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response importData(
//            @FormDataParam("payload") InputStream fileInputStream,
            @PathParam(CoreRsConstants.QUERYPARMRESOURCE) String resource,
            @QueryParam(CoreRsConstants.QUERYPARMPROPERTY) String property,
            @QueryParam(CoreRsConstants.QUERYPARMSESSION) String sessionId)
            throws MtsException, NotFoundException, AuthenticationException, AuthorizationException, ValidationException, ConstraintViolationException {
        return importDataMain(resource, property, sessionId);
    }

    @GET
    @Path("{resource}/export")
    @Produces({MediaType.APPLICATION_JSON})
    public Response exportData(@QueryParam(CoreRsConstants.QUERYPARMFILTER) String filter, @PathParam(CoreRsConstants.QUERYPARMRESOURCE) String resource,
            @QueryParam(CoreRsConstants.QUERYPARMPROPERTY) String property, @QueryParam(CoreRsConstants.QUERYPARMSESSION) String sessionId)
            throws MtsException, NotFoundException, AuthenticationException, AuthorizationException, ValidationException, ConstraintViolationException {
        return exportDataMain(filter, resource, property, sessionId);
    }

    @GET
    @Path("{resource}/report")
    @Produces({MediaType.APPLICATION_OCTET_STREAM})
    public Response getReport(@QueryParam(CoreRsConstants.QUERYPARMFILTER) String filter, @PathParam(CoreRsConstants.QUERYPARMRESOURCE) String resource,
            @QueryParam(CoreRsConstants.QUERYPARMPROPERTY) String property, @QueryParam(CoreRsConstants.QUERYPARAMREPORTPARAMS) String reportParameters, 
            @QueryParam(CoreRsConstants.QUERYPARMSESSION) String sessionId)
            throws MtsException, NotFoundException, AuthenticationException, AuthorizationException, ValidationException, ConstraintViolationException {
        return getReportMain(filter, resource, property, reportParameters, sessionId);
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("ping")
    public String ping(@QueryParam("message") String message) throws ValidationException {
        return pingMain(message);
    }

}
