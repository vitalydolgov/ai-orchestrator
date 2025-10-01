package com.github.vitalydolgov.llmpipeline.nodes

import cats.effect.IO
import com.anthropic.client.AnthropicClient
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import scala.util.*

trait PdfProcess { self: LlmNode =>
  def analyzeFiles(dirPath: String, query: String)(using
      client: AnthropicClient
  ): IO[Either[NodeError, String]] = {
    for {
      files <- IO(listPdfFiles(dirPath))
      result <- files match {
        case Array() =>
          IO.pure(Left(NoDataError(s"No PDF files found in $dirPath")))
        case _ =>
          processFiles(files, query)
      }
    } yield result
  }

  private def processFiles(files: Array[File], query: String)(using
      client: AnthropicClient
  ): IO[Either[NodeError, String]] = {
    IO.blocking {
      files.flatMap(f =>
        extractText(f.getAbsolutePath).toOption
          .map(text => s"=== ${f.getName} ===\n$text")
      )
    }.flatMap { contents =>
      if (contents.isEmpty) {
        IO.pure(Left(NoDataError("Failed to extract text from any PDF files")))
      } else {
        val prompt = buildPrompt(query, contents)
        ask(prompt).map(Right(_))
      }
    }
  }

  private def buildPrompt(query: String, contents: Array[String]): String =
    s"""
      | Analyze the following PDF content and answer this query: $query
      |
      | Focus on extracting relevant facts, data points, and key insights. Provide direct answers without preamble or meta-commentary.
      |
      | PDF content:
      | ${contents.mkString("\n\n")}
      """.stripMargin

  private def listPdfFiles(dirPath: String): Array[File] = {
    val folder = File(dirPath)
    if (folder.exists() && folder.isDirectory) {
      folder.listFiles().filter(_.getName.toLowerCase.endsWith(".pdf"))
    } else {
      Array.empty[File]
    }
  }

  private def extractText(filePath: String): Try[String] = {
    Using(Loader.loadPDF(File(filePath))) { document =>
      val stripper = PDFTextStripper()
      stripper.getText(document)
    }
  }
}
