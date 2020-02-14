/*
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.protocoladapter;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.eclipse.ditto.json.JsonArray;
import org.eclipse.ditto.json.JsonCollectors;
import org.eclipse.ditto.json.JsonFactory;
import org.eclipse.ditto.json.JsonFieldSelector;
import org.eclipse.ditto.json.JsonParseException;
import org.eclipse.ditto.json.JsonPointer;
import org.eclipse.ditto.json.JsonValue;
import org.eclipse.ditto.model.things.ThingId;
import org.eclipse.ditto.signals.commands.things.query.RetrieveAcl;
import org.eclipse.ditto.signals.commands.things.query.RetrieveAclEntry;
import org.eclipse.ditto.signals.commands.things.query.RetrieveAttribute;
import org.eclipse.ditto.signals.commands.things.query.RetrieveAttributes;
import org.eclipse.ditto.signals.commands.things.query.RetrieveFeature;
import org.eclipse.ditto.signals.commands.things.query.RetrieveFeatureDefinition;
import org.eclipse.ditto.signals.commands.things.query.RetrieveFeatureProperties;
import org.eclipse.ditto.signals.commands.things.query.RetrieveFeatureProperty;
import org.eclipse.ditto.signals.commands.things.query.RetrieveFeatures;
import org.eclipse.ditto.signals.commands.things.query.RetrieveThing;
import org.eclipse.ditto.signals.commands.things.query.RetrieveThingDefinition;
import org.eclipse.ditto.signals.commands.things.query.RetrieveThings;
import org.eclipse.ditto.signals.commands.things.query.ThingQueryCommand;

/**
 * Adapter for mapping a {@link ThingQueryCommand} to and from an {@link Adaptable}.
 */
final class ThingQueryCommandAdapter extends AbstractAdapter<ThingQueryCommand> {

    private ThingQueryCommandAdapter(
            final Map<String, JsonifiableMapper<ThingQueryCommand>> mappingStrategies,
            final HeaderTranslator headerTranslator) {
        super(mappingStrategies, headerTranslator);
    }

    /**
     * Returns a new ThingQueryCommandAdapter.
     *
     * @param headerTranslator translator between external and Ditto headers.
     * @return the adapter.
     */
    public static ThingQueryCommandAdapter of(final HeaderTranslator headerTranslator) {
        return new ThingQueryCommandAdapter(mappingStrategies(), requireNonNull(headerTranslator));
    }

    private static Map<String, JsonifiableMapper<ThingQueryCommand>> mappingStrategies() {
        final Map<String, JsonifiableMapper<ThingQueryCommand>> mappingStrategies = new HashMap<>();

        mappingStrategies.put(RetrieveThing.TYPE, adaptable -> RetrieveThing.getBuilder(getThingId(adaptable),
                adaptable.getDittoHeaders())
                .withSelectedFields(getSelectedFieldsOrNull(adaptable))
                .build());

        mappingStrategies.put(RetrieveThings.TYPE, adaptable -> RetrieveThings.getBuilder(thingsIdsFrom(adaptable))
                .dittoHeaders(adaptable.getDittoHeaders())
                .namespace(getNamespaceOrNull(adaptable))
                .selectedFields(getSelectedFieldsOrNull(adaptable)).build());

        mappingStrategies.put(RetrieveAcl.TYPE, adaptable -> RetrieveAcl.of(getThingId(adaptable),
                adaptable.getDittoHeaders()));

        mappingStrategies.put(RetrieveAclEntry.TYPE, adaptable -> RetrieveAclEntry.of(getThingId(adaptable),
                getAuthorizationSubject(adaptable), getSelectedFieldsOrNull(adaptable), adaptable.getDittoHeaders()));

        mappingStrategies.put(RetrieveAttributes.TYPE, adaptable -> RetrieveAttributes.of(getThingId(adaptable),
                getSelectedFieldsOrNull(adaptable), adaptable.getDittoHeaders()));

        mappingStrategies.put(RetrieveAttribute.TYPE, adaptable -> RetrieveAttribute.of(getThingId(adaptable),
                getAttributePointerOrThrow(adaptable), adaptable.getDittoHeaders()));

        mappingStrategies.put(RetrieveThingDefinition.TYPE, adaptable -> RetrieveThingDefinition.of(getThingId(adaptable),
                adaptable.getDittoHeaders()));

        mappingStrategies.put(RetrieveFeatures.TYPE, adaptable -> RetrieveFeatures.of(getThingId(adaptable),
                getSelectedFieldsOrNull(adaptable), adaptable.getDittoHeaders()));

        mappingStrategies.put(RetrieveFeature.TYPE, adaptable -> RetrieveFeature.of(getThingId(adaptable),
                getFeatureIdOrThrow(adaptable), getSelectedFieldsOrNull(adaptable), adaptable.getDittoHeaders()));

        mappingStrategies.put(RetrieveFeatureDefinition.TYPE, adaptable ->
                RetrieveFeatureDefinition.of(getThingId(adaptable), getFeatureIdOrThrow(adaptable),
                        adaptable.getDittoHeaders()));

        mappingStrategies.put(RetrieveFeatureProperties.TYPE, adaptable ->
                RetrieveFeatureProperties.of(getThingId(adaptable), getFeatureIdOrThrow(adaptable),
                        getSelectedFieldsOrNull(adaptable), adaptable.getDittoHeaders()));

        mappingStrategies.put(RetrieveFeatureProperty.TYPE, adaptable ->
                RetrieveFeatureProperty.of(getThingId(adaptable), getFeatureIdOrThrow(adaptable),
                        getFeaturePropertyPointerOrThrow(adaptable), adaptable.getDittoHeaders()));

        return mappingStrategies;
    }

