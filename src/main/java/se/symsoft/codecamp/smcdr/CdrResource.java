/*
 * Copyright Symsoft AB 1996-2015. All Rights Reserved.
 */
package se.symsoft.codecamp.smcdr;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
import se.symsoft.codecamp.myservice.logutil.Logged;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/myresources")
public class CdrResource {

    // Application (i.e the MyService class) is injected automatically here by JAX-RS
    @Context
    Application app;

    private DynamoDBMapper getDynamoDB() {
        return ((SmCdrWriter) app).getDynamoDB();
    };


    /**
     * Get all items
     * @param asyncResponse asynchronous response object
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Logged
    public void list(@Suspended final AsyncResponse asyncResponse) {
        PaginatedScanList<SmCdrData> pageList = getDynamoDB().scan(SmCdrData.class, new DynamoDBScanExpression());
        asyncResponse.resume(pageList);
    }

    /**
     * Create new item
     * @param asyncResponse asynchronous response object
     * @param data data container
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Logged
    public void create(@Suspended final AsyncResponse asyncResponse,
                       final SmCdrData data) {
        try {
            getDynamoDB().save(data);
            asyncResponse.resume(data);
        } catch (Exception e) {
            e.printStackTrace();
            asyncResponse.resume(e);
        }
    }


    /**
     * Get item with given key
     * @param asyncResponse asynchronous response object
     * @param foo the key
     */
    @GET
    @Path("{foo}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Logged
    public void create(@Suspended final AsyncResponse asyncResponse,
                       @PathParam("foo") final String foo)  {
        try {
            asyncResponse.resume(getDynamoDB().load(SmCdrData.class, foo));
        } catch (Exception e) {
            e.printStackTrace();
            asyncResponse.resume(e);
        }
    }

}
