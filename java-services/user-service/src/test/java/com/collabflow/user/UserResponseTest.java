package com.collabflow.user;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.collabflow.user.dto.response.UserResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Smoke placeholder — keeps the test source set green. */
class UserResponseTest {

    @Test
    void recordHoldsFields() {
        UUID id = UUID.randomUUID();
        UserResponse r = new UserResponse(id, "a@b.c", "Ada", null, true, true, false);
        assertEquals(id, r.id());
        assertEquals("Ada", r.name());
    }
}
