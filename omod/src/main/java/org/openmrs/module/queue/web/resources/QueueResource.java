/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.web.resources;

import javax.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import lombok.Setter;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.search.QueueSearchCriteria;
import org.openmrs.module.queue.model.Queue;
import org.openmrs.module.queue.web.resources.parser.QueueSearchCriteriaParser;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.annotation.PropertyGetter;
import org.openmrs.module.webservices.rest.web.annotation.Resource;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
import org.openmrs.module.webservices.rest.web.representation.DefaultRepresentation;
import org.openmrs.module.webservices.rest.web.representation.FullRepresentation;
import org.openmrs.module.webservices.rest.web.representation.RefRepresentation;
import org.openmrs.module.webservices.rest.web.representation.Representation;
import org.openmrs.module.webservices.rest.web.resource.api.PageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingCrudResource;
import org.openmrs.module.webservices.rest.web.resource.impl.DelegatingResourceDescription;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.response.ObjectNotFoundException;
import org.openmrs.module.webservices.rest.web.response.ResourceDoesNotSupportOperationException;
import org.openmrs.module.webservices.rest.web.response.ResponseException;

@SuppressWarnings("unused")
@Resource(name = RestConstants.VERSION_1 + "/queue", supportedClass = Queue.class, supportedOpenmrsVersions = {
        "2.3 - 9.*" })
@Setter
public class QueueResource extends DelegatingCrudResource<Queue> {
	
	private QueueServicesWrapper services;
	
	private QueueSearchCriteriaParser searchCriteriaParser;
	
	public QueueResource() {
	}
	
	public QueueResource(QueueServicesWrapper services, QueueSearchCriteriaParser searchCriteriaParser) {
		this.services = services;
		this.searchCriteriaParser = searchCriteriaParser;
	}
	
	@Override
	public NeedsPaging<Queue> doGetAll(RequestContext requestContext) throws ResponseException {
		return new NeedsPaging<>(new ArrayList<>(getServices().getQueueService().getAllQueues()), requestContext);
	}
	
	@Override
	public Queue getByUniqueId(@NotNull String uuid) {
		Optional<Queue> optionalQueue = getServices().getQueueService().getQueueByUuid(uuid);
		if (!optionalQueue.isPresent()) {
			throw new ObjectNotFoundException("Could not find queue with UUID " + uuid);
		}
		return optionalQueue.get();
	}
	
	@Override
	protected void delete(Queue queue, String retireReason, RequestContext requestContext) throws ResponseException {
		Optional<Queue> optionalQueue = getServices().getQueueService().getQueueByUuid(queue.getUuid());
		if (!optionalQueue.isPresent()) {
			throw new ObjectNotFoundException("Could not find queue with uuid " + queue.getUuid());
		}
		getServices().getQueueService().retireQueue(queue, retireReason);
	}
	
	@Override
	public Queue newDelegate() {
		return new Queue();
	}
	
	@Override
	public Queue save(Queue queue) {
		return getServices().getQueueService().saveQueue(queue);
	}
	
	@Override
	public void purge(Queue queue, RequestContext requestContext) throws ResponseException {
		getServices().getQueueService().purgeQueue(queue);
	}
	
	@Override
	public DelegatingResourceDescription getRepresentationDescription(Representation representation) {
		DelegatingResourceDescription resourceDescription = new DelegatingResourceDescription();
		if (representation instanceof RefRepresentation) {
			this.addSharedResourceDescriptionProperties(resourceDescription);
			resourceDescription.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
		} else if (representation instanceof DefaultRepresentation) {
			this.addSharedResourceDescriptionProperties(resourceDescription);
			resourceDescription.addProperty("location", Representation.REF);
			resourceDescription.addProperty("service", Representation.REF);
			resourceDescription.addProperty("priorityConceptSet", Representation.REF);
			resourceDescription.addProperty("statusConceptSet", Representation.REF);
			resourceDescription.addProperty("allowedPriorities", Representation.REF);
			resourceDescription.addProperty("allowedStatuses", Representation.REF);
			resourceDescription.addLink("full", ".?v=" + RestConstants.REPRESENTATION_FULL);
		} else if (representation instanceof FullRepresentation) {
			this.addSharedResourceDescriptionProperties(resourceDescription);
			resourceDescription.addProperty("location", Representation.FULL);
			resourceDescription.addProperty("service", Representation.FULL);
			resourceDescription.addProperty("priorityConceptSet", Representation.REF);
			resourceDescription.addProperty("statusConceptSet", Representation.REF);
			resourceDescription.addProperty("allowedPriorities", Representation.REF);
			resourceDescription.addProperty("allowedStatuses", Representation.REF);
			resourceDescription.addProperty("auditInfo");
		} else if (representation instanceof CustomRepresentation) {
			//For custom representation, must be null
			// - let the user decide which properties should be included in the response
			resourceDescription = null;
		}
		return resourceDescription;
	}
	
