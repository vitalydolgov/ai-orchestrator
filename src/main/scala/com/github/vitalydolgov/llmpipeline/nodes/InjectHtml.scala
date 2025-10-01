package com.github.vitalydolgov.llmpipeline.nodes

import cats.effect.IO
import com.anthropic.client.AnthropicClient
import scala.io.Source
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.nio.charset.StandardCharsets

trait InjectHtml { self: LlmNode =>
  def injectData(
      templatePath: String,
      data: String,
      outputPath: String
  )(using client: AnthropicClient): IO[Unit] = {
    for {
      // Read the template file
      template <- IO.blocking(Source.fromFile(templatePath).mkString)

      // Ask LLM to inject data into the template
      prompt =
        s"""Given the following HTML template and data, inject the data into the template with the appropriate information.
        |
        |HTML Template:
        |$template
        |
        |Data to inject:
        |$data
        |
        |When injecting the data:
        |- Format lists of statements, items, or multiple related points as HTML bullet points (<ul><li>...</li></ul>)
        |- Use bullet points for any enumerated items, features, or lists in the data
        |- Maintain proper HTML structure and formatting
        |
        |Please return only the HTML with injected data without any explanation or markdown formatting.""".stripMargin

      injectedHtml <- ask(prompt)

      // Write the HTML with injected data to output file
      _ <- IO.blocking {
        Files.write(
          Paths.get(outputPath),
          injectedHtml.getBytes(StandardCharsets.UTF_8),
          StandardOpenOption.CREATE,
          StandardOpenOption.TRUNCATE_EXISTING
        )
      }
    } yield ()
  }
}