    @Nullable
    private static JsonFieldSelector getSelectedFieldsOrNull(final Adaptable adaptable) {
        final Payload payload = adaptable.getPayload();
        return payload.getFields().orElse(null);
    }

    private static Adaptable handleSingleRetrieve(final ThingQueryCommand<?> command, final TopicPath.Channel channel) {
        final TopicPathBuilder topicPathBuilder = ProtocolFactory.newTopicPathBuilder(command.getThingEntityId());

        final CommandsTopicPathBuilder commandsTopicPathBuilder;
        commandsTopicPathBuilder = fromTopicPathBuilderWithChannel(topicPathBuilder, channel);

        final String commandName = command.getClass().getSimpleName().toLowerCase();
        if (!commandName.startsWith(TopicPath.Action.RETRIEVE.toString())) {
            throw UnknownCommandException.newBuilder(commandName).build();
        }

        final PayloadBuilder payloadBuilder = Payload.newBuilder(command.getResourcePath());
        command.getSelectedFields().ifPresent(payloadBuilder::withFields);

        return Adaptable.newBuilder(commandsTopicPathBuilder.retrieve().build())
                .withPayload(payloadBuilder.build())
                .withHeaders(ProtocolFactory.newHeadersWithDittoContentType(command.getDittoHeaders()))
                .build();
    }

    private static Adaptable handleMultipleRetrieve(final RetrieveThings command,
            final TopicPath.Channel channel) {

        final String commandName = command.getClass().getSimpleName().toLowerCase();
        if (!commandName.startsWith(TopicPath.Action.RETRIEVE.toString())) {
            throw UnknownCommandException.newBuilder(commandName).build();
        }

        final String namespace = command.getNamespace().orElse("_");
        final TopicPathBuilder topicPathBuilder = ProtocolFactory.newTopicPathBuilderFromNamespace(namespace);
        final CommandsTopicPathBuilder commandsTopicPathBuilder =
                fromTopicPathBuilderWithChannel(topicPathBuilder, channel);

        final PayloadBuilder payloadBuilder = Payload.newBuilder(command.getResourcePath());
        command.getSelectedFields().ifPresent(payloadBuilder::withFields);
        payloadBuilder.withValue(createIdsPayload(command.getThingEntityIds()));

        return Adaptable.newBuilder(commandsTopicPathBuilder.retrieve().build())
                .withPayload(payloadBuilder.build())
                .withHeaders(ProtocolFactory.newHeadersWithDittoContentType(command.getDittoHeaders()))
                .build();
    }

    private static List<ThingId> thingsIdsFrom(final Adaptable adaptable) {
        final JsonArray array = adaptable.getPayload()
                .getValue()
                .filter(JsonValue::isObject)
                .map(JsonValue::asObject)
                .orElseThrow(() -> new JsonParseException("Adaptable payload was non existing or no JsonObject"))
                .getValue(RetrieveThings.JSON_THING_IDS)
                .filter(JsonValue::isArray)
                .map(JsonValue::asArray)
                .orElseThrow(() -> new JsonParseException("Could not map 'thingIds' value to expected JsonArray"));

        return array.stream()
                .map(JsonValue::asString)
                .map(ThingId::of)
                .collect(Collectors.toList());
    }

    private static JsonValue createIdsPayload(final Collection<ThingId> ids) {
        final JsonArray thingIdsArray = ids.stream()
                .map(String::valueOf)
                .map(JsonFactory::newValue)
                .collect(JsonCollectors.valuesToArray());
        return JsonFactory.newObject().setValue(RetrieveThings.JSON_THING_IDS.getPointer(), thingIdsArray);
    }

    @Override
    protected String getType(final Adaptable adaptable) {
        final TopicPath topicPath = adaptable.getTopicPath();
        if (topicPath.isWildcardTopic()) {
            return RetrieveThings.TYPE;
        } else {
            final JsonPointer path = adaptable.getPayload().getPath();
            final String commandName = getActionOrThrow(topicPath) + upperCaseFirst(PathMatcher.match(path));
            return topicPath.getGroup() + "." + topicPath.getCriterion() + ":" + commandName;
        }
    }

    @Override
    public Adaptable constructAdaptable(final ThingQueryCommand command, final TopicPath.Channel channel) {
        if (command instanceof RetrieveThings) {
            return handleMultipleRetrieve((RetrieveThings) command, channel);
        } else {
            return handleSingleRetrieve(command, channel);
        }
    }

}
