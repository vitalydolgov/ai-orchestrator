package com.github.vitalydolgov.llmpipeline.nodes

import cats.effect.IO
import com.anthropic.client.AnthropicClient
import com.anthropic.models.messages.*
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

trait WebSearch { self: LlmNode =>
  def search(prompt: String)(using client: AnthropicClient): IO[String] =
    IO.blocking {
      val message = MessageCreateParams
        .builder()
        .model(model.name)
        .maxTokens(maxTokens)
        .system(instructions)
        .temperature(temperature.value)
        .addUserMessage(prompt)
        .addTool(
          WebSearchTool20250305
            .builder()
            .build()
        )
        .build()

      val response = client.messages().create(message)

      response
        .content()
        .asScala
        .flatMap {
          _.text().toScala.map(_.text())
        }
        .mkString("\n")
    }
}
