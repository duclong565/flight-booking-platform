package com.deanflights.auth.dto;

import com.deanflights.auth.Role;
import com.deanflights.auth.User;

/**
 * Output shape returned to clients. The static from(...) factory does the manual
 * mapping from the entity — this is the seam that keeps our API independent of the
 * DB schema. Note the password hash is deliberately never mapped out.
 */
public record UserResponse(
        Long id,
        String username,
        Role role
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getRole());
    }
}
