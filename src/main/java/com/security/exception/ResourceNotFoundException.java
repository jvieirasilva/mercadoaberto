package com.security.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Lançada quando um recurso solicitado não é encontrado na base de dados.
 * Mapeada para HTTP 404 Not Found automaticamente pelo Spring.
 *
 * Utilizada em:
 *  - CartService     (cart, cartItem, product, user)
 *  - ProductService  (product)
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