	private void addSharedResourceDescriptionProperties(DelegatingResourceDescription resourceDescription) {
		resourceDescription.addSelfLink();
		resourceDescription.addProperty("uuid");
		resourceDescription.addProperty("display");
		resourceDescription.addProperty("name");
		resourceDescription.addProperty("description");
	}
	
	@Override
	public DelegatingResourceDescription getCreatableProperties() throws ResourceDoesNotSupportOperationException {
		DelegatingResourceDescription resourceDescription = new DelegatingResourceDescription();
		resourceDescription.addProperty("name");
		resourceDescription.addProperty("description");
		resourceDescription.addProperty("location");
		resourceDescription.addProperty("service");
		resourceDescription.addProperty("priorityConceptSet");
		resourceDescription.addProperty("statusConceptSet");
		return resourceDescription;
	}
	
	@Override
	public DelegatingResourceDescription getUpdatableProperties() throws ResourceDoesNotSupportOperationException {
		return this.getCreatableProperties();
	}
	
	@Override
	public Model getGETModel(Representation rep) {
		ModelImpl model = (ModelImpl) super.getGETModel(rep);
		if (rep instanceof RefRepresentation) {
			model.property("uuid", new StringProperty().example("uuid")).property("display", new StringProperty())
			        .property("name", new StringProperty()).property("description", new StringProperty());
		}
		if (rep instanceof DefaultRepresentation) {
			model.property("uuid", new StringProperty().example("uuid")).property("display", new StringProperty())
			        .property("name", new StringProperty()).property("description", new StringProperty())
			        .property("location", new RefProperty("#/definitions/LocationGet"))
			        .property("service", new RefProperty("#/definitions/ConceptGet"))
			        .property("priorityConceptSet", new RefProperty("#/definitions/ConceptGet"))
			        .property("statusConceptSet", new RefProperty("#/definitions/ConceptGet"))
			        .property("allowedPriorities", new RefProperty("#/definitions/ConceptGet"))
			        .property("allowedStatuses", new RefProperty("#/definitions/ConceptGet"));
		}
		if (rep instanceof FullRepresentation) {
			model.property("uuid", new StringProperty().example("uuid")).property("display", new StringProperty())
			        .property("name", new StringProperty()).property("description", new StringProperty())
			        .property("location", new RefProperty("#/definitions/LocationGet"))
			        .property("service", new RefProperty("#/definitions/ConceptGet"))
			        .property("priorityConceptSet", new RefProperty("#/definitions/ConceptGet"))
			        .property("statusConceptSet", new RefProperty("#/definitions/ConceptGet"))
			        .property("allowedPriorities", new RefProperty("#/definitions/ConceptGet"))
			        .property("allowedStatuses", new RefProperty("#/definitions/ConceptGet"))
			        .property("auditInfo", new StringProperty());
		}
		return model;
	}
	
	@Override
	public Model getCREATEModel(Representation rep) {
		return new ModelImpl().property("name", new StringProperty()).property("description", new StringProperty())
		        .property("location", new RefProperty("#/definitions/LocationCreate"))
		        .property("service", new RefProperty("#/definitions/ConceptCreate"))
		        .property("priorityConceptSet", new RefProperty("#/definitions/ConceptCreate"))
		        .property("statusConceptSet", new RefProperty("#/definitions/ConceptCreate"));
	}
	
	@Override
	public Model getUPDATEModel(Representation rep) {
		return getCREATEModel(rep);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	protected PageableResult doSearch(RequestContext requestContext) {
		Map<String, String[]> parameters = requestContext.getRequest().getParameterMap();
		QueueSearchCriteria criteria = getSearchCriteriaParser().constructFromRequest(parameters);
		List<Queue> queueEntries = getServices().getQueueService().getQueues(criteria);
		return new NeedsPaging<>(queueEntries, requestContext);
	}
	
	@PropertyGetter("display")
	public String getDisplay(Queue queue) {
		return queue.getName();
	}
	
	@PropertyGetter("allowedPriorities")
	public Object getAllowedPriorities(Queue delegate) {
		return getServices().getAllowedPriorities(delegate);
	}
	
	@PropertyGetter("allowedStatuses")
	public Object getAllowedStatuses(Queue delegate) {
		return getServices().getAllowedStatuses(delegate);
	}
	
	@Override
	public String getResourceVersion() {
		return "2.3";
	}
	
	public QueueServicesWrapper getServices() {
		if (services == null) {
			services = Context.getRegisteredComponents(QueueServicesWrapper.class).get(0);
		}
		return services;
	}
	
	public QueueSearchCriteriaParser getSearchCriteriaParser() {
		if (searchCriteriaParser == null) {
			searchCriteriaParser = Context.getRegisteredComponents(QueueSearchCriteriaParser.class).get(0);
		}
		return searchCriteriaParser;
	}
}
