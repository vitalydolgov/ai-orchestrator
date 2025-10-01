package com.github.vitalydolgov.llmpipeline.nodes

import cats.effect.IO
import com.anthropic.client.AnthropicClient
import com.anthropic.models.messages.*
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*

import com.github.vitalydolgov.llmpipeline.domain.llms.*

trait Node

trait OpNode[A] extends Node {
  def execute(input: A): IO[String]
}

trait LlmNode extends Node {
  def instructions: String
  def maxTokens: Long
  def temperature: Temperature
  def model: LLM

  def ask(prompt: String)(using client: AnthropicClient): IO[String] =
    IO.blocking {
      val message = MessageCreateParams
        .builder()
        .model(model.name)
        .maxTokens(maxTokens)
        .system(instructions)
        .temperature(temperature.value)
        .addUserMessage(prompt)
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
