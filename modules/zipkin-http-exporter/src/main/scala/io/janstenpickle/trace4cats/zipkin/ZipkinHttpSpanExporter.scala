package io.janstenpickle.trace4cats.zipkin

import cats.Foldable
import cats.effect.kernel.Async
import cats.syntax.either._
import cats.syntax.functor._
import io.janstenpickle.trace4cats.`export`.HttpSpanExporter
import io.janstenpickle.trace4cats.kernel.SpanExporter
import org.http4s.Uri
import org.http4s.client.Client

object ZipkinHttpSpanExporter {

  def apply[F[_]: Async, G[_]: Foldable](
    client: Client[F],
    host: String = "localhost",
    port: Int = 9411,
    protocol: String = "http"
  ): F[SpanExporter[F, G]] =
    Uri.fromString(s"$protocol://$host:$port/api/v2/spans").liftTo[F].map(uri => apply(client, uri))

  def apply[F[_]: Async, G[_]: Foldable](client: Client[F], uri: Uri): SpanExporter[F, G] =
    HttpSpanExporter[F, G, String](client, uri, ZipkinSpan.toJsonString[G](_))
}
