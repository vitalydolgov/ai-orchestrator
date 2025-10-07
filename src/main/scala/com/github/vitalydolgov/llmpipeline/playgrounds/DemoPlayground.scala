package com.github.vitalydolgov.llmpipeline.playgrounds

import cats.effect.*
import pureconfig.ConfigSource
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import scala.io.Source
import java.io.File

import com.github.vitalydolgov.llmpipeline.nodes.*
import com.github.vitalydolgov.llmpipeline.dsl.*
import com.github.vitalydolgov.llmpipeline.domain.llms.*
import com.github.vitalydolgov.llmpipeline.config.AppConfig

object DemoPlayground extends IOApp.Simple {
  val config = ConfigSource.default.loadOrThrow[AppConfig]

  given client: AnthropicClient = AnthropicOkHttpClient
    .builder()
    .apiKey(config.anthropicApiKey)
    .build()

  private def whatWeKnowArtifactProgram(companyName: String): IO[Unit] = {
    val webNode = new LlmNode with WebSearch {
      val instructions = prompt"""
        You are a web research assistant.
        Extract factual information about companies including their business activities, industry sector, key products/services, and recent economic developments.
        Focus on verified, current information.
        """
      val maxTokens = 4096L
      val temperature = Temperature.Precise
      val model = LLM.Haiku35
    }

    val pdfNode = new LlmNode with PdfProcess {
      val instructions = prompt"""
        You are a document analysis assistant.
        Extract relevant information from PDF documents, focusing on facts, data points, and key insights.
        Provide direct answers without preamble.
        """
      val maxTokens = 4096L
      val temperature = Temperature.Precise
      val model = LLM.Sonnet45
    }

    val combineNode = new LlmNode with Combine {
      val instructions = prompt"""
        You are a helpful assistant that synthesizes information from multiple sources.
        Provide direct, concise reports without preamble or meta-commentary about the task.
        """
      val maxTokens = 4096L
      val temperature = Temperature.Creative
      val model = LLM.Sonnet45
    }

    val compressNode = new LlmNode with Compress {
      val instructions = prompt"""
        You are a content compression assistant.
        Distill information to its essential facts while maintaining accuracy.
        Output concise statements or bullet points without titles or headings.
        """
      val maxTokens = 2048L
      val temperature = Temperature.Precise
      val model = LLM.Sonnet45
    }

    val htmlNode = new LlmNode with InjectHtml {
      val instructions = prompt"""
        You are a helpful assistant that injects data formatted in markdown into HTML templates.
        Reduce any titles by two levels (e.g., H1 becomes H3, H2 becomes H4) as the presentation already contains a title.
        """
      val maxTokens = 4096L
      val temperature = Temperature.Precise
      val model = LLM.Haiku35
    }

    val programName = "What we know"

    for {
      _ <- IO.println(s"Starting '$programName' program for '$companyName'...")

      _ <- IO.println(s"Gathering information:")
      (webResult, pdfResult) <- (
        IO.println(s"- Searching information online...") *>
          webNode.search(s"Find general and economic information about $companyName company."),
        IO.println(s"- Processing document library...") *>
          pdfNode
            .analyzeFiles("data", s"Extract any information relevant to $companyName")
            .map(_.getOrElse(""))
      ).parTupled

      _ <- IO.println("Combining information...")
      combinedData <- combineNode.combine(
        List(webResult, pdfResult),
        prompt"""
          Synthesize the information into a cohesive report.
          Use 'the company' or 'they' instead of repeating the company name.
          """
      )

      _ <- IO.println("Compressing content for slides...")
      (factsCompressed, plansCompressed) <- (
        compressNode.compress(
          combinedData,
          prompt"""
            Compress this content to fit on a single presentation slide (maximum 500 characters).
            Focus on the most important facts about the company.
            Do not include any title or heading - only the key facts as concise statements or bullet points.
            """
        ),
        compressNode.compress(
          pdfResult,
          prompt"""
            Compress this content to fit on a single presentation slide (maximum 500 characters).
            Focus on the most important facts about commission plans.
            Do not include any title or heading - only the key facts as concise statements or bullet points.
            """
        )
      ).parTupled

      _ <- IO.println(s"Generating artifacts:")
      _ <- (
        IO.println("- Generating presentation slide #1...")
          *> htmlNode.injectData(
            "templates/presentation-slide.html",
            s"Title: What we know\n\nContent:\n${factsCompressed}",
            s"${config.outputArtifacts}/what-we-know.html"
          ),
        IO.println("- Generating presentation slide #2...")
          *> htmlNode.injectData(
            "templates/presentation-slide.html",
            s"Title: Plans Summary\n\nContent:\n${plansCompressed}",
            s"${config.outputArtifacts}/plans-summary.html"
          )
      ).parTupled

      _ <- IO.println(s"'$programName' program finished.")
    } yield ()
  }

  private val estimationArtifactProgram: IO[Unit] = {
    val injectHtml = new LlmNode with InjectHtml {
      val instructions = prompt"""
        Add borders to the table and align its content nicely. 
        Reduce title size twice, if table doesn't fit vertically. 
        Do not change paddings of the table.
        """
      val maxTokens = 2048L
      val temperature = Temperature.Precise
      val model = LLM.Sonnet45
    }

    val programName = "Estimations"

    for {
      _ <- IO.println(s"Starting '$programName' program...")

      _ <- IO.println(s"Generating artifacts:")

      tablePhase1 <- ImportTable.execute(s"${config.inputData}/estimation/Estimation (Phase 1).csv")
      tablePhase2 <- ImportTable.execute(s"${config.inputData}/estimation/Estimation (Phase 2).csv")

      _ <- (
        IO.println("- Generating presentation slide #1...")
          *> injectHtml.injectData(
            "templates/presentation-slide.html",
            s"Title: Spiff Scope (Phase 1)\n\nContent:\n${tablePhase1}",
            s"${config.outputArtifacts}/scope-phase-1.html"
          ),
        IO.println("- Generating presentation slide #2...")
          *> injectHtml.injectData(
            "templates/presentation-slide.html",
            s"Title: Spiff Scope (Phase 2)\n\nContent:\n${tablePhase2}",
            s"${config.outputArtifacts}/scope-phase-2.html"
          )
      ).parTupled

      _ <- IO.println(s"'$programName' program finished.")
    } yield ()
  }

  override def run: IO[Unit] =
    whatWeKnowArtifactProgram("Summit Companies") *> estimationArtifactProgram
}
