package com.github.vitalydolgov.llmpipeline.nodes

import cats.effect.IO
import com.anthropic.client.AnthropicClient

trait Compress { self: LlmNode =>
  def compress(
      content: String,
      constraint: String
  )(using client: AnthropicClient): IO[String] = {

    val prompt =
      s"""Compress the following content to fit the constraint while preserving the most important information.
                    |
                    |Constraint: $constraint
                    |
                    |Content to compress:
                    |$content
                    |
                    |Provide only the compressed content without any explanation or preamble.""".stripMargin

    ask(prompt)
  }
}
