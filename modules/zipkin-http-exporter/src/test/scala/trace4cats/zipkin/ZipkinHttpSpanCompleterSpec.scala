package trace4cats.zipkin

import java.time.Instant

import cats.effect.IO
import fs2.Chunk
import org.http4s.blaze.client.BlazeClientBuilder
import trace4cats.model.{Batch, CompletedSpan, TraceProcess}
import trace4cats.test.jaeger.BaseJaegerSpec
import trace4cats.{CompleterConfig, SemanticTags}

import scala.concurrent.duration._

class ZipkinHttpSpanCompleterSpec extends BaseJaegerSpec {
  it should "Send a span to Zipkin" in forAll { (span: CompletedSpan.Builder, serviceName: String) =>
    val process = TraceProcess(serviceName)

    val updatedSpan = span.copy(
      start = Instant.now(),
      end = Instant.now(),
      attributes = span.attributes.filterNot { case (key, _) =>
        excludedTagKeys.contains(key)
      }
    )
    val batch = Batch(Chunk(updatedSpan.build(process)))
    val completer = BlazeClientBuilder[IO].resource.flatMap { client =>
      ZipkinHttpSpanCompleter(client, process, "localhost", 9411, CompleterConfig(batchTimeout = 50.millis))
    }

    testCompleter(
      completer,
      updatedSpan,
      process,
      batchToJaegerResponse(
        batch,
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
