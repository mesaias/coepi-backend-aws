package org.coepi.api.server

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.parametersOf
import io.ktor.request.receive
import io.ktor.response.respondBytes
import io.ktor.routing.*
import io.ktor.util.toMap
import org.coepi.api.common.orEmpty
import org.coepi.api.common.toByteBuffer
import org.coepi.api.v4.dao.TCNReportsDao
import org.coepi.api.v4.http.HttpResponse
import org.coepi.api.v4.http.TCNHttpHandler
import org.coepi.api.v4.reports.TCNReportService
import java.time.Clock

fun Routing.tcnReports() {
    val tcnHandler = TCNHttpHandler(
        ObjectMapper(),
        TCNReportService(
            Clock.systemUTC(),
            TCNReportsDao()
        )
    )

    route("/v4/tcnreport") {
        get {
            // Existing API does not make use of multi-value query parameters.
            val parameters = parametersOf().toMap().mapValues { (_, v) -> v.first() }
            val tcnResponse = tcnHandler.getReport(parameters)
            call.respondWith(tcnResponse)
        }

        post {
            val body = call.receive<ByteArray>()
            val tcnResponse = tcnHandler.postReport(body.toByteBuffer())
            call.respondWith(tcnResponse)
        }

    }
}

private suspend fun ApplicationCall.respondWith(tcnResponse: HttpResponse) {
    respondBytes(
        ContentType.Application.Json,
        HttpStatusCode.fromValue(tcnResponse.status)
    ) {
        tcnResponse.body?.array().orEmpty()
    }
}
