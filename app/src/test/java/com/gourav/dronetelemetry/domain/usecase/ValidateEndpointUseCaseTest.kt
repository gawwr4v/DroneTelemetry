package com.gourav.dronetelemetry.domain.usecase

import com.gourav.dronetelemetry.domain.model.EndpointProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateEndpointUseCaseTest {
    private val validateEndpoint = ValidateEndpointUseCase()

    @Test
    fun validIpv4AndPortCreatesEndpoint() {
        val result = validateEndpoint(EndpointProtocol.UDP, "0.0.0.0", "14550")

        assertTrue(result.isValid)
        assertEquals("0.0.0.0", result.endpoint?.host)
        assertEquals(14550, result.endpoint?.port)
    }

    @Test
    fun invalidIpv4ShowsHostError() {
        val result = validateEndpoint(EndpointProtocol.TCP, "999.1.1.1", "14550")

        assertFalse(result.isValid)
        assertEquals("Enter a valid IPv4 address or host", result.hostError)
    }

    @Test
    fun invalidPortShowsPortError() {
        val result = validateEndpoint(EndpointProtocol.UDP, "192.168.1.12", "70000")

        assertFalse(result.isValid)
        assertEquals("Port must be 1 to 65535", result.portError)
    }
}
