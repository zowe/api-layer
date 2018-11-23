/* eslint-disable no-undef */
import * as React from "react";
import { shallow } from "enzyme";
import SwaggerUI from "./Swagger";

describe(">>> Swagger component tests", () => {
    it("should not render swagger if apiDoc is null", () => {
        const service = {
            "serviceId": "testservice",
            "title": "Spring Boot Enabler Service",
            "description": "Dummy Service for enabling others",
            "status": "UP",
            "secured": false,
            "homePageUrl": "http://localhost:10013/enabler/",
            "apiDoc": null
        };
        const wrapper = shallow(
            <div>
                <SwaggerUI service={service}/>
            </div>
        );
        const swaggerDiv = wrapper.find("#swaggerContainer");

        expect(swaggerDiv.length).toEqual(0);
    });
});
