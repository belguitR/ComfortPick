package com.comfortpick.api.admin

import com.comfortpick.application.service.DatabaseImportService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/admin")
class AdminImportController(
    private val databaseImportService: DatabaseImportService,
    @Value("\${comfortpick.admin.import-enabled:false}") private val importEnabled: Boolean,
    @Value("\${comfortpick.admin.import-token:}") private val importToken: String,
) {

    @GetMapping("/import-status")
    fun importStatus(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(
            mapOf(
                "enabled" to importEnabled,
                "tokenConfigured" to importToken.isNotBlank(),
            ),
        )
    }

    @PostMapping("/import-db")
    fun importDatabase(
        @RequestHeader("X-Import-Token", required = false) providedToken: String?,
        @RequestBody dump: String,
    ): ResponseEntity<Map<String, String>> {
        if (!importEnabled) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND)
        }

        if (importToken.isBlank() || providedToken != importToken) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }

        databaseImportService.importSqlDump(dump)
        return ResponseEntity.ok(mapOf("status" to "IMPORTED"))
    }
}
