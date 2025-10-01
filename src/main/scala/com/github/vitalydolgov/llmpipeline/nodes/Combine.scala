package com.github.vitalydolgov.llmpipeline.nodes

import cats.effect.IO
import com.anthropic.client.AnthropicClient

trait Combine { self: LlmNode =>
  def combine(outputs: List[String], query: String)(using client: AnthropicClient): IO[String] = {
    val combinedContent = outputs.zipWithIndex
      .map { case (output, idx) =>
        s"=== Output ${idx + 1} ===\n$output"
      }
      .mkString("\n\n")

    val prompt = s"""
      | Synthesize information from the following sources to answer this query: $query
      |
      | Combine complementary information, eliminate redundancy, and provide a cohesive answer without preamble or meta-commentary.
      |
      | Source outputs:
      | $combinedContent
      """.stripMargin

    ask(prompt)
  }
}
