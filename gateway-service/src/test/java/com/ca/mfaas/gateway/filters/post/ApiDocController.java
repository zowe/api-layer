/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.filters.post;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiDocController {

    public static String apiDocResult = "{\n" +
        "  \"swagger\": \"2.0\",\n" +
        "  \"info\": {\n" +
        "    \"version\": \"1.0.0\",\n" +
        "    \"title\": \"Swagger Petstore\",\n" +
        "    \"description\": \"A sample API that uses a petstore as an example to demonstrate features in the swagger-2.0 specification\",\n" +
        "    \"termsOfService\": \"http://swagger.io/terms/\",\n" +
        "    \"contact\": {\n" +
        "      \"name\": \"Swagger API Team\"\n" +
        "    },\n" +
        "    \"license\": {\n" +
        "      \"name\": \"MIT\"\n" +
        "    }\n" +
        "  },\n" +
        "  \"host\": \"petstore.swagger.io\",\n" +
        "  \"basePath\": \"/discovered-service\",\n" +
        "  \"schemes\": [\n" +
        "    \"http\"\n" +
        "  ],\n" +
        "  \"consumes\": [\n" +
        "    \"application/json\"\n" +
        "  ],\n" +
        "  \"produces\": [\n" +
        "    \"application/json\"\n" +
        "  ],\n" +
        "  \"paths\": {\n" +
        "    \"/v1/pets\": {\n" +
        "      \"get\": {\n" +
        "        \"description\": \"Returns all pets from the system that the user has access to\",\n" +
        "        \"operationId\": \"findPets\",\n" +
        "        \"produces\": [\n" +
        "          \"application/json\",\n" +
        "          \"application/xml\",\n" +
        "          \"text/xml\",\n" +
        "          \"text/html\"\n" +
        "        ],\n" +
        "        \"parameters\": [\n" +
        "          {\n" +
        "            \"name\": \"tags\",\n" +
        "            \"in\": \"query\",\n" +
        "            \"description\": \"tags to filter by\",\n" +
        "            \"required\": false,\n" +
        "            \"type\": \"array\",\n" +
        "            \"items\": {\n" +
        "              \"type\": \"string\"\n" +
        "            },\n" +
        "            \"collectionFormat\": \"csv\"\n" +
        "          },\n" +
        "          {\n" +
        "            \"name\": \"limit\",\n" +
        "            \"in\": \"query\",\n" +
        "            \"description\": \"maximum number of results to return\",\n" +
        "            \"required\": false,\n" +
        "            \"type\": \"integer\",\n" +
        "            \"format\": \"int32\"\n" +
        "          }\n" +
        "        ],\n" +
        "        \"responses\": {\n" +
        "          \"200\": {\n" +
        "            \"description\": \"pet response\",\n" +
        "            \"schema\": {\n" +
        "              \"type\": \"array\",\n" +
        "              \"items\": {\n" +
        "                \"$ref\": \"#/definitions/Pet\"\n" +
        "              }\n" +
        "            }\n" +
        "          },\n" +
        "          \"default\": {\n" +
        "            \"description\": \"unexpected error\",\n" +
        "            \"schema\": {\n" +
        "              \"$ref\": \"#/definitions/ErrorModel\"\n" +
        "            }\n" +
        "          }\n" +
        "        }\n" +
        "      },\n" +
        "      \"post\": {\n" +
        "        \"description\": \"Creates a new pet in the store.  Duplicates are allowed\",\n" +
        "        \"operationId\": \"addPet\",\n" +
        "        \"produces\": [\n" +
        "          \"application/json\"\n" +
        "        ],\n" +
        "        \"parameters\": [\n" +
        "          {\n" +
        "            \"name\": \"pet\",\n" +
        "            \"in\": \"body\",\n" +
        "            \"description\": \"Pet to add to the store\",\n" +
        "            \"required\": true,\n" +
        "            \"schema\": {\n" +
        "              \"$ref\": \"#/definitions/NewPet\"\n" +
        "            }\n" +
        "          }\n" +
        "        ],\n" +
        "        \"responses\": {\n" +
        "          \"200\": {\n" +
        "            \"description\": \"pet response\",\n" +
        "            \"schema\": {\n" +
        "              \"$ref\": \"#/definitions/Pet\"\n" +
        "            }\n" +
        "          },\n" +
        "          \"default\": {\n" +
        "            \"description\": \"unexpected error\",\n" +
        "            \"schema\": {\n" +
        "              \"$ref\": \"#/definitions/ErrorModel\"\n" +
        "            }\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "    },\n" +
        "    \"/v1/pets/{id}\": {\n" +
        "      \"get\": {\n" +
        "        \"description\": \"Returns a user based on a single ID, if the user does not have access to the pet\",\n" +
        "        \"operationId\": \"findPetById\",\n" +
        "        \"produces\": [\n" +
        "          \"application/json\",\n" +
        "          \"application/xml\",\n" +
        "          \"text/xml\",\n" +
        "          \"text/html\"\n" +
        "        ],\n" +
        "        \"parameters\": [\n" +
        "          {\n" +
        "            \"name\": \"id\",\n" +
        "            \"in\": \"path\",\n" +
        "            \"description\": \"ID of pet to fetch\",\n" +
        "            \"required\": true,\n" +
        "            \"type\": \"integer\",\n" +
        "            \"format\": \"int64\"\n" +
        "          }\n" +
        "        ],\n" +
        "        \"responses\": {\n" +
        "          \"200\": {\n" +
        "            \"description\": \"pet response\",\n" +
        "            \"schema\": {\n" +
        "              \"$ref\": \"#/definitions/Pet\"\n" +
        "            }\n" +
        "          },\n" +
        "          \"default\": {\n" +
        "            \"description\": \"unexpected error\",\n" +
        "            \"schema\": {\n" +
        "              \"$ref\": \"#/definitions/ErrorModel\"\n" +
        "            }\n" +
        "          }\n" +
        "        }\n" +
        "      },\n" +
        "      \"delete\": {\n" +
        "        \"description\": \"deletes a single pet based on the ID supplied\",\n" +
        "        \"operationId\": \"deletePet\",\n" +
        "        \"parameters\": [\n" +
        "          {\n" +
        "            \"name\": \"id\",\n" +
        "            \"in\": \"path\",\n" +
        "            \"description\": \"ID of pet to delete\",\n" +
        "            \"required\": true,\n" +
        "            \"type\": \"integer\",\n" +
        "            \"format\": \"int64\"\n" +
        "          }\n" +
        "        ],\n" +
        "        \"responses\": {\n" +
        "          \"204\": {\n" +
        "            \"description\": \"pet deleted\"\n" +
        "          },\n" +
        "          \"default\": {\n" +
        "            \"description\": \"unexpected error\",\n" +
        "            \"schema\": {\n" +
        "              \"$ref\": \"#/definitions/ErrorModel\"\n" +
        "            }\n" +
        "          }\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  },\n" +
        "  \"definitions\": {\n" +
        "    \"Pet\": {\n" +
        "      \"type\": \"object\",\n" +
        "      \"allOf\": [\n" +
        "        {\n" +
        "          \"$ref\": \"#/definitions/NewPet\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"required\": [\n" +
        "            \"id\"\n" +
        "          ],\n" +
        "          \"properties\": {\n" +
        "            \"id\": {\n" +
        "              \"type\": \"integer\",\n" +
        "              \"format\": \"int64\"\n" +
        "            }\n" +
        "          }\n" +
        "        }\n" +
        "      ]\n" +
        "    },\n" +
        "    \"NewPet\": {\n" +
        "      \"type\": \"object\",\n" +
        "      \"required\": [\n" +
        "        \"name\"\n" +
        "      ],\n" +
        "      \"properties\": {\n" +
        "        \"name\": {\n" +
        "          \"type\": \"string\"\n" +
        "        },\n" +
        "        \"tag\": {\n" +
        "          \"type\": \"string\"\n" +
        "        }\n" +
        "      }\n" +
        "    },\n" +
        "    \"ErrorModel\": {\n" +
        "      \"type\": \"object\",\n" +
        "      \"required\": [\n" +
        "        \"code\",\n" +
        "        \"message\"\n" +
        "      ],\n" +
        "      \"properties\": {\n" +
        "        \"code\": {\n" +
        "          \"type\": \"integer\",\n" +
        "          \"format\": \"int32\"\n" +
        "        },\n" +
        "        \"message\": {\n" +
        "          \"type\": \"string\"\n" +
        "        }\n" +
        "      }\n" +
        "    }\n" +
        "  }\n" +
        "}";

    @GetMapping(value = "/api-doc", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getApiDoc(@RequestParam(value = "group", required = false) String apiDocGroup) throws JsonProcessingException {
        return apiDocResult;
    }
}
