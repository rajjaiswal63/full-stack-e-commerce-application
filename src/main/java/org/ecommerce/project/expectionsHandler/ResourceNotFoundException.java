package org.ecommerce.project.expectionsHandler;

public class ResourceNotFoundException extends RuntimeException {
    String resourceName;
    String field;
    String fieldName;
    Long fieldid;

    public ResourceNotFoundException(String resourceName, String field, String fieldName) {
        super(String.format("%s not found with %s : %s", resourceName, field, fieldName));
        this.resourceName = resourceName;
        this.field = field;
        this.fieldName = fieldName;
    }

    public ResourceNotFoundException(String resourceName, String field, Long fieldid) {
        super(String.format("%s not found with %s : %d", resourceName, field, fieldid));
        this.resourceName = resourceName;
        this.field = field;
        this.fieldid = fieldid;
    }
}
