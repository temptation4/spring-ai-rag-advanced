package com.example.springaidemo.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class EmployeeTool {

    @Tool(description = "Get employee information by employee name")
    public Employee getEmployee(String name) {

        if ("Neelu".equalsIgnoreCase(name)) {
            return new Employee(
                    "Neelu",
                    "Java Backend Developer",
                    8
            );
        }

        return new Employee(
                name,
                "Unknown",
                0
        );
    }

    public record Employee(
            String name,
            String role,
            int experience
    ) {
    }
}