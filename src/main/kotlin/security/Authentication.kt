package org.burgas.security

import io.ktor.server.application.*
import io.ktor.server.auth.*
import org.burgas.service.Authority
import org.burgas.service.IdentityTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

fun Application.configureAuthentication() {

    authentication {

        basic(name = "basic-auth-all") {
            validate { credentials ->
                val identity = transaction {
                    IdentityTable.select(IdentityTable.fields).where { IdentityTable.email eq credentials.name }
                        .singleOrNull()
                }
                if (
                    identity != null &&
                    BCrypt.checkpw(credentials.password, identity[IdentityTable.password]) &&
                    identity[IdentityTable.enabled]
                ) {
                    UserPasswordCredential(credentials.name, credentials.password)

                } else {
                    null
                }
            }
        }

        basic(name = "basic-auth-admin") {
            validate { credentials ->
                val identity = transaction {
                    IdentityTable.select(IdentityTable.fields).where { IdentityTable.email eq credentials.name }
                        .singleOrNull()
                }
                if (
                    identity != null &&
                    BCrypt.checkpw(credentials.password, identity[IdentityTable.password]) &&
                    identity[IdentityTable.authority] == Authority.ADMIN &&
                    identity[IdentityTable.enabled]
                ) {
                    UserPasswordCredential(credentials.name, credentials.password)

                } else {
                    null
                }
            }
        }
    }
}