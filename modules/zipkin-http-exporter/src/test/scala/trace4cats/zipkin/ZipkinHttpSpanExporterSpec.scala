package trace4cats.zipkin

import java.time.Instant

import cats.effect.{IO, Resource}
import fs2.Chunk
import org.http4s.blaze.client.BlazeClientBuilder
import trace4cats.SemanticTags
import trace4cats.model.{Batch, TraceProcess}
import trace4cats.test.jaeger.BaseJaegerSpec

class ZipkinHttpSpanExporterSpec extends BaseJaegerSpec {
  it should "Send a batch of spans to Zipkin" in forAll { (batch: Batch[Chunk], process: TraceProcess) =>
    val updatedBatch =
      Batch(
        batch.spans.map(span =>
          span.copy(
            serviceName = process.serviceName,
            attributes = (process.attributes ++ span.attributes)
              .filterNot { case (key, _) =>
                excludedTagKeys.contains(key)
              },
            start = Instant.now(),
            end = Instant.now()
          )
        )
      )
    val exporter = BlazeClientBuilder[IO].resource.flatMap { client =>
      Resource.eval(ZipkinHttpSpanExporter[IO, Chunk](client, "localhost", 9411))
    }

    testExporter(
      exporter,
      updatedBatch,
      batchToJaegerResponse(
        updatedBatch,
        process,
        kindToAttributes,
        SemanticTags.statusTags(prefix = "", requireMessage = false),
        processToAttributes,
        convertAttributes = convertAttributes,
        internalSpanFormat = "zipkin",
        followsFrom = false
      )
    )
  }
}
